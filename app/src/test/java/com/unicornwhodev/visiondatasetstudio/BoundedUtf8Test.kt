package com.unicornwhodev.visiondatasetstudio

import com.unicornwhodev.visiondatasetstudio.core.storage.BoundedUtf8
import org.junit.Assert.*
import org.junit.Test

class BoundedUtf8Test {
    @Test fun exactByteBoundaryAcceptsUnicodeWithoutReplacement() {
        val text="étiquette 🦄";val bytes=text.toByteArray()
        assertEquals(text,BoundedUtf8.reader(bytes.inputStream(),bytes.size.toLong()).use { it.readText() })
    }
    @Test fun multibyteTextCannotExceedTheActualByteBudget() {
        try { BoundedUtf8.reader("éé".toByteArray().inputStream(),3).use { it.readText() };fail("Must reject") }
        catch(_: IllegalArgumentException) { }
    }
    @Test fun malformedUtf8IsNotSilentlyChanged() {
        try { BoundedUtf8.reader(byteArrayOf(0xc3.toByte(),0x28).inputStream(),8).use { it.readText() };fail("Must reject") }
        catch(_: java.nio.charset.CharacterCodingException) { }
    }
}
