package com.unicornwhodev.visiondatasetstudio

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Decode and draw the actual installed launcher resources, including R8 Release. */
@RunWith(AndroidJUnit4::class)
class LauncherResourcesTest {
    @Test fun launcherIconsAreDrawableAtEveryDensity() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // Resolve from the installed package: R classes may be removed by R8.
        val roundIcon = context.resources.getIdentifier("ic_launcher_round", "mipmap", context.packageName)
        assertNotEquals("Round launcher resource must be installed", 0, roundIcon)
        for (id in listOf(context.applicationInfo.icon, roundIcon)) {
            for (density in listOf(160, 240, 320, 480, 640)) {
                val icon = requireNotNull(context.resources.getDrawableForDensity(id, density, context.theme))
                assertTrue("Adaptive Cadryl icon required", icon is AdaptiveIconDrawable)
                val bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
                try {
                    icon.setBounds(0, 0, 96, 96)
                    icon.draw(Canvas(bitmap))
                    assertNotEquals("Icon must draw visible pixels", 0, bitmap.getPixel(48, 48))
                } finally {
                    bitmap.recycle()
                }
            }
        }
    }
}
