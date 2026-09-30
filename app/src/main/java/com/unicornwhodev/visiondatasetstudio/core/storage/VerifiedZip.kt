package com.unicornwhodev.visiondatasetstudio.core.storage

import java.io.File
import java.util.zip.ZipFile

/** Exact payload readback; a valid ZIP header alone does not establish a complete export. */
object VerifiedZip {
    data class Payload(val bytes: Long, val sha256: String)

    fun snapshot(files: List<Pair<String, File>>, checkCancelled: () -> Unit = {}): Map<String, Payload> {
        require(files.isNotEmpty())
        val result = linkedMapOf<String, Payload>()
        files.forEach { (name, file) ->
            checkCancelled()
            require(safeName(name) && file.isFile && name !in result)
            result[name] = Payload(file.length(), file.inputStream().use { DurableFiles.hash(it,Long.MAX_VALUE,checkCancelled) })
        }
        return result
    }

    fun verify(archive: File, expected: Map<String, Payload>, checkCancelled: () -> Unit = {}) {
        require(expected.isNotEmpty())
        ZipFile(archive).use { zip ->
            val seen = mutableSetOf<String>()
            zip.entries().asSequence().forEach { entry ->
                checkCancelled()
                require(!entry.isDirectory && safeName(entry.name) && seen.add(entry.name))
                val payload = expected[entry.name] ?: error("Unexpected ZIP entry")
                require(entry.size == payload.bytes)
                val actual = zip.getInputStream(entry).use { input ->
                    DurableFiles.hash(input, maxOf(1L, payload.bytes), checkCancelled)
                }
                check(actual == payload.sha256) { "ZIP payload mismatch" }
            }
            check(seen == expected.keys) { "Incomplete ZIP payload" }
        }
    }

    private fun safeName(name: String) = name.isNotEmpty() && !name.startsWith('/') &&
        name.none { it == '\\' || it == ':' || it.code < 32 } &&
        name.split('/').all { it.isNotEmpty() && it != "." && it != ".." }
}
