package com.unicornwhodev.visiondatasetstudio.ui.screens

import com.unicornwhodev.visiondatasetstudio.core.i18n.tr

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.unicornwhodev.visiondatasetstudio.core.workflow.StudioWorkflow
import com.unicornwhodev.visiondatasetstudio.data.model.SampleEntity
import com.unicornwhodev.visiondatasetstudio.data.preferences.ProjectSettings
import com.unicornwhodev.visiondatasetstudio.ui.*
import com.unicornwhodev.visiondatasetstudio.ui.components.*
import java.io.File

@Composable
fun StudioHomeScreen(viewModel: MainViewModel) {
    val tutorialVisible by viewModel.tutorialVisible.collectAsState()
    val tutorialSession by viewModel.tutorialSession.collectAsState()
    val tutorialOnLaunch by viewModel.tutorialOnLaunch.collectAsState()
    val tutorialSaveError by viewModel.tutorialSaveError.collectAsState()
    val project by viewModel.projectFlow.collectAsState()
    val samples by viewModel.batchSamples.collectAsState()
    val batch by viewModel.activeBatchNumber.collectAsState()
    val busy by viewModel.isBusy.collectAsState()
    val policy = project?.let(ProjectSettings::read)
    val configured = !project?.hfSourceRepo.isNullOrBlank() || (policy?.sourceMode == "LOCAL_INDEX" && policy.sourceIndexReady)
    val reviewed = samples.count { it.annotationStatus in setOf("VALIDATED", "REJECTED") }
    val completion by animateFloatAsState(if(samples.isEmpty()) 0f else reviewed.toFloat() / samples.size, label = "batch review")
    var previewId by rememberSaveable(project?.id, batch) { mutableStateOf<String?>(null) }
    val preview = samples.firstOrNull { it.sampleId == previewId }
        ?: samples.firstOrNull { StudioWorkflow.isPending(it.annotationStatus) && it.localImagePath != null }
        ?: samples.firstOrNull()
    val previewEditable = preview?.let { StudioWorkflow.canEdit(it.acquisitionStatus, it.syncStatus, it.localImagePath != null) } == true
    var moreMenu by remember { mutableStateOf(false) }
    val openPreview: () -> Unit = {
        preview?.takeIf { previewEditable && !busy }?.let { viewModel.openSampleInEditor(it.sampleId) }
    }
    val primary: @Composable () -> Unit = {
        val label=when { previewEditable -> tr("Reprendre l’annotation","Continue annotating")
            samples.isNotEmpty() -> tr("Ouvrir le lot","Open batch")
            configured -> tr("Préparer le lot","Prepare batch")
            else -> tr("Configurer mon projet","Set up my project") }
        StudioAction(label,onClick={when {
            previewEditable -> openPreview()
            samples.isNotEmpty() -> viewModel.navigateTo(Screen.BatchGrid)
            configured -> viewModel.fetchAndPrepareBatch(batch)
            else -> viewModel.navigateTo(Screen.Setup)
        }},icon=if(samples.isEmpty()) Icons.Default.Add else Icons.Default.ArrowForward,
            primary=true,enabled=!busy && project!=null,modifier=Modifier.fillMaxWidth().testTag("home_primary"))
    }
    Scaffold(contentWindowInsets = WindowInsets(0), topBar = {
        WorkspaceTopBar(viewModel, project?.name ?: tr("Atelier", "Studio"), tr("Cadryl  /  Lot ${batch.toString().padStart(2, '0')}", "Cadryl  /  Batch ${batch.toString().padStart(2, '0')}"), actions = {
            IconButton(onClick = { viewModel.navigateTo(Screen.Controls) }, enabled = !busy, modifier = Modifier.testTag("controls_shortcut")) {
                Icon(Icons.Default.FolderOpen, tr("Gérer les projets", "Manage projects"), Modifier.size(19.dp))
            }
            Box {
                IconButton(onClick = { moreMenu = true },modifier=Modifier.testTag("home_more")) { Icon(Icons.Default.MoreVert, tr("Plus d’options", "More options")) }
                DropdownMenu(moreMenu, { moreMenu = false }) {
                    DropdownMenuItem(text = { Text(tr("Réglages", "Settings")) },modifier=Modifier.testTag("home_settings"), onClick = { moreMenu = false; viewModel.navigateTo(Screen.Preferences) })
                    DropdownMenuItem(text = { Text(tr("Workflows et agent", "Workflows and agent")) }, enabled = !busy, onClick = { moreMenu = false; viewModel.navigateTo(Screen.Workflow) })
                    DropdownMenuItem(text = { Text(tr("Configuration guidée", "Guided setup")) }, enabled = !busy, onClick = { moreMenu = false; viewModel.navigateTo(Screen.Setup) })
                    DropdownMenuItem(text = { Text(tr("Source et import avancé", "Source & advanced import")) }, enabled = !busy, onClick = { moreMenu = false; viewModel.navigateTo(Screen.SourceSettings) })
                }
            }
        })
    }) { inset ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(inset),contentAlignment=Alignment.TopCenter) {
            val wide=maxWidth>=760.dp
            LazyColumn(Modifier.widthIn(max=1120.dp).fillMaxSize(),contentPadding=PaddingValues(20.dp),
                verticalArrangement=Arrangement.spacedBy(20.dp)) {
                item {
                    Surface(shape=RoundedCornerShape(24.dp),color=MaterialTheme.colorScheme.surface,
                        border=BorderStroke(1.dp,MaterialTheme.colorScheme.primary.copy(alpha=.22f))) {
                        Column(Modifier.background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer.copy(alpha=.5f),
                            MaterialTheme.colorScheme.surface,MaterialTheme.colorScheme.secondaryContainer.copy(alpha=.25f))))
                            .padding(22.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                                StatusPill(tr("Lot ${batch.toString().padStart(2,'0')}","Batch ${batch.toString().padStart(2,'0')}"),Icons.Default.Layers)
                                Spacer(Modifier.weight(1f))
                                if(wide) Box(Modifier.widthIn(max=300.dp)) { primary() } else Text(if(busy) tr("En cours","Working") else tr("Sur cet appareil","On this device"),
                                    style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(if(samples.isEmpty()) tr("Vos images. Votre dataset.","Your images. Your dataset.")
                                else if(reviewed==samples.size) tr("Votre lot est relu","Your batch is reviewed")
                                else tr("Continuez votre lot","Continue your batch"),style=MaterialTheme.typography.headlineMedium)
                            Text(if(samples.isEmpty()) tr("Importez, annotez, exportez. L’IA reste facultative.","Import, annotate, export. AI is optional.")
                                else tr("$reviewed / ${samples.size} images relues · ${samples.size-reviewed} à traiter",
                                    "$reviewed / ${samples.size} images reviewed · ${samples.size-reviewed} remaining"),
                                style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            if(!wide) primary()
                        }
                    }
                }
                if(tutorialVisible) item(key="tutorial") {
                    key(tutorialSession) {
                        FirstLaunchTutorial(!tutorialOnLaunch,tutorialSaveError,viewModel::finishTutorial,viewModel::postponeTutorial)
                    }
                }
                if(samples.isNotEmpty()) item {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(20.dp)) {
                        StudioSection(tr("Prochaine image","Next image"),icon=Icons.Default.Edit,modifier=Modifier.weight(1f)) {
                            WorkspacePreview(preview,Modifier.fillMaxWidth().height(if(wide) 220.dp else 180.dp),previewEditable && !busy,openPreview)
                            PreviewCaption(preview)
                            LinearProgressIndicator(progress={completion},modifier=Modifier.fillMaxWidth().height(5.dp),trackColor=MaterialTheme.colorScheme.outlineVariant)
                            TextButton(onClick={viewModel.navigateTo(Screen.BatchGrid)},modifier=Modifier.fillMaxWidth()) {
                                Text(tr("Voir les images du lot","View batch images"));Spacer(Modifier.width(8.dp));Icon(Icons.Default.ArrowForward,null,Modifier.size(18.dp))
                            }
                        }
                        if(wide) StudioSection(tr("File de travail","Work queue"),icon=Icons.Default.Checklist,modifier=Modifier.weight(.7f)) {
                            samples.take(5).forEachIndexed { index,sample -> QueueRow(sample,index,sample.sampleId==preview?.sampleId) { previewId=sample.sampleId } }
                        }
                    }
                }
                item {
                    ProjectTool(tr("Modèles et inférence","Models and inference"),tr("Catalogue, import et réglages","Catalog, import and settings"),
                        Icons.Default.Memory,!busy) { viewModel.navigateTo(Screen.Models) }
                    Spacer(Modifier.height(10.dp))
                    ProjectTool(tr("Entraîner une copie","Train a copy"),tr("Sur les annotations relues","From reviewed annotations"),
                        Icons.Default.ModelTraining,!busy,tag="home_training") { viewModel.navigateTo(Screen.Training) }
                }
                if(samples.isEmpty()) item {
                    StudioDisclosure(tr("Comment ça fonctionne ?","How does it work?"),icon=Icons.Default.Checklist) {
                        HomeGuideStep("1",tr("Importer vos images","Import your images"),tr("Dossier local ou dataset Hugging Face.","Local folder or Hugging Face dataset."))
                        HomeGuideStep("2",tr("Annoter et relire","Annotate and review"),tr("À la main ou avec un modèle compatible.","By hand or with a compatible model."))
                        HomeGuideStep("3",tr("Exporter votre travail","Export your work"),tr("Choisissez les formats et vérifiez la copie.","Choose formats and verify the copy."))
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeGuideStep(number: String, title: String, help: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(vertical = 6.dp)) {
        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(8.dp)) {
            Text(number, Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        Column { Text(title, style = MaterialTheme.typography.titleSmall); Text(help, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun WorkspacePreview(sample: SampleEntity?, modifier: Modifier, editable: Boolean, onClick: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerLowest)
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .6f), RoundedCornerShape(16.dp))
        .clickable(enabled = editable, role = Role.Button, onClickLabel = tr("Annoter cette image", "Annotate this image"), onClick = onClick), contentAlignment = Alignment.Center) {
        if(sample?.localImagePath != null) AsyncImage(model = File(sample.localImagePath), contentDescription = tr("Aperçu ${sample.assetId}", "Preview ${sample.assetId}"), contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(8.dp))
        else Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.CropFree, null, Modifier.size(30.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(if(sample == null) tr("Votre espace de travail", "Your workspace") else tr("Aperçu indisponible", "Preview unavailable"), style = MaterialTheme.typography.titleSmall)
            Text(if(sample == null) tr("Importez un dossier ou un dataset.", "Import a folder or dataset.") else tr("Ouvrez le lot pour vérifier l’acquisition.", "Open the batch to check acquisition."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PreviewCaption(sample: SampleEntity?) {
    Row(Modifier.fillMaxWidth().heightIn(min = 36.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(sample?.assetId ?: tr("Aucune image sélectionnée", "No image selected"), Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if(sample?.imageWidth != null && sample.imageHeight != null) Text("${sample.imageWidth} × ${sample.imageHeight}", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun QueueRow(sample: SampleEntity, index: Int, selected: Boolean, onClick: () -> Unit) {
    val background by animateColorAsState(if(selected) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.background, label = "preview selection")
    val status = when(sample.annotationStatus) { "VALIDATED" -> tr("Traitée manuellement", "Manually reviewed"); "REJECTED" -> tr("Rejetée", "Rejected"); "IN_PROGRESS" -> tr("Traité manuellement · à valider", "Manually handled · needs approval"); "DEFERRED" -> tr("À revoir", "To review"); "DRAFTS_AVAILABLE" -> tr("Brouillon importé · à relire", "Imported draft · needs review"); "PROPOSALS_AVAILABLE" -> tr("Suggestions IA", "AI suggestions"); else -> tr("À traiter", "To process") }
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(background).clickable(role = Role.Tab, onClick = onClick)
        .semantics { this.selected = selected }.padding(horizontal = 8.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text((index+1).toString().padStart(2,'0'), style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(Modifier.size(44.dp, 36.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            if(sample.localImagePath != null) AsyncImage(File(sample.localImagePath), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            else Icon(Icons.Default.Image, null, Modifier.size(16.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(sample.assetId, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(status, style = MaterialTheme.typography.labelSmall, color = if(selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ProjectTool(title:String,detail:String,icon:ImageVector,enabled:Boolean,tag:String="home_tool",onClick:()->Unit) {
    Surface(onClick=onClick,enabled=enabled,shape=RoundedCornerShape(18.dp),color=MaterialTheme.colorScheme.surface,
        border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant.copy(alpha=.45f)),modifier=Modifier.fillMaxWidth().testTag(tag)) {
        Row(Modifier.padding(16.dp),horizontalArrangement=Arrangement.spacedBy(14.dp),verticalAlignment=Alignment.CenterVertically) {
            Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.secondaryContainer) {
                Icon(icon,null,Modifier.padding(12.dp).size(22.dp),tint=MaterialTheme.colorScheme.secondary)
            }
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Text(title,style=MaterialTheme.typography.titleSmall)
                Text(detail,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight,null,Modifier.size(20.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
