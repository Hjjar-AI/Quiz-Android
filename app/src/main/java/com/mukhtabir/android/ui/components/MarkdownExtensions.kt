package com.mukhtabir.android.ui.components

/** Converts Vue's parenthesized scripts without changing escaped text or code. */
internal fun scriptMarkdown(source: String): String {
    var fence: Char? = null
    var fenceSize = 0
    var codeSize = 0
    var sourceOffset = 0
    fun hasClosingCodeRun(start: Int, size: Int): Boolean {
        var cursor = start
        while (cursor < source.length) {
            if (source[cursor] != '`') { cursor++; continue }
            val count = source.drop(cursor).takeWhile { it == '`' }.length
            if (count == size) return true
            cursor += count
        }
        return false
    }
    return source.split('\n').joinToString("\n") { line ->
        val offset = sourceOffset
        sourceOffset += line.length + 1
        val leading = line.takeWhile { it == ' ' }.length
        val trimmed = line.drop(leading)
        val marker = trimmed.firstOrNull()
        val run = if (marker == '`' || marker == '~') trimmed.takeWhile { it == marker }.length else 0
        if (leading <= 3 && run >= 3 && codeSize == 0 &&
            (fence == null || (marker == fence && run >= fenceSize && trimmed.drop(run).isBlank()))) {
            if (fence == null) { fence = marker; fenceSize = run } else fence = null
            line
        } else if (fence != null || (codeSize == 0 && (leading >= 4 || line.startsWith('\t')))) line
        else buildString {
            var index = 0
            while (index < line.length) {
                val char = line[index]
                if (char == '\\' && codeSize == 0 && index + 1 < line.length) {
                    append(line, index, index + 2); index += 2; continue
                }
                if (char == '`') {
                    val size = line.drop(index).takeWhile { it == '`' }.length
                    if (codeSize == 0) {
                        // Match the exact delimiter run, including multiline inline code.
                        if (hasClosingCodeRun(offset + index + size, size)) codeSize = size
                    } else if (codeSize == size) codeSize = 0
                    append("`".repeat(size)); index += size; continue
                }
                if (codeSize == 0 && char == ']' && line.getOrNull(index + 1) == '(') {
                    var end = index + 2
                    var depth = 1
                    while (end < line.length && depth > 0) {
                        if (line[end] == '\\') { end += 2; continue }
                        if (line[end] == '(') depth++
                        if (line[end] == ')') depth--
                        end++
                    }
                    if (depth == 0) { append(line, index, end); index = end; continue }
                }
                if (codeSize == 0 && char == '<') {
                    val end = line.indexOf('>', index + 1)
                    if (end >= 0) { append(line, index, end + 1); index = end + 1; continue }
                }
                if (codeSize == 0 && (char == '^' || char == '~') && line.getOrNull(index + 1) == '(') {
                    var end = index + 2
                    var depth = 1
                    while (end < line.length && depth > 0) {
                        if (line[end] == '\\') { end += 2; continue }
                        if (line[end] == '(') depth++
                        if (line[end] == ')') depth--
                        end++
                    }
                    if (depth == 0 && line.getOrNull(end) == char && end > index + 3) {
                        val tag = if (char == '^') "sup" else "sub"
                        append("<$tag>"); append(line, index + 2, end - 1); append("</$tag>")
                        index = end + 1; continue
                    }
                }
                append(char); index++
            }
        }
    }
}
