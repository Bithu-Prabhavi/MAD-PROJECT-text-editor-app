package com.example.mad_mini_project.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mad_mini_project.data.AppDatabase
import com.example.mad_mini_project.data.RecentFile
import kotlinx.coroutines.launch
import java.io.File

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val dao = db.editorDao()

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

    // Room list for Recent Files
    var recentFiles by mutableStateOf<List<RecentFile>>(emptyList())
        private set

    init {
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

            viewModelScope.launch {
                dao.insertRecentFile(RecentFile(file.absolutePath, file.name))
                dao.trimRecentFiles()
                loadRecentFiles()
            }
            statusMessage = "Saved ${file.name}"
        } catch (e: Exception) {
            statusMessage = "Error saving file: ${e.localizedMessage}"
        }
    }

    // --- Room Database Queries ---
    fun loadRecentFiles() {
        viewModelScope.launch {
            recentFiles = dao.getRecentFiles()
        }
    }

    fun listAppFiles(): List<File> {
        val dir = getApplication<Application>().filesDir
        return dir.listFiles()?.filter { it.isFile && it.name != "temp_autosave.txt" }?.toList() ?: emptyList()
    }
}
