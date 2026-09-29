package com.unicornwhodev.visiondatasetstudio.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.unicornwhodev.visiondatasetstudio.core.i18n.tr
import com.unicornwhodev.visiondatasetstudio.domain.inference.*

@Composable
fun ModelCompatibilityPanel(config: ModelConfig?, tasks: String, classes: String) {
    if (config == null) {
        Text(tr("Vous pouvez tout annoter à la main. Un modèle pourra vous aider plus tard.",
            "You can annotate everything manually. A model can help later."), style = MaterialTheme.typography.bodyMedium)
        return
    }
    val check = remember(config, tasks, classes) { ModelClassCompatibility.inspect(config, tasks, classes) }
    Column(Modifier.fillMaxWidth().testTag("class_compatibility")) {
        StatusPill(check.summary, if (check.canAssist) Icons.Default.CheckCircleOutline else Icons.Default.Info)
        if (check.checked && check.missing.isNotEmpty()) {
            Text(tr("À annoter à la main : ", "Annotate manually: ") + check.missing.take(8).joinToString() +
                if (check.missing.size > 8) tr("… (${check.missing.size} classes)", "… (${check.missing.size} classes)") else "",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(when (check.kind) {
            ModelVocabularyKind.FIXED -> tr("Vérifié avec la liste du modèle. La casse et les espaces sont normalisés, mais les classes restent liées aux indices exacts du modèle. Un essai sur image permet de contrôler le résultat.",
                "Checked against the model's class list. Case and whitespace are normalized, while classes remain tied to the model's exact indices. An image trial lets you check the result.")
            ModelVocabularyKind.TEXT_CANDIDATES -> tr("Ce modèle compare vos textes à l’image. La liste est configurable ; un essai reste nécessaire.",
                "This model compares your text with the image. The candidate list is editable; an image trial is still needed.")
            ModelVocabularyKind.INTERACTIVE -> tr("Dessinez d’abord une boîte ou placez un point sur l’objet. Le masque utilisera la classe choisie dans les réglages du modèle.",
                "First draw a box or place a point on the object. The mask uses the class chosen in model settings.")
            ModelVocabularyKind.OPEN -> tr("Ce modèle produit du texte libre. Sa liste de classes ne garantit pas les objets qu’il sait reconnaître ; essayez-le sur une image.",
                "This model produces free text. Its class list does not guarantee which objects it recognizes; try it on an image.")
            ModelVocabularyKind.NONE -> tr("Ce fichier demande une configuration avancée pour proposer des annotations. Vous pouvez continuer à la main.",
                "This file needs advanced configuration to suggest annotations. You can continue manually.")
        }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (check.tasks.uncoveredTasks.isNotEmpty()) Text(tr("Outils à utiliser à la main : ", "Manual tools: ") + check.tasks.uncoveredTasks.joinToString { it.title },
            style = MaterialTheme.typography.bodySmall)
        if (ModelContract.adapter(config) == "fireviewer_dinov3_multitask") Text(
            tr("Les points et masques ciblent « ${config.spatialLabel} ». La classification utilise sa propre liste.",
                "Points and masks target “${config.spatialLabel}”. Classification uses its own list."), style = MaterialTheme.typography.bodySmall)
    }
}
