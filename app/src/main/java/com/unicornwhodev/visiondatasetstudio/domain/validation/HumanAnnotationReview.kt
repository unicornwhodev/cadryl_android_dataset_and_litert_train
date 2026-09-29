package com.unicornwhodev.visiondatasetstudio.domain.validation

import com.unicornwhodev.visiondatasetstudio.data.model.*

/** Human review replaces model proposals semantically while preserving model baselines for audit/adaptation. */
object HumanAnnotationReview {
    private fun origin(source:String):String = when {
        source.startsWith("human_correction:") -> source.substringAfter("human_correction:")
        source.startsWith("human_validated:") -> source.substringAfter("human_validated:")
        else -> source
    }
    fun isModelAssistedSource(source:String):Boolean = origin(source).startsWith("model_")
    private fun machine(source:String)=isModelAssistedSource(source)

    fun markEdited(before:SampleAnnotations, after:SampleAnnotations):SampleAnnotations {
        val points=before.points.associateBy{it.id}
        val boxes=before.boxes.associateBy{it.id}
        val masks=before.masks.associateBy{it.id}
        val tags=before.tags.associateBy{it.id}
        val captions=before.captions.associateBy{it.id}
        val grounding=before.groundings.associateBy{it.id}
        val vqa=before.vqaList.associateBy{it.id}
        val counts=before.counts.associateBy{it.id}

        fun provenance(old:String?, current:String):String {
            val source=old ?: current
            val original=origin(source)
            return when {
                original=="human" || original.startsWith("human_") -> "human"
                original.isBlank() -> "human"
                else -> "human_correction:$original"
            }
        }

        return after.copy(
            points=after.points.map { value ->
                val old=points[value.id]
                if(old==null) value.copy(isHumanVerified=true,sourceProvenance="human")
                else if(old!=value) value.copy(isHumanVerified=true,sourceProvenance=provenance(old.sourceProvenance,value.sourceProvenance),
                    explicitlyAdjusted=value.explicitlyAdjusted || machine(old.sourceProvenance))
                else value
            },
            boxes=after.boxes.map { value ->
                val old=boxes[value.id]
                if(old==null) value.copy(isHumanVerified=true,sourceProvenance="human")
                else if(old!=value) value.copy(isHumanVerified=true,sourceProvenance=provenance(old.sourceProvenance,value.sourceProvenance),
                    explicitlyAdjusted=value.explicitlyAdjusted || machine(old.sourceProvenance))
                else value
            },
            masks=after.masks.map { value ->
                val old=masks[value.id]
                if(old==null) value.copy(isHumanVerified=true,sourceProvenance="human")
                else if(old!=value) value.copy(isHumanVerified=true,sourceProvenance=provenance(old.sourceProvenance,value.sourceProvenance),
                    explicitlyAdjusted=value.explicitlyAdjusted || machine(old.sourceProvenance))
                else value
            },
            tags=after.tags.map { value ->
                val old=tags[value.id]
                if(old==null) value.copy(isHumanVerified=true,sourceProvenance="human")
                else if(old!=value) value.copy(isHumanVerified=true,sourceProvenance=provenance(old.sourceProvenance,value.sourceProvenance))
                else value
            },
            captions=after.captions.map { value ->
                val old=captions[value.id]
                if(old==null) value.copy(isHumanVerified=true,sourceProvenance="human")
                else if(old!=value) value.copy(isHumanVerified=true,sourceProvenance=provenance(old.sourceProvenance,value.sourceProvenance))
                else value
            },
            groundings=after.groundings.map { value ->
                val old=grounding[value.id]
                if(old==null) value.copy(isHumanVerified=true,sourceProvenance="human")
                else if(old!=value) value.copy(isHumanVerified=true,sourceProvenance=provenance(old.sourceProvenance,value.sourceProvenance))
                else value
            },
            vqaList=after.vqaList.map { value ->
                val old=vqa[value.id]
                if(old==null) value.copy(isHumanVerified=true,sourceProvenance="human")
                else if(old!=value) value.copy(isHumanVerified=true,sourceProvenance=provenance(old.sourceProvenance,value.sourceProvenance))
                else value
            },
            counts=after.counts.map { value ->
                val old=counts[value.id]
                if(old==null) value.copy(isHumanVerified=true,sourceProvenance="human")
                else if(old!=value) value.copy(isHumanVerified=true,sourceProvenance=provenance(old.sourceProvenance,value.sourceProvenance))
                else value
            }
        )
    }

