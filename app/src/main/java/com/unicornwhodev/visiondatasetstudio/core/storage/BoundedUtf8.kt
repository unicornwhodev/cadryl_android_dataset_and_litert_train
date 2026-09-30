package com.unicornwhodev.visiondatasetstudio.core.storage

import java.io.BufferedReader
import java.io.FilterInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.CodingErrorAction

/** Bound actual UTF-8 bytes, and reject malformed encoding instead of replacing human text. */
object BoundedUtf8 {
    fun reader(input: InputStream, maxBytes: Long): BufferedReader {
        require(maxBytes>0)
        val bounded=object: FilterInputStream(input) {
            private var consumed=0L
            private fun count(n: Int): Int {
                if(n>0) {
                    require(n.toLong()<=maxBytes-consumed) { "UTF-8 document exceeds its byte limit" }
                    consumed+=n
                }
                return n
            }
            override fun read(): Int = super.read().also { if(it>=0) count(1) }
            override fun read(bytes: ByteArray, offset: Int, length: Int): Int = count(`in`.read(bytes,offset,length))
        }
        val decoder=Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
        return BufferedReader(InputStreamReader(bounded,decoder))
    }
}
