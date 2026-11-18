package com.example.data.parser

import com.example.data.model.ChannelCategory
import java.io.BufferedReader

data class ParsedM3uChannel(
    val name: String,
    val streamUrl: String,
    val logoUrl: String?,
    val category: String
)

object M3uParser {

    fun parse(content: String, maxChannels: Int = Int.MAX_VALUE): List<ParsedM3uChannel> {
        return parse(content.reader().buffered(), maxChannels)
    }

    fun parse(reader: BufferedReader, maxChannels: Int = Int.MAX_VALUE): List<ParsedM3uChannel> {
        val result = ArrayList<ParsedM3uChannel>(minOf(maxChannels, 4096))
        val seenUrls = HashSet<String>(minOf(maxChannels, 4096))

        var pendingName: String? = null
        var pendingLogo: String? = null
        var pendingGroup: String? = null

        reader.forEachLine { rawLine ->
            if (result.size >= maxChannels) return@forEachLine
            val line = rawLine.trim()
            if (line.isEmpty()) return@forEachLine

            when {
                line.startsWith("#EXTM3U", ignoreCase = true) -> Unit
                line.startsWith("#EXTINF:", ignoreCase = true) -> {
                    pendingLogo = extractAttr(line, "tvg-logo=\"")
                        ?.takeIf { it.startsWith("http", ignoreCase = true) }
                    pendingGroup = extractAttr(line, "group-title=\"")
                    val commaIndex = line.lastIndexOf(',')
                    val rawTitle = if (commaIndex != -1 && commaIndex + 1 < line.length) {
                        line.substring(commaIndex + 1).trim()
                    } else {
                        extractAttr(line, "tvg-name=\"").orEmpty()
                    }
                    pendingName = rawTitle.ifBlank { extractAttr(line, "tvg-name=\"").orEmpty() }
                }
                line.startsWith("#") -> Unit
                isSupportedStreamUrl(line) -> {
                    if (seenUrls.add(line)) {
                        val safeName = pendingName?.takeIf { it.isNotBlank() }
                            ?: deriveNameFromUrl(line)
                        val mappedCategory = ChannelCategory.fromRawGroup(pendingGroup).labelFa
                        result.add(
                            ParsedM3uChannel(
                                name = safeName,
                                streamUrl = line,
                                logoUrl = pendingLogo,
                                category = mappedCategory
                            )
                        )
                    }
                    pendingName = null
                    pendingLogo = null
                    pendingGroup = null
                }
            }
        }
        return result
    }

    private fun extractAttr(line: String, keyWithQuote: String): String? {
        val startIdx = line.indexOf(keyWithQuote, ignoreCase = true)
        if (startIdx == -1) return null
        val valueStart = startIdx + keyWithQuote.length
        val endIdx = line.indexOf('"', valueStart)
        if (endIdx <= valueStart) return null
        return line.substring(valueStart, endIdx).trim()
    }

    fun isSupportedStreamUrl(url: String): Boolean {
        val lower = url.trim().lowercase()
        return lower.startsWith("https://") ||
            lower.startsWith("http://") ||
            lower.startsWith("rtsp://")
    }

    private fun deriveNameFromUrl(url: String): String {
        val clean = url.substringBefore('?').trimEnd('/')
        val segment = clean.substringAfterLast('/', "شبکه زنده")
            .substringBeforeLast('.')
        return segment.ifBlank { "شبکه زنده" }
    }
}
