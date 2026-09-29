package com.unicornwhodev.visiondatasetstudio
import com.unicornwhodev.visiondatasetstudio.data.hf.*
import com.unicornwhodev.visiondatasetstudio.core.storage.DownloadCheckpoint
import com.unicornwhodev.visiondatasetstudio.core.geometry.HashUtils
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import okio.Buffer
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.TimeUnit

/** Loopback fault injection into the production OkHttp client. Does NOT contact real HF. */
class HfTransferV4Test {
    @get:Rule val temp=TemporaryFolder()
    private fun exercise(block:suspend (MockWebServer,HfApiClient)->Unit) = runBlocking {
        // Pin the fault-injection server to IPv4. After the injected disconnect,
        // OkHttp can try localhost's ::1 route although this server listens on IPv4 only.
        val server=MockWebServer();server.start(java.net.InetAddress.getByName("127.0.0.1"),0)
        try {
            val client=OkHttpClient.Builder().readTimeout(3,TimeUnit.SECONDS).retryOnConnectionFailure(false).addInterceptor{ chain ->
                val request=chain.request();val target=server.url(request.url.encodedPath).newBuilder().host("127.0.0.1").encodedQuery(request.url.encodedQuery).build()
                chain.proceed(request.newBuilder().url(target).build())
            }.build()
            block(server,HfApiClient(client){"test-only-placeholder"})
        } finally {server.shutdown()}
    }
    @Test fun partialViewerResponseKeepsUsableRows()=exercise { server,api ->
        server.enqueue(MockResponse().setHeader("Content-Type","application/json").setBody("""{
          "features":[{"feature_idx":0,"name":"image","type":{"_type":"Image"}},{"feature_idx":1,"name":"original_image","type":{"_type":"Image"}}],
          "rows":[{"row_idx":0,"row":{"image":{"src":"https://datasets-server.huggingface.co/assets/example.jpg"},"original_image":null},"truncated_cells":["original_image"]}],
          "num_rows_per_page":1,
          "partial":true
        }"""))
        val result=api.fetchViewerRows("example/dataset","prepared","unvalidated",0,1)
        assertTrue(result.success)
        assertTrue(result.partial)
        assertEquals(listOf("image","original_image"),result.columns)
        assertEquals(1,result.rows.size)
        assertEquals(listOf("original_image"),result.rows.single().truncatedCells)
    }

    @Test fun missingViewerRowsIsAnErrorRatherThanEndOfSource()=exercise { server,api ->
        for(body in listOf("{}", "null", "{\"rows\":null}")) {
            server.enqueue(MockResponse().setBody(body))
            val result=api.fetchViewerRows("example/dataset","default","train",0,1)
            assertFalse("Invalid payload accepted: $body",result.success)
            assertTrue(result.rows.isEmpty())
        }
    }

    @Test fun explicitEmptyViewerRowsRemainAValidEndOfSource()=exercise { server,api ->
        server.enqueue(MockResponse().setBody("{\"rows\":[]}"))
        val result=api.fetchViewerRows("example/dataset","default","train",0,1)
        assertTrue(result.success);assertTrue(result.rows.isEmpty())
    }

    @Test fun oversizedViewerPageCannotAdvanceBeyondTheRequestedWindow()=exercise { server,api ->
        server.enqueue(MockResponse().setBody("""{"rows":[{"row_idx":0,"row":{}},{"row_idx":1,"row":{}}]}"""))
        val result=api.fetchViewerRows("example/dataset","default","train",0,1)
        assertFalse(result.success);assertTrue(result.rows.isEmpty())
    }

    @Test fun invalidViewerRowIdentitiesAreRejected()=exercise { server,api ->
        for(body in listOf(
            """{"rows":[{"row_idx":-1,"row":{}}]}""",
            """{"rows":[{"row_idx":7,"row":{}},{"row_idx":7,"row":{}}]}"""
        )) {
            server.enqueue(MockResponse().setBody(body))
            val result=api.fetchViewerRows("example/dataset","default","train",0,2)
            assertFalse(result.success);assertTrue(result.rows.isEmpty())
        }
    }

