package com.unicornwhodev.visiondatasetstudio
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StudioComposeV4Test {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()
    @Test fun controlsAndCatalogueRenderWithoutDownloadingModels() {
        rule.waitUntil(10000) { rule.onAllNodesWithTag("home_primary").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("controls_shortcut").assertIsDisplayed()
        rule.onNodeWithTag("controls_shortcut").performClick()
        rule.onAllNodesWithText(com.unicornwhodev.visiondatasetstudio.core.i18n.tr("Mes projets", "My projects")).onFirst().assertIsDisplayed()
        rule.onNodeWithTag("nav_Models").performClick()
        rule.onNodeWithText("SSD MobileNet V1").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText(com.unicornwhodev.visiondatasetstudio.core.i18n.tr("Réglages", "Settings")).performClick()
        rule.onNodeWithTag("nav_Models").assertIsSelected()
        rule.onNodeWithText(com.unicornwhodev.visiondatasetstudio.core.i18n.tr("Contrat JSON, mesures et outils avancés", "JSON contract, benchmarks & advanced tools")).performScrollTo().performClick()
        rule.onNodeWithText(com.unicornwhodev.visiondatasetstudio.core.i18n.tr("Réglages avancés du modèle", "Advanced model settings")).assertIsDisplayed()
    }
    @Test fun exportDestinationStaysInExportWorkspace() {
        rule.waitUntil(10000) { rule.onAllNodesWithTag("nav_Publication").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("nav_Publication").performClick()
        rule.onNodeWithText(com.unicornwhodev.visiondatasetstudio.core.i18n.tr("Destination et stockage", "Destination & storage")).performClick()
        rule.onNodeWithTag("nav_Publication").assertIsSelected()
        rule.onNodeWithText(com.unicornwhodev.visiondatasetstudio.core.i18n.tr("Dépôt de destination", "Destination repository")).assertIsDisplayed()
        rule.onNodeWithText(com.unicornwhodev.visiondatasetstudio.core.i18n.tr("Jeton HF", "HF token")).performScrollTo().assertExists()
    }
    @Test fun compactNavigationKeepsImportAndExportAccessible() {
        rule.waitUntil(10000) { rule.onAllNodesWithTag("nav_Models").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("nav_Models").performClick()
        rule.onNodeWithText(rule.activity.getString(R.string.models_tab_import), useUnmergedTree = true).assertIsDisplayed().performClick()
        rule.onNodeWithText(rule.activity.getString(R.string.models_choose_file)).assertIsDisplayed()
        rule.onNodeWithTag("nav_Publication").performClick()
        rule.onNodeWithText(rule.activity.getString(R.string.publication_local_archive)).assertExists()
        rule.onNodeWithTag("nav_QualityDashboard").performClick()
        rule.onNodeWithTag("nav_QualityDashboard").assertIsSelected()
        rule.onNodeWithText(rule.activity.getString(R.string.quality_storage)).assertExists()
        rule.onNodeWithTag("nav_Home").performClick()
        // The workspace command must now be immediately available on both layouts.
        rule.onNodeWithTag("home_primary").assertIsDisplayed().assertHasClickAction()
    }
}
