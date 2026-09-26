package com.unicornwhodev.visiondatasetstudio.data.source

import com.unicornwhodev.visiondatasetstudio.data.hf.ViewerRowData
import java.net.URI

/** Recognize a single image without silently choosing one member of a multi-image sample. */
object SourceImageColumn {
    fun reference(value: Any?): String? {
        val candidate = when (value) {
            is String -> value
            is Map<*, *> -> (value["src"] ?: value["url"]) as? String
            is List<*> -> return value.singleOrNull()?.let(::reference)
            else -> null
        } ?: return null
        return candidate.takeIf { runCatching {
            val uri = URI(it)
            uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.userInfo == null
        }.getOrDefault(false) }
    }

    fun candidates(columns: List<String>, rows: List<ViewerRowData>): List<String> = columns.filter { column ->
        rows.any { column !in it.truncatedCells && reference(it.rowData[column]) != null }
    }

    fun suggest(columns: List<String>, rows: List<ViewerRowData>): String? {
        val usable = candidates(columns, rows)
        return usable.firstOrNull { it.equals("image", true) }
            ?: usable.firstOrNull { it.contains("image", true) || it.contains("photo", true) }
            ?: usable.singleOrNull()
    }
}
