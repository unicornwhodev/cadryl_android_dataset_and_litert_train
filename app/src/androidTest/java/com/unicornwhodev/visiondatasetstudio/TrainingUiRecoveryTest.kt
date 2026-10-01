package com.unicornwhodev.visiondatasetstudio

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import com.unicornwhodev.visiondatasetstudio.core.i18n.tr
import com.unicornwhodev.visiondatasetstudio.domain.inference.ModelConfig
import com.unicornwhodev.visiondatasetstudio.domain.training.DeviceTrainingRun
import com.unicornwhodev.visiondatasetstudio.domain.training.OnDeviceTraining
import com.unicornwhodev.visiondatasetstudio.ui.*
import com.unicornwhodev.visiondatasetstudio.ui.theme.VisionDatasetStudioTheme
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class TrainingUiRecoveryTest {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()

    private fun withProject(test:suspend (MainViewModel,Long)->Unit)=runBlocking {
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
            act{createProject("UI recovery fixture")};created=vm.activeProjectId.value
            test(vm,created!!)
        } finally {
            created?.let { act{selectProject(it)};act{deleteCurrentProject()} }
            if(vm.db.projectDao().getProjectSync(previous)!=null)act{selectProject(previous)}
            withContext(Dispatchers.Main){store.clear()}
        }
    }

    @Test fun openingTrainingReadsTheDurableFailureWithoutRestarting()=withProject { vm,id ->
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        // Only a failed UI receipt is seeded; no optimizer execution is represented by this fixture.
        val directory=File(context.cacheDir,"training-ui-$id").apply{mkdirs()}
        val run=DeviceTrainingRun("ui-failure-$id",id,File(directory,"model.tflite").path,"0".repeat(64),
            ModelConfig(),emptyList(),phase="failed",completedSteps=111,totalSteps=111,
            sourceBatchNumber=1,error="Saved training failure fixture")
        OnDeviceTraining(context).write(run)
        assertNull(vm.trainingRun.value)
        withContext(Dispatchers.Main){vm.navigateTo(Screen.Training)}
        rule.setStudioTestContent { VisionDatasetStudioTheme(darkTheme=false){StudioRoot(vm)} }
        rule.waitUntil(10_000){vm.trainingRun.value?.id==run.id}
        assertEquals("failed",vm.trainingRun.value!!.phase)
        assertEquals(111,vm.trainingRun.value!!.completedSteps)
        rule.onNodeWithText("Saved training failure fixture").performScrollTo().assertIsDisplayed()
        directory.listFiles()?.forEach{it.delete()};directory.delete()
    }

    @Test fun guidedSetupKeepsAnExternallyUpdatedStorageBudget()=withProject { vm,id ->
        withContext(Dispatchers.Main){vm.navigateTo(Screen.Setup)}
        rule.setStudioTestContent { VisionDatasetStudioTheme(darkTheme=false){StudioRoot(vm)} }
        rule.onNodeWithText(tr("Sur Hugging Face","On Hugging Face")).performClick()
        rule.onNodeWithTag("source_repo_input").performScrollTo().performTextReplacement("example/fixture")
        androidx.test.espresso.Espresso.closeSoftKeyboard();rule.waitForIdle()
        val project=vm.db.projectDao().getProjectSync(id)!!
        vm.db.projectDao().saveProject(project.copy(diskBudgetMb=2500))
        rule.waitUntil(10_000){vm.projectFlow.value?.diskBudgetMb==2500L}
        repeat(3){rule.onNodeWithTag("setup_next").performClick()}
        rule.onNodeWithText(tr("Stockage : 2500 Mio","Storage: 2500 MiB")).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText(tr("Stockage avancé","Advanced storage")).performScrollTo().performClick()
        rule.onNodeWithText("2500").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("setup_prepare").performScrollTo().performClick()
        rule.onNodeWithTag("setup_next").performClick()
        rule.waitUntil(10_000){!vm.isBusy.value && vm.currentScreen.value==Screen.Home}
        assertEquals(2500L,vm.db.projectDao().getProjectSync(id)!!.diskBudgetMb)
    }
}
