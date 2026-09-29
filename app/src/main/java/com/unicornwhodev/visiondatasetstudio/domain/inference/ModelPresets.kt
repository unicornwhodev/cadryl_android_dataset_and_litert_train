package com.unicornwhodev.visiondatasetstudio.domain.inference

import com.unicornwhodev.visiondatasetstudio.core.i18n.tr

data class ModelPresetDefinition(
    val id:String,
    val title:String,
    val category:String,
    val description:String,
    val task:String
)

/** Contract templates, never claims that arbitrary weights satisfy a decoder/preprocessing contract. */
object ModelPresets {
    val catalog get() = listOf(
        ModelPresetDefinition("ssd","SSD / DetectionPostProcess",tr("Détection","Detection"),tr("4 sorties : boîtes, classes, scores, nombre.","4 outputs: boxes, classes, scores, count."),"DETECTION"),
        ModelPresetDefinition("yolo_bcn","YOLO · BCN",tr("Détection","Detection"),tr("Sortie [1,C,N] sans objectness séparée.","[1,C,N] output without separate objectness."),"DETECTION"),
        ModelPresetDefinition("yolo_bnc","YOLO · BNC",tr("Détection","Detection"),tr("Sortie [1,N,C] sans objectness séparée.","[1,N,C] output without separate objectness."),"DETECTION"),
        ModelPresetDefinition("yolo_objectness","YOLO · objectness",tr("Détection","Detection"),tr("YOLO avec score objectness distinct.","YOLO with separate objectness score."),"DETECTION"),
        ModelPresetDefinition("rfdetr","RF-DETR · COCO slots",tr("Détection","Detection"),tr("Boîtes + logits avec slots COCO réservés conservés.","Boxes + logits with reserved COCO slots preserved."),"DETECTION"),
        ModelPresetDefinition("rtmdet","RTMDet",tr("Détection","Detection"),tr("Détection multi-niveaux, NMS côté application.","Multi-level detection with app-side NMS."),"DETECTION"),
        ModelPresetDefinition("xyxy","XYXY + score + classe",tr("Détection","Detection"),tr("Sortie directe boîtes XYXY, score et classe.","Direct XYXY box, score and class output."),"DETECTION"),
        ModelPresetDefinition("points","Points directs",tr("Pointing","Pointing"),tr("Coordonnées de points produites directement.","Direct point coordinates."),"POINTING"),
        ModelPresetDefinition("heatmap_nhwc","Heatmap · NHWC",tr("Pointing","Pointing"),tr("Cartes de chaleur avec classes en dernier axe.","Heatmaps with classes on last axis."),"POINTING"),
        ModelPresetDefinition("heatmap_nchw","Heatmap · NCHW",tr("Pointing","Pointing"),tr("Cartes de chaleur avec classes sur l’axe C.","Heatmaps with classes on C axis."),"POINTING"),
        ModelPresetDefinition("classification_softmax","Classification · softmax",tr("Classification","Classification"),tr("Une classe dominante, scores softmax.","Single dominant class, softmax scores."),"CLASSIFICATION"),
        ModelPresetDefinition("classification_sigmoid","Classification · multi-label",tr("Classification","Classification"),tr("Classes indépendantes avec activation sigmoid.","Independent classes with sigmoid activation."),"CLASSIFICATION"),
        ModelPresetDefinition("embedding","Embedding visuel",tr("Représentation","Embedding"),tr("Encodeur visuel sans annotation automatique.","Visual encoder without automatic annotation."),"EMBEDDING"),
        ModelPresetDefinition("tinyclip","TinyCLIP · image/texte",tr("Bundle connu","Known bundle"),tr("Classification zero-shot et similarité image-texte.","Zero-shot classification and image-text similarity."),"CLASSIFICATION"),
        ModelPresetDefinition("efficientvit_sam","EfficientViT-SAM L0",tr("Bundle connu","Known bundle"),tr("Segmentation interactive guidée par point ou boîte.","Interactive segmentation guided by point or box."),"SEGMENTATION"),
        ModelPresetDefinition("florence2","Florence-2 Base",tr("Bundle connu","Known bundle"),tr("Captioning et détection via le bundle Florence-2 pris en charge.","Captioning and detection through the supported Florence-2 bundle."),"CAPTIONING"),
        ModelPresetDefinition("http_detection","Détection · serveur local",tr("Endpoint local","Local endpoint"),tr("Propositions canoniques via HTTP loopback.","Canonical proposals over loopback HTTP."),"DETECTION"),
        ModelPresetDefinition("http_caption","Caption · serveur local",tr("Endpoint local","Local endpoint"),tr("Texte de légende via HTTP loopback.","Caption text over loopback HTTP."),"CAPTIONING"),
        ModelPresetDefinition("http_grounding","Grounding · serveur local",tr("Endpoint local","Local endpoint"),tr("Phrases liées à des boîtes/points explicites.","Phrases linked to explicit boxes/points."),"GROUNDING"),
        ModelPresetDefinition("http_vqa","VQA · serveur local",tr("Endpoint local","Local endpoint"),tr("Réponses VQA canoniques via endpoint local.","Canonical VQA responses via local endpoint."),"VQA"),
        ModelPresetDefinition("http_classification","Classification · serveur local",tr("Endpoint local","Local endpoint"),tr("Tags canoniques via endpoint local.","Canonical tags via local endpoint."),"CLASSIFICATION")
    )

