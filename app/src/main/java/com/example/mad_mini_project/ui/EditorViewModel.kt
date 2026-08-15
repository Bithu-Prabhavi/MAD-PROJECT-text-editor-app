package com.example.mad_mini_project.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mad_mini_project.data.AppDatabase
import com.example.mad_mini_project.data.DocumentVersion
import com.example.mad_mini_project.data.RecentFile
import com.example.mad_mini_project.util.DiffUtilsHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val dao = db.editorDao()
    private val tempFile = File(application.filesDir, "temp_autosave.txt")

    var textContent by mutableStateOf("")
        private set

    var currentFilePath by mutableStateOf<String?>(null)
        private set

    var currentFileName by mutableStateOf("Untitled.txt")
        private set

    var isReadOnly by mutableStateOf(false)
    var isWordWrap by mutableStateOf(true)

    // Undo / Redo stacks (Max 25 states)
    private val undoStack = ArrayDeque<String>()
    private val redoStack = ArrayDeque<String>()

    // Search and Replace state
    var isSearchOpen by mutableStateOf(false)
    var isReplaceMode by mutableStateOf(false)
    var searchQuery by mutableStateOf("")
    var replaceQuery by mutableStateOf("")
    var searchMatchIndex by mutableIntStateOf(0)
    var statusMessage by mutableStateOf<String?>(null)

    fun openSearch(replaceMode: Boolean = false) {
        isReplaceMode = replaceMode
        isSearchOpen = true
    }

    fun previousMatch() {
        val matches = getSearchMatches()
        if (matches.isEmpty()) return
        val total = matches.size
        searchMatchIndex = (searchMatchIndex - 1 + total) % total
    }

    fun nextMatch() {
        val matches = getSearchMatches()
        if (matches.isEmpty()) return
        val total = matches.size
        searchMatchIndex = (searchMatchIndex + 1) % total
    }

    // Room lists
    var recentFiles by mutableStateOf<List<RecentFile>>(emptyList())
        private set
    var versionHistory by mutableStateOf<List<DocumentVersion>>(emptyList())
        private set

    init {
        restoreTempAutoSave()
        startAutoSaveTimer()
        loadRecentFiles()
    }

    // Match calculations for Search & Replace
    fun getSearchMatches(): List<Int> {
        if (searchQuery.isEmpty() || textContent.isEmpty()) return emptyList()
        val matches = mutableListOf<Int>()
        var index = textContent.indexOf(searchQuery, 0, ignoreCase = true)
        while (index != -1) {
            matches.add(index)
            index = textContent.indexOf(searchQuery, index + searchQuery.length, ignoreCase = true)
        }
        return matches
    }

    val matchCount: Int
        get() = getSearchMatches().size

    val currentMatchNumber: Int
        get() {
            val total = matchCount
            if (total == 0) return 0
            return (searchMatchIndex % total) + 1
        }

    // --- Undo / Redo ---
    fun onTextChange(newText: String) {
        if (isReadOnly || newText == textContent) return
        if (undoStack.lastOrNull() != textContent) {
            undoStack.addLast(textContent)
            if (undoStack.size > 25) {
                undoStack.removeFirst()
            }
        }
        redoStack.clear()
        textContent = newText
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            redoStack.addLast(textContent)
            textContent = undoStack.removeLast()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack.addLast(textContent)
            textContent = redoStack.removeLast()
        }
    }

    // --- Search & Replace ---
    fun replaceNext() {
        val matches = getSearchMatches()
        if (matches.isEmpty()) return

        val currentIndex = searchMatchIndex % matches.size
        val matchStart = matches[currentIndex]

        val updatedText = textContent.replaceRange(
            matchStart,
            matchStart + searchQuery.length,
            replaceQuery
        )
        onTextChange(updatedText)

        // Advance to next match position
        val nextMatches = getSearchMatches()
        searchMatchIndex = if (nextMatches.isNotEmpty()) {
            currentIndex % nextMatches.size
        } else {
            0
        }
    }

    fun replaceAll() {
        if (searchQuery.isEmpty()) return
        onTextChange(textContent.replace(searchQuery, replaceQuery, ignoreCase = true))
        searchMatchIndex = 0
    }

    // --- File Operations ---
    fun newFile() {
        undoStack.clear()
        redoStack.clear()
        textContent = ""
        currentFilePath = null
        currentFileName = "Untitled.txt"
        versionHistory = emptyList()
        statusMessage = "New file created"
    }

    fun openFile(file: File) {
        try {
            if (!file.exists()) return
            val content = file.readText()
            undoStack.clear()
            redoStack.clear()
            textContent = content
            currentFilePath = file.absolutePath
            currentFileName = file.name
            statusMessage = "Opened ${file.name}"

            viewModelScope.launch {
                dao.insertRecentFile(RecentFile(file.absolutePath, file.name))
                dao.trimRecentFiles()
                loadRecentFiles()
                loadVersionHistory(file.absolutePath)
            }
        } catch (e: Exception) {
            statusMessage = "Error opening file: ${e.localizedMessage}"
        }
    }

    fun saveFile(filename: String = currentFileName) {
        try {
            val file = File(getApplication<Application>().filesDir, filename)
            file.writeText(textContent)
            currentFilePath = file.absolutePath
            currentFileName = file.name

            // Delete temporary autosave file upon successful manual save
            if (tempFile.exists()) {
                tempFile.delete()
            }

            viewModelScope.launch {
                // Fetch existing versions chronologically (oldest to newest)
                val existingVersions = dao.getVersionsForFile(file.absolutePath)

                val patchString: String = if (existingVersions.isEmpty()) {
                    // Version 1: Store complete base document
                    textContent
                } else {
                    // Version N: Reconstruct latest document text and compute unified diff patch
                    val latestVersionText = reconstructTextFromVersions(existingVersions, existingVersions.last().id)
                    DiffUtilsHelper.createDiff(latestVersionText, textContent, file.name)
                }

                dao.insertRecentFile(RecentFile(file.absolutePath, file.name))
                dao.trimRecentFiles()
                dao.insertVersion(DocumentVersion(filePath = file.absolutePath, patchString = patchString))

                loadRecentFiles()
                loadVersionHistory(file.absolutePath)
            }
            statusMessage = "Saved ${file.name}"
        } catch (e: Exception) {
            statusMessage = "Error saving file: ${e.localizedMessage}"
        }
    }

    // --- Auto-Save & Recovery ---
    private fun startAutoSaveTimer() {
        viewModelScope.launch {
            while (isActive) {
                delay(10000) // 10 seconds
                try {
                    if (textContent.isNotEmpty()) {
                        tempFile.writeText(textContent)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun restoreTempAutoSave() {
        try {
            if (tempFile.exists() && tempFile.length() > 0) {
                textContent = tempFile.readText()
                statusMessage = "Restored auto-saved draft"
            }
        } catch (_: Exception) {}
    }

    // --- Room Database Queries ---
    fun loadRecentFiles() {
        viewModelScope.launch {
            recentFiles = dao.getRecentFiles()
        }
    }

    fun loadVersionHistory(path: String? = currentFilePath) {
        if (path == null) return
        viewModelScope.launch {
            versionHistory = dao.getVersionsForFile(path).reversed() // Display newest first in UI
        }
    }

    // Reconstruct document text up to target version ID
    private fun reconstructTextFromVersions(versionsAsc: List<DocumentVersion>, targetVersionId: Int): String {
        if (versionsAsc.isEmpty()) return ""
        // Version 1 holds the full base text
        var text = versionsAsc.first().patchString
        for (i in 1 until versionsAsc.size) {
            val v = versionsAsc[i]
            text = DiffUtilsHelper.applyDiff(text, v.patchString)
            if (v.id == targetVersionId) break
        }
        return text
    }

    fun restoreVersion(version: DocumentVersion) {
        val path = currentFilePath ?: return
        viewModelScope.launch {
            val versionsAsc = dao.getVersionsForFile(path)
            val reconstructed = reconstructTextFromVersions(versionsAsc, version.id)
            onTextChange(reconstructed)
            statusMessage = "Restored version snapshot"
        }
    }

    fun listAppFiles(): List<File> {
        val dir = getApplication<Application>().filesDir
        return dir.listFiles()?.filter { it.isFile && it.name != "temp_autosave.txt" }?.toList() ?: emptyList()
    }
}
