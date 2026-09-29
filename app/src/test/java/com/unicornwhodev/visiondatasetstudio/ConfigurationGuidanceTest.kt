package com.unicornwhodev.visiondatasetstudio

import com.unicornwhodev.visiondatasetstudio.core.workflow.ProjectVocabulary
import com.unicornwhodev.visiondatasetstudio.data.hf.ViewerRowData
import com.unicornwhodev.visiondatasetstudio.data.hf.HfTreeItem
import com.unicornwhodev.visiondatasetstudio.data.source.SourcePageResult
import com.unicornwhodev.visiondatasetstudio.data.source.SourceImageColumn
import com.unicornwhodev.visiondatasetstudio.data.source.ViewerCoverage
import com.unicornwhodev.visiondatasetstudio.data.model.*
import com.unicornwhodev.visiondatasetstudio.core.workflow.ProcessingSettings
import com.unicornwhodev.visiondatasetstudio.domain.export.ExportReadiness
import com.unicornwhodev.visiondatasetstudio.domain.inference.*
import org.junit.Assert.*
import org.junit.Test

class ConfigurationGuidanceTest {
    @Test fun pastedClassesKeepMultiwordLabelsAndStableIndices() {
        assertEquals(listOf("traffic light", "cat", "dog"), ProjectVocabulary.parse("traffic light;cat\r\ndog,cat\n"))
        assertEquals("dog,cat,traffic light", ProjectVocabulary.append("dog,cat", listOf("cat", "traffic light")))
    }

    @Test fun imageColumnUsesPreviewValuesAndRejectsAmbiguousLists() {
        val rows = listOf(ViewerRowData(0, mapOf("image_id" to "123", "photo" to mapOf("src" to "https://example.org/a.png"))))
        assertEquals("photo", SourceImageColumn.suggest(listOf("image_id", "photo"), rows))
        assertEquals(1, SourceImageColumn.usableCount("photo", rows))
        assertEquals(0, SourceImageColumn.usableCount("image_id", rows))
        assertEquals("https://example.org/a.png", SourceImageColumn.reference(listOf(mapOf("src" to "https://example.org/a.png"))))
        assertNull(SourceImageColumn.reference(listOf("https://example.org/a.png", "https://example.org/b.png")))
        assertNull(SourceImageColumn.reference("https://token@example.org/a.png"))
        assertNull(SourceImageColumn.reference("file:///private/image.png"))
        assertNull(SourceImageColumn.suggest(listOf("photo"), listOf(rows.single().copy(truncatedCells=listOf("photo")))))
    }

    @Test fun viewerCoverageRequiresContiguousRowsAndDeclaredEnd() {
        val rows=listOf(
            ViewerRowData(100,mapOf("image" to mapOf("src" to "https://example.org/100.jpg"))),
            ViewerRowData(101,mapOf("image" to mapOf("src" to "https://example.org/101.jpg")))
        )
        assertTrue(ViewerCoverage.contiguous(rows,100))
        assertFalse(ViewerCoverage.contiguous(listOf(rows[0],rows[1].copy(rowIdx=103)),100))
        assertTrue(ViewerCoverage.reachesRequestedOrEnd(100,2,2,200))
        assertTrue(ViewerCoverage.reachesRequestedOrEnd(199,2,1,200))
        assertFalse(ViewerCoverage.reachesRequestedOrEnd(100,2,1,200))
    }

    @Test fun partialViewerIsExplicitOptIn() {
        assertFalse(ProcessingSettings().allowPartialViewer)
        assertTrue(ProcessingSettings(allowPartialViewer=true).validate().allowPartialViewer)
    }

    @Test fun sourcePageCanAdvancePastRejectedRowsWithoutFabricatingEntries() {
        val page=SourcePageResult(emptyList(),consumed=3,rejected=3,diagnostics=listOf("3 skipped"))
        assertEquals(3,page.consumed)
        assertEquals(3,page.rejected)
        assertTrue(page.entries.isEmpty())
        assertTrue(runCatching { SourcePageResult(emptyList(),consumed=1,rejected=2) }.isFailure)
    }