    val names get() = linkedMapOf<String,String>().apply { catalog.forEach { put(it.id,it.title) } }

    fun recommended(tasksCsv:String):List<ModelPresetDefinition> {
        val tasks=tasksCsv.split(',').map{it.trim().uppercase()}.toSet()
        return catalog.sortedBy { if(it.task in tasks || (it.task=="POINTING" && "POINTING_MULTI" in tasks)) 0 else 1 }
    }

    private fun labels(value:List<String>)=value.filter{it.isNotBlank()}.distinct().ifEmpty{listOf("class_0")}

    fun create(id:String,labels:List<String>):ModelConfig {
        val classes=labels(labels)
        return when(id) {
            "ssd"->ModelConfig.defaultDetectionPreset(classes).copy(adapter="ssd",resizeMode="stretch")
            "yolo_bcn"->ModelConfig(adapter="yolo",inputWidth=640,inputHeight=640,inputLayout="NCHW",mean=0f,std=255f,labels=classes,
                coordinates="pixels",outputLayout="BCN",resizeMode="letterbox",dynamicMinSize=32,dynamicMaxSize=1280,dynamicStride=32)
            "yolo_bnc"->ModelConfig(adapter="yolo",inputWidth=640,inputHeight=640,inputLayout="NCHW",mean=0f,std=255f,labels=classes,
                coordinates="pixels",outputLayout="BNC",resizeMode="letterbox",dynamicMinSize=32,dynamicMaxSize=1280,dynamicStride=32)
            "yolo_objectness"->ModelConfig(adapter="yolo",inputWidth=640,inputHeight=640,inputLayout="NCHW",mean=0f,std=255f,labels=classes,
                coordinates="pixels",outputLayout="BNC",resizeMode="letterbox",yoloObjectness=true,scoreActivation="sigmoid",
                dynamicMinSize=32,dynamicMaxSize=1280,dynamicStride=32)
            "rfdetr"->ModelConfig(adapter="rfdetr",inputWidth=560,inputHeight=560,inputLayout="NCHW",mean=0f,std=1f,
                channelMean=listOf(123.675f,116.28f,103.53f),channelStd=listOf(58.395f,57.12f,57.375f),
                labels=CommunityModelCatalog.cocoSlots,resizeMode="stretch",outputIndexBoxes=0,outputIndexScores=1)
            "rtmdet"->ModelConfig(adapter="rtmdet",inputWidth=320,inputHeight=320,inputLayout="NCHW",mean=0f,std=1f,
                channelMean=listOf(103.53f,116.28f,123.675f),channelStd=listOf(57.375f,57.12f,58.395f),
                labels=classes,isRgb=false,coordinates="pixels",scoreActivation="sigmoid",resizeMode="letterbox",
                dynamicMinSize=32,dynamicMaxSize=1280,dynamicStride=32)
            "xyxy"->ModelConfig(adapter="xyxy_score_class",inputWidth=640,inputHeight=640,mean=0f,std=255f,labels=classes,
                coordinates="pixels",resizeMode="letterbox")
            "points"->ModelConfig(task="pointing",adapter="points",inputWidth=224,inputHeight=224,mean=0f,std=255f,labels=classes,resizeMode="stretch",outputMode="points")
            "heatmap_nhwc"->ModelConfig(task="pointing",adapter="heatmap",inputWidth=256,inputHeight=256,mean=0f,std=255f,labels=classes,outputLayout="NHWC",resizeMode="stretch",outputMode="points",scoreActivation="clamp")
            "heatmap_nchw"->ModelConfig(task="pointing",adapter="heatmap",inputWidth=256,inputHeight=256,inputLayout="NCHW",mean=0f,std=255f,labels=classes,outputLayout="NCHW",resizeMode="stretch",outputMode="points",scoreActivation="clamp")
            "classification_softmax"->ModelConfig.defaultClassifierPreset(classes).copy(adapter="classification",scoreActivation="softmax",resizeMode="center_crop")
            "classification_sigmoid"->ModelConfig.defaultClassifierPreset(classes).copy(adapter="classification",scoreActivation="sigmoid",resizeMode="center_crop")
            "embedding"->ModelConfig(task="embedding",adapter="embedding",inputWidth=224,inputHeight=224,inputLayout="NCHW",mean=0f,std=1f,resizeMode="center_crop",embeddingOutputIndex=0)
            "tinyclip"->ModelConfig(task="classification",adapter="tinyclip",bundleKind="tinyclip",inputWidth=224,inputHeight=224,inputLayout="NCHW",
                inputType="FLOAT32",mean=0f,std=1f,resizeMode="center_crop",labels=classes,
                channelMean=listOf(122.77094f,116.74601f,104.09374f),channelStd=listOf(68.50053f,66.63216f,70.32316f))
            "efficientvit_sam"->ModelConfig(task="object_detection",adapter="sam_box",bundleKind="efficientvit_sam",inputWidth=512,inputHeight=512,inputLayout="NCHW",
                inputType="FLOAT32",mean=0f,std=1f,resizeMode="stretch",labels=classes,spatialLabel=classes.first())
            "florence2"->ModelConfig(task="multitask",adapter="florence2",bundleKind="florence2",inputWidth=768,inputHeight=768,inputLayout="NCHW",
                inputType="FLOAT32",mean=0f,std=1f,resizeMode="stretch",labels=classes,prompt="<CAPTION>",captionLanguage="en")
            "http_detection"->ModelConfig(task="object_detection",runtime="local_http",labels=classes,endpoint="http://127.0.0.1:8080/predict",httpOutputMode="proposals",responsePath="predictions")
            "http_caption"->ModelConfig(task="captioning",runtime="local_http",labels=classes,endpoint="http://127.0.0.1:8080/predict",httpOutputMode="caption_text",responsePath="caption",prompt="Describe the visible image without speculation.")
            "http_grounding"->ModelConfig(task="grounding",runtime="local_http",labels=classes,endpoint="http://127.0.0.1:8080/predict",httpOutputMode="grounding_proposals",responsePath="predictions",prompt="Return phrases and explicit box or point proposal references.")
            "http_vqa"->ModelConfig(task="vqa",runtime="local_http",labels=classes,endpoint="http://127.0.0.1:8080/predict",httpOutputMode="proposals",responsePath="predictions")
            "http_classification"->ModelConfig(task="classification",runtime="local_http",labels=classes,endpoint="http://127.0.0.1:8080/predict",httpOutputMode="proposals",responsePath="predictions")
            else->error(tr("Gabarit inconnu", "Unknown template"))
        }.also(ModelContract::validate)
    }
}
