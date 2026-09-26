package com.unicornwhodev.visiondatasetstudio.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
    val p = project ?: return
    val config = remember(p.modelConfigJson) { runCatching {
        p.modelConfigJson?.let { StudioJson.moshi.adapter(ModelConfig::class.java).fromJson(it) }
    }.getOrNull() }
    var classes by remember(p.id, p.classesCsv) { mutableStateOf(p.classesCsv) }
    LaunchedEffect(p.id, p.modelPath, p.modelConfigJson) { vm.isBusy.first { !it }; vm.inspectActiveModelInput() }
    Scaffold(contentWindowInsets = WindowInsets(0), modifier = Modifier.imePadding(), topBar = {
        WorkspaceTopBar(vm, tr("Réglages du modèle", "Model settings"), p.name)
    }) { inset ->
        Box(Modifier.fillMaxSize().padding(inset), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 800.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (config == null || ModelContract.adapter(config) == "inspect_only") {
                    Text(tr("Choisissez un modèle du catalogue pour obtenir ses réglages. Un fichier brut demande le contrat de son auteur.",
                        "Choose a catalog model to get its settings. A raw file needs its author's contract."))
                    OutlinedButton(onClick = { vm.navigateTo(Screen.Models) }) { Text(tr("Choisir un modèle", "Choose a model")) }
                }
                StudioDisclosure(tr("Classes du projet", "Project classes"), Icons.Default.Label, true) {
                    ModelCompatibilityPanel(config, p.activeTasksCsv, classes)
                    ClassVocabularyEditor(classes, { classes = it }, !busy, config?.let { ModelClassCompatibility.inspect(it, p.activeTasksCsv, classes).available }.orEmpty())
                    Button(onClick = { vm.saveProjectClasses(classes) }, enabled = !busy && classes.isNotBlank() && classes != p.classesCsv) {
                        Text(tr("Enregistrer les classes", "Save classes"))
                    }
                }
                ModelSettingsPanel(p, busy, vm::saveModelConfig, spec)
                StudioSection(tr("Essayer sur une image", "Try on an image"),
                    tr("Utilise les réglages enregistrés. Les annotations sont conservées.", "Uses saved settings. Annotations are preserved."), Icons.Default.Science) {
                    Button(onClick = vm::dryRunActiveModel, enabled = !busy && config != null && ModelContract.adapter(config) != "inspect_only") {
                        Text(tr("Tester le modèle", "Test model"))
                    }
                    result?.let { Text(if (!it.success) it.error.orEmpty() else if (it.proposals.isEmpty())
                        tr("Exécution réussie, aucune proposition au seuil actuel. Vérifiez les classes recherchées et le seuil.",
                            "Execution succeeded, with no proposals at the current threshold. Check the target classes and threshold.")
                    else tr("${it.proposals.size} propositions · ${it.latencyMs} ms", "${it.proposals.size} proposals · ${it.latencyMs} ms"),
                        color = if (it.success) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error) }
                    StudioDisclosure(tr("Diagnostics et erreurs précédentes", "Diagnostics & previous errors"), Icons.Default.History) {
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
