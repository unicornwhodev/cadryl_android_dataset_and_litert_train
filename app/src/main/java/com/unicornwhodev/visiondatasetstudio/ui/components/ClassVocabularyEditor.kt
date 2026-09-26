package com.unicornwhodev.visiondatasetstudio.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.unicornwhodev.visiondatasetstudio.core.i18n.tr
import com.unicornwhodev.visiondatasetstudio.core.workflow.ProjectVocabulary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClassVocabularyEditor(value: String, onChange: (String) -> Unit, enabled: Boolean,
                          suggestions: List<String> = emptyList()) {
    val labels = ProjectVocabulary.parse(value)
    var query by remember { mutableStateOf("") }
    var limit by remember(query) { mutableIntStateOf(18) }
    OutlinedTextField(value, onChange, enabled = enabled, minLines = 2, maxLines = 5,
        label = { Text(tr("Classes à annoter", "Annotation classes")) },
        supportingText = { Text(tr("Collez une liste : virgules, points-virgules ou une classe par ligne.",
            "Paste a list: commas, semicolons or one class per line.")) },
        modifier = Modifier.fillMaxWidth().testTag("project_classes"))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        labels.take(24).forEach { label -> InputChip(selected = true, enabled = enabled,
            onClick = { onChange(ProjectVocabulary.format(labels.filterNot { it == label })) }, label = { Text(label) },
            modifier = Modifier.testTag("project_class_$label"),
            trailingIcon = { Icon(Icons.Default.Close, tr("Retirer $label", "Remove $label"), Modifier.size(14.dp)) }) }
    }
    if (labels.size > 24) Text(tr("${labels.size} classes au total", "${labels.size} classes in total"), style = MaterialTheme.typography.bodySmall)
    if (suggestions.isNotEmpty()) StudioDisclosure(tr("Choisir parmi les ${suggestions.size} classes du modèle", "Choose from ${suggestions.size} model classes"), Icons.Default.Search, true) {
        OutlinedTextField(query, { query = it }, enabled = enabled, singleLine = true,
            label = { Text(tr("Rechercher une classe", "Search for a class")) },
            leadingIcon = { Icon(Icons.Default.Search, null) }, modifier = Modifier.fillMaxWidth().testTag("class_search"))
        val results = suggestions.distinct().filter { it.contains(query, ignoreCase = true) }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            results.take(limit).forEach { label ->
                FilterChip(selected = label in labels, enabled = enabled,
                    onClick = { onChange(if (label in labels) ProjectVocabulary.format(labels - label) else ProjectVocabulary.append(value, listOf(label))) },
                    label = { Text(label) }, modifier = Modifier.testTag("model_class_$label"))
            }
        }
        if (results.isEmpty()) Text(tr("Aucune correspondance. Vous pouvez garder votre classe pour l’annotation manuelle.",
            "No match. You can keep your class for manual annotation."), style = MaterialTheme.typography.bodySmall)
        if (results.size > limit) TextButton(onClick = { limit += 36 }) { Text(tr("Voir plus (${results.size - limit})", "Show more (${results.size - limit})")) }
        val missing = suggestions.filterNot { it in labels }.distinct()
        if (missing.isNotEmpty()) TextButton(onClick = { onChange(ProjectVocabulary.append(value, missing)) }, enabled = enabled) {
            Text(if (missing.size == 1) tr("Ajouter la classe restante", "Add remaining class") else tr("Ajouter les ${missing.size} classes du modèle", "Add ${missing.size} model classes"))
        }
    }
}
