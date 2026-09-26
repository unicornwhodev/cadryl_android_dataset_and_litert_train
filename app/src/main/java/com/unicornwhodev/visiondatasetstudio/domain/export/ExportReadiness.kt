package com.unicornwhodev.visiondatasetstudio.domain.export

import com.unicornwhodev.visiondatasetstudio.core.workflow.ProjectVocabulary
import com.unicornwhodev.visiondatasetstudio.data.model.SampleAnnotations

data class ExportReadiness(val missingClasses: List<String>, val boxes: Int, val masks: Int, val questions: Int,
                           val missingBoxClasses: List<String>) {
    fun missingFor(coco: Boolean, yolo: Boolean): List<String> = when {
        coco -> missingClasses
        yolo -> missingBoxClasses
        else -> emptyList()
    }
    companion object {
        fun inspect(classesCsv: String, annotations: List<SampleAnnotations>): ExportReadiness {
            val classes = ProjectVocabulary.parse(classesCsv).toSet()
            val used = annotations.flatMap { it.boxes.map { box -> box.label } + it.masks.map { mask -> mask.label } }.distinct()
            return ExportReadiness(used.filterNot { it in classes }, annotations.sumOf { it.boxes.size },
                annotations.sumOf { it.masks.size }, annotations.sumOf { it.vqaList.size },
                annotations.flatMap { it.boxes.map { box -> box.label } }.distinct().filterNot { it in classes })
        }
    }
}
