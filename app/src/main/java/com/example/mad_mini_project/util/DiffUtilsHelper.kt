package com.example.mad_mini_project.util

import com.github.difflib.DiffUtils
import com.github.difflib.patch.Patch
import com.github.difflib.UnifiedDiffUtils

object DiffUtilsHelper {
    fun createDiff(originalText: String, newText: String, filename: String = "document.txt"): String {
        val originalLines = originalText.lines()
        val newLines = newText.lines()
        val patch: Patch<String> = DiffUtils.diff(originalLines, newLines)
        val unifiedDiff = UnifiedDiffUtils.generateUnifiedDiff(filename, filename, originalLines, patch, 3)
        return unifiedDiff.joinToString("\n")
    }

    fun applyDiff(baseText: String, diffPatchString: String): String {
        if (diffPatchString.isBlank()) return baseText
        val baseLines = baseText.lines()
        val patchLines = diffPatchString.lines()
        return try {
            val patch = UnifiedDiffUtils.parseUnifiedDiff(patchLines)
            val resultLines = DiffUtils.patch(baseLines, patch)
            resultLines.joinToString("\n")
        } catch (e: Exception) {
            baseText
        }
    }
}
