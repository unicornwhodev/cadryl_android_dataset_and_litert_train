package com.unicornwhodev.visiondatasetstudio

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.platform.app.InstrumentationRegistry
import com.unicornwhodev.visiondatasetstudio.domain.inference.*
import com.unicornwhodev.visiondatasetstudio.data.hf.HfTreeItem
import com.unicornwhodev.visiondatasetstudio.data.json.StudioJson
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Debug
import android.os.ParcelFileDescriptor
import com.unicornwhodev.visiondatasetstudio.core.geometry.HashUtils
import org.json.JSONObject
import org.json.JSONArray

class HfModelRuntimeTest {
    /** Separate Debug DocumentsUI bridge; real files, source index, Room and acquisition. */
    @Test fun realPublicCorpus1000SurvivesRoomReopen() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue("Explicit public corpus QA required", args.getString("publicCorpusAudit") == "true")
        val case = requireNotNull(args.getString("corpusCase")); require(case.matches(Regex("[a-f0-9]{12}")))
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val target = instrumentation.targetContext
        val root = File(target.filesDir, "qa-evidence/external-faults/$case")
        check(!root.exists()); root.mkdirs()
        // The Debug-only host runner stages this public manifest with run-as.
        // Avoid reserving UiAutomation while the host drives DocumentsUI.
        val manifest = JSONObject(File(target.filesDir, "qa-corpus-input-$case.json").readText())
        check(manifest.getString("license") == "cc0-1.0")
        val images = manifest.getJSONArray("images"); check(images.length() == 1000)
        val expected = (0 until images.length()).associate { n ->
            val image = images.getJSONObject(n); image.getString("source_filename") to image.getString("sha256")
        }
        check(expected.size == 1000)
        target.startActivity(android.content.Intent().setClassName(target.packageName, target.packageName + ".qa.QaDocumentActivity")
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("faultCase", case).putExtra("publicCorpusFolder", true))
        val marker = File(root, "document.json")
        val deadline = android.os.SystemClock.elapsedRealtime() + 180_000
        while (!marker.exists() && android.os.SystemClock.elapsedRealtime() < deadline) kotlinx.coroutines.delay(200)
        check(marker.isFile) { "Select the public corpus folder in DocumentsUI" }
        val uri = android.net.Uri.parse(JSONObject(marker.readText()).getString("uri"))
        check(android.provider.DocumentsContract.getTreeDocumentId(uri) == "primary:Download/Cadryl_RC8_CC0_1000")
        check(target.contentResolver.persistedUriPermissions.any { it.uri == uri && it.isReadPermission })
        val context = object : android.content.ContextWrapper(target) {
            override fun getFilesDir() = File(root, "corpus-app/files").apply { mkdirs() }
            override fun getCacheDir() = File(root, "corpus-app/cache").apply { mkdirs() }
        }
        val name = "qa-public-corpus-$case.db"
        val db = androidx.room.Room.databaseBuilder(target, com.unicornwhodev.visiondatasetstudio.data.db.AppDatabase::class.java, name).build()
        val hf = com.unicornwhodev.visiondatasetstudio.data.hf.HfApiClient { null }
        val storage = com.unicornwhodev.visiondatasetstudio.core.storage.StorageManager(context)
        val runtime = LiteRtEngine()
        val project = com.unicornwhodev.visiondatasetstudio.data.model.ProjectEntity(id = 910000L, name = "QA CC0 1000", classesCsv = "", activeTasksCsv = "", diskBudgetMb = 4096)
        val started = android.os.SystemClock.elapsedRealtime()
        val seen = mutableSetOf<String>(); var batches = 0; var duplicates = 0
        try {
            db.projectDao().saveProject(project)
            val catalog = com.unicornwhodev.visiondatasetstudio.data.source.SourceCatalog(context, db, hf)
            assertEquals(1000L, catalog.importFolder(project, uri))
            val engine = com.unicornwhodev.visiondatasetstudio.domain.batch.BatchEngine(db, storage, hf, runtime,
                com.unicornwhodev.visiondatasetstudio.domain.export.DatasetExporters(storage, hf))
            while (db.projectDao().getProjectSync(project.id)!!.lastRowCursor < 1000) {
                batches++; check(batches <= 20)
                engine.prepareUniqueBatch(project.id, batches, 100) { _, _ -> }
                val samples = db.sampleDao().getSamplesForBatchSync(project.id, batches)
                check(samples.isNotEmpty())
                samples.forEach { sample ->
                    assertTrue(seen.add(sample.assetId))
                    if (sample.annotationStatus == "DUPLICATE") duplicates++ else {
                        assertEquals("AVAILABLE", sample.acquisitionStatus)
                        assertTrue(sample.imageWidth > 0 && sample.imageHeight > 0)
                        val file = File(requireNotNull(sample.localImagePath))
                        assertEquals(expected[sample.assetId], HashUtils.computeSha256(file))
                    }
                }
            }
            assertEquals(expected.keys, seen)
        } finally { runtime.close(); db.close() }
        val fresh = androidx.room.Room.databaseBuilder(target, com.unicornwhodev.visiondatasetstudio.data.db.AppDatabase::class.java, name).build()
        try { assertEquals(1000L, fresh.projectDao().getProjectSync(project.id)!!.lastRowCursor) } finally { fresh.close() }
        val report = JSONObject().put("success", true).put("input_images", 1000).put("consumed_source_rows", seen.size)
            .put("batches", batches).put("duplicate_pixels", duplicates).put("copied_images", seen.size - duplicates)
            .put("source", manifest.getString("dataset")).put("revision", manifest.getString("revision"))
            .put("license", "cc0-1.0").put("genuine_documentsui_grant", true).put("room_reopened_cursor", 1000)
            .put("milliseconds", android.os.SystemClock.elapsedRealtime() - started).put("accuracy_evaluated", false)
        File(root, "corpus-result.json").writeText(report.toString(2))
        instrumentation.addResults(Bundle().apply { putString("public_corpus_evidence", report.toString()) })
    }

    /** Opt-in sustained CPU inference, with public inputs staged through shell.
     * No private app API, model weights in the APK, host inference or accuracy claim. */
    @Test fun prolongedRealPhotoCpuInference() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue("Explicit prolonged qualification required", args.getString("prolongedInference") == "true")
        val seconds = requireNotNull(args.getString("prolongedSeconds")).toInt()
        require(seconds in 600..3600)
        val case = requireNotNull(args.getString("benchmarkCase"))
        require(case.matches(Regex("[a-f0-9]{12}")))
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val root = File(instrumentation.targetContext.filesDir, "qa-evidence/prolonged-inference/$case")
        check(!root.exists()); root.mkdirs()
        fun shellFile(name: String): java.io.InputStream {
            require(name.matches(Regex("[A-Za-z0-9_.-]+")))
            return ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(
                "cat /data/local/tmp/vds-qa-public-$case/$name"))
        }
        val manifest = JSONObject(shellFile("staging-manifest.json").bufferedReader().use { it.readText() })
        val files = manifest.getJSONObject("files")
        files.keys().forEach { name ->
            val file = File(root, name)
            shellFile(name).use { input -> file.outputStream().use { input.copyTo(it) } }
            val expected = files.getJSONObject(name)
            assertEquals(expected.getLong("bytes"), file.length())
            assertEquals(expected.getString("sha256"), HashUtils.computeSha256(file))
        }
        val id = manifest.getString("model_id")
        val entry = (CommunityModelCatalog.entries + CommunityModelCatalog.fireviewerEntries).single { it.id == id }
        val item = CommunityModelCatalog.Availability(entry, manifest.getString("revision"),
            root.listFiles()!!.filter { it.isFile }.map { HfTreeItem("models/$id/${it.name}", "file", it.length()) },
            true, true, "Pinned public physical QA")
        val model = root.listFiles()!!.filter { it.extension == "tflite" }.single()
        val photo = File(root, "photo.png")
        val bitmap = requireNotNull(BitmapFactory.decodeFile(photo.path))
        val config = CommunityModelCatalog.suggestedConfig(item, model).copy(threads = 2)
        val timings = ArrayList<Double>()
        val memory = JSONArray()
        try {
            LiteRtEngine().use { engine ->
                assertTrue(engine.lastError, engine.loadModel(model, config.threads))
                repeat(5) { engine.runInference(bitmap, config).orThrow() }
                val started = android.os.SystemClock.elapsedRealtime()
                var sampled = -10_000L
                do {
                    val tick = System.nanoTime()
                    val inference = engine.runInference(bitmap, config)
                    val proposals = inference.orThrow()
                    assertTrue(proposals.all { it.score.isFinite() })
                    assertNull(engine.lastError, engine.lastError)
                    timings.add((System.nanoTime() - tick) / 1_000_000.0)
                    val elapsed = android.os.SystemClock.elapsedRealtime() - started
                    if (elapsed - sampled >= 10_000) {
                        memory.put(JSONObject().put("elapsed_ms", elapsed).put("pss_kib", Debug.getPss())
                            .put("native_heap_bytes", Debug.getNativeHeapAllocatedSize()))
                        sampled = elapsed
                    }
                } while (android.os.SystemClock.elapsedRealtime() - started < seconds * 1000L)
                assertTrue("At least 100 actual inferences required", timings.size >= 100)
                if (ModelContract.adapter(config) == "embedding") {
                    assertTrue(requireNotNull(engine.lastEmbedding).isNotEmpty())
                    assertTrue(requireNotNull(engine.lastEmbedding).all(Float::isFinite))
                }
                files.keys().forEach { name ->
                    assertEquals(files.getJSONObject(name).getString("sha256"), HashUtils.computeSha256(File(root, name)))
                }
                val sorted = timings.sorted()
                fun percentile(p: Double) = sorted[kotlin.math.ceil(p * sorted.size).toInt().coerceIn(1, sorted.size) - 1]
                val report = JSONObject().put("success", true).put("model_id", id)
                    .put("revision", manifest.getString("revision")).put("inputs", files)
                    .put("runtime", "Android LiteRT CPU").put("threads", config.threads)
                    .put("abi", android.os.Build.SUPPORTED_ABIS[0]).put("android_api", android.os.Build.VERSION.SDK_INT)
                    .put("duration_ms", android.os.SystemClock.elapsedRealtime() - started)
                    .put("warmup_inferences", 5).put("measured_inferences", timings.size)
                    .put("latency_ms", JSONObject().put("p50", percentile(.5)).put("p95", percentile(.95)))
                    .put("memory_samples", memory).put("inputs_preserved", true)
                    .put("accuracy_evaluated", false).put("representative_corpus", false)
                File(root, "result.json").writeText(report.toString(2))
                instrumentation.addResults(Bundle().apply { putString("prolonged_inference_evidence", report.toString()) })
            }
        } finally { bitmap.recycle() }
    }

    private fun run(id:String) = runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val root=File(context.filesDir,"hf-runtime-fixture/$id")
        assumeTrue("Stage the pinned, public HF fixture",File(root,"artifact_manifest.json").isFile)
        val spec=CommunityModelCatalog.entries.single{it.id==id}
        val revision=(File(root,"revision.txt").takeIf{it.isFile} ?: File(context.filesDir,"hf-runtime-fixture/revision.txt")).readText().trim()
        val item=CommunityModelCatalog.Availability(spec,revision,root.walkTopDown().filter{it.isFile}.map{HfTreeItem("models/$id/"+it.relativeTo(root).invariantSeparatorsPath,"file",it.length())}.toList(),true,true,"QA")
        val file=if(spec.expectedFiles.size==1)File(root,spec.expectedFiles.single()) else {
            val manifest=BundleManifest(kind=id,revision=revision,files=root.walkTopDown().filter{it.isFile && it.name!="bundle.json" && !it.name.startsWith("android-")}.associate{it.relativeTo(root).invariantSeparatorsPath to com.unicornwhodev.visiondatasetstudio.core.geometry.HashUtils.computeSha256(it)})
            File(root,"bundle.json").apply{writeText(StudioJson.moshi.adapter(BundleManifest::class.java).toJson(manifest))}
        }
        val base=CommunityModelCatalog.suggestedConfig(item,file)
        val config=when(id){"tinyclip"->base.copy(labels=listOf("red rectangle","blue circle"),threshold=0f);"efficientvit_sam"->base.copy(promptBox=listOf(.2f,.2f,.7f,.8f),threshold=0f);else->base}
        val image=Bitmap.createBitmap(128,96,Bitmap.Config.ARGB_8888)
        val canvas=android.graphics.Canvas(image);canvas.drawColor(Color.rgb(30,40,60));canvas.drawRect(24f,18f,92f,78f,android.graphics.Paint().apply{color=Color.RED})
        val start=System.nanoTime()
        try {
            LiteRtEngine().use{engine->
                assertTrue(engine.lastError,engine.loadModel(file));val out=engine.runInference(image,config).orThrow()
                assertNull(engine.lastError,engine.lastError)
                if(id in setOf("dinov2","repvit_m1","hgnetv2_b0","convformer_s18","tinyclip"))assertTrue(requireNotNull(engine.lastEmbedding).all(Float::isFinite))
                if(id=="dinov2")assertEquals(listOf(1,256,384),engine.lastPatches?.shape)
                if(id=="tinyclip")assertEquals(2,out.size)
                if(id=="efficientvit_sam")assertNotNull(engine.lastMask)
                File(root,"android-result.json").writeText(StudioJson.moshi.adapter(Any::class.java).indent("  ").toJson(mapOf("model" to id,"revision" to revision,"runtime" to "Android LiteRT 1.4.2 CPU / Select TF Ops 2.16.1","success" to true,"proposals" to out.size,"embedding_size" to engine.lastEmbedding?.size,"milliseconds" to (System.nanoTime()-start)/1_000_000,"accuracy_evaluated" to false,"note" to engine.lastNote)))
            }
        }finally{image.recycle()}
    }
    @Test fun dinov2()=run("dinov2")
    @Test fun convformer()=run("convformer_s18")
    @Test fun repvit()=run("repvit_m1")
    @Test fun hgnet()=run("hgnetv2_b0")
    @Test fun vitpose()=run("vitpose")
    @Test fun rfdetr()=run("rfdetr")
    @Test fun tinyclip()=run("tinyclip")
    @Test fun sam()=run("efficientvit_sam")
    @Test fun florence()=run("florence2")
}
