package com.unicornwhodev.visiondatasetstudio

import com.unicornwhodev.visiondatasetstudio.data.hf.HfTreeItem
import com.unicornwhodev.visiondatasetstudio.domain.inference.*
import org.junit.Assert.*
import org.junit.Test

class ModelCatalogRc8Test {
    @Test fun everyBasePresetBuildsAValidContract() {
        val labels=listOf("smoke_visible","flame_visible")
        assertTrue(ModelPresets.catalog.size >= 18)
        ModelPresets.catalog.forEach { preset ->
            val config=ModelPresets.create(preset.id,labels)
            ModelContract.validate(config)
        }
    }

    @Test fun fireviewerIsAStandardDynamicCatalogSource() {
        assertTrue(CommunityModelCatalog.standardSources.any { it.repository==CommunityModelCatalog.fireviewerRepoId })
        assertTrue(CommunityModelCatalog.standardSources.any { it.repository==CommunityModelCatalog.repoId })
        assertEquals(6,CommunityModelCatalog.fireviewerEntries.size)
    }

    @Test fun fireviewerKnownFolderIsRecognizedButNotFalselyQualified() {
        val source=CommunityModelCatalog.Source(CommunityModelCatalog.fireviewerRepoId,"main","models")
        val id="fireviewer_rtdetr_v2_r50_learning"
        val tree=listOf(
            HfTreeItem("models/$id/model.tflite","file",178_000_000),
            HfTreeItem("models/$id/android_model_config.json","file",1100),
            HfTreeItem("models/$id/runtime_contract.json","file",3100)
        )
        val item=CommunityModelCatalog.fromTree(source,"a".repeat(40),tree).single()
        assertEquals(id,item.entry.id)
        assertTrue(item.entry.title.contains("FireViewer"))
        assertTrue(item.installableNow)
        assertEquals(QualificationStatus.UNTESTED,item.entry.capabilities.qualification)
    }

    @Test fun unknownFutureContractModelRemainsDiscoverable() {
        val source=CommunityModelCatalog.Source(CommunityModelCatalog.fireviewerRepoId,"main","models")
        val id="future_fireviewer_variant"
        val item=CommunityModelCatalog.fromTree(source,"b".repeat(40),listOf(
            HfTreeItem("models/$id/model.tflite","file",1024),
            HfTreeItem("models/$id/android_model_config.json","file",512)
        )).single()
        assertEquals(id,item.entry.id)
        assertEquals("contract",item.entry.adapterStatus)
        assertTrue(item.installableNow)
    }
}
