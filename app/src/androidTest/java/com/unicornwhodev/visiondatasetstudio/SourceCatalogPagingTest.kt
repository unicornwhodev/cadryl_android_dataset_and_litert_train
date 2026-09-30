package com.unicornwhodev.visiondatasetstudio

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.unicornwhodev.visiondatasetstudio.core.storage.StorageManager
import com.unicornwhodev.visiondatasetstudio.core.workflow.ProcessingSettings
import com.unicornwhodev.visiondatasetstudio.data.db.AppDatabase
import com.unicornwhodev.visiondatasetstudio.data.hf.HfApiClient
import com.unicornwhodev.visiondatasetstudio.data.model.ProjectEntity
import com.unicornwhodev.visiondatasetstudio.data.model.SourceEntryEntity
import com.unicornwhodev.visiondatasetstudio.data.preferences.ProjectSettings
import com.unicornwhodev.visiondatasetstudio.data.source.SourceCatalog
import com.unicornwhodev.visiondatasetstudio.domain.batch.BatchEngine
import com.unicornwhodev.visiondatasetstudio.domain.export.DatasetExporters
import com.unicornwhodev.visiondatasetstudio.domain.inference.LiteRtEngine
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test

/** Real Room + production Viewer parsing/pagination, with synthetic HTTP responses.
 * The interceptor returns before any socket opens; no HF access or user data is used.
 */
class SourceCatalogPagingTest {
    private val context=InstrumentationRegistry.getInstrumentation().targetContext

    private fun manifestUri() = android.net.Uri.parse("content://${InstrumentationRegistry.getInstrumentation().context.packageName}.documents/atomic-manifest")

    @Test fun malformedManifestAfterOneInsertedChunkPreservesThePreviousIndex() = exercise { db, _, catalog ->
        val p=project(ProcessingSettings(sourceMode="LOCAL_INDEX",sourceIndexReady=true))
        db.projectDao().saveProject(p)
        val old=SourceEntryEntity(p.id,0,"previous","https://example.invalid/previous.png")
        db.sourceEntryDao().insert(listOf(old))
        context.contentResolver.openOutputStream(manifestUri())!!.bufferedWriter().use { out ->
            repeat(257) { out.appendLine("""{"image":"https://example.invalid/$it.png"}""") }
            out.appendLine("{broken-json")
        }
        var rejected=false
        try { catalog.importManifest(p,manifestUri(),null) } catch (_: Exception) { rejected=true }
        assertTrue(rejected)
        assertEquals(listOf(old),db.sourceEntryDao().page(p.id,0,100))
        assertEquals(p,db.projectDao().getProjectSync(p.id))
    }

    @Test fun emptyManifestPreservesThePreviousIndex() = exercise { db, _, catalog ->
        val p=project(ProcessingSettings(sourceMode="LOCAL_INDEX",sourceIndexReady=true))
        db.projectDao().saveProject(p)
        val old=SourceEntryEntity(p.id,0,"previous","https://example.invalid/previous.png")
        db.sourceEntryDao().insert(listOf(old))
        context.contentResolver.openOutputStream(manifestUri())!!.use { it.write("\n".toByteArray()) }
        requireRejected { catalog.importManifest(p,manifestUri(),null) }
        assertEquals(listOf(old),db.sourceEntryDao().page(p.id,0,100))
        assertEquals(p,db.projectDao().getProjectSync(p.id))
    }

    private class ViewerFixture {
        val bodies=java.util.ArrayDeque<String>()
        val requests=mutableListOf<HttpUrl>()
        val api=HfApiClient(OkHttpClient.Builder().addInterceptor { chain ->
            val request=chain.request();requests+=request.url
            check(bodies.isNotEmpty()) { "Unexpected Viewer request" }
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body(bodies.removeFirst().toResponseBody("application/json".toMediaType())).build()
        }.build()) { null }
    }