    /** Final manual review: every surviving annotation becomes a human decision. */
    fun validateAll(value:SampleAnnotations):SampleAnnotations = value.copy(
        points=value.points.map{it.copy(isHumanVerified=true,sourceProvenance=validatedProvenance(it.sourceProvenance))},
        boxes=value.boxes.map{it.copy(isHumanVerified=true,sourceProvenance=validatedProvenance(it.sourceProvenance))},
        masks=value.masks.map{it.copy(isHumanVerified=true,sourceProvenance=validatedProvenance(it.sourceProvenance))},
        tags=value.tags.map{it.copy(isHumanVerified=true,sourceProvenance=validatedProvenance(it.sourceProvenance))},
        captions=value.captions.map{it.copy(isHumanVerified=true,sourceProvenance=validatedProvenance(it.sourceProvenance))},
        groundings=value.groundings.map{it.copy(isHumanVerified=true,sourceProvenance=validatedProvenance(it.sourceProvenance))},
        vqaList=value.vqaList.map{it.copy(isHumanVerified=true,sourceProvenance=validatedProvenance(it.sourceProvenance))},
        counts=value.counts.map{it.copy(isHumanVerified=true,sourceProvenance=validatedProvenance(it.sourceProvenance))}
    )

    private fun validatedProvenance(source:String)=when {
        source=="human" -> source
        source.startsWith("human_correction") -> source
        source.startsWith("human_validated") -> source
        source.startsWith("human_") -> source
        source.isBlank() -> "human"
        else -> "human_validated:${origin(source)}"
    }

    private fun unreviewedSources(value:SampleAnnotations):List<String> =
        value.points.filter{!it.isHumanVerified}.map{it.sourceProvenance} +
        value.boxes.filter{!it.isHumanVerified}.map{it.sourceProvenance} +
        value.masks.filter{!it.isHumanVerified}.map{it.sourceProvenance} +
        value.tags.filter{!it.isHumanVerified}.map{it.sourceProvenance} +
        value.captions.filter{!it.isHumanVerified}.map{it.sourceProvenance} +
        value.groundings.filter{!it.isHumanVerified}.map{it.sourceProvenance} +
        value.vqaList.filter{!it.isHumanVerified}.map{it.sourceProvenance} +
        value.counts.filter{!it.isHumanVerified}.map{it.sourceProvenance}

    fun unreviewedModelCount(value:SampleAnnotations):Int=unreviewedSources(value).count(::isModelAssistedSource)
    fun unreviewedDraftCount(value:SampleAnnotations):Int=(unreviewedSources(value).size-unreviewedModelCount(value)).coerceAtLeast(0)

    fun pendingStatus(value:SampleAnnotations):String=when {
        value.unreviewedCount==0 -> AnnotationStatus.IN_PROGRESS.name
        unreviewedModelCount(value)>0 -> AnnotationStatus.PROPOSALS_AVAILABLE.name
        else -> AnnotationStatus.DRAFTS_AVAILABLE.name
    }

    fun wasModelAssisted(value:SampleAnnotations):Boolean =
        value.points.any{isModelAssistedSource(it.sourceProvenance)} ||
        value.boxes.any{isModelAssistedSource(it.sourceProvenance)} ||
        value.masks.any{isModelAssistedSource(it.sourceProvenance)} ||
        value.tags.any{isModelAssistedSource(it.sourceProvenance)} ||
        value.captions.any{isModelAssistedSource(it.sourceProvenance)} ||
        value.groundings.any{isModelAssistedSource(it.sourceProvenance)} ||
        value.vqaList.any{isModelAssistedSource(it.sourceProvenance)} ||
        value.counts.any{isModelAssistedSource(it.sourceProvenance)}

    fun isManuallyTreated(value:SampleAnnotations):Boolean {
        val all=listOf(
            value.points.map{it.isHumanVerified}, value.boxes.map{it.isHumanVerified}, value.masks.map{it.isHumanVerified},
            value.tags.map{it.isHumanVerified}, value.captions.map{it.isHumanVerified}, value.groundings.map{it.isHumanVerified},
            value.vqaList.map{it.isHumanVerified}, value.counts.map{it.isHumanVerified}
        ).flatten()
        return all.isNotEmpty() && all.all{it}
    }
}
