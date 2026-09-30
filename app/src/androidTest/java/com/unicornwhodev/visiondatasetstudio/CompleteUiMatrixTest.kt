package com.unicornwhodev.visiondatasetstudio

import android.app.Application
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.*
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import com.unicornwhodev.visiondatasetstudio.data.model.*
import com.unicornwhodev.visiondatasetstudio.ui.*
import com.unicornwhodev.visiondatasetstudio.ui.theme.VisionDatasetStudioTheme
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.Locale

/** Opt-in: all routes, two languages, both themes and 100/200% Compose text.
 * Real orientation/window size is supplied by the device runner, not a fake screen configuration.
 * Screenshots and semantics establish a reproducible review matrix, not human/Play certification.
 */
class CompleteUiMatrixTest {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()

    @Test fun everyRouteAndOfflineLegalDocumentIsReviewable()=runBlocking {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val requested=requireNotNull(InstrumentationRegistry.getArguments().getString("uiMatrixOrientation"))
        require(requested in listOf("portrait","landscape"))
        val originalOrientation=rule.activity.requestedOrientation
        rule.runOnUiThread { rule.activity.requestedOrientation=if(requested=="landscape")
            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
        rule.waitUntil(15_000) {
            rule.activity.resources.configuration.orientation==if(requested=="landscape") Configuration.ORIENTATION_LANDSCAPE else Configuration.ORIENTATION_PORTRAIT
        }
        val app=instrumentation.targetContext.applicationContext as Application
        val store=ViewModelStore();val previousLocale=Locale.getDefault()
        val vm=withContext(Dispatchers.Main) { ViewModelProvider(store,ViewModelProvider.AndroidViewModelFactory.getInstance(app))[MainViewModel::class.java] }
        val originalProject=vm.preferenceStore.activeProjectId;val oldPreferences=vm.preferences.value
        var created:Long?=null
        val documents=listOf("privacy","notices")
        suspend fun act(action:MainViewModel.()->Unit) {
            withContext(Dispatchers.Main) { vm.action() }
            withTimeout(30_000) { while(vm.isBusy.value || vm.editorBusy.value)delay(25) }
            withTimeout(10_000) { while(vm.projectFlow.value?.id!=vm.activeProjectId.value)delay(25) }
        }
        val orientation=if(app.resources.configuration.orientation==Configuration.ORIENTATION_LANDSCAPE) "landscape" else "portrait"
        val case=requireNotNull(InstrumentationRegistry.getArguments().getString("uiMatrixCase"))
        require(case.matches(Regex("[a-f0-9]{12}")))
        // A test-only provider transfers descriptors across Android's separate app UIDs.
        fun artifact(name:String)=requireNotNull(app.contentResolver.openOutputStream(android.net.Uri.parse(
            "content://${instrumentation.context.packageName}.qa-evidence/final01-ui/$case/$orientation/$name"),"w"))
        fun settle() {
            rule.mainClock.advanceTimeBy(600)
            rule.waitForIdle()
            // Compose's test clock does not drive WindowManager fades or SurfaceFlinger readback.
            Thread.sleep(400)
        }
        fun capture(key:String) {
            settle()
            val roots=rule.onAllNodes(isRoot(),useUnmergedTree=true)
            val tree=roots.fetchSemanticsNodes().indices.joinToString("\n") { roots[it].printToString() }
                .replace(Regex("\\bhf_[A-Za-z0-9]{16,}\\b"),"[redacted HF credential]")
            assertTrue("Empty semantics: $key",tree.isNotBlank())
            artifact("$key.txt").use { it.write(tree.toByteArray(Charsets.UTF_8)) }
            val image=instrumentation.uiAutomation.takeScreenshot() ?: error("Screenshot unavailable")
            try { artifact("$key.png").use { assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,it)) } }
            finally { image.recycle() }
        }
        try {
            act { createProject("QA final01 UI matrix") };created=vm.activeProjectId.value
            val image=vm.storageManager.getImageFile("matrix-$created","png")
            val bitmap=Bitmap.createBitmap(48,32,Bitmap.Config.ARGB_8888).apply { eraseColor(Color.CYAN) }
            try { image.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) } } finally { bitmap.recycle() }
            val sampleId="matrix-$created"
            val sample=SampleEntity(sampleId,created!!,1,"UI synthetic",0,sourceFileUrl=null,localImagePath=image.path,
                imageWidth=48,imageHeight=32,acquisitionStatus="AVAILABLE",annotationStatus="PENDING",syncStatus="NOT_EXPORTED")
            vm.db.batchDao().insertOrReplace(BatchEntity(created!!,1,"IN_PROGRESS",totalCases=1))
            vm.db.sampleDao().insertSamples(listOf(sample))
            val routes=listOf(Screen.Home,Screen.Setup,Screen.BatchGrid,Screen.Publication,Screen.QualityDashboard,Screen.Preferences,
                Screen.Controls,Screen.SourceSettings,Screen.TransferSettings,Screen.ModelSettings,Screen.ModelAdvanced,
                Screen.Models,Screen.Training,Screen.Similarity,Screen.Workflow,Screen.AnnotationEditor(sampleId))
            val routeNames=listOf("Home","Setup","BatchGrid","Publication","QualityDashboard","Preferences","Controls",
                "SourceSettings","TransferSettings","ModelSettings","ModelAdvanced","Models","Training","Similarity","Workflow","AnnotationEditor")
            for(language in listOf("fr","en")) for(dark in listOf(true,false)) for(scale in listOf(1f,2f)) {
                Locale.setDefault(Locale.forLanguageTag(language))
                val config=Configuration(app.resources.configuration).apply { setLocale(Locale.forLanguageTag(language));fontScale=scale }
                val localized=app.createConfigurationContext(config)
                val density=Density(app.resources.displayMetrics.density,scale)
                rule.setStudioTestContent {
                    val registry=requireNotNull(LocalActivityResultRegistryOwner.current)
                    CompositionLocalProvider(LocalActivityResultRegistryOwner provides registry,LocalContext provides localized,
                        LocalConfiguration provides config,LocalDensity provides density) {
                        VisionDatasetStudioTheme(darkTheme=dark) { StudioRoot(vm) }
                    }
                }
                val prefix="$language-${if(dark) "dark" else "light"}-${scale.toInt()*100}"
                for((routeIndex,route) in routes.withIndex()) {
                    if(route is Screen.AnnotationEditor) act { openSampleInEditor(sampleId) } else act { navigateTo(route) }
                    rule.waitUntil(10_000) { rule.onAllNodesWithTag("route_${routeNames[routeIndex]}",useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty() }
                    capture("$prefix-${routeNames[routeIndex]}")
                }
                act { navigateTo(Screen.Preferences) }
                settle()
                for(document in documents) {
                    rule.onNodeWithTag("legal_$document").performScrollTo().assertIsDisplayed()
                    settle()
                    rule.onNodeWithTag("legal_$document").performClick()
                    settle()
                    // Yield the instrumentation coroutine while the real IO read completes.
                    withTimeout(15_000) {
                        while(rule.onAllNodesWithTag("legal_document",useUnmergedTree=true).fetchSemanticsNodes().isEmpty()) {
                            rule.mainClock.advanceTimeByFrame()
                            delay(25)
                        }
                    }
                    capture("$prefix-legal-$document")
                    rule.onNodeWithText(if(language=="fr") "Fermer" else "Close").performClick()
                    settle()
                }
            }
            artifact("matrix-status.json").use { it.write("""{"routes":16,"languages":["fr","en"],"themes":["dark","light"],"compose_font_scales":[1,2],"orientation":"$orientation","monetization_mode":"disabled","captures":${8*(16+documents.size)},"human_certified":false,"billing_live_tested":false}""".toByteArray(Charsets.UTF_8)) }
        } catch(error:Throwable) {
            runCatching { capture("failure") }
            throw error
        } finally {
            created?.let { if(vm.db.projectDao().getProjectSync(it)!=null) { act { selectProject(it) };act { deleteCurrentProject() } } }
            if(vm.db.projectDao().getProjectSync(originalProject)!=null)act { selectProject(originalProject) }
            vm.updatePreferences(oldPreferences)
            withContext(Dispatchers.Main) { store.clear() }
            Locale.setDefault(previousLocale)
            rule.runOnUiThread { rule.activity.requestedOrientation=originalOrientation }
        }
    }
}
