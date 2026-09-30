package com.unicornwhodev.visiondatasetstudio

import com.unicornwhodev.visiondatasetstudio.core.storage.DurableFiles
import com.unicornwhodev.visiondatasetstudio.core.storage.VerifiedZip
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.CancellationException

class VerifiedZipTest {
    private fun exercise(block: (File) -> Unit) {
        val root = Files.createTempDirectory("verified-zip-").toFile()
        try { block(root) } finally { root.deleteRecursively() }
    }
    private fun zip(root: File, entries: List<Pair<String, String>>): File = File(root, "candidate.zip").also { file ->
        ZipOutputStream(file.outputStream()).use { out -> entries.forEach { (name, body) ->
            out.putNextEntry(ZipEntry(name)); out.write(body.toByteArray()); out.closeEntry()
        } }
    }
    private fun expected(root: File) = VerifiedZip.snapshot(listOf("images/a.txt" to
        File(root, "a.txt").apply { writeText("source") }))
    private fun rejected(block: () -> Unit) {
        try { block(); fail("Candidate must be rejected") } catch (_: Exception) { /* Expected rejection. */ }
    }
    @Test fun exactReadbackAcceptsOnlyTheCompletePayload() = exercise { root ->
        VerifiedZip.verify(zip(root, listOf("images/a.txt" to "source")), expected(root))
    }
    @Test fun modifiedBytesOfTheSameLengthAreRejected() = exercise { root ->
        rejected { VerifiedZip.verify(zip(root, listOf("images/a.txt" to "forged")), expected(root)) }
    }
    @Test fun missingAndUnexpectedEntriesAreRejected() = exercise { root ->
        val expected = expected(root)
        rejected { VerifiedZip.verify(zip(root, emptyList()), expected) }
        rejected { VerifiedZip.verify(zip(root, listOf("images/a.txt" to "source", "extra" to "x")), expected) }
    }
    @Test fun unsafeNamesAreRejected() = exercise { root ->
        val expected = expected(root)
        for (name in listOf("../a", "/a", "images/../a", "images\\a", "a:b")) {
            rejected { VerifiedZip.verify(zip(root, listOf(name to "source")), expected) }
            rejected { VerifiedZip.snapshot(listOf(name to File(root, "a.txt"))) }
        }
    }
    @Test fun duplicateSourceNamesAreRejected() = exercise { root ->
        val source = File(root, "a").apply { writeText("a") }
        rejected { VerifiedZip.snapshot(listOf("a" to source, "a" to source)) }
    }
    @Test fun failedReadbackPreservesThePreviousArchive() = exercise { root ->
        val target = File(root, "valid.zip").apply { writeText("previous valid archive") }
        rejected { DurableFiles.replace(target, verify = { error("readback failed") }) { it.write("partial".toByteArray()) } }
        assertEquals("previous valid archive", target.readText())
        assertFalse(root.listFiles()!!.any { it.name.endsWith(".pending") })
    }
    @Test fun cancellationPreservesThePreviousArchive() = exercise { root ->
        val target = File(root, "valid.zip").apply { writeText("previous") }
        try {
            DurableFiles.replace(target, verify = { throw CancellationException("interrupted") }) { it.write(1) }
            fail("Cancellation expected")
        } catch (_: CancellationException) { assertEquals("previous", target.readText()) }
    }
    @Test fun truncatedZipIsRejected() = exercise { root ->
        val expected = expected(root)
        val candidate = zip(root, listOf("images/a.txt" to "source"))
        candidate.writeBytes(candidate.readBytes().copyOf(15))
        rejected { VerifiedZip.verify(candidate, expected) }
    }
}
