package com.unicornwhodev.visiondatasetstudio

import com.unicornwhodev.visiondatasetstudio.data.model.*
import com.unicornwhodev.visiondatasetstudio.domain.validation.HumanAnnotationReview
import com.unicornwhodev.visiondatasetstudio.domain.validation.AnnotationReview
import com.unicornwhodev.visiondatasetstudio.core.workflow.StudioTask
import org.junit.Assert.*
import org.junit.Test

class HumanAnnotationReviewTest {
    @Test fun explicitApprovalAcceptsImportedAndModelAnnotationsTogetherWithoutChangingGeometry() {
        val imported=BoxTarget("imported",.1f,.2f,.4f,.8f,"smoke",sourceProvenance="import")
        val model=BoxTarget("model",.5f,.1f,.9f,.3f,"flame",sourceProvenance="model_litert",modelScore=.72f)
        val source=SampleAnnotations(boxes=listOf(imported,model))
        assertFalse(AnnotationReview.problems(source,setOf(StudioTask.DETECTION)).isEmpty())
        val approved=HumanAnnotationReview.validateAll(source)
        assertTrue(AnnotationReview.problems(approved,setOf(StudioTask.DETECTION)).isEmpty())
        assertEquals(imported.copy(isHumanVerified=true,sourceProvenance="human_validated:import"),approved.boxes[0])
        assertEquals(model.copy(isHumanVerified=true,sourceProvenance="human_validated:model_litert"),approved.boxes[1])
        assertFalse(source.boxes[0].isHumanVerified)
        assertEquals(approved,HumanAnnotationReview.validateAll(approved))
    }

    @Test fun explicitApprovalDoesNotBypassInvalidGeometryOrUncertainty() {
        val invalid=SampleAnnotations(boxes=listOf(BoxTarget("b",.8f,.1f,.2f,.4f,"smoke",sourceProvenance="import")))
        assertTrue(AnnotationReview.problems(HumanAnnotationReview.validateAll(invalid),setOf(StudioTask.DETECTION)).isNotEmpty())
        val uncertain=invalid.copy(boxes=listOf(invalid.boxes.single().copy(xmin=.1f,xmax=.8f)),quality=QualityAuditTarget(isUncertain=true))
        val approved=HumanAnnotationReview.validateAll(uncertain)
        assertTrue(approved.quality.isUncertain)
        assertTrue(AnnotationReview.problems(approved,setOf(StudioTask.DETECTION)).isNotEmpty())
        assertTrue(AnnotationReview.problems(HumanAnnotationReview.validateAll(SampleAnnotations()),setOf(StudioTask.DETECTION)).isNotEmpty())
    }

    @Test fun explicitApprovalAcceptsEveryAnnotationTypeAndPreservesExistingCorrections() {
        val value=SampleAnnotations(
            boxes=listOf(BoxTarget("b",.1f,.1f,.4f,.4f,"smoke",isHumanVerified=true,sourceProvenance="human_correction:model_litert",modelXmax=.3f,explicitlyAdjusted=true)),
            points=listOf(PointTarget("p",.2f,.2f,"smoke",sourceProvenance="import")),
            masks=listOf(MaskTarget("m","smoke",2,2,listOf(0,4),sourceProvenance="import")),
            tags=listOf(TagTarget("t","smoke",sourceProvenance="import")),
            captions=listOf(CaptionTarget("caption","Fumée visible",sourceProvenance="import")),
            groundings=listOf(GroundingTarget("g","fumée",boxIds=listOf("b"),sourceProvenance="import")),
            vqaList=listOf(VqaTarget("v","De la fumée ?","Oui",targetIds=listOf("b"),sourceProvenance="import")),
            counts=listOf(CountingTarget("c","smoke",1,linkedInstanceIds=listOf("b"),sourceProvenance="import")))
        val approved=HumanAnnotationReview.validateAll(value)
        assertEquals(0,approved.unreviewedCount)
        assertEquals(value.boxes,approved.boxes)
        assertEquals(value.quality,approved.quality)
        assertTrue(AnnotationReview.problems(approved,setOf(StudioTask.DETECTION,StudioTask.SEGMENTATION,StudioTask.CLASSIFICATION,StudioTask.CAPTIONING,StudioTask.GROUNDING,StudioTask.VQA,StudioTask.COUNTING)).isEmpty())
    }

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

    @Test fun pendingStatusSeparatesImportedDraftsFromAiSuggestions() {
        val imported=SampleAnnotations(tags=listOf(TagTarget("i","smoke",sourceProvenance="import")))
        val model=SampleAnnotations(tags=listOf(TagTarget("m","smoke",sourceProvenance="model_litert")))
        assertEquals(AnnotationStatus.DRAFTS_AVAILABLE.name,HumanAnnotationReview.pendingStatus(imported))
        assertEquals(AnnotationStatus.PROPOSALS_AVAILABLE.name,HumanAnnotationReview.pendingStatus(model))
        assertEquals(1,HumanAnnotationReview.unreviewedDraftCount(imported))
        assertEquals(1,HumanAnnotationReview.unreviewedModelCount(model))
    }

    @Test fun correctedModelSourceRemainsRecoverableForAdaptiveTraining() {
        val source="human_correction:model_litert:weights:contract"
        assertEquals("model_litert:weights:contract",HumanAnnotationReview.modelSource(source))
        assertTrue(HumanAnnotationReview.isModelAssistedSource(source))
        assertNull(HumanAnnotationReview.modelSource("human_correction:import"))
    }

    @Test fun importedReviewIsHumanButNotModelAssisted() {
        val reviewed=HumanAnnotationReview.validateAll(SampleAnnotations(tags=listOf(TagTarget("t","smoke",sourceProvenance="import"))))
        assertEquals("human_validated:import",reviewed.tags.single().sourceProvenance)
        assertFalse(HumanAnnotationReview.wasModelAssisted(reviewed))
    }
}
