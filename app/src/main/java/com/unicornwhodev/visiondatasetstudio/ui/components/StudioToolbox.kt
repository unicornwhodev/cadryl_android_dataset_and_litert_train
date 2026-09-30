package com.unicornwhodev.visiondatasetstudio.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import com.unicornwhodev.visiondatasetstudio.core.i18n.tr
import com.unicornwhodev.visiondatasetstudio.ui.Screen
import com.unicornwhodev.visiondatasetstudio.ui.icons.CadrylIcons

val LocalStudioToolbox = staticCompositionLocalOf<(() -> Unit)?> { null }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioToolbox(projectName: String, current: Screen, enabled: Boolean, onDismiss: () -> Unit, navigate: (Screen) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface, modifier = Modifier.testTag("studio_toolbox")) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(tr("Outils du projet", "Project tools"), style = MaterialTheme.typography.titleLarge)
                    Text(projectName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDismiss,modifier=Modifier.testTag("toolbox_close")) { Icon(CadrylIcons.Close, tr("Fermer", "Close")) }
            }
            Spacer(Modifier.height(12.dp))
            fun open(screen: Screen) { onDismiss(); navigate(screen) }
            ToolboxRow(tr("Atelier", "Studio"), CadrylIcons.SpaceDashboard, enabled, "nav_Home",current==Screen.Home) { open(Screen.Home) }
            ToolboxRow(tr("Images du lot", "Batch images"), CadrylIcons.GridView, enabled, "nav_BatchGrid",current==Screen.BatchGrid) { open(Screen.BatchGrid) }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            ToolboxRow(tr("Modèles", "Models"), CadrylIcons.Memory, enabled, "nav_Models",current==Screen.Models) { open(Screen.Models) }
            ToolboxRow(tr("Contrat du modèle", "Model contract"), CadrylIcons.Tune, enabled, "nav_ModelSettings",current==Screen.ModelSettings) { open(Screen.ModelSettings) }
            ToolboxRow(tr("Apprentissage", "Training"), CadrylIcons.ModelTraining, enabled, "nav_Training",current==Screen.Training) { open(Screen.Training) }
            ToolboxRow(tr("Exporter", "Export"), CadrylIcons.IosShare, enabled, "nav_Publication",current==Screen.Publication) { open(Screen.Publication) }
            ToolboxRow(tr("Qualité du projet", "Project quality"), CadrylIcons.Insights, enabled, "nav_QualityDashboard",current==Screen.QualityDashboard) { open(Screen.QualityDashboard) }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            ToolboxRow(tr("Mes projets", "My projects"), CadrylIcons.FolderOpen, enabled, "nav_Controls",current==Screen.Controls) { open(Screen.Controls) }
            ToolboxRow(tr("Importer et configurer", "Import and configure"), CadrylIcons.UploadFile, enabled, "nav_Setup",current==Screen.Setup) { open(Screen.Setup) }
            ToolboxRow(tr("Source et lots", "Source and batches"), CadrylIcons.Storage, enabled, "nav_SourceSettings",current==Screen.SourceSettings) { open(Screen.SourceSettings) }
            ToolboxRow(tr("Destination et stockage", "Destination and storage"), CadrylIcons.CloudUpload, enabled, "nav_TransferSettings",current==Screen.TransferSettings) { open(Screen.TransferSettings) }
            ToolboxRow(tr("Workflows et agent", "Workflows and agent"), CadrylIcons.AccountTree, enabled, "nav_Workflow",current==Screen.Workflow) { open(Screen.Workflow) }
            ToolboxRow(tr("Réglages", "Settings"), CadrylIcons.Tune, enabled, "home_settings",current==Screen.Preferences) { open(Screen.Preferences) }
        }
    }
}

@Composable
private fun ToolboxRow(label: String, icon: ImageVector, enabled: Boolean, tag: String, selected: Boolean=false, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().testTag(tag).clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .semantics {this.selected=selected}.heightIn(min = 48.dp).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(icon, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
        Icon(if(selected) CadrylIcons.Check else CadrylIcons.ChevronRight, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
