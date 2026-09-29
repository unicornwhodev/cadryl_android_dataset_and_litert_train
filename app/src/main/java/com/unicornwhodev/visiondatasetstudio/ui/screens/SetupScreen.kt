package com.unicornwhodev.visiondatasetstudio.ui.screens

import com.unicornwhodev.visiondatasetstudio.core.i18n.tr
import androidx.compose.ui.res.stringResource
import com.unicornwhodev.visiondatasetstudio.R

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.unicornwhodev.visiondatasetstudio.core.workflow.*
import com.unicornwhodev.visiondatasetstudio.ui.MainViewModel
import com.unicornwhodev.visiondatasetstudio.ui.Screen
import com.unicornwhodev.visiondatasetstudio.data.preferences.ProjectSettings
import com.unicornwhodev.visiondatasetstudio.data.json.StudioJson
import com.unicornwhodev.visiondatasetstudio.domain.inference.*
import com.unicornwhodev.visiondatasetstudio.ui.components.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SetupScreen(viewModel: MainViewModel) {
    val project by viewModel.projectFlow.collectAsState()
    val auth by viewModel.authStatus.collectAsState()
    val inspection by viewModel.sourceInspection.collectAsState()
    val busy by viewModel.isBusy.collectAsState()
    val batches by viewModel.batches.collectAsState()
    val models by viewModel.modelProfiles.collectAsState()
    val p = project ?: return
    val modelConfig = remember(p.modelConfigJson) { runCatching { p.modelConfigJson?.let {
        StudioJson.moshi.adapter(ModelConfig::class.java).fromJson(it)
    } }.getOrNull() }
    val policy = ProjectSettings.read(p)
    val scroll = rememberScrollState()
    var step by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(step) { scroll.scrollTo(0) }
    var name by rememberSaveable { mutableStateOf(p.name) }
    var source by rememberSaveable { mutableStateOf(p.hfSourceRepo) }
    var config by rememberSaveable { mutableStateOf(p.sourceConfig) }
    var split by rememberSaveable { mutableStateOf(p.sourceSplit) }
    var imageColumn by rememberSaveable { mutableStateOf(p.imageColumn) }
    var classes by rememberSaveable { mutableStateOf(p.classesCsv) }
    var tasksCsv by rememberSaveable { mutableStateOf(p.activeTasksCsv) }
    val tasks = StudioWorkflow.parseTasks(tasksCsv)
    val classesOk = !ProjectVocabulary.requiredFor(tasks) || ProjectVocabulary.parse(classes).isNotEmpty()
    var budget by rememberSaveable { mutableStateOf(p.diskBudgetMb.toString()) }
    var prepare by rememberSaveable { mutableStateOf(batches.isEmpty()) }
    var advanced by rememberSaveable { mutableStateOf(false) }
    var showToken by remember { mutableStateOf(false) }
    var token by remember { mutableStateOf("") } // Never place a credential in saved instance state.
    var sourceKind by rememberSaveable { mutableStateOf(if (policy.sourceMode=="LOCAL_INDEX") "local" else "hf") }
    var assistance by rememberSaveable { mutableStateOf(policy.autoPreannotate && modelConfig != null) }
    val chooseFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri -> if (uri != null) viewModel.importSourceFolder(uri) }
    LaunchedEffect(p.id, p.settingsJson, p.hfSourceRepo) {
        val savedPolicy = com.unicornwhodev.visiondatasetstudio.data.preferences.ProjectSettings.read(p)
        sourceKind = if(savedPolicy.sourceMode=="LOCAL_INDEX") "local" else "hf"
        if(sourceKind=="hf") source=p.hfSourceRepo
    }
    LaunchedEffect(inspection) {
        if (batches.isEmpty() && inspection.isInspected && inspection.repoId == StudioWorkflow.normalizeRepo(source)) {
            config = inspection.selectedConfig
            split = inspection.selectedSplit
            if (inspection.selectedImageColumn.isNotBlank()) imageColumn = inspection.selectedImageColumn
        }
    }
    val indexedSourceReady=when {
        sourceKind=="local" -> policy.sourceMode=="LOCAL_INDEX" && policy.sourceIndexReady
        sourceKind=="hf" -> policy.sourceMode=="HF_MANIFEST" && policy.sourceIndexReady && source==p.hfSourceRepo
        else -> false
    }
    val sourceOk = if (sourceKind == "local") indexedSourceReady else StudioWorkflow.normalizeRepo(source) != null
    val coverage = modelConfig?.let { ModelClassCompatibility.inspect(it, tasksCsv, classes) }
    val modelLabels = coverage?.available.orEmpty()
    val setupModels=remember(models,tasksCsv,classes) {
        models.sortedByDescending { profile ->
            runCatching {
                StudioJson.moshi.adapter(ModelConfig::class.java).fromJson(profile.configJson)
                    ?.let { ModelClassCompatibility.inspect(it,tasksCsv,classes).canAssist } == true
            }.getOrDefault(false)
        }
    }
    val canAutomate = coverage?.canAssist == true && coverage.kind != ModelVocabularyKind.INTERACTIVE &&
        (!p.modelPath.isNullOrBlank() || modelConfig?.runtime == "local_http")
    val hfPreviewReady = inspection.isInspected && inspection.repoId == StudioWorkflow.normalizeRepo(source) &&
        inspection.selectedConfig == config && inspection.selectedSplit == split &&
        imageColumn in com.unicornwhodev.visiondatasetstudio.data.source.SourceImageColumn.candidates(inspection.availableColumns, inspection.previewRows)
    val viewerExpectedRows=inspection.splits.firstOrNull { it.config==config && it.split==split }?.numRows
    val viewerCoverageContract=viewerExpectedRows!=null && policy.filterExpression.isBlank() && policy.orderBy.isBlank()
    val sourceReadyToPrepare = when(sourceKind) {
        "local" -> indexedSourceReady
        else -> indexedSourceReady || (hfPreviewReady && (!inspection.viewerPartial || viewerCoverageContract || policy.allowPartialViewer))
    }
    val frozen = batches.any { it.status in setOf("PREPARED", "PUBLISHING", "PUBLISHED", "CONFLICT", "PURGING") }
    val steps = listOf(tr("Images", "Images"), tr("Annotations", "Annotations"), tr("Modèle IA", "AI model"), tr("Vérification", "Review"))
    Scaffold(contentWindowInsets = WindowInsets(0), modifier = Modifier.imePadding(), topBar = {
        StudioTopBar(stringResource(R.string.screen_setup), tr("Étape ${step + 1} sur 4 · ${steps[step]}", "Step ${step + 1} of 4 · ${steps[step]}"), onBack = viewModel::back)
    }, bottomBar = {
        Surface(shadowElevation = 3.dp) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (step > 0) OutlinedButton(onClick = { step-- }, enabled = !busy, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.common_previous)) }
                Button(onClick = {
                    if (step < 3) step++ else viewModel.saveSetup(name, if (sourceKind == "local") "" else source, p.hfDestRepo, config, split, imageColumn, classes,
                        budget.toLongOrNull() ?: 500L, tasks, prepare, assistance && canAutomate,
                        when {
                            sourceKind=="local" -> "LOCAL_INDEX"
                            policy.sourceMode=="HF_MANIFEST" && policy.sourceIndexReady && source==p.hfSourceRepo -> "HF_MANIFEST"
                            else -> "HF_VIEWER"
                        })
                }, enabled = !busy && !frozen && when(step) {
                    0 -> sourceOk
                    1 -> classesOk
                    2 -> true
                    else -> sourceOk && classesOk && (!prepare || sourceReadyToPrepare) && (budget.toLongOrNull() ?: 0L) in 128L..65536L
                },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("setup_next")) {
                    Text(if (step < 3) tr("Continuer", "Continue") else if (prepare) stringResource(R.string.setup_start) else tr("Enregistrer", "Save"))
                }
            }
        }
    }) { inset ->
        Box(Modifier.fillMaxSize().padding(inset), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 820.dp).fillMaxWidth().verticalScroll(scroll).padding(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                LinearProgressIndicator(progress = { (step + 1) / 4f }, modifier = Modifier.fillMaxWidth())
                if (frozen) {
                    Text(tr("Un transfert attend d’être terminé. Vos réglages sont conservés.", "A transfer needs to be completed. Your settings are preserved."))
                    TextButton(onClick = { viewModel.navigateTo(Screen.Publication) }) { Text(tr("Reprendre l’export", "Resume export")) }
                }
                when(step) {
                    0 -> {
                        Text(tr("D’où viennent vos images ?", "Where are your images?"), style = MaterialTheme.typography.headlineSmall)
                        OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.setup_project_name)) }, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !busy)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = sourceKind == "local", onClick = { sourceKind = "local" }, enabled = !busy && batches.isEmpty(),
                                label = { Text(tr("Sur cet appareil", "On this device")) })
                            FilterChip(selected = sourceKind == "hf", onClick = { sourceKind = "hf" }, enabled = !busy && batches.isEmpty(),
                                label = { Text(tr("Sur Hugging Face", "On Hugging Face")) })
                        }
                        if (sourceKind == "local") {
                        StudioSection(stringResource(R.string.setup_local_images), stringResource(R.string.setup_index_help), Icons.Default.FolderOpen) {
                            val localPolicy = com.unicornwhodev.visiondatasetstudio.data.preferences.ProjectSettings.read(p)
                            if (localPolicy.sourceMode == "LOCAL_INDEX" && localPolicy.sourceIndexReady) Text(localPolicy.localSourceLabel.ifBlank { stringResource(R.string.setup_folder_indexed) }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            OutlinedButton(onClick = { chooseFolder.launch(null) }, enabled = !busy && batches.isEmpty(), shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                                Icon(Icons.Default.FolderOpen, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.setup_choose_folder))
                            }
                        }
                        } else {
                        StudioSection(stringResource(R.string.setup_hf_dataset), icon = Icons.Default.CloudDownload) {
                            if(policy.sourceMode=="HF_MANIFEST" && policy.sourceIndexReady && source==p.hfSourceRepo) {
                                StatusPill(tr("Manifeste HF indexé · révision épinglée", "HF manifest indexed · revision pinned"),Icons.Default.CheckCircleOutline)
                                Text(tr("Ce projet utilise le manifeste JSONL configuré dans Source avancée. L’assistant conserve ce mode tant que la source n’est pas remplacée.",
                                    "This project uses the JSONL manifest configured in Advanced source. Guided setup keeps this mode until the source is replaced."),
                                    style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                                TextButton(onClick={viewModel.navigateTo(Screen.SourceSettings)},enabled=!busy) {
                                    Text(tr("Ouvrir les réglages du manifeste", "Open manifest settings"))
                                }
                            }
                            OutlinedTextField(source, { source = it }, label = { Text(stringResource(R.string.setup_dataset_link)) }, placeholder = { Text(tr("organisation/dataset", "organization/dataset")) },
                                isError = source.isNotBlank() && !sourceOk, supportingText = { Text(stringResource(R.string.setup_dataset_url_help)) },
                                enabled = !busy, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("source_repo_input"))
                            FilledTonalButton(onClick = { viewModel.inspectSourceDataset(source, config, split) }, enabled = sourceOk && source.isNotBlank() && !busy, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Search, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.setup_inspect_source))
                            }
                            if (inspection.isInspected && inspection.repoId == StudioWorkflow.normalizeRepo(source)) {
                                val previewUsable=com.unicornwhodev.visiondatasetstudio.data.source.SourceImageColumn.usableCount(inspection.selectedImageColumn,inspection.previewRows)
                                StatusPill(tr("$previewUsable/${inspection.previewRows.size} lignes d’aperçu avec image exploitable",
                                    "$previewUsable/${inspection.previewRows.size} preview rows have a usable image"), Icons.Default.CheckCircleOutline)
                                if(inspection.viewerPartial) {
                                    val partialMessage=when {
                                        viewerCoverageContract -> tr("HF signale une vue partielle, mais /splits annonce $viewerExpectedRows lignes. Cadryl exigera une pagination continue jusqu’à ce total et interrompra l’import au premier trou.",
                                            "HF reports a partial view, but /splits announces $viewerExpectedRows rows. Cadryl will require continuous pagination to that total and stop at the first gap.")
                                        policy.allowPartialViewer -> tr("Vue HF partielle autorisée : seuls les cas exposés par le Viewer seront parcourus. Ce mode ne constitue pas un import exhaustif du corpus.",
                                            "Partial HF Viewer enabled: only samples exposed by the Viewer will be traversed. This is not an exhaustive dataset import.")
                                        else -> tr("Le Viewer HF indique une couverture partielle sans total vérifiable. L’import est bloqué par défaut pour ne pas présenter cette vue comme le corpus complet.",
                                            "The HF Viewer reports partial coverage without a verifiable total. Import is blocked by default so this view is not presented as the complete dataset.")
                                    }
                                    Text(partialMessage, style=MaterialTheme.typography.bodySmall,
                                        color=if(viewerCoverageContract) MaterialTheme.colorScheme.primary else if(policy.allowPartialViewer) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error)
                                    if(!viewerCoverageContract) TextButton(onClick={viewModel.navigateTo(Screen.SourceSettings)},enabled=!busy) {
                                        Text(tr("Réglages de source", "Source settings"))
                                    }
                                }
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    inspection.splits.forEach { item ->
                                        FilterChip(selected = config == item.config && split == item.split, onClick = {
                                            config = item.config; split = item.split
                                            viewModel.inspectSourceDataset(source, item.config, item.split)
                                        }, label = { Text("${item.config} / ${item.split}") })
                                    }
                                }
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    com.unicornwhodev.visiondatasetstudio.data.source.SourceImageColumn.candidates(inspection.availableColumns, inspection.previewRows).forEach { column -> FilterChip(selected = imageColumn == column, onClick = { imageColumn = column }, label = { Text(column) }) }
                                }
                                Text(stringResource(R.string.setup_verify_image_column,imageColumn), style = MaterialTheme.typography.bodySmall)
                            }
                            if (inspection.repoId == StudioWorkflow.normalizeRepo(source) && inspection.error != null) Text(inspection.error.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            if (inspection.isInspected && inspection.repoId == StudioWorkflow.normalizeRepo(source) && inspection.selectedImageColumn.isBlank()) Text(tr("Choisissez la colonne image. Pour plusieurs images par ligne ou une source sans Viewer, utilisez Source > manifeste JSONL.", "Choose the image column. For multiple images per row or a source without a Viewer, use Source > JSONL manifest."), style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = { advanced = !advanced }) { Text(if (advanced) stringResource(R.string.setup_collapse) else stringResource(R.string.setup_advanced)) }
                            if (advanced) {
                                OutlinedTextField(config, { config = it }, label = { Text(stringResource(R.string.setup_hf_config)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(split, { split = it }, label = { Text(stringResource(R.string.setup_source_split)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(imageColumn, { imageColumn = it }, label = { Text(stringResource(R.string.setup_image_column)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                TextButton(onClick = { viewModel.navigateTo(Screen.SourceSettings) }) { Text(tr("Autres sources et manifeste JSONL", "Other sources & JSONL manifest")) }
                            }
                            if (batches.isNotEmpty()) StudioDetails(stringResource(R.string.setup_provenance_locked), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        StudioDisclosure(stringResource(R.string.setup_hf_login), Icons.Default.Key) {
                            Text(if (auth?.isValid == true) stringResource(R.string.setup_connected,auth?.username.orEmpty()) else stringResource(R.string.setup_public_read), style = MaterialTheme.typography.bodyMedium)
                            OutlinedTextField(token, { token = it }, label = { Text(stringResource(R.string.setup_hf_token)) }, placeholder = { Text("hf_…") }, singleLine = true,
                                visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth().testTag("token_input"),
                                trailingIcon = { IconButton(onClick = { showToken = !showToken }) { Icon(if (showToken) Icons.Default.VisibilityOff else Icons.Default.Visibility, tr("Afficher ou masquer le jeton", "Show or hide token")) } })
                            Button(onClick = { viewModel.saveToken(token); token = "" }, enabled = token.isNotBlank() && !busy) { Text(stringResource(R.string.setup_connect)) }
                            if (auth?.error != null) Text(auth?.error ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(stringResource(R.string.setup_token_security), style = MaterialTheme.typography.bodySmall)
                        }
                        }
                        if (!sourceOk) Text(if (sourceKind == "local") tr("Choisissez un dossier pour continuer.", "Choose a folder to continue.") else tr("Indiquez le lien de votre dataset pour continuer.", "Enter your dataset link to continue."),
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    1 -> {
                        Text(tr("Que voulez-vous annoter ?", "What do you want to annotate?"), style = MaterialTheme.typography.headlineSmall)
                        listOf(
                            Triple(StudioTask.DETECTION, tr("Entourer des objets", "Draw boxes around objects"), tr("Une boîte et un nom pour chaque objet.", "A box and a name for each object.")),
                            Triple(StudioTask.CLASSIFICATION, tr("Classer des images", "Classify images"), tr("Une ou plusieurs étiquettes par image.", "One or more labels per image.")),
                            Triple(StudioTask.CAPTIONING, tr("Décrire des images", "Describe images"), tr("Une description écrite pour chaque image.", "A written description for each image."))
                        ).forEach { (task, title, help) ->
                            Surface(color = if (tasks == setOf(task)) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                                shape = MaterialTheme.shapes.medium) {
                                Row(Modifier.fillMaxWidth().selectable(selected = tasks == setOf(task), enabled = !busy, role = Role.RadioButton,
                                    onClick = { tasksCsv = task.name }).padding(12.dp).testTag("setup_task_${task.name}"), verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = tasks == setOf(task), onClick = null)
                                    Column(Modifier.padding(start = 12.dp)) { Text(title, style = MaterialTheme.typography.titleSmall); Text(help, style = MaterialTheme.typography.bodySmall) }
                                }
                            }
                        }
                        StudioDisclosure(tr("Autres outils et combinaisons", "More tools & combinations"), Icons.Default.Tune) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StudioTask.entries.forEach { task -> FilterChip(selected = task in tasks, enabled = !busy, onClick = { tasksCsv = StudioWorkflow.tasksCsv(StudioWorkflow.toggleTask(tasks, task)) }, label = { Text(task.title) }) }
                            }
                        }
                        Text(tr("Outils choisis : ", "Selected tools: ") + tasks.joinToString { it.title }, style = MaterialTheme.typography.bodySmall)
                        if (ProjectVocabulary.requiredFor(tasks)) {
                            StudioSection(tr("Quelles classes rechercher ?", "Which classes should you look for?"),
                                tr("Une classe est le nom d’un objet ou d’une catégorie.", "A class is the name of an object or category."), Icons.Default.Label) {
                                ClassVocabularyEditor(classes, { classes = it }, !busy && !frozen, modelLabels)
                                if (!classesOk) Text(tr("Ajoutez une classe ou choisissez-la dans la liste du modèle pour continuer.", "Add a class or choose one from the model's list to continue."), style=MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    2 -> {
                        Text(tr("Souhaitez-vous une aide IA ?", "Would you like AI assistance?"), style = MaterialTheme.typography.headlineSmall)
                        Text(tr("Cette étape est facultative. Sans modèle, Cadryl fonctionne entièrement en annotation manuelle.",
                            "This step is optional. Without a model, Cadryl works entirely with manual annotation."), style=MaterialTheme.typography.bodyMedium)

                        StudioSection(tr("Mode de traitement", "Processing mode"), icon=Icons.Default.AutoAwesome) {
                            Row(Modifier.fillMaxWidth().selectable(selected=!assistance || !canAutomate,onClick={assistance=false},enabled=!busy).padding(vertical=6.dp),
                                verticalAlignment=Alignment.CenterVertically) {
                                RadioButton(selected=!assistance || !canAutomate,onClick=null)
                                Column(Modifier.padding(start=10.dp)) {
                                    Text(tr("Annotation manuelle", "Manual annotation"),style=MaterialTheme.typography.titleSmall)
                                    Text(tr("Aucune proposition automatique. Vous gardez tous les outils de correction.",
                                        "No automatic suggestions. All correction tools remain available."),style=MaterialTheme.typography.bodySmall)
                                }
                            }
                            if(modelConfig!=null) {
                                HorizontalDivider()
                                Row(Modifier.fillMaxWidth().selectable(selected=assistance && canAutomate,onClick={ assistance=true },enabled=!busy && canAutomate).padding(vertical=6.dp),
                                    verticalAlignment=Alignment.CenterVertically) {
                                    RadioButton(selected=assistance && canAutomate,onClick=null,enabled=canAutomate)
                                    Column(Modifier.padding(start=10.dp).weight(1f)) {
                                        Text(tr("Propositions IA à relire", "AI suggestions to review"),style=MaterialTheme.typography.titleSmall)
                                        Text(coverage?.summary ?: tr("Compatibilité à vérifier", "Compatibility needs review"),style=MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }

                        if(models.isNotEmpty()) {
                            StudioSection(tr("Modèles installés", "Installed models"), tr("Choisissez le modèle utilisé pour les propositions.", "Choose the model used for suggestions."), Icons.Default.Memory) {
                                setupModels.take(4).forEach { profile ->
                                    val cfg=remember(profile.configJson){runCatching{StudioJson.moshi.adapter(ModelConfig::class.java).fromJson(profile.configJson)}.getOrNull()}
                                    val selectedProfile=if(profile.modelPath.isNotBlank()) p.modelPath==profile.modelPath && p.modelConfigJson==profile.configJson
                                        else p.modelPath.isNullOrBlank() && p.modelConfigJson==profile.configJson
                                    Surface(shape=MaterialTheme.shapes.medium,color=if(selectedProfile)MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow) {
                                        Row(Modifier.fillMaxWidth().clickable(enabled=!busy && !frozen){viewModel.selectModelProfile(profile.id)}.padding(12.dp),
                                            verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                                            RadioButton(selected=selectedProfile,onClick=null)
                                            Column(Modifier.weight(1f)) {
                                                Text(profile.name,style=MaterialTheme.typography.titleSmall)
                                                cfg?.let { Text(ModelClassCompatibility.inspect(it,tasksCsv,classes).summary,style=MaterialTheme.typography.bodySmall) }
                                            }
                                        }
                                    }
                                }
                            }
                            if(models.size>4) Text(tr("+${models.size-4} autre(s) modèle(s) dans la bibliothèque",
                                "+${models.size-4} more model(s) in the library"),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        modelConfig?.let {
                            ModelCompatibilityPanel(it,tasksCsv,classes)
                            if(ModelContract.adapter(it)=="inspect_only") {
                                Text(tr("Le fichier est chargé mais son contrat n’est pas encore configuré.",
                                    "The file is loaded but its contract is not configured yet."),color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)
                                FilledTonalButton(onClick={viewModel.navigateTo(Screen.ModelSettings)},enabled=!busy,modifier=Modifier.fillMaxWidth()) {
                                    Text(tr("Configurer ce modèle", "Configure this model"))
                                }
                            } else {
                                Row(verticalAlignment=Alignment.CenterVertically) {
                                    Switch(assistance && canAutomate,{assistance=it},enabled=!busy && canAutomate,modifier=Modifier.testTag("setup_assistance"))
                                    Column(Modifier.padding(start=12.dp)) {
                                        Text(tr("Préannoter automatiquement le prochain lot", "Automatically preannotate the next batch"))
                                        if(!canAutomate) Text(tr("Installez les poids ou configurez un endpoint local compatible pour activer ce mode.",
                                            "Install model weights or configure a compatible local endpoint to enable this mode."),style=MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        } ?: Text(tr("Aucun modèle actif. Vous pouvez continuer en manuel ou ouvrir le catalogue.",
                            "No active model. Continue manually or open the catalog."),style=MaterialTheme.typography.bodySmall)

                        OutlinedButton(onClick={viewModel.navigateTo(Screen.Models)},enabled=!busy,modifier=Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Memory,null);Spacer(Modifier.width(8.dp));Text(tr("Parcourir modèles et presets", "Browse models and presets"))
                        }
                    }
                    3 -> {
                        Text(tr("Vérifiez avant de démarrer", "Review before you start"), style = MaterialTheme.typography.headlineSmall)
                        val budgetOk=(budget.toLongOrNull() ?: 0L) in 128L..65536L
                        val sourceReady=!prepare || sourceReadyToPrepare
                        StudioSection(tr("État de préparation", "Readiness"),
                            tr("Chaque bloc doit être clair avant de créer le premier lot.", "Each block should be clear before creating the first batch."),
                            Icons.Default.FactCheck) {
                            StatusPill(
                                if(sourceReady) tr("Source prête", "Source ready") else tr("Source à vérifier", "Source needs review"),
                                if(sourceReady) Icons.Default.CheckCircleOutline else Icons.Default.ErrorOutline,
                                attention=!sourceReady)
                            StatusPill(
                                if(classesOk) tr("Annotations configurées", "Annotations configured") else tr("Classes manquantes", "Missing classes"),
                                if(classesOk) Icons.Default.CheckCircleOutline else Icons.Default.ErrorOutline,
                                attention=!classesOk)
                            StatusPill(
                                if(assistance && canAutomate) tr("Aide IA prête · propositions à relire", "AI assistance ready · suggestions require review")
                                else tr("Mode manuel prêt", "Manual mode ready"),
                                if(!assistance || canAutomate) Icons.Default.CheckCircleOutline else Icons.Default.ErrorOutline,
                                attention=assistance && !canAutomate)
                            StatusPill(
                                if(budgetOk) tr("Stockage : $budget Mio", "Storage: $budget MiB") else tr("Budget stockage invalide", "Invalid storage budget"),
                                if(budgetOk) Icons.Default.CheckCircleOutline else Icons.Default.ErrorOutline,
                                attention=!budgetOk)
                            if(!sourceReady) TextButton(onClick={step=0},enabled=!busy){Text(tr("Corriger la source", "Fix source"))}
                            if(!classesOk) TextButton(onClick={step=1},enabled=!busy){Text(tr("Corriger les annotations", "Fix annotations"))}
                            if(assistance && !canAutomate) TextButton(onClick={step=2},enabled=!busy){Text(tr("Corriger le modèle", "Fix model"))}
                        }
                        StudioSection(name, icon = Icons.Default.CheckCircleOutline) {
                            Text(if (sourceKind == "local") policy.localSourceLabel.ifBlank { tr("Dossier local", "Local folder") } else source)
                            Text(tasks.joinToString { it.title })
                            val count = ProjectVocabulary.parse(classes).size
                            Text((if (count == 1) tr("1 classe", "1 class") else tr("$count classes", "$count classes")) + tr(" · lots de ${policy.batchSize} images maximum", " · batches of up to ${policy.batchSize} images"))
                            ModelCompatibilityPanel(modelConfig, tasksCsv, classes)
                        }
                        if (sourceKind == "hf" && !indexedSourceReady && batches.isEmpty()) {
                            val previewChecked = hfPreviewReady
                            val usablePreview=com.unicornwhodev.visiondatasetstudio.data.source.SourceImageColumn.usableCount(imageColumn,inspection.previewRows)
                            Text(if (previewChecked) tr("$usablePreview/${inspection.previewRows.size} lignes d’aperçu sont exploitables. Les lignes incompatibles seront ignorées plutôt que de bloquer le lot.",
                                    "$usablePreview/${inspection.previewRows.size} preview rows are usable. Incompatible rows will be skipped instead of blocking the batch.")
                                else tr("La source n’est pas encore vérifiée. Contrôlez son accès et ses images avant le premier lot.", "The source is not checked yet. Check access and images before the first batch."), style=MaterialTheme.typography.bodySmall)
                            if (!previewChecked) OutlinedButton(onClick={viewModel.inspectSourceDataset(source,config,split)}, enabled=!busy && sourceOk) {
                                Text(tr("Vérifier la source", "Check source"))
                            }
                            if (inspection.repoId == StudioWorkflow.normalizeRepo(source) && inspection.error != null) Text(inspection.error.orEmpty(), color=MaterialTheme.colorScheme.error)
                            if (!previewChecked && inspection.repoId == StudioWorkflow.normalizeRepo(source) && (inspection.isInspected || inspection.error != null)) {
                                Text(tr("Si le Viewer ne fournit pas d’image exploitable, utilisez un dossier local ou un manifeste JSONL dans les options de source.",
                                    "If the Viewer does not provide usable images, use a local folder or a JSONL manifest in source options."), style=MaterialTheme.typography.bodySmall)
                                TextButton(onClick={step=0}, enabled=!busy) { Text(tr("Revoir la source", "Review source")) }
                            }
                        }
                        Text(if(assistance && canAutomate)
                            tr("Aide IA : activée · toutes les propositions restent à relire.", "AI assistance: enabled · every suggestion still requires review.")
                            else tr("Aide IA : désactivée · annotation manuelle.", "AI assistance: disabled · manual annotation."),
                            style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        StudioSection(tr("Et ensuite ?", "What happens next?"), icon = Icons.Default.ArrowForward) {
                            Text(tr("1. Ouvrez une image du lot.\n2. Annotez puis validez chaque image.\n3. Dans Exporter, enregistrez votre archive ou publiez sur Hugging Face.",
                                "1. Open an image from the batch.\n2. Annotate and approve each image.\n3. In Export, save your archive or publish on Hugging Face."))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Checkbox(prepare, { prepare = it }, enabled = !busy, modifier = Modifier.testTag("setup_prepare"))
                            Text(stringResource(R.string.setup_prepare_next), style = MaterialTheme.typography.bodyMedium)
                        }
                        StudioDisclosure(tr("Stockage avancé", "Advanced storage"), Icons.Default.Storage) {
                            OutlinedTextField(budget, { budget = it.filter(Char::isDigit).take(5) }, label = { Text(stringResource(R.string.setup_local_budget)) }, supportingText = { Text(stringResource(R.string.setup_budget_help)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            StudioDetails(stringResource(R.string.setup_network_safety), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
