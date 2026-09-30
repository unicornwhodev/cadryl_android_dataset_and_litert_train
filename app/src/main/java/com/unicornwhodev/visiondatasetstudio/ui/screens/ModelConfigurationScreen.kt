package com.unicornwhodev.visiondatasetstudio.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.unicornwhodev.visiondatasetstudio.ui.icons.CadrylIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.unicornwhodev.visiondatasetstudio.core.i18n.tr
import com.unicornwhodev.visiondatasetstudio.data.json.StudioJson
import com.unicornwhodev.visiondatasetstudio.domain.inference.*
import com.unicornwhodev.visiondatasetstudio.ui.*
import com.unicornwhodev.visiondatasetstudio.ui.components.*
import kotlinx.coroutines.flow.first

@Composable
fun ModelConfigurationScreen(vm: MainViewModel) {
    val project by vm.projectFlow.collectAsState()
    val busy by vm.isBusy.collectAsState()
    val result by vm.dryRunResult.collectAsState()
    val diagnostics by vm.modelDiagnostics.collectAsState()
    val receipts by vm.inferenceReceipts.collectAsState()
    val spec by vm.modelInputSpec.collectAsState()
    val samples by vm.batchSamples.collectAsState()
    val p = project ?: return
    val config = remember(p.modelConfigJson) { runCatching {
        p.modelConfigJson?.let { StudioJson.moshi.adapter(ModelConfig::class.java).fromJson(it) }
    }.getOrNull() }
    val runtimeReady=config?.let { it.runtime=="local_http" || !p.modelPath.isNullOrBlank() } == true
    val testImageReady=samples.any { it.acquisitionStatus=="AVAILABLE" && it.localImagePath!=null }
    LaunchedEffect(p.id, p.modelPath, p.modelConfigJson) { vm.isBusy.first { !it }; vm.inspectActiveModelInput() }
    Scaffold(contentWindowInsets = WindowInsets(0), modifier = Modifier.imePadding(), topBar = {
        WorkspaceTopBar(vm, tr("Réglages du modèle", "Model settings"), p.name)
    }) { inset ->
        Box(Modifier.fillMaxSize().padding(inset), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 800.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (config == null) {
                    Text(tr("Aucun modèle actif. Installez un modèle, importez un .tflite ou configurez un endpoint local.",
                        "No active model. Install a model, import a .tflite file, or configure a local endpoint."))
                    OutlinedButton(onClick = { vm.navigateTo(Screen.Models) }) { Text(tr("Ouvrir Modèles et presets", "Open Models & presets")) }
                } else if(ModelContract.adapter(config)=="inspect_only") {
                    StudioDetails(tr("Le fichier LiteRT est chargé, mais Cadryl ne devine pas la sémantique de ses sorties. Utilisez l’assistant ci-dessous avec la documentation du modèle, ou appliquez un preset compatible depuis Modèles.",
                        "The LiteRT file is loaded, but Cadryl does not guess its output semantics. Use the guided settings below with the model documentation, or apply a compatible preset from Models."),
                        style=MaterialTheme.typography.bodyMedium)
                    OutlinedButton(onClick = { vm.navigateTo(Screen.Models) }) { Text(tr("Parcourir les presets", "Browse presets")) }
                }
                StudioSection(tr("Compatibilité avec le projet", "Project compatibility"),
                    tr("Les tâches et classes du dataset se configurent à un seul endroit : Configuration du projet. Ici, vous vérifiez uniquement ce que le modèle peut proposer.",
                        "Dataset tasks and classes are configured in one place: Project setup. Here you only check what the model can propose."),
                    CadrylIcons.Checklist) {
                    ModelCompatibilityPanel(config, p.activeTasksCsv, p.classesCsv)
                    val classCount=com.unicornwhodev.visiondatasetstudio.core.workflow.ProjectVocabulary.parse(p.classesCsv).size
                    Text(tr("${p.activeTasksCsv.ifBlank{"Aucune tâche"}} · $classCount classe(s)",
                        "${p.activeTasksCsv.ifBlank{"No task"}} · $classCount class(es)"),style=MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick={vm.navigateTo(Screen.Setup)},enabled=!busy,modifier=Modifier.fillMaxWidth()) {
                        Text(tr("Modifier tâches et classes", "Edit tasks and classes"))
                    }
                }
                StudioDisclosure(tr("Options du modèle", "Model options"),CadrylIcons.Tune,keepContent=true) {
                    ModelSettingsPanel(p,busy,vm::saveModelConfig,spec)
                }
                StudioSection(tr("Essayer sur une image", "Try on an image"),
                    tr("Utilise les réglages enregistrés. Les annotations sont conservées.", "Uses saved settings. Annotations are preserved."), CadrylIcons.Science) {
                    if(config!=null && ModelContract.adapter(config)!="inspect_only" && !runtimeReady) {
                        StudioDetails(tr("Le contrat est configuré, mais aucun poids LiteRT ou endpoint local n’est actif. Installez ou importez le modèle avant l’essai.",
                            "The contract is configured, but no LiteRT weights or local endpoint is active. Install or import the model before testing."),
                            style=MaterialTheme.typography.bodySmall)
                        OutlinedButton(onClick={vm.navigateTo(Screen.Models)},enabled=!busy) { Text(tr("Ouvrir Modèles", "Open Models")) }
                    }
                    if(runtimeReady && !testImageReady) Text(tr("Préparez au moins une image du lot pour activer l’essai.",
                        "Prepare at least one batch image to enable the trial."),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = vm::dryRunActiveModel, enabled = !busy && runtimeReady && testImageReady && config != null && ModelContract.adapter(config) != "inspect_only") {
                        Text(tr("Tester le modèle", "Test model"))
                    }
                    result?.let { Text(if (!it.success) it.error.orEmpty() else if (it.proposals.isEmpty())
                        tr("Exécution réussie, aucune proposition au seuil actuel. Vérifiez les classes recherchées et le seuil.",
                            "Execution succeeded, with no proposals at the current threshold. Check the target classes and threshold.")
                    else tr("${it.proposals.size} propositions · ${it.latencyMs} ms", "${it.proposals.size} proposals · ${it.latencyMs} ms"),
                        color = if (it.success) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error) }
                    StudioDisclosure(tr("Diagnostics et erreurs précédentes", "Diagnostics & previous errors"), CadrylIcons.History) {
                        TextButton(onClick = { vm.refreshInferenceReceipts(true) }, enabled = !busy) { Text(tr("Afficher les derniers échecs", "Show recent failures")) }
                        TextButton(onClick = { vm.refreshInferenceReceipts() }, enabled = !busy) { Text(tr("Afficher les derniers essais", "Show recent trials")) }
                        if (diagnostics.isNotBlank()) Text(diagnostics, style = MaterialTheme.typography.bodySmall)
                        if (receipts.isNotBlank()) Text(receipts, style = MaterialTheme.typography.bodySmall)
                    }
                }
                TextButton(onClick = { vm.navigateTo(Screen.ModelAdvanced) }, enabled = !busy) {
                    Text(tr("Contrat JSON, mesures et outils avancés", "JSON contract, benchmarks & advanced tools"))
                }
            }
        }
    }
}
