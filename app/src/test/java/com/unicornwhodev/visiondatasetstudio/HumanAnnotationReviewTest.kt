package com.unicornwhodev.visiondatasetstudio

import com.unicornwhodev.visiondatasetstudio.data.model.*
import com.unicornwhodev.visiondatasetstudio.domain.validation.HumanAnnotationReview
import org.junit.Assert.*
import org.junit.Test

class HumanAnnotationReviewTest {
    @Test fun correctedModelProposalBecomesHumanCorrection() {
        val before=SampleAnnotations(boxes=listOf(BoxTarget("b",.1f,.1f,.4f,.4f,"smoke_visible",
            sourceProvenance="model_litert",modelXmin=.1f,modelYmin=.1f,modelXmax=.4f,modelYmax=.4f)))
        val after=before.copy(boxes=listOf(before.boxes.single().copy(xmax=.6f)))
        val corrected=HumanAnnotationReview.markEdited(before,after).boxes.single()
        assertTrue(corrected.isHumanVerified)
        assertTrue(corrected.explicitlyAdjusted)
        assertEquals("human_correction:model_litert",corrected.sourceProvenance)
        assertEquals(.4f,corrected.modelXmax!!,0f)
        assertEquals(.6f,corrected.xmax,0f)
    }

    @Test fun manualAdditionIsImmediatelyIdentifiedAsHuman() {
        val after=SampleAnnotations(tags=listOf(TagTarget("t","flame_visible")))
        val tag=HumanAnnotationReview.markEdited(SampleAnnotations(),after).tags.single()
        assertTrue(tag.isHumanVerified)
        assertEquals("human",tag.sourceProvenance)
    }

    @Test fun finalValidationTurnsRemainingSuggestionsIntoHumanDecisions() {
        val value=SampleAnnotations(
            points=listOf(PointTarget("p",.5f,.7f,"smoke_visible",sourceProvenance="model_litert")),
            tags=listOf(TagTarget("t","smoke_visible",sourceProvenance="import")))
        val reviewed=HumanAnnotationReview.validateAll(value)
        assertTrue(reviewed.points.single().isHumanVerified)
        assertTrue(reviewed.tags.single().isHumanVerified)
        assertEquals("human_validated:model_litert",reviewed.points.single().sourceProvenance)
        assertEquals("human_validated:import",reviewed.tags.single().sourceProvenance)
        assertTrue(HumanAnnotationReview.isManuallyTreated(reviewed))
        assertTrue(HumanAnnotationReview.wasModelAssisted(reviewed))
    }

    @Test fun importedReviewIsHumanButNotModelAssisted() {
        val reviewed=HumanAnnotationReview.validateAll(SampleAnnotations(tags=listOf(TagTarget("t","smoke",sourceProvenance="import"))))
        assertEquals("human_validated:import",reviewed.tags.single().sourceProvenance)
        assertFalse(HumanAnnotationReview.wasModelAssisted(reviewed))
    }
}
