package com.unicornwhodev.visiondatasetstudio.domain.inference

import com.unicornwhodev.visiondatasetstudio.core.i18n.tr

/** Read from the actual graph/signature. Negative signature dimensions alone are resizable. */
data class ModelInputSpec(val shape: List<Int>, val signature: List<Int>, val type: String) {
    fun validate(config: ModelConfig) {
        val expected = if (config.inputLayout == "NCHW")
            listOf(1, config.inputChannels, config.inputHeight, config.inputWidth)
        else listOf(1, config.inputHeight, config.inputWidth, config.inputChannels)
        require(type == config.inputType) {
            tr("Le modèle attend $type ; le réglage indique ${config.inputType}.", "The model expects $type; settings specify ${config.inputType}.")
        }
        require(signature.size == 4 && signature.indices.all { signature[it] < 0 || signature[it] == expected[it] }) {
            tr("Entrée attendue : $signature ($type). Réglage actuel : $expected. Ajustez les dimensions et le layout dans Modèles > Réglages.",
                "Expected input: $signature ($type). Current settings: $expected. Adjust dimensions and layout in Models > Settings.")
        }
        val spatialAxes = if (config.inputLayout == "NCHW") listOf(2, 3) else listOf(1, 2)
        val dynamicSizes = spatialAxes.filter { signature[it] < 0 }.map { expected[it] }
        if (dynamicSizes.isNotEmpty()) {
            require(config.dynamicStride > 0 && dynamicSizes.all {
                it in config.dynamicMinSize..config.dynamicMaxSize && it % config.dynamicStride == 0
            }) {
                tr("Dimensions : ${config.dynamicMinSize} à ${config.dynamicMaxSize} px, par multiples de ${config.dynamicStride}.",
                    "Dimensions: ${config.dynamicMinSize} to ${config.dynamicMaxSize} px, in multiples of ${config.dynamicStride}.")
            }
        }
    }

    /** Safe input-only hint: infer dtype/layout/channels when the channel axis is explicit.
     * Spatial dimensions, preprocessing and output semantics are never guessed. */
    fun imageConfigHint(config: ModelConfig): ModelConfig? {
        if (signature.size != 4 || (signature[0] != 1 && signature[0] >= 0) || type !in setOf("FLOAT32", "UINT8", "INT8")) return null
        val nhwc = signature[3] in setOf(1, 3)
        val nchw = signature[1] in setOf(1, 3)
        if (nhwc == nchw) return null
        return config.copy(
            inputLayout = if (nhwc) "NHWC" else "NCHW",
            inputType = type,
            inputChannels = signature[if (nhwc) 3 else 1],
            quantizationMode = if (type == "INT8") "tensor" else config.quantizationMode
        )
    }

    /** Only unambiguous fixed image shapes; no output semantics or normalization guesses. */
    fun fixedImageConfig(config: ModelConfig): ModelConfig? {
        val hinted=imageConfigHint(config) ?: return null
        val nhwc=hinted.inputLayout=="NHWC"
        val spatial=listOf(if(nhwc)1 else 2,if(nhwc)2 else 3)
        if(spatial.any { signature[it] <= 0 }) return null
        val result = hinted.copy(
            inputWidth = signature[if (nhwc) 2 else 3],
            inputHeight = signature[if (nhwc) 1 else 2]
        )
        return result.takeIf { it.inputWidth in 1..2048 && it.inputHeight in 1..2048 }
    }
}
