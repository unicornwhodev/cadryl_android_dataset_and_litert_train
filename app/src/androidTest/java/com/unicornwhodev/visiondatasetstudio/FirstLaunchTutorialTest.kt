package com.unicornwhodev.visiondatasetstudio

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Uses the actual app and restores the original launch preference after each test. */
@RunWith(AndroidJUnit4::class)
class FirstLaunchTutorialTest {
    @get:Rule val rule = createEmptyComposeRule()
    private val prefs get() = InstrumentationRegistry.getInstrumentation().targetContext
        .getSharedPreferences("studio_ui_v2", Context.MODE_PRIVATE)
    private var activity: ActivityScenario<MainActivity>? = null

    private fun withTutorial(block: () -> Unit) {
        val existed = prefs.contains("tutorial_on_launch")
        val original = prefs.getBoolean("tutorial_on_launch", true)
        try {
            assertTrue(prefs.edit().putBoolean("tutorial_on_launch", true).commit())
            launch()
            block()
        } finally {
            activity?.close()
            activity = null
            val editor = prefs.edit()
            if (existed) editor.putBoolean("tutorial_on_launch", original) else editor.remove("tutorial_on_launch")
            assertTrue(editor.commit())
        }
    }
    private fun launch() {
        activity?.close()
        activity = ActivityScenario.launch(MainActivity::class.java)
        rule.waitUntil(15000) { rule.onAllNodesWithTag("home_primary").fetchSemanticsNodes().isNotEmpty() }
    }
    private fun click(tag: String) {
        rule.onNode(hasScrollToIndexAction()).performScrollToNode(hasTestTag(tag))
        rule.onNodeWithTag(tag).assertIsDisplayed().performClick()
        rule.waitForIdle()
    }
    private fun complete(disable: Boolean) {
        click("tutorial_source_local")
        click("tutorial_next")
        click("tutorial_mark_target")
        click("tutorial_next")
        click("tutorial_mode_model")
        click("tutorial_next")
        click("tutorial_export_coco")
        click("tutorial_copy_verified")
        rule.onNodeWithTag("tutorial_copy_verified").assertIsOn()
        if (disable) click("tutorial_disable_next_launch")
        click("tutorial_next")
        rule.waitUntil(10000) { rule.onAllNodesWithTag("tutorial_card").fetchSemanticsNodes().isEmpty() }
    }
    @Test fun checkedFinishDisablesFutureLaunchesAndSettingsReplays() = withTutorial {
        complete(true)
        assertFalse(prefs.getBoolean("tutorial_on_launch", true))
        launch()
        rule.onNodeWithTag("tutorial_card").assertDoesNotExist()
        rule.onNodeWithTag("home_more").performClick()
        rule.onNodeWithTag("home_settings").performClick()
        rule.onNodeWithTag("tutorial_replay").performScrollTo().performClick()
        rule.waitUntil(10000) { rule.onAllNodesWithTag("route_Home").fetchSemanticsNodes().isNotEmpty() }
        click("tutorial_source_hf")
        assertFalse(prefs.getBoolean("tutorial_on_launch", true))
    }
    @Test fun uncheckedFinishShowsTutorialOnNextLaunch() = withTutorial {
        complete(false)
        assertTrue(prefs.getBoolean("tutorial_on_launch", false))
        launch()
        click("tutorial_source_local")
    }
    @Test fun laterOnlyDismissesCurrentSession() = withTutorial {
        click("tutorial_later")
        rule.onNodeWithTag("tutorial_card").assertDoesNotExist()
        assertTrue(prefs.getBoolean("tutorial_on_launch", false))
        launch()
        click("tutorial_source_local")
    }
}
