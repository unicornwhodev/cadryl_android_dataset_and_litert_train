package com.unicornwhodev.visiondatasetstudio.domain.inference

import com.unicornwhodev.visiondatasetstudio.core.i18n.tr
import com.unicornwhodev.visiondatasetstudio.core.workflow.ProjectVocabulary
import java.util.Locale

/** Vocabulary coverage is distinct from task coverage and from accuracy on real images. */
enum class ModelVocabularyKind { FIXED, TEXT_CANDIDATES, INTERACTIVE, OPEN, NONE }

data class ModelClassCoverage(
    val kind: ModelVocabularyKind,
    val available: List<String>,
    val supported: List<String>,
    val missing: List<String>,
    val tasks: TaskCompatibility,
    val needsClasses: Boolean
) {
    val checked get() = needsClasses && kind in setOf(ModelVocabularyKind.FIXED, ModelVocabularyKind.TEXT_CANDIDATES, ModelVocabularyKind.INTERACTIVE)
    val canAssist get() = tasks.canRun && (!checked || supported.isNotEmpty() || (tasks.usableOutputs - ModelClassCompatibility.labelOutputs).isNotEmpty())
    val summary: String get() = when {
        !tasks.canRun -> tr("Ce modèle ne réalise pas l’objectif choisi", "This model does not perform the selected task")
        !needsClasses -> tr("Aucune liste de classes nécessaire pour cet objectif", "This task does not need a class list")
        kind == ModelVocabularyKind.OPEN -> tr("Classes à vérifier sur une image", "Classes need an image trial")
        supported.isEmpty() -> tr("Aucune classe du projet prise en charge", "No project classes supported")
        supported.size == 1 && missing.isEmpty() -> tr("1 classe prise en charge", "1 class supported")
        missing.isEmpty() -> tr("${supported.size} classes sur ${supported.size} prises en charge", "${supported.size} of ${supported.size} classes supported")
        else -> tr("Classes prises en charge : ${supported.size} / ${supported.size + missing.size}", "${supported.size} of ${supported.size + missing.size} classes supported")
    }
}

object ModelClassCompatibility {
    val labelOutputs = setOf("box", "point", "mask", "tag", "count")

    /** Comparison key only. Original model labels and their numeric indices are never rewritten. */
    fun classKey(value: String): String = value.trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")

    fun kind(c: ModelConfig): ModelVocabularyKind = when {
        c.runtime == "local_http" || c.bundleKind == "florence2" -> ModelVocabularyKind.OPEN
        c.bundleKind == "tinyclip" -> ModelVocabularyKind.TEXT_CANDIDATES
        c.bundleKind == "efficientvit_sam" -> ModelVocabularyKind.INTERACTIVE
        ModelContract.adapter(c) in setOf("embedding", "inspect_only") -> ModelVocabularyKind.NONE
        else -> ModelVocabularyKind.FIXED
    }

    fun available(c: ModelConfig, outputs: Set<String> = ModelContract.outputTypes(c)): List<String> = when {
        c.runtime != "local_http" && ModelContract.adapter(c) == "fireviewer_dinov3_multitask" ->
            ((if ("tag" in outputs) c.labels else emptyList()) + (if (outputs.any { it in setOf("mask", "point") }) listOf(c.spatialLabel) else emptyList())).distinct()
        else -> when (kind(c)) {
        ModelVocabularyKind.OPEN, ModelVocabularyKind.NONE -> emptyList()
        ModelVocabularyKind.INTERACTIVE -> listOf(c.spatialLabel).filter { it.isNotBlank() }
        else -> c.labels.filterIndexed { index, label ->
            // RF-DETR reserves these output indices. Keep the original tensor vocabulary intact.
            ModelContract.adapter(c) != "rfdetr" || (index != 0 && !label.startsWith("unused_"))
        }.distinct()
        }
    }

    fun inspect(c: ModelConfig, tasksCsv: String, classesCsv: String): ModelClassCoverage {
        val tasks = ModelContract.compatibility(c, tasksCsv)
        val labels = ProjectVocabulary.parse(classesCsv)
        val available = available(c, tasks.usableOutputs)
        val availableKeys = available.map(::classKey).toSet()
        return ModelClassCoverage(kind(c), available, labels.filter { classKey(it) in availableKeys }, labels.filterNot { classKey(it) in availableKeys },
            tasks, tasks.usableOutputs.any { it in labelOutputs })
    }

    fun requireAssistance(c: ModelConfig, tasks: String, classes: String) {
        ModelContract.requireTaskCompatibility(c, tasks)
        check(inspect(c, tasks, classes).canAssist) {
            tr("Aucune classe du projet n’est proposée par ce modèle. Dans Configurer, choisissez ses classes, changez de modèle ou continuez à la main.",
                "This model supports none of the project classes. In Setup, choose its classes, change the model or continue manually.")
        }
    }

    /** Restrict known-vocabulary proposals, never human annotations or open text responses. */
    fun forProject(proposals: List<ModelProposal>, c: ModelConfig, classesCsv: String): List<ModelProposal> {
        if (kind(c) in setOf(ModelVocabularyKind.OPEN, ModelVocabularyKind.NONE)) return proposals
        val labels = ProjectVocabulary.parse(classesCsv).map(::classKey).toSet()
        val kept = proposals.filter {
            it.type !in labelOutputs || classKey((if (it.type == "mask") it.mask?.label else it.label).orEmpty()) in labels
        }
        val ids = kept.map { it.proposalId }.filter { it.isNotBlank() }.toSet()
        return kept.filter { it.type != "grounding" || it.linkedProposalIds.all(ids::contains) }
    }
}