    private fun project(settings:ProcessingSettings)=ProjectEntity(id=908L,name="QA source paging",
        hfSourceRepo="example/synthetic",settingsJson=ProjectSettings.write(settings))
    private fun row(index:Long,image:String="{\"src\":\"https://example.invalid/$index.png\"}",
                    annotations:String="{}",truncated:String="[]")=
        """{"row_idx":$index,"row":{"image":$image,"annotations":$annotations},"truncated_cells":$truncated}"""
    private fun page(rows:List<String>,partial:Boolean=false)=
        """{"rows":[${rows.joinToString(",")}],"partial":$partial}"""
    private fun exercise(block:suspend (AppDatabase,ViewerFixture,SourceCatalog)->Unit)=runBlocking {
        val db=Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).build()
        val fixture=ViewerFixture()
        try { block(db,fixture,SourceCatalog(context,db,fixture.api)) } finally {db.close()}
    }
    private suspend fun requireRejected(block:suspend ()->Unit) {
        var rejected=false
        try {block()} catch(_:IllegalStateException) {rejected=true}
        assertTrue("Source must fail without returning a partial success",rejected)
    }

    @Test fun exhaustivePagingSplitsAt100AndStopsAtTheKnownEnd()=exercise { _,fixture,catalog ->
        val p=project(ProcessingSettings(viewerExpectedRows=250))
        fixture.bodies+=page((100L..199L).map{row(it)},partial=true)
        fixture.bodies+=page((200L..249L).map{row(it)},partial=true)
        val result=catalog.page(p,100,150)
        assertEquals(150,result.consumed);assertEquals(0,result.rejected)
        assertEquals((100L..249L).toList(),result.entries.map{it.ordinal})
        assertEquals((100L..249L).toList(),result.entries.map{it.sourceRowIndex})
        assertEquals(listOf("100","200"),fixture.requests.map{it.queryParameter("offset")})
        assertEquals(listOf("100","50"),fixture.requests.map{it.queryParameter("length")})
        assertEquals(0,catalog.page(p,250,150).consumed)
        assertEquals(2,fixture.requests.size)
    }

    @Test fun exhaustiveShortPageCannotSilentlyCompleteTheCorpus()=exercise { _,fixture,catalog ->
        fixture.bodies+=page(listOf(row(0)))
        requireRejected {catalog.page(project(ProcessingSettings(viewerExpectedRows=3)),0,3)}
    }

    @Test fun exhaustiveDiscontinuousPageCannotClaimFullCoverage()=exercise { _,fixture,catalog ->
        fixture.bodies+=page(listOf(row(0),row(2)))
        requireRejected {catalog.page(project(ProcessingSettings(viewerExpectedRows=2)),0,2)}
    }

    @Test fun filteredPartialViewRequiresConsentAndPreservesOriginalIndices()=exercise { _,fixture,catalog ->
        val settings=ProcessingSettings(filterExpression="score > 0",orderBy="score DESC")
        val response=page(listOf(row(42),row(3)),partial=true)
        fixture.bodies+=response
        requireRejected {catalog.page(project(settings),0,2)}
        fixture.bodies+=response
        val result=catalog.page(project(settings.copy(allowPartialViewer=true)),0,2)
        assertEquals(2,result.consumed)
        assertEquals(listOf(0L,1L),result.entries.map{it.ordinal})
        assertEquals(listOf(42L,3L),result.entries.map{it.sourceRowIndex})
        assertTrue(fixture.requests.all{it.encodedPath=="/filter"})
    }

    @Test fun truncatedCanonicalAnnotationsAreRejectedOnlyWhenImported()=exercise { _,fixture,catalog ->
        val response=page(listOf(row(0,truncated="[\"annotations\"]"),row(1,truncated="[\"other\"]")))
        val settings=ProcessingSettings(viewerExpectedRows=2,importAnnotations=true)
        fixture.bodies+=response
        val result=catalog.page(project(settings),0,2)
        assertEquals(2,result.consumed);assertEquals(1,result.rejected)
        assertEquals(1L,result.entries.single().ordinal)
        assertNotNull(result.entries.single().annotationJson)
        assertTrue(result.diagnostics.single().contains("annotations"))
        fixture.bodies+=response
        val imagesOnly=catalog.page(project(settings.copy(importAnnotations=false)),0,2)
        assertEquals(2,imagesOnly.entries.size);assertEquals(0,imagesOnly.rejected)
        assertTrue(imagesOnly.entries.all{it.annotationJson==null})
    }

    @Test fun allRejectedRowsAdvanceThePersistedCursorWithoutCreatingABatch()=exercise { db,fixture,_ ->
        val p=project(ProcessingSettings(viewerExpectedRows=3,importAnnotations=true))
        db.projectDao().saveProject(p)
        fixture.bodies+=page(listOf(row(0,image="null"),row(1,truncated="[\"image\"]"),row(2,annotations="\"invalid\"")))
        val storage=StorageManager(context);val runtime=LiteRtEngine()
        try {
            val engine=BatchEngine(db,storage,fixture.api,runtime,DatasetExporters(storage,fixture.api))
            val result=engine.discoverViewerBatch(p,1,2)
            assertTrue(result.error,result.success);assertFalse(result.endOfSource)
            assertEquals(0,result.totalDiscovered)
            assertEquals(3L,db.projectDao().getProjectSync(p.id)!!.lastRowCursor)
            assertNull(db.batchDao().getBatchSync(p.id,1))
            assertTrue(db.sampleDao().getSamplesForBatchSync(p.id,1).isEmpty())
            assertTrue(engine.discoverViewerBatch(p,1,2).endOfSource)
        } finally {runtime.close()}
    }

    @Test fun malformedViewerReplyCannotAdvanceCursorOrReportEndOfSource()=exercise { db,fixture,_ ->
        val p=project(ProcessingSettings(viewerExpectedRows=3))
        db.projectDao().saveProject(p);fixture.bodies+="{}"
        val storage=StorageManager(context);val runtime=LiteRtEngine()
        try {
            val result=BatchEngine(db,storage,fixture.api,runtime,DatasetExporters(storage,fixture.api)).discoverViewerBatch(p,1,2)
            assertFalse(result.success);assertFalse(result.endOfSource)
            assertEquals(0L,db.projectDao().getProjectSync(p.id)!!.lastRowCursor)
            assertNull(db.batchDao().getBatchSync(p.id,1))
        } finally {runtime.close()}
    }

    @Test fun localIndexPagingCrossesTheRoomChunkBoundary()=exercise { db,fixture,catalog ->
        val p=project(ProcessingSettings(sourceMode="LOCAL_INDEX",sourceIndexReady=true))
        db.projectDao().saveProject(p)
        db.sourceEntryDao().insert((0L..32L).map{SourceEntryEntity(p.id,it,"asset-$it","content://synthetic/$it")})
        val result=catalog.page(p,2,30)
        assertEquals(30,result.consumed);assertEquals((2L..31L).toList(),result.entries.map{it.ordinal})
        assertEquals(1,catalog.page(p,32,30).consumed)
        assertEquals(0,catalog.page(p,33,30).consumed)
        assertTrue(fixture.requests.isEmpty())
    }
}
