package com.unicornwhodev.visiondatasetstudio
import androidx.compose.ui.test.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StudioComposeV4Test {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()
    private fun ready() = rule.waitUntil(10000) { rule.onAllNodesWithTag("studio_tools").fetchSemanticsNodes().isNotEmpty() }
    private fun navigate(tag:String) {
        ready();rule.onNodeWithTag("studio_tools").performClick()
        rule.onNodeWithTag(tag).performScrollTo().performClick()
    }
    private fun selected(tag:String) {
        ready();rule.onNodeWithTag("studio_tools").performClick()
        rule.onNodeWithTag(tag).performScrollTo().assertIsSelected()
        rule.onNodeWithTag("toolbox_close").performClick()
    }
    @Test fun controlsAndCatalogueRenderWithoutDownloadingModels() {
        rule.waitUntil(10000) { rule.onAllNodesWithTag("home_primary").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("controls_shortcut").assertIsDisplayed()
        rule.onNodeWithTag("controls_shortcut").performClick()
        rule.onAllNodesWithText(com.unicornwhodev.visiondatasetstudio.core.i18n.tr("Mes projets", "My projects")).onFirst().assertIsDisplayed()
        navigate("nav_Models")
        rule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("SSD MobileNet V1"))
        rule.onNodeWithText("SSD MobileNet V1").assertIsDisplayed()
        rule.onNodeWithTag("model_settings").performClick()
        selected("nav_ModelSettings")
        rule.onNodeWithText(com.unicornwhodev.visiondatasetstudio.core.i18n.tr("Contrat JSON, mesures et outils avancés", "JSON contract, benchmarks & advanced tools")).performScrollTo().performClick()
        rule.onNodeWithText(com.unicornwhodev.visiondatasetstudio.core.i18n.tr("Réglages avancés du modèle", "Advanced model settings")).assertIsDisplayed()
    }
    @Test fun exportDestinationStaysInExportWorkspace() {
        ready()
        navigate("nav_Publication")
        rule.onNodeWithText(com.unicornwhodev.visiondatasetstudio.core.i18n.tr("Destination et stockage", "Destination & storage")).performClick()
        selected("nav_TransferSettings")
        rule.onNodeWithText(com.unicornwhodev.visiondatasetstudio.core.i18n.tr("Dépôt de destination", "Destination repository")).assertIsDisplayed()
        rule.onNodeWithText(com.unicornwhodev.visiondatasetstudio.core.i18n.tr("Jeton HF", "HF token")).performScrollTo().assertExists()
    }
    @Test fun compactNavigationKeepsImportAndExportAccessible() {
        ready()
        navigate("nav_Models")
        rule.onNodeWithText(rule.activity.getString(R.string.models_tab_import), useUnmergedTree = true).assertIsDisplayed().performClick()
        rule.onNodeWithText(rule.activity.getString(R.string.models_choose_file)).assertIsDisplayed()
        navigate("nav_Publication")
        rule.onNodeWithText(rule.activity.getString(R.string.publication_local_archive)).assertExists()
        navigate("nav_QualityDashboard")
        selected("nav_QualityDashboard")
        rule.onNodeWithText(rule.activity.getString(R.string.quality_storage)).assertExists()
        navigate("nav_Home")
        // The workspace command must now be immediately available on both layouts.
        rule.onNodeWithTag("home_primary").assertIsDisplayed().assertHasClickAction()
    }
    @Test fun homeKeepsTrainingExplicitlyAccessible() {
        ready()
        rule.onNode(hasScrollToIndexAction()).performScrollToNode(hasTestTag("home_training"))
        rule.onNodeWithTag("home_training").assertIsDisplayed().performClick()
        rule.onNodeWithTag("route_Training").assertExists()
        selected("nav_Training")
        navigate("nav_Home")
        rule.onNodeWithTag("route_Home").assertExists()
    }

    @Test fun foldingModelOptionsKeepsUncommittedInput() {
        rule.setStudioTestContent {
            androidx.compose.material3.MaterialTheme {
                com.unicornwhodev.visiondatasetstudio.ui.components.StudioDisclosure("Options",initiallyExpanded=true,keepContent=true) {
                    var draft by remember { mutableStateOf("") }
                    androidx.compose.material3.OutlinedTextField(draft,{draft=it},modifier=Modifier.testTag("qa_unsaved_model_input"))
                }
            }
        }
        rule.onNodeWithTag("qa_unsaved_model_input").performTextInput("unsaved model option")
        rule.onNodeWithText("Options").performClick()
        rule.onNodeWithTag("qa_unsaved_model_input",useUnmergedTree=true).assertIsNotDisplayed()
        rule.onNodeWithText("Options").performClick()
        rule.onNodeWithTag("qa_unsaved_model_input").assertTextContains("unsaved model option")
    }

}
