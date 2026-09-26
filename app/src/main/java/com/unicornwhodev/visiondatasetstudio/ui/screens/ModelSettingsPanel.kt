package com.unicornwhodev.visiondatasetstudio.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.unicornwhodev.visiondatasetstudio.core.i18n.tr
import com.unicornwhodev.visiondatasetstudio.data.json.StudioJson
import com.unicornwhodev.visiondatasetstudio.data.model.ProjectEntity
import com.unicornwhodev.visiondatasetstudio.domain.inference.*
import com.unicornwhodev.visiondatasetstudio.ui.components.StudioDisclosure

/** Project overrides do not mutate a shared model profile or its weights. */
@Composable
fun ModelSettingsPanel(project: ProjectEntity, busy: Boolean, save: (String) -> Unit, inputSpec: ModelInputSpec? = null) {
    val adapter=remember { StudioJson.moshi.adapter(ModelConfig::class.java).failOnUnknown() }
    val original=remember(project.id,project.modelConfigJson) { runCatching { project.modelConfigJson?.let(adapter::fromJson) }.getOrNull() } ?: return
    var config by remember(project.id,project.modelConfigJson) { mutableStateOf(original) }
    var threads by remember(project.id,project.modelConfigJson) { mutableStateOf(original.threads.toString()) }
    var count by remember(project.id,project.modelConfigJson) { mutableStateOf(if(original.bundleKind=="tinyclip" || ModelContract.adapter(original)=="classification") original.topK.toString() else original.maxDetections.toString()) }
    val kind=ModelContract.adapter(config)
    val classification=config.bundleKind=="tinyclip" || kind=="classification"
    val scored=config.httpOutputMode!="caption_text" && (config.bundleKind=="tinyclip" || (config.bundleKind.isBlank() && kind !in setOf("embedding","inspect_only")))
    val bounded=config.runtime!="local_http" && (classification || (config.bundleKind.isBlank() && kind in setOf("ssd","rfdetr","rtmdet","yolo","xyxy_score_class","points")))
    var width by remember(project.id,project.modelConfigJson) { mutableStateOf(original.inputWidth.toString()) }
    var height by remember(project.id,project.modelConfigJson) { mutableStateOf(original.inputHeight.toString()) }
    val candidate=config.copy(inputWidth=width.toIntOrNull() ?: 0, inputHeight=height.toIntOrNull() ?: 0,threads=threads.toIntOrNull() ?: 0,
        topK=if(classification)count.toIntOrNull() ?: 0 else config.topK,
        maxDetections=if(bounded && !classification)count.toIntOrNull() ?: 0 else config.maxDetections)
    val validation=runCatching {
        ModelContract.validate(candidate)
        if (candidate.runtime != "local_http" && candidate.bundleKind.isBlank() && kind != "inspect_only") inputSpec?.validate(candidate)
    }
    val valid=validation.isSuccess
    StudioDisclosure(tr("Réglages du modèle actif", "Active model settings"), initiallyExpanded=true) {
        Text(tr("$width × $height px · ${config.labels.size} classes", "$width × $height px · ${config.labels.size} classes"),style=MaterialTheme.typography.labelSmall)
        if (config.runtime == "litert_interpreter" && config.bundleKind.isBlank()) StudioDisclosure(tr("Dimensions de l’image du modèle", "Model image dimensions")) {
            Text(tr("Les photos sont adaptées à cette taille pendant l’inférence. Les originaux sont conservés.", "Photos are fitted to this size during inference. Originals are preserved."), style=MaterialTheme.typography.bodySmall)
            inputSpec?.let { Text(tr("Entrée du fichier : ${it.signature} · ${it.type}", "File input: ${it.signature} · ${it.type}"),style=MaterialTheme.typography.bodySmall) }
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(width,{width=it},enabled=!busy,label={Text(tr("Largeur", "Width"))},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.weight(1f))
                OutlinedTextField(height,{height=it},enabled=!busy,label={Text(tr("Hauteur", "Height"))},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.weight(1f))
            }
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                listOf("NHWC", "NCHW").forEach { layout -> FilterChip(config.inputLayout==layout,onClick={config=config.copy(inputLayout=layout)},enabled=!busy,label={Text(layout)}) }
            }
            inputSpec?.fixedImageConfig(config)?.let { fitted ->
                TextButton(onClick={config=fitted;width=fitted.inputWidth.toString();height=fitted.inputHeight.toString()},enabled=!busy) { Text(tr("Utiliser les dimensions du fichier", "Use file dimensions")) }
            }
        }
        if(scored) {
            Text(tr("Seuil de confiance", "Confidence threshold")+" · "+"%.2f".format(java.util.Locale.ROOT,config.threshold))
            Slider(config.threshold,{config=config.copy(threshold=it)},enabled=!busy,valueRange=0f..1f)
        }
        if(ModelPrompts.supportsText(config)) {
            OutlinedTextField(config.prompt,{config=config.copy(prompt=it.take(16000))},enabled=!busy,
                label={Text(tr("Prompt du modèle", "Model prompt"))},minLines=2,maxLines=5,modifier=Modifier.fillMaxWidth())
            Text(when(config.bundleKind) {
                "tinyclip"->tr("{label} insère chaque classe. Sans ce marqueur, la classe est ajoutée au prompt.", "{label} inserts each class. Without it, the class is appended to the prompt.")
                "florence2"->tr("<CAPTION>, <DETAILED_CAPTION>, <OD>, <OCR> ou consigne libre. Génération gloutonne, limite de 128 jetons du convertisseur.", "<CAPTION>, <DETAILED_CAPTION>, <OD>, <OCR> or a custom instruction. Greedy decoding, converter limit of 128 tokens.")
                else->tr("Transmis au serveur local dans le champ prompt ou dans le modèle de requête JSON.", "Sent to the local server in the prompt field or custom JSON request template.")
            },style=MaterialTheme.typography.bodySmall)
        }
        if("caption" in ModelContract.outputTypes(config)) {
            OutlinedTextField(config.captionLanguage,{config=config.copy(captionLanguage=it)},enabled=!busy,
                label={Text(tr("Langue enregistrée pour les légendes générées", "Language recorded for generated captions"))},singleLine=true,modifier=Modifier.fillMaxWidth())
        }
        if(config.bundleKind=="tinyclip") {
            var labels by remember(project.id,project.modelConfigJson) { mutableStateOf(com.unicornwhodev.visiondatasetstudio.core.workflow.ProjectVocabulary.format(original.labels)) }
            Text(tr("Classes recherchées par le modèle", "Classes the model should find"), style=MaterialTheme.typography.labelLarge)
            com.unicornwhodev.visiondatasetstudio.ui.components.ClassVocabularyEditor(labels,
                { labels=it;config=config.copy(labels=com.unicornwhodev.visiondatasetstudio.core.workflow.ProjectVocabulary.parse(it)) }, !busy)
            TextButton(onClick = { labels = project.classesCsv; config = config.copy(labels=com.unicornwhodev.visiondatasetstudio.core.workflow.ProjectVocabulary.parse(labels)) }, enabled = !busy) {
                Text(tr("Utiliser les classes du projet", "Use project classes"))
            }
        }
        if(config.bundleKind=="efficientvit_sam") {
            Text(tr("Placez un point ou une boîte dans l’éditeur, puis lancez le modèle pour obtenir le masque.", "Place a point or box in the editor, then run the model to get the mask."),style=MaterialTheme.typography.bodySmall)
            OutlinedTextField(config.spatialLabel, { config=config.copy(spatialLabel=it) }, enabled=!busy,
                label={Text(tr("Classe du masque", "Mask class"))}, singleLine=true, modifier=Modifier.fillMaxWidth())
        }
        if(config.runtime=="litert_interpreter") StudioDisclosure(tr("Performances et limites", "Performance & limits")) {
            OutlinedTextField(threads,{threads=it},enabled=!busy,
                label={Text(tr("Threads CPU (1–8)","CPU threads (1–8)"))},singleLine=true,
                keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.fillMaxWidth())
            if(bounded) OutlinedTextField(count,{count=it},enabled=!busy,label={Text(tr("Résultats maximum (1–1000)","Maximum results (1–1000)"))},singleLine=true,
                keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.fillMaxWidth())
            if(config.bundleKind.isBlank() && kind in setOf("yolo","rtmdet")) {
                Text(tr("Recouvrement NMS", "NMS overlap")+" · "+"%.2f".format(java.util.Locale.ROOT,config.nmsIou))
                Slider(config.nmsIou,{config=config.copy(nmsIou=it)},enabled=!busy,valueRange=0f..1f)
            }
        }
        if(!valid) Text(validation.exceptionOrNull()?.message ?: tr("Vérifiez les champs avant d’enregistrer.","Check the fields before saving."),color=MaterialTheme.colorScheme.error)
        Button(onClick={save(adapter.toJson(candidate))},enabled=!busy && valid && candidate!=original) { Text(tr("Enregistrer les réglages", "Save model settings")) }
    }
}