    @Test fun filteredViewerOrderUsesOriginalRowIdentities()=exercise { server,api ->
        server.enqueue(MockResponse().setBody("""{"rows":[{"row_idx":42,"row":{}},{"row_idx":3,"row":{}}]}"""))
        val result=api.fetchViewerRows("example/dataset","default","train",0,2,"score > 0","score DESC")
        assertTrue(result.success);assertEquals(listOf(42L,3L),result.rows.map{it.rowIdx})
        assertTrue(server.takeRequest().path!!.startsWith("/filter?"))
    }

    @Test fun disconnectedDownloadResumesHashedPrefix()=exercise { server,api ->
        val bytes=ByteArray(256*1024){(it%251).toByte()};val file=File(temp.root,"image.bin")
        server.enqueue(MockResponse().setBody(Buffer().write(bytes)).addHeader("ETag","\"v1\"").setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY))
        assertFalse(api.downloadImage("https://storage.googleapis.com/fixture",file,maxBytes=bytes.size.toLong()))
        assertFalse(file.exists());val part=File(file.path+".part");val offset=part.length();assertTrue(offset in 1 until bytes.size.toLong())
        assertNull(server.takeRequest().getHeader("Authorization"))
        server.enqueue(MockResponse().setResponseCode(206).addHeader("ETag","\"v1\"").addHeader("Content-Range","bytes $offset-${bytes.lastIndex}/${bytes.size}").setBody(Buffer().write(bytes,offset.toInt(),bytes.size-offset.toInt())))
        assertTrue(api.downloadImage("https://storage.googleapis.com/fixture",file,maxBytes=bytes.size.toLong()))
        assertArrayEquals(bytes,file.readBytes());assertEquals("bytes=$offset-",server.takeRequest().getHeader("Range"))
        assertFalse(part.exists());assertFalse(File(file.path+".range").exists())
    }
    @Test fun fullResponseRestartsInsteadOfAppending()=exercise { server,api ->
        val file=File(temp.root,"full.bin");val part=File(file.path+".part").apply{writeText("old")}
        DownloadCheckpoint.save(part,File(file.path+".range"),com.unicornwhodev.visiondatasetstudio.core.storage.DurableFiles.hash("https://storage.googleapis.com/fixture".byteInputStream()),"\"old\"",8)
        server.enqueue(MockResponse().setBody("new-data").addHeader("ETag","\"new\""))
        assertTrue(api.downloadImage("https://storage.googleapis.com/fixture",file));assertEquals("new-data",file.readText())
    }
    @Test fun wrongRangeNeverOverwritesFinalFile()=exercise { server,api ->
        val file=File(temp.root,"range.bin").apply{writeText("protected")};val part=File(file.path+".part").apply{writeText("old")}
        DownloadCheckpoint.save(part,File(file.path+".range"),com.unicornwhodev.visiondatasetstudio.core.storage.DurableFiles.hash("https://storage.googleapis.com/fixture".byteInputStream()),"\"old\"",8)
        server.enqueue(MockResponse().setResponseCode(206).addHeader("ETag","\"old\"").addHeader("Content-Range","bytes 2-7/8").setBody("wrong!"))
        assertFalse(api.downloadImage("https://storage.googleapis.com/fixture",file));assertEquals("protected",file.readText())
    }
    @Test fun overSizeResponseRejected()=exercise {server,api ->
        val file=File(temp.root,"too-large");server.enqueue(MockResponse().setBody("too much"))
        assertFalse(api.downloadImage("https://storage.googleapis.com/fixture",file,maxBytes=2));assertFalse(file.exists())
    }
    @Test fun forbiddenDownloadHasActionableDiagnosticAndPreservesExistingFile()=exercise { server,api ->
        val file=File(temp.root,"protected.bin").apply { writeText("existing") }
        var message:String?=null
        server.enqueue(MockResponse().setResponseCode(403).setBody("private response must not appear"))
        assertFalse(api.downloadImage("https://huggingface.co/fixture",file,onFailure={message=it}))
        assertEquals("existing",file.readText())
        assertTrue(message.orEmpty().contains("403"))
        assertFalse(message.orEmpty().contains("private response"))
        assertFalse(message.orEmpty().contains("test-only-placeholder"))
    }
    @Test fun commitConflictPreservesPinnedParent()=exercise {server,api ->
        val f=File(temp.root,"data.bin").apply{writeText("content")};val parent="a".repeat(40)
        server.enqueue(MockResponse().setBody("{\"files\":[{\"path\":\"batches/data.bin\",\"uploadMode\":\"regular\"}]}"))
        server.enqueue(MockResponse().setResponseCode(409).setBody("conflict"))
        val result=api.uploadBatchFiles("user/test","main","test",listOf("batches/data.bin" to f),parent)
        assertFalse(result.success);assertTrue(result.conflict);assertTrue(f.isFile)
        assertTrue(server.takeRequest().path!!.contains("/preupload/"));val commit=server.takeRequest()
        assertTrue(commit.body.readUtf8().contains("\"parentCommit\":\"$parent\""));assertEquals(2,server.requestCount)
    }
    @Test fun lostCommitReplyIsNotAutomaticallyReplayed()=exercise {server,api ->
        val f=File(temp.root,"data.bin").apply{writeText("content")}
        server.enqueue(MockResponse().setBody("{\"files\":[{\"path\":\"data.bin\",\"uploadMode\":\"regular\"}]}"))
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST))
        val result=api.uploadBatchFiles("user/test","main","test",listOf("data.bin" to f),"a".repeat(40))
        assertFalse(result.success);assertEquals(2,server.requestCount);assertEquals("content",f.readText())
    }
    @Test fun remoteReceiptVerifiesWithoutLocalPayload()=exercise {server,api ->
        val f=File(temp.root,"data").apply{writeText("payload")};val hash=HashUtils.computeSha256(f);f.delete()
        val sha="b".repeat(40);server.enqueue(MockResponse().setBody("{\"sha\":\"$sha\"}"));server.enqueue(MockResponse().setBody("payload"))
        assertTrue(api.verifyRemoteDigests("user/test",sha,listOf(RemoteFileDigest("data.bin",7,hash))))
    }
    @Test fun occupiedPathIsNeverOverwritten()=exercise {server,api ->
        server.enqueue(MockResponse().setResponseCode(200))
        var refused=false;try{api.requirePathsAbsent("user/test","a".repeat(40),listOf("data.bin"))}catch(_:IllegalStateException){refused=true}
        assertTrue(refused)
    }

    private fun lfsAcknowledgement(body: String, status: Int)=exercise { server,api ->
        val file=File(temp.root,"synthetic.png").apply{writeText("synthetic fixture bytes")}
        val hash=HashUtils.computeSha256(file);val parent="a".repeat(40);val commit="b".repeat(40)
        server.enqueue(MockResponse().setBody("""{"files":[{"path":"synthetic.png","uploadMode":"lfs"}]}"""))
        server.enqueue(MockResponse().setBody("""{"objects":[{"oid":"$hash","actions":{"upload":{"href":"https://storage.googleapis.com/fixture-put"},"verify":{"href":"https://huggingface.co/fixture-verify"}}}]}"""))
        server.enqueue(MockResponse().setResponseCode(200))
        server.enqueue(MockResponse().setResponseCode(status).addHeader("Content-Type","text/plain").setBody(body))
        if(status==200) server.enqueue(MockResponse().setBody("""{"commitOid":"$commit"}"""))
        val result=api.uploadBatchFiles("user/test",commitMessage="fixture",files=listOf("synthetic.png" to file),expectedParentCommit=parent)
        assertEquals(status==200,result.success)
        assertEquals(if(status==200) commit else null,result.commitSha)
        assertEquals(if(status==200) 5 else 4,server.requestCount)
        server.takeRequest();server.takeRequest()
        val upload=server.takeRequest();assertEquals("PUT",upload.method);assertNull(upload.getHeader("Authorization"))
        val verify=server.takeRequest();assertEquals("POST",verify.method)
        assertEquals("application/vnd.git-lfs+json; charset=utf-8",verify.getHeader("Content-Type"))
        assertTrue(file.isFile)
    }
    @Test fun realHubPlainTextLfsAcknowledgementAllowsCommit()=lfsAcknowledgement("OK",200)
    @Test fun emptyLfsAcknowledgementAllowsCommit()=lfsAcknowledgement("",200)
    @Test fun failedLfsAcknowledgementForbidsCommit()=lfsAcknowledgement("Denied",403)
}
