package com.unicornwhodev.visiondatasetstudio

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import com.unicornwhodev.visiondatasetstudio.core.i18n.tr
import com.unicornwhodev.visiondatasetstudio.data.json.StudioJson
import com.unicornwhodev.visiondatasetstudio.data.preferences.ProjectSettings
import com.unicornwhodev.visiondatasetstudio.domain.inference.ModelConfig
import com.unicornwhodev.visiondatasetstudio.ui.*
import com.unicornwhodev.visiondatasetstudio.ui.theme.VisionDatasetStudioTheme
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class GuidedSetupTest {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()

    @Test fun beginnerCanChooseAnExactClassAndSaveManualSetupWithoutAnAccount()=runBlocking {
        val app=InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application
        val store=ViewModelStore()
        val vm=withContext(Dispatchers.Main){ViewModelProvider(store,ViewModelProvider.AndroidViewModelFactory.getInstance(app))[MainViewModel::class.java]}
        val previous=vm.preferenceStore.activeProjectId
        var created:Long?=null
        suspend fun act(block:MainViewModel.()->Unit) {
            withContext(Dispatchers.Main){vm.clearOperationProgress();vm.block()}
            withTimeout(30_000){while(vm.isBusy.value || vm.editorBusy.value)delay(25)}
            assertFalse(vm.operationProgress.value.toString(),vm.operationProgress.value?.isError==true)
            withTimeout(10_000){while(vm.projectFlow.value?.id!=vm.activeProjectId.value)delay(25)}
        }
        try {
            act{createProject("Guided setup fixture")}; created=vm.activeProjectId.value
            val config=ModelConfig.defaultClassifierPreset(listOf("cat","dog","traffic light"))
            act{saveModelConfig(StudioJson.moshi.adapter(ModelConfig::class.java).toJson(config))}
            act{navigateTo(Screen.Home)}
            rule.setStudioTestContent { VisionDatasetStudioTheme(darkTheme=true) { StudioRoot(vm) } }
            rule.onNodeWithTag("home_primary").assertIsDisplayed().performClick()
            rule.onNodeWithTag("setup_next").assertIsNotEnabled()
            rule.onNodeWithTag("nav_Home").assertDoesNotExist() // The guide has one navigation level.
            rule.onNodeWithText(tr("Sur Hugging Face","On Hugging Face")).performClick()
            rule.onNodeWithTag("source_repo_input").performScrollTo().performTextReplacement("example/fixture")
            androidx.test.espresso.Espresso.closeSoftKeyboard();rule.waitForIdle()
            rule.onNodeWithTag("setup_next").performClick()
            rule.onNodeWithTag("setup_task_CLASSIFICATION").performScrollTo().performClick()
            rule.onNodeWithTag("project_classes").performScrollTo().performTextReplacement("chat")
            androidx.test.espresso.Espresso.closeSoftKeyboard();rule.waitForIdle()
            rule.onNodeWithTag("class_search").performScrollTo().performTextReplacement("do")
            androidx.test.espresso.Espresso.closeSoftKeyboard();rule.waitForIdle()
            rule.onNodeWithTag("model_class_dog").performScrollTo().performClick()
            rule.onNodeWithTag("project_class_chat").performScrollTo().performClick()
            rule.onNodeWithTag("setup_next").performClick()
            rule.onNodeWithText(tr("1 classe prise en charge","1 class supported")).performScrollTo().assertIsDisplayed()
            rule.onNodeWithTag("setup_assistance").performScrollTo().assertIsOff().assertIsNotEnabled() // Contract exists, but no weights installed.
            rule.onNodeWithTag("setup_next").performClick()
            rule.onNodeWithTag("setup_prepare").performScrollTo().performClick() // Save only: no download or external write.
            rule.onNodeWithTag("setup_next").performClick()
            rule.waitUntil(10_000){!vm.isBusy.value && vm.currentScreen.value == Screen.Home}
            val saved=vm.db.projectDao().getProjectSync(created!!)!!
            assertEquals("dog",saved.classesCsv)
            assertEquals("CLASSIFICATION",saved.activeTasksCsv)
            assertEquals("example/fixture",saved.hfSourceRepo)
            assertEquals("",saved.hfDestRepo)
            assertFalse(ProjectSettings.read(saved).autoPreannotate)
            assertEquals(config.labels,StudioJson.moshi.adapter(ModelConfig::class.java).fromJson(saved.modelConfigJson!!)!!.labels)
        } finally {
            if (created != null) { act{selectProject(created!!)};act{deleteCurrentProject()} }
            if (vm.db.projectDao().getProjectSync(previous)!=null) act{selectProject(previous)}
            withContext(Dispatchers.Main){store.clear()}
        }
    }
}
