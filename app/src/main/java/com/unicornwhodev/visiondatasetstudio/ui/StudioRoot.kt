package com.unicornwhodev.visiondatasetstudio.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.unicornwhodev.visiondatasetstudio.ui.components.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import com.unicornwhodev.visiondatasetstudio.R
import androidx.compose.ui.semantics.*
import com.unicornwhodev.visiondatasetstudio.ui.screens.*

private data class StudioDestination(val screen: Screen, val label: String, val icon: ImageVector, val key: String)

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
    val destinations = listOf(
        StudioDestination(Screen.Home, stringResource(R.string.nav_studio), Icons.Default.SpaceDashboard, "Home"),
        StudioDestination(Screen.BatchGrid, stringResource(R.string.nav_batch), Icons.Default.GridView, "BatchGrid"),
        StudioDestination(Screen.Models, stringResource(R.string.nav_models), Icons.Default.Memory, "Models"),
        StudioDestination(Screen.Publication, stringResource(R.string.nav_export), Icons.Default.IosShare, "Publication"),
        StudioDestination(Screen.QualityDashboard, stringResource(R.string.nav_quality), Icons.Default.Insights, "QualityDashboard")
    )
    BackHandler(enabled = screen !is Screen.Home) { viewModel.back() }
    CompositionLocalProvider(LocalStudioGuidance provides preferences.showGuidance) {
    BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout))) {
        val editor = screen is Screen.AnnotationEditor
        val showNavigation = !editor && screen !is Screen.Preferences && screen !is Screen.Setup
        val rail = maxWidth >= 720.dp && showNavigation
        Row(Modifier.fillMaxSize()) {
            if (rail) {
                Column(Modifier.width(104.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surfaceContainerLowest), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.fillMaxWidth().height(72.dp), contentAlignment = Alignment.Center) {
                        Icon(painterResource(R.drawable.ic_cadryl), stringResource(R.string.app_name), Modifier.size(26.dp), tint = Color.Unspecified)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f))
                    Spacer(Modifier.height(12.dp))
                    destinations.forEach { item ->
                        StudioRailItem(item.label, item.icon, screen.workspace() == item.screen, !editorBusy && !busy,
                            Modifier.testTag("nav_${item.key}")) { viewModel.navigateTo(item.screen) }
                    }
                    Spacer(Modifier.weight(1f))
                    StudioRailItem(stringResource(R.string.nav_settings), Icons.Default.Tune, false, !editorBusy && !busy) { viewModel.navigateTo(Screen.Preferences) }
                    Spacer(Modifier.height(8.dp))
                }
                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f))
            }
            Column(Modifier.weight(1f)) {
                Box(Modifier.weight(1f).testTag("route_${screen.routeTag()}")) {
                    val screenKey = when (val s = screen) {
                        is Screen.AnnotationEditor -> "editor" // One remembered workspace; case-specific drafts use sampleId keys.
                        else -> s.javaClass.simpleName
                    }
                    if(activeProject?.id != projectId) CircularProgressIndicator() else StudioRouteMotion("$projectId:$screenKey") { holder.SaveableStateProvider("$projectId:$screenKey") {
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
                    } }
                }
                OperationBanner(progress, busy, viewModel::clearOperationProgress)
                if (showNavigation && !rail) Surface(color=MaterialTheme.colorScheme.surface,shadowElevation=8.dp) {
                    Row(Modifier.fillMaxWidth().padding(horizontal=8.dp,vertical=8.dp)) {
                        destinations.forEach { item ->
                            val selected=screen.workspace()==item.screen
                            val tint by animateColorAsState(if(selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,label="navigation")
                            Column(Modifier.weight(1f).testTag("nav_${item.key}").clip(RoundedCornerShape(16.dp))
                                .clickable(enabled=!editorBusy && !busy,role=Role.Tab) { viewModel.navigateTo(item.screen) }
                                .semantics { this.selected=selected;contentDescription=item.label }
                                .heightIn(min=64.dp).padding(vertical=4.dp),horizontalAlignment=Alignment.CenterHorizontally,
                                verticalArrangement=Arrangement.spacedBy(5.dp)) {
                                Box(Modifier.width(48.dp).height(30.dp).clip(RoundedCornerShape(50))
                                    .background(if(selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent),contentAlignment=Alignment.Center) {
                                    Icon(item.icon,null,Modifier.size(22.dp),tint=tint)
                                }
                                Text(item.label,style=MaterialTheme.typography.labelSmall,color=tint,maxLines=1,
                                    overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            }
                        }
                    }
                }

            }
        }
    }
    }
}

@Composable
private fun StudioRailItem(label: String, icon: ImageVector, selected: Boolean, enabled: Boolean,
                           modifier: Modifier = Modifier, onClick: () -> Unit) {
    val accent by animateColorAsState(if(selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, label = "rail selection")
    Box(modifier.fillMaxWidth().heightIn(min = 76.dp).clickable(enabled = enabled, role = Role.Tab, onClick = onClick)
        .semantics { this.selected = selected }, contentAlignment = Alignment.Center) {
        if (selected) Box(Modifier.align(Alignment.CenterStart).width(2.dp).height(22.dp).background(accent))
        Column(Modifier.padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(if(selected) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent), contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(22.dp), tint = accent)
            }
            Text(label, style = MaterialTheme.typography.labelSmall, color = accent)
        }
    }
}
