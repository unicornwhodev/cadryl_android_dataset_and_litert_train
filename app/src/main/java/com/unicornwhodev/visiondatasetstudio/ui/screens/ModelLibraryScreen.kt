package com.unicornwhodev.visiondatasetstudio.ui.screens

import com.unicornwhodev.visiondatasetstudio.core.i18n.tr
import androidx.compose.ui.res.stringResource
import com.unicornwhodev.visiondatasetstudio.R

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.unicornwhodev.visiondatasetstudio.ui.MainViewModel
import com.unicornwhodev.visiondatasetstudio.ui.Screen
import com.unicornwhodev.visiondatasetstudio.ui.components.*
import com.unicornwhodev.visiondatasetstudio.domain.inference.ModelCapability
import com.unicornwhodev.visiondatasetstudio.domain.inference.QualificationStatus
import com.unicornwhodev.visiondatasetstudio.domain.inference.ModelPresets
import com.unicornwhodev.visiondatasetstudio.domain.inference.CommunityModelCatalog
import com.unicornwhodev.visiondatasetstudio.domain.inference.PublicModelCatalog

@Composable
fun ModelLibraryScreen(vm: MainViewModel) {
    val source by vm.catalogSource.collectAsState()
    var editSource by remember { mutableStateOf(false) }
    val remote by vm.communityModels.collectAsState()
    val warnings by vm.modelCatalogWarnings.collectAsState()
    val catalogLoading by vm.modelCatalogLoading.collectAsState()
    val local by vm.modelProfiles.collectAsState()
    val project by vm.projectFlow.collectAsState()
    val busy by vm.isBusy.collectAsState()
    var tab by rememberSaveable { mutableStateOf(0) }
    var url by rememberSaveable { mutableStateOf("") }
    var deleteId by remember { mutableStateOf<String?>(null) }
    var search by rememberSaveable { mutableStateOf("") }
    var compatibleOnly by rememberSaveable { mutableStateOf(false) }
    val activeTasks=project?.activeTasksCsv.orEmpty()
    val visibleRemote = remote.filter {
        (search.isBlank() || it.entry.title.contains(search, true) || it.entry.purpose.contains(search, true) || it.sourceRepo.contains(search,true)) &&
            (!compatibleOnly || it.entry.capabilities.supports(activeTasks))
    }
    val visibleLocal = local.filter { search.isBlank() || it.name.contains(search, true) }
    val visiblePublic = PublicModelCatalog.entries.filter {
        (search.isBlank() || it.title.contains(search,true) || it.purpose.contains(search,true)) &&
            (!compatibleOnly || when(it.family) {
                "ssd" -> activeTasks.split(',').any { task -> task.trim() in setOf("DETECTION","GROUNDING") }
                "classification" -> "CLASSIFICATION" in activeTasks.split(',').map(String::trim)
                else -> true
            })
    }
    val visiblePresets = ModelPresets.recommended(project?.activeTasksCsv.orEmpty()).filter {
        search.isBlank() || it.title.contains(search,true) || it.description.contains(search,true) || it.category.contains(search,true)
    }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    LaunchedEffect(tab) { listState.scrollToItem(0) }
    val weights = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(vm::importModel) }
    Scaffold(contentWindowInsets = WindowInsets(0), topBar = {
        WorkspaceTopBar(vm, stringResource(R.string.screen_models), stringResource(R.string.models_summary,local.size), actions = {
            IconButton(onClick = { vm.navigateTo(Screen.Training) }, enabled = !busy) { Icon(Icons.Default.ModelTraining, tr("Apprentissage sur cet appareil", "Training on this device"), Modifier.size(19.dp)) }
            IconButton(onClick = { editSource = true }, enabled = !busy) { Icon(Icons.Default.Storage, tr("Source HF supplémentaire", "Additional HF source"), Modifier.size(19.dp)) }
            IconButton(onClick = vm::refreshCommunityModelCatalog, enabled = !busy && !catalogLoading) { Icon(Icons.Default.Refresh, tr("Actualiser le catalogue", "Refresh catalog"), Modifier.size(19.dp)) }
        })
    }) { inset ->
        Column(Modifier.fillMaxSize().padding(inset)) {
            StudioTabs(listOf(tr("Catalogue","Catalog"),tr("Installés","Installed"),tr("Presets","Presets"),tr("Importer","Import")), tab, { tab = it }, Modifier.padding(horizontal = 16.dp))
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                LazyColumn(Modifier.widthIn(max = 1000.dp).fillMaxSize(), state = listState, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (tab != 3) item {
                        OutlinedTextField(search, { search = it }, label = { Text(tr("Rechercher un modèle", "Search models")) }, singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, null) }, modifier = Modifier.fillMaxWidth())
                        Text(tr("Choisissez un modèle pour votre objectif. Ses classes seront vérifiées une fois installé.", "Choose a model for your task. Its classes will be checked once installed."),
                            Modifier.padding(vertical = 8.dp), style = MaterialTheme.typography.bodySmall)
                        if(tab==0) {
                            FilterChip(selected=compatibleOnly,onClick={compatibleOnly=!compatibleOnly},
                                leadingIcon=if(compatibleOnly)({ Icon(Icons.Default.FilterAlt,null,Modifier.size(16.dp)) }) else null,
                                label={Text(tr("Compatibles avec le projet", "Compatible with project"))})
                        }
                        if(tab==0 && warnings.isNotEmpty()) {
                            StudioDetails(tr("Certaines sources HF n’ont pas pu être chargées :\n", "Some HF sources could not be loaded:\n") + warnings.joinToString("\n"),
                                style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error)
                        }
                        val emptySearch=when(tab){0->visibleRemote.isEmpty() && visiblePublic.isEmpty();1->visibleLocal.isEmpty();2->visiblePresets.isEmpty();else->false}
                        if (search.isNotBlank() && emptySearch) Text(tr("Aucun résultat ne correspond à cette recherche.", "No results match this search."))
                    }
                    if (tab == 0) {
                        item {
                            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.models_catalogue), Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(stringResource(R.string.models_downloadable,remote.count { it.installableNow }), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                        if(remote.isEmpty() && catalogLoading) item {
                            Column(Modifier.fillMaxWidth().padding(vertical=36.dp),horizontalAlignment=Alignment.CenterHorizontally,
                                verticalArrangement=Arrangement.spacedBy(12.dp)) {
                                CircularProgressIndicator()
                                Text(tr("Chargement des catalogues…", "Loading catalogs…"),style=MaterialTheme.typography.bodyMedium)
                                Text(tr("Inspection de FireViewer, du catalogue communautaire et de la source HF supplémentaire.",
                                    "Inspecting FireViewer, the community catalog, and the additional HF source."),
                                    style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else if(remote.isEmpty()) item {
                            EmptyWorkspace(stringResource(R.string.models_empty_title), stringResource(R.string.models_empty_body), Icons.Default.Memory,
                                if (!busy) stringResource(R.string.models_explore) else null, vm::refreshCommunityModelCatalog)
                        }
                        items(visibleRemote.size, key = { visibleRemote[it].sourceRepo + "@" + visibleRemote[it].repoSha + ":" + visibleRemote[it].entry.id }) { index ->
                            val item = visibleRemote[index]
                            var showInfo by remember { mutableStateOf(false) }
                            val state = stringResource(when {
                                !item.available -> R.string.model_state_upcoming
                                !item.installableNow -> R.string.model_state_unsupported
                                item.entry.adapterStatus == "contract" -> R.string.model_state_contract
                                item.entry.adapterStatus == "heatmap" -> R.string.model_state_heatmap
                                item.entry.adapterStatus == "embedding" -> R.string.model_state_embedding
                                item.entry.adapterStatus == "bundle" -> R.string.model_state_bundle
                                item.entry.adapterStatus == "rfdetr" -> R.string.model_state_detection
                                else -> R.string.model_state_inspection
                            })
                            val capabilityNames=mutableListOf<String>();for(capability in item.entry.capabilities.values)capabilityNames+=when(capability) {
                                ModelCapability.DETECTION->stringResource(R.string.cap_detection);ModelCapability.POINTING->stringResource(R.string.cap_pointing);ModelCapability.SEGMENTATION->stringResource(R.string.cap_segmentation)
                                ModelCapability.CLASSIFICATION->stringResource(R.string.cap_classification);ModelCapability.CAPTIONING->stringResource(R.string.cap_captioning);ModelCapability.EMBEDDING->stringResource(R.string.cap_embedding)
                                ModelCapability.SIMILARITY->stringResource(R.string.cap_similarity);ModelCapability.INTERACTIVE_SEGMENTATION->stringResource(R.string.cap_interactive_segmentation);ModelCapability.TRAINING->stringResource(R.string.cap_training)
                                ModelCapability.INSPECTION_ONLY->stringResource(R.string.cap_inspection)
                                ModelCapability.VQA->stringResource(R.string.cap_vqa)
                                ModelCapability.COUNTING->stringResource(R.string.cap_counting)
                                ModelCapability.GROUNDING->stringResource(R.string.cap_grounding)
                            };val capabilityText=capabilityNames.joinToString(" · ") + " · " + when(item.entry.capabilities.qualification) {
                                QualificationStatus.QUALIFIED->stringResource(R.string.qualification_qualified);QualificationStatus.PARTIALLY_QUALIFIED->stringResource(R.string.qualification_partial)
                                QualificationStatus.INFERENCE_ONLY->stringResource(R.string.qualification_inference);QualificationStatus.TRAINING_QUALIFIED->stringResource(R.string.qualification_training)
                                QualificationStatus.FAILED->stringResource(R.string.qualification_failed);QualificationStatus.UNTESTED->stringResource(R.string.qualification_untested)
                            }
                            Column {
                                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Memory, null, Modifier.size(18.dp), tint = if(item.installableNow) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(item.entry.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        val sourceName=when(item.sourceRepo) {
                                            CommunityModelCatalog.fireviewerRepoId -> "FireViewer"
                                            CommunityModelCatalog.repoId -> tr("Conversions communautaires","Community conversions")
                                            else -> item.sourceRepo
                                        }
                                        val bytes=item.files.filter { it.path.endsWith(".tflite",true) }.sumOf { it.size }
                                        val sizeText=when {
                                            bytes>=1024L*1024*1024 -> "%.1f Gio".format(java.util.Locale.ROOT,bytes/1073741824.0)
                                            bytes>=1024L*1024 -> "%.0f Mio".format(java.util.Locale.ROOT,bytes/1048576.0)
                                            else -> ""
                                        }
                                        Text(sourceName + if(sizeText.isNotBlank()) " · $sizeText" else "",style=MaterialTheme.typography.labelSmall,
                                            color=if(item.sourceRepo==CommunityModelCatalog.fireviewerRepoId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(item.entry.purpose, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        Text("$state  ·  ${item.entry.upstreamLicense}", style = MaterialTheme.typography.labelSmall, color = if(item.installableNow) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(capabilityText,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2)
                                    }
                                    IconButton(onClick = { showInfo = true }) { Icon(Icons.Default.Info, stringResource(R.string.models_details,item.entry.title), Modifier.size(17.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                                    if(item.installableNow) IconButton(onClick = { vm.downloadCommunityModel(item.entry.id,item.sourceRepo,item.repoSha) }, enabled = !busy) {
                                        Icon(Icons.Default.Download, stringResource(R.string.models_install,item.entry.title), Modifier.size(19.dp), tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f))
                            }
                            if(showInfo) AlertDialog(onDismissRequest = { showInfo = false }, title = { Text(item.entry.title) },
                                text = { Text((item.entry.purpose)+"\n\n$capabilityText\n"+stringResource(R.string.model_files,item.entry.expectedFiles.size)+"\n\n"+(item.note)) },
                                confirmButton = { TextButton(onClick = { showInfo = false }) { Text(stringResource(R.string.action_close)) } })
                        }
                        if(visiblePublic.isNotEmpty()) {
                            item {
                                Spacer(Modifier.height(14.dp))
                                Row(Modifier.fillMaxWidth().padding(vertical=10.dp),verticalAlignment=Alignment.CenterVertically) {
                                    Text(tr("Modèles publics avec métadonnées","Public metadata models"),Modifier.weight(1f),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(tr("TensorFlow examples","TensorFlow examples"),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
                            }
                            items(visiblePublic.size,key={ "public:"+visiblePublic[it].id }) { index ->
                                val item=visiblePublic[index]
                                Row(Modifier.fillMaxWidth().padding(vertical=10.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) {
                                    Icon(Icons.Default.Memory,null,Modifier.size(18.dp),tint=MaterialTheme.colorScheme.secondary)
                                    Column(Modifier.weight(1f)) {
                                        Text(item.title,style=MaterialTheme.typography.titleSmall)
                                        Text(item.purpose,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(tr("Métadonnées et tenseurs vérifiés à l’installation","Metadata and tensors checked at install"),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    StudioAction(tr("Installer","Install"),{vm.downloadCatalogModel(item.id)},enabled=!busy)
                                }
                                HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant.copy(alpha=.5f))
                            }
                        }
                    } else if(tab == 1) {
                        if(local.isEmpty()) item { EmptyWorkspace(stringResource(R.string.models_none_installed), stringResource(R.string.models_import_compatible), Icons.Default.Memory, tr("Importer","Import"), { tab = 3 }) }
                        items(visibleLocal.size, key = { visibleLocal[it].id }) { index ->
                            val profile = visibleLocal[index]
                            val active = if(profile.modelPath.isNotBlank()) project?.modelPath == profile.modelPath
                                else project?.modelPath.isNullOrBlank() && project?.modelConfigJson == profile.configJson
                            Column {
                                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Memory, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(profile.name, style = MaterialTheme.typography.titleSmall)
                                        Text(profile.tensorReport.lineSequence().firstOrNull().orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        val config=remember(profile.configJson){runCatching{com.unicornwhodev.visiondatasetstudio.data.json.StudioJson.moshi.adapter(com.unicornwhodev.visiondatasetstudio.domain.inference.ModelConfig::class.java).fromJson(profile.configJson)}.getOrNull()}
                                        config?.let { cfg -> Text(com.unicornwhodev.visiondatasetstudio.domain.inference.ModelClassCompatibility.inspect(cfg, project?.activeTasksCsv.orEmpty(), project?.classesCsv.orEmpty()).summary,
                                            style=MaterialTheme.typography.bodySmall, color=MaterialTheme.colorScheme.primary) }
                                        config?.let { cfg -> val names=mutableListOf<String>();for(cap in com.unicornwhodev.visiondatasetstudio.domain.inference.ModelCapabilities.fromConfig(cfg).values)names+=when(cap){
                                            ModelCapability.DETECTION->stringResource(R.string.cap_detection);ModelCapability.POINTING->stringResource(R.string.cap_pointing);ModelCapability.SEGMENTATION->stringResource(R.string.cap_segmentation);ModelCapability.CLASSIFICATION->stringResource(R.string.cap_classification);ModelCapability.CAPTIONING->stringResource(R.string.cap_captioning);ModelCapability.VQA->stringResource(R.string.cap_vqa);ModelCapability.COUNTING->stringResource(R.string.cap_counting);ModelCapability.GROUNDING->stringResource(R.string.cap_grounding);ModelCapability.EMBEDDING->stringResource(R.string.cap_embedding);ModelCapability.SIMILARITY->stringResource(R.string.cap_similarity);ModelCapability.INTERACTIVE_SEGMENTATION->stringResource(R.string.cap_interactive_segmentation);ModelCapability.TRAINING->stringResource(R.string.cap_training);ModelCapability.INSPECTION_ONLY->stringResource(R.string.cap_inspection)};Text(names.joinToString(" · "),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
                                        if(active) Text(stringResource(R.string.models_active), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                    if(!active) StudioAction(stringResource(R.string.models_use), { vm.selectModelProfile(profile.id) }, enabled = !busy)
                                    else StudioAction(tr("Configurer", "Setup"), { vm.navigateTo(Screen.ModelSettings) }, enabled = !busy)
                                    IconButton(onClick = { deleteId = profile.id }, enabled = !busy) { Icon(Icons.Default.DeleteOutline, stringResource(R.string.models_delete,profile.name), Modifier.size(18.dp)) }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                        if(!project?.modelPath.isNullOrBlank() || project?.modelConfigJson != null) item {
                            TextButton(onClick = vm::detachModel, enabled = !busy) { Text(stringResource(R.string.models_detach)) }
                        }
                    } else if(tab == 2) {
                        item {
                            StudioSection(tr("Presets de configuration","Configuration presets"),
                                tr("Un preset configure le contrat de base mais ne prétend jamais reconnaître automatiquement un fichier. Vérifiez toujours le prétraitement, les sorties et les classes du modèle.",
                                    "A preset configures a base contract but never claims to automatically recognize a file. Always verify preprocessing, outputs and model classes."),
                                Icons.Default.Tune) {
                                Text(tr("${visiblePresets.size} gabarit(s) · les plus proches des tâches du projet sont affichés en premier.",
                                    "${visiblePresets.size} template(s) · presets closest to the project tasks are shown first."),style=MaterialTheme.typography.bodySmall)
                            }
                        }
                        items(visiblePresets.size,key={visiblePresets[it].id}) { index ->
                            val preset=visiblePresets[index]
                            Column {
                                Row(Modifier.fillMaxWidth().padding(vertical=10.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) {
                                    Icon(Icons.Default.Tune,null,Modifier.size(18.dp),tint=MaterialTheme.colorScheme.secondary)
                                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)) {
                                        Text(preset.title,style=MaterialTheme.typography.titleSmall)
                                        Text(preset.category+" · "+preset.task,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)
                                        Text(preset.description,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    val bundleQuery=when(preset.id) {
                                        "tinyclip" -> "TinyCLIP"
                                        "efficientvit_sam" -> "EfficientViT"
                                        "florence2" -> "Florence"
                                        else -> null
                                    }
                                    if(bundleQuery!=null) StudioAction(tr("Voir le bundle","View bundle"),{
                                        search=bundleQuery;compatibleOnly=false;tab=0
                                    },enabled=!busy)
                                    else StudioAction(tr("Appliquer","Apply"),{vm.applyModelPreset(preset.id)},enabled=!busy)
                                }
                                HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant.copy(alpha=.5f))
                            }
                        }
                    } else {
                        item {
                            Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.InsertDriveFile, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Column {
                                        Text(stringResource(R.string.models_litert_file), style = MaterialTheme.typography.titleMedium)
                                        Text(stringResource(R.string.models_device_weights), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                StudioAction(stringResource(R.string.models_choose_file), { weights.launch(arrayOf("application/octet-stream", "*/*")) }, icon = Icons.Default.UploadFile, primary = true, enabled = !busy)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(Modifier.height(16.dp))
                        }
                        item { StudioDisclosure(stringResource(R.string.models_from_url), Icons.Default.Link) {
                            OutlinedTextField(url, { url = it }, label = { Text(stringResource(R.string.models_url_label)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            StudioAction(stringResource(R.string.models_import_url), { vm.importModelUrl(url) }, enabled = !busy && url.startsWith("https://"))
                        } }
                        item { TextButton(onClick = { vm.navigateTo(Screen.ModelAdvanced) }, enabled = !busy) { Text(stringResource(R.string.models_advanced), style = MaterialTheme.typography.labelMedium) } }
                    }
                }
            }
        }
    }
    if (editSource) {
        var repo by remember { mutableStateOf(source.repository) }
        var revision by remember { mutableStateOf(source.revision) }
        var folder by remember { mutableStateOf(source.folder) }
        AlertDialog(onDismissRequest = { editSource = false }, title = { Text(tr("Source HF supplémentaire", "Additional HF source")) }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(tr("FireViewer et le catalogue communautaire restent toujours chargés. Ce dépôt s’ajoute aux sources standard.",
                    "FireViewer and the community catalog are always loaded. This repository is added to the standard sources."),
                    style=MaterialTheme.typography.bodySmall)
                OutlinedTextField(repo, { repo = it }, label = { Text(stringResource(R.string.models_repo)) }, singleLine = true)
                OutlinedTextField(revision, { revision = it }, label = { Text(stringResource(R.string.models_revision)) }, singleLine = true)
                OutlinedTextField(folder, { folder = it }, label = { Text(stringResource(R.string.models_folder)) }, singleLine = true)
                Text(stringResource(R.string.models_private_access), style = MaterialTheme.typography.bodySmall)
                if(source.repository!=CommunityModelCatalog.repoId || source.revision!="main" || source.folder!="models") {
                    TextButton(onClick={
                        vm.setModelCatalog(CommunityModelCatalog.repoId,"main","models")
                        editSource=false
                    }) { Text(tr("Retirer la source supplémentaire", "Remove additional source")) }
                }
            }
        }, confirmButton = { TextButton(onClick = { vm.setModelCatalog(repo, revision, folder); editSource = false }) { Text(stringResource(R.string.action_open)) } },
            dismissButton = { TextButton(onClick = { editSource = false }) { Text(stringResource(R.string.action_cancel)) } })
    }
    deleteId?.let { id -> AlertDialog(onDismissRequest = { deleteId = null }, title = { Text(stringResource(R.string.models_delete_title)) },
        text = { Text(stringResource(R.string.models_delete_body)) },
        confirmButton = { TextButton(onClick = { deleteId = null; vm.removeModelProfile(id) }) { Text(stringResource(R.string.action_delete)) } },
        dismissButton = { TextButton(onClick = { deleteId = null }) { Text(stringResource(R.string.action_cancel)) } }) }
}
