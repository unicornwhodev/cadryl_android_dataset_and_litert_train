package com.unicornwhodev.visiondatasetstudio

import com.unicornwhodev.visiondatasetstudio.core.workflow.ProjectVocabulary
import com.unicornwhodev.visiondatasetstudio.data.model.*
import com.unicornwhodev.visiondatasetstudio.domain.inference.*
import org.junit.Assert.*
import org.junit.Test

class ModelClassCompatibilityTest {
    private val detector = ModelConfig(adapter="yolo", labels=listOf("cat", "dog", "traffic light"))

    @Test fun exactClassCoverageKeepsModelIndicesAndAllowsPartialAssistance() {
        val check=ModelClassCompatibility.inspect(detector,"DETECTION","dog;chat;traffic light")
        assertTrue(check.canAssist)
        assertEquals(listOf("dog","traffic light"),check.supported)
        assertEquals(listOf("chat"),check.missing)
        assertEquals(listOf("cat","dog","traffic light"),detector.labels)
        assertTrue(ModelClassCompatibility.inspect(detector,"DETECTION","Cat,chat").canAssist)
        assertEquals(listOf("Cat"),ModelClassCompatibility.inspect(detector,"DETECTION","  Cat  ,chat").supported)
        assertTrue(runCatching{ModelClassCompatibility.requireAssistance(detector,"DETECTION","chat")}.isFailure)
        assertFalse(ModelClassCompatibility.inspect(detector,"CLASSIFICATION","cat").canAssist)
    }

    @Test fun rfDetrReservedSlotsAreNeverSuggestedOrReindexed() {
        val config=detector.copy(adapter="rfdetr",labels=listOf("background","cat","unused_2","dog"))
        assertEquals(listOf("cat","dog"),ModelClassCompatibility.available(config))
        assertFalse(ModelClassCompatibility.inspect(config,"DETECTION","background,unused_2").canAssist)
        assertEquals("dog",config.labels[3])
    }

    @Test fun freeTextAndInteractiveModelsDoNotClaimFixedVocabularyRecognition() {
        val open=detector.copy(runtime="local_http",labels=listOf("cat"))
        val check=ModelClassCompatibility.inspect(open,"DETECTION","rabbit")
        assertEquals(ModelVocabularyKind.OPEN,check.kind)
        assertFalse(check.checked)
        assertTrue(check.canAssist)
        assertTrue(check.available.isEmpty())
        val clip=detector.copy(bundleKind="tinyclip")
        assertEquals(ModelVocabularyKind.TEXT_CANDIDATES,ModelClassCompatibility.inspect(clip,"CLASSIFICATION","cat").kind)
        assertFalse(ModelClassCompatibility.inspect(clip,"CLASSIFICATION","rabbit").canAssist)
        val sam=detector.copy(bundleKind="efficientvit_sam",spatialLabel="leaf")
        assertEquals(listOf("leaf"),ModelClassCompatibility.available(sam))
        assertTrue(ModelClassCompatibility.inspect(sam,"SEGMENTATION","leaf").canAssist)
        assertFalse(ModelClassCompatibility.inspect(sam,"SEGMENTATION","cat").canAssist)
    }

    @Test fun spatialAndClassificationHeadsHaveDifferentVocabularies() {
        val config=detector.copy(adapter="fireviewer_dinov3_multitask",spatialLabel="target")
        assertFalse(ModelClassCompatibility.inspect(config,"SEGMENTATION","cat").canAssist)
        assertTrue(ModelClassCompatibility.inspect(config,"CLASSIFICATION","cat").canAssist)
        assertTrue(ModelClassCompatibility.inspect(config,"POINTING,SEGMENTATION","target").canAssist)
    }

    @Test fun filteringOnlyChangesNewMachineProposalsAndPreservesHumanLabels() {
        val human=BoxTarget("human",.1f,.1f,.5f,.5f,"custom human class",isHumanVerified=true)
        val annotations=SampleAnnotations(boxes=listOf(human))
        val proposals=listOf(ModelProposal("box","cat",.9f),ModelProposal("box","dog",.8f))
        val selected=ModelClassCompatibility.forProject(proposals,detector,"dog")
        assertEquals(listOf("dog"),selected.map{it.label})
        val merged=ProposalMerger.merge(annotations,selected,"DETECTION")
        assertEquals(human,merged.boxes.first())
        assertEquals(listOf("custom human class","dog"),merged.boxes.map{it.label})
        assertEquals(proposals,ModelClassCompatibility.forProject(proposals,detector.copy(runtime="local_http"),"rabbit"))
    }

    @Test fun quotedLabelsSurviveSelectionStorageAndExportParsing() {
        val labels=listOf("tench, Tinca tinca", "a; b", "say \"hello\"", "traffic light")
        assertEquals(labels,ProjectVocabulary.parse(ProjectVocabulary.format(labels)))
        val config=detector.copy(labels=labels)
        assertEquals(labels,ModelClassCompatibility.inspect(config,"DETECTION",ProjectVocabulary.format(labels)).supported)
        assertEquals(labels,ProjectVocabulary.parse(ProjectVocabulary.append("",labels)))
    }
}