    @Test fun communityCatalogRecognizesPortableContractFileNames() {
        val source=CommunityModelCatalog.Source(repository="example/models",revision="main",folder="models")
        val tree=listOf(
            HfTreeItem("models/custom/model.tflite","file",1024),
            HfTreeItem("models/custom/contract.json","file",512)
        )
        val item=CommunityModelCatalog.fromTree(source,"0123456789012345678901234567890123456789",tree).single()
        assertEquals("contract",item.entry.adapterStatus)
        assertTrue(item.installableNow)
    }

    @Test fun fixedModelDimensionsAreCheckedBeforeSavingWithoutInventingSemantics() {
        val config = ModelConfig(inputWidth=640,inputHeight=640,labels=listOf("cat"))
        val input = ModelInputSpec(listOf(1,320,320,3), listOf(1,320,320,3), "FLOAT32")
        assertTrue(runCatching { input.validate(config) }.isFailure)
        val corrected = requireNotNull(input.fixedImageConfig(config))
        input.validate(corrected)
        assertEquals(320, corrected.inputWidth)
        assertEquals(config.labels, corrected.labels)
        assertEquals(config.adapter, corrected.adapter)
        assertEquals(config.mean, corrected.mean)
        assertEquals(config.training, corrected.training)
    }

    @Test fun dynamicModelKeepsDeclaredBoundsStrideChannelsAndType() {
        val input=ModelInputSpec(listOf(1,3,640,640),listOf(1,3,-1,-1),"FLOAT32")
        val config=ModelConfig(inputLayout="NCHW",inputWidth=704,inputHeight=576,dynamicMinSize=32,dynamicMaxSize=1280,dynamicStride=32)
        input.validate(config)
        assertNull(input.fixedImageConfig(config))
        val importedHint=requireNotNull(input.imageConfigHint(ModelConfig(inputWidth=512,inputHeight=384)))
        assertEquals("NCHW",importedHint.inputLayout)
        assertEquals("FLOAT32",importedHint.inputType)
        assertEquals(3,importedHint.inputChannels)
        assertEquals(512,importedHint.inputWidth)
        assertEquals(384,importedHint.inputHeight)
        for (invalid in listOf(config.copy(inputWidth=700),config.copy(inputHeight=1312),config.copy(inputChannels=1),config.copy(inputType="UINT8")))
            assertTrue(runCatching { input.validate(invalid) }.isFailure)
    }

    @Test fun exportPreviewFindsActualAnnotationClassesWithoutChangingThem() {
        val annotation=SampleAnnotations(boxes=listOf(BoxTarget(id="box",label="new class",xmin=.1f,ymin=.1f,xmax=.5f,ymax=.5f)))
        val state=ExportReadiness.inspect("old class",listOf(annotation))
        assertEquals(listOf("new class"),state.missingClasses)
        assertEquals(1,state.boxes)
        assertEquals("new class",annotation.boxes.single().label)
        assertTrue(ExportReadiness.inspect("old class,new class",listOf(annotation)).missingClasses.isEmpty())
    }
    @Test fun yoloOnlyDoesNotRequireClassesUsedOnlyByMasks() {
        val annotations=SampleAnnotations(masks=listOf(MaskTarget("mask","mask-only",2,2,listOf(0,4))))
        val state=ExportReadiness.inspect("box",listOf(annotations))
        assertTrue(state.missingFor(coco=false,yolo=true).isEmpty())
        assertEquals(listOf("mask-only"),state.missingFor(coco=true,yolo=false))
    }
    @Test fun variableBatchDoesNotMakeFixedImageDimensionsResizable() {
        val input=ModelInputSpec(listOf(1,299,299,3),listOf(-1,299,299,3),"FLOAT32")
        val config=ModelConfig(inputWidth=299,inputHeight=299,dynamicStride=32)
        input.validate(config)
        assertEquals(299,input.fixedImageConfig(config.copy(inputWidth=320))!!.inputWidth)
        assertTrue(runCatching { input.validate(config.copy(inputWidth=320)) }.isFailure)
        // Only the resizable spatial axis is constrained by the dynamic stride.
        ModelInputSpec(listOf(1,320,299,3),listOf(-1,-1,299,3),"FLOAT32")
            .validate(config.copy(inputHeight=320))
    }
}
