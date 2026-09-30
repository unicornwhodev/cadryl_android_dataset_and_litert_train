package com.unicornwhodev.visiondatasetstudio

import androidx.test.platform.app.InstrumentationRegistry
import java.security.MessageDigest
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class LegalAssetsTest {
    @Test fun privacyTermsAndOriginalNoticesAreActuallyBundledAndMatchTheirReceipt() {
        val assets=InstrumentationRegistry.getInstrumentation().targetContext.assets
        val files=JSONObject(assets.open("legal/manifest.json").bufferedReader().use { it.readText() }).getJSONObject("files")
        for (name in listOf("privacy.txt","privacy-en.txt","notices.txt")) {
            val digest=MessageDigest.getInstance("SHA-256")
            assets.open("legal/$name").use { input ->
                val buffer=ByteArray(65536)
                while(true) { val size=input.read(buffer);if(size<0)break;digest.update(buffer,0,size) }
            }
            assertEquals(files.getJSONObject(name).getString("sha256"),digest.digest().joinToString("") { "%02x".format(it) })
        }
        val privacy=assets.open("legal/privacy.txt").bufferedReader().use { it.readText() }
        assertTrue(privacy.contains("unicornwhodev@gmail.com"))
        val notices=assets.open("legal/notices.txt").bufferedReader().use { it.readText() }
        assertTrue(notices.contains("Apache License"))
        assertTrue(notices.contains("CPUinfo") || notices.contains("cpuinfo"))
    }
}
