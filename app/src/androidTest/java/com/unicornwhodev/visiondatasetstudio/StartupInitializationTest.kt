package com.unicornwhodev.visiondatasetstudio

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import com.unicornwhodev.visiondatasetstudio.data.db.AppDatabase
import com.unicornwhodev.visiondatasetstudio.data.model.BatchEntity
import com.unicornwhodev.visiondatasetstudio.data.model.ProjectEntity
import com.unicornwhodev.visiondatasetstudio.data.preferences.StudioPreferenceStore
import com.unicornwhodev.visiondatasetstudio.ui.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Restore a real saved QA project with warm Room queries; never reset installed data. */
class StartupInitializationTest {
    @Test fun cachedProjectsRestoreTheirBatchAcrossRepeatedViewModelCreation()=runBlocking {
        val app=InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application
        val db=AppDatabase.getInstance(app)
        val preferences=StudioPreferenceStore(app)
        val originalProject=preferences.activeProjectId
        val originalBatch=preferences.lastBatch
        var qaProject=System.currentTimeMillis()
        while(db.projectDao().getProjectSync(qaProject)!=null)qaProject++
        db.projectDao().saveProject(ProjectEntity(id=qaProject,name="QA startup restore",classesCsv="object",activeTasksCsv="DETECTION"))
        db.batchDao().insertOrReplace(BatchEntity(projectId=qaProject,batchNumber=7,status="IN_PROGRESS",totalCases=0))
        try {
            preferences.activeProjectId=qaProject
            preferences.lastBatch=7
            repeat(8) {
                val store=ViewModelStore()
                try {
                    val vm=withContext(Dispatchers.Main.immediate) {
                        ViewModelProvider(store,ViewModelProvider.AndroidViewModelFactory.getInstance(app))[MainViewModel::class.java]
                    }
                    withTimeout(15_000) {
                        while(vm.projectFlow.value?.id!=qaProject || vm.activeBatchNumber.value!=7)delay(20)
                    }
                    assertEquals(qaProject,vm.activeProjectId.value)
                    assertEquals(7,vm.activeBatchNumber.value)
                    assertNull(vm.benchmarkReport.value)
                    // Exercise the same synchronous path again after constructor completion.
                    withContext(Dispatchers.Main.immediate) { vm.loadBatch(7) }
                    assertNull(vm.benchmarkReport.value)
                } finally { withContext(Dispatchers.Main.immediate) { store.clear() } }
            }
        } finally {
            preferences.activeProjectId=originalProject
            preferences.lastBatch=originalBatch
            db.batchDao().deleteProjectBatches(qaProject)
            db.projectDao().deleteProject(qaProject)
        }
    }
}
