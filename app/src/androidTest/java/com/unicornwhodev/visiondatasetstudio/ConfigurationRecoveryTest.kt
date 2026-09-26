package com.unicornwhodev.visiondatasetstudio

import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import com.unicornwhodev.visiondatasetstudio.core.geometry.HashUtils
import com.unicornwhodev.visiondatasetstudio.core.storage.StorageManager
import com.unicornwhodev.visiondatasetstudio.data.db.AppDatabase
import com.unicornwhodev.visiondatasetstudio.data.hf.*
import com.unicornwhodev.visiondatasetstudio.data.json.StudioJson
import com.unicornwhodev.visiondatasetstudio.data.model.*
import com.unicornwhodev.visiondatasetstudio.domain.batch.*
import com.unicornwhodev.visiondatasetstudio.domain.export.DatasetExporters
import com.unicornwhodev.visiondatasetstudio.domain.inference.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.zip.ZipFile

class ConfigurationRecoveryTest {
    @Test fun incompatibleVocabularyStopsBeforeDecodingAndPreservesAnnotations()=runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val db=androidx.room.Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).build()
        val storage=StorageManager(context); val hf=HfApiClient { null }; val runtime=LiteRtEngine()
        val engine=BatchEngine(db,storage,hf,runtime,DatasetExporters(storage,hf))
        val project=ProjectEntity(id=980002,name="Class check",classesCsv="chat",activeTasksCsv="DETECTION")
        val sample=SampleEntity("class-guard",project.id,1,"fixture",0,sourceFileUrl=null,localImagePath="/must-not-decode.png",acquisitionStatus="AVAILABLE",annotationStatus="PENDING",syncStatus="NOT_EXPORTED")
        val human=AnnotationRecord(sample.sampleId,"""{"boxes":[{"id":"human","label":"chat","xmin":0.1,"ymin":0.1,"xmax":0.5,"ymax":0.5,"isHumanVerified":true}]}""")
        try {
            db.projectDao().saveProject(project); db.batchDao().insertOrReplace(BatchEntity(project.id,1,"IN_PROGRESS"))
            db.sampleDao().insertSamples(listOf(sample)); db.annotationDao().insertOrReplace(human)
            val error=runCatching { engine.runBatchInference(project.id,1,ModelConfig(adapter="yolo",labels=listOf("cat")),replaceExistingProposals=true) }.exceptionOrNull()
            assertNotNull(error)
            assertFalse(error.toString().contains("must-not-decode"))
            assertTrue(error.toString(),error is IllegalStateException)
            assertEquals(human,db.annotationDao().getAnnotationSync(sample.sampleId))
            assertEquals(sample,db.sampleDao().getSampleSync(sample.sampleId))
        } finally { runtime.close(); db.close() }
    }

    @Test fun realSignatureRejectsWrongDimensionsBeforeInference() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val fixture=File(context.filesDir,"training-fixture")
        val file=File(fixture,"trainable-vision-fixture.tflite")
        assertTrue("Stage the real training fixture",file.isFile)
        val config=StudioJson.moshi.adapter(ModelConfig::class.java).fromJson(File(fixture,"model_config.json").readText())!!
        LiteRtEngine().use { engine ->
            assertTrue(engine.loadModel(file))
            engine.validateInput(config)
            assertNotNull(engine.inputSpec(config))
            assertTrue(runCatching { engine.validateInput(config.copy(inputChannels=1)) }.isFailure)
            assertTrue(runCatching { engine.validateInput(config.copy(inputType="UINT8")) }.isFailure)
        }
    }

    @Test fun recoveryCopiesFrozenBytesWithoutClosingOrRebasingUpload()=runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val db=AppDatabase.getInstance(context)
        val storage=StorageManager(context)
        val hf=HfApiClient { null }
        val runtime=LiteRtEngine()
        val exporter=DatasetExporters(storage,hf)
        val engine=BatchEngine(db,storage,hf,runtime,exporter)
        val id=System.currentTimeMillis()
        val project=ProjectEntity(id=id,name="Recovery fixture",classesCsv="fixture",diskBudgetMb=8192)
        val image=storage.getImageFile("recovery-$id","png")
        Bitmap.createBitmap(16,16,Bitmap.Config.ARGB_8888).let { bitmap ->
            image.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) };bitmap.recycle()
        }
        val sample=SampleEntity("recovery-$id",id,1,"fixture",0,sourceFileUrl=null,localImagePath=image.path,
            imageWidth=16,imageHeight=16,sha256=HashUtils.computeSha256(image),acquisitionStatus="AVAILABLE",annotationStatus="VALIDATED",syncStatus="NOT_EXPORTED")
        val annotations=SampleAnnotations(boxes=listOf(BoxTarget("human",.1f,.1f,.5f,.5f,"fixture",isHumanVerified=true)))
        try {
            db.projectDao().saveProject(project)
            db.batchDao().insertOrReplace(BatchEntity(id,1,"IN_PROGRESS",totalCases=1))
            db.sampleDao().insertSamples(listOf(sample))
            val annotationJson=StudioJson.moshi.adapter(SampleAnnotations::class.java).toJson(annotations)
            db.annotationDao().insertOrReplace(AnnotationRecord(sample.sampleId,annotationJson))
            val packaged=exporter.packageBatchForHf(project,1,listOf(sample to annotations),false,true,false,false,false)
            assertTrue(packaged.error,packaged.success)
            val prefix="recovery-fixture"
            val files=packaged.generatedFiles.sortedBy { it.first }
            val receipt=RemoteReceipt(files=files.map { (path,file) -> RemoteFileDigest("$prefix/$path",file.length(),HashUtils.computeSha256(file)) })
            val pending=BatchEntity(id,1,"PUBLISHING",totalCases=1,validatedCases=1,
                archiveSnapshot=engine.snapshot(id,1),remotePrefix=prefix,remoteBranch="main",remoteRepoId="fixture/unused",
                remoteParentCommit="a".repeat(40),preparedManifestSha256=HashUtils.computeSha256(files.single { it.first.endsWith("/manifest.json") }.second),
                remoteReceiptJson=StudioJson.moshi.adapter(RemoteReceipt::class.java).toJson(receipt),lastTransferError="HTTP 403")
            db.batchDao().updateBatch(pending)
            val zip=engine.recoverPendingArchive(id,1)
            val zipHash=HashUtils.computeSha256(zip)
            ZipFile(zip).use { archive ->
                assertEquals(files.size,archive.size())
                files.forEach { (name,file) -> assertArrayEquals(file.readBytes(),archive.getInputStream(archive.getEntry(name)).readBytes()) }
            }
            assertEquals(pending,db.batchDao().getBatchSync(id,1))
            assertEquals(annotationJson,db.annotationDao().getAnnotationSync(sample.sampleId)!!.dataJson)
            assertTrue(image.isFile)
            assertTrue(runCatching { engine.purgeReviewedBatch(id,1,true) }.isFailure)
            files.first().second.appendText("tampered")
            assertTrue(runCatching { engine.recoverPendingArchive(id,1) }.isFailure)
            assertEquals(zipHash,HashUtils.computeSha256(zip))
        } finally {
            runtime.close()
            // This test owns its unique synthetic project and never touches an installed user project.
            db.batchDao().updateBatch(BatchEntity(id,1,"IN_PROGRESS",totalCases=1))
            ProjectMaintenance(context,db,storage).deleteProject(id)
        }
    }
}
