package com.unicornwhodev.visiondatasetstudio.core.workflow

/** A project vocabulary is editable; a model's numeric label order is never rewritten here. */
object ProjectVocabulary {
    fun requiredFor(tasks: Set<StudioTask>) = tasks.any { it !in setOf(StudioTask.CAPTIONING, StudioTask.VQA, StudioTask.NEGATIVE) }

    fun parse(text: String): List<String> {
        val labels = mutableListOf<String>()
        val cell = StringBuilder()
        var quoted = false
        var index = 0
        fun flush() { cell.toString().trim().takeIf { it.isNotEmpty() }?.let(labels::add); cell.clear() }
        while (index < text.length) {
            val char = text[index++]
            when {
                char == '"' && quoted && text.getOrNull(index) == '"' -> { cell.append('"'); index++ }
                char == '"' && (quoted || cell.isBlank()) -> quoted = !quoted
                !quoted && char in charArrayOf(',', ';', '\n', '\r') -> flush()
                else -> cell.append(char)
            }
        }
        flush()
        return labels.distinct()
    }

    fun format(labels: List<String>): String = labels.distinct().joinToString(",") { label ->
        if (label.any { it in charArrayOf(',', ';', '\n', '\r', '"') }) "\"${label.replace("\"", "\"\"")}\"" else label
    }

    fun append(current: String, labels: List<String>): String =
        format(parse(current) + labels)
}
