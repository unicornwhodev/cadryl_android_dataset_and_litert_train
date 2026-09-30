package com.unicornwhodev.visiondatasetstudio.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import com.unicornwhodev.visiondatasetstudio.ui.components.*
import androidx.compose.ui.platform.testTag
import com.unicornwhodev.visiondatasetstudio.ui.screens.*


private fun Screen.routeTag():String=when(this) {
    is Screen.Home->"Home"; is Screen.Setup->"Setup"; is Screen.BatchGrid->"BatchGrid"
    is Screen.Publication->"Publication"; is Screen.QualityDashboard->"QualityDashboard"
    is Screen.Preferences->"Preferences"; is Screen.Controls->"Controls"
    is Screen.SourceSettings->"SourceSettings"; is Screen.TransferSettings->"TransferSettings"
    is Screen.ModelSettings->"ModelSettings"; is Screen.ModelAdvanced->"ModelAdvanced"
    is Screen.Models->"Models"; is Screen.Training->"Training"; is Screen.Similarity->"Similarity"
    is Screen.Workflow->"Workflow"; is Screen.AnnotationEditor->"AnnotationEditor"
}

@Composable
fun StudioRoot(viewModel: MainViewModel) {
    val screen by viewModel.currentScreen.collectAsState()
    val progress by viewModel.operationProgress.collectAsState()
    val busy by viewModel.isBusy.collectAsState()
    val projectId by viewModel.activeProjectId.collectAsState()
    val activeProject by viewModel.projectFlow.collectAsState()
    val editorBusy by viewModel.editorBusy.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val holder = rememberSaveableStateHolder()
    var toolboxOpen by remember { mutableStateOf(false) }
    BackHandler(enabled = screen !is Screen.Home) { viewModel.back() }
    CompositionLocalProvider(LocalStudioGuidance provides preferences.showGuidance,
        LocalStudioToolbox provides { toolboxOpen = true }) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout))) {
            Box(Modifier.weight(1f).testTag("route_${screen.routeTag()}")) {
                val screenKey = when (val s = screen) {
                    is Screen.AnnotationEditor -> "editor"
                    else -> s.javaClass.simpleName
                }
                if(activeProject?.id != projectId) CircularProgressIndicator() else StudioRouteMotion("$projectId:$screenKey") {
                    holder.SaveableStateProvider("$projectId:$screenKey") {
                        when (val s = screen) {
                            is Screen.Home -> StudioHomeScreen(viewModel)
                            is Screen.Setup -> SetupScreen(viewModel)
                            is Screen.BatchGrid -> BatchGridScreen(viewModel)
                            is Screen.AnnotationEditor -> AnnotationEditorScreen(s.sampleId, viewModel)
                            is Screen.Models -> ModelLibraryScreen(viewModel)
                            is Screen.Training -> TrainingScreen(viewModel)
                            is Screen.Similarity -> SimilarityScreen(viewModel)
                            is Screen.Workflow -> WorkflowScreen(viewModel)
                            is Screen.Publication -> PublicationScreen(viewModel)
                            is Screen.QualityDashboard -> QualityDashboardScreen(viewModel)
                            is Screen.Preferences -> StudioPreferencesScreen(viewModel)
                            is Screen.Controls -> StudioControlsScreen(viewModel)
                            is Screen.SourceSettings -> StudioControlsScreen(viewModel, 1)
                            is Screen.TransferSettings -> StudioControlsScreen(viewModel, 2)
                            is Screen.ModelSettings -> ModelConfigurationScreen(viewModel)
                            is Screen.ModelAdvanced -> StudioControlsScreen(viewModel, 3)
                        }
                    }
                }
            }
            OperationBanner(progress, busy, viewModel::clearOperationProgress)
        }
        if(toolboxOpen) StudioToolbox(activeProject?.name.orEmpty(), screen, !busy && !editorBusy,
            { toolboxOpen = false }, viewModel::navigateTo)
    }
}
