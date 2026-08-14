package com.example.mad_mini_project.ui

import android.app.Application
import androidx.compose.runtime.getValue
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

    var statusMessage by mutableStateOf<String?>(null)

    // Room list for Recent Files
    var recentFiles by mutableStateOf<List<RecentFile>>(emptyList())
        private set

    init {
        loadRecentFiles()
    }

    fun onTextChange(newText: String) {
        textContent = newText
    }

    // --- File Operations ---
    fun newFile() {
        textContent = ""
        currentFilePath = null
        currentFileName = "Untitled.txt"
        statusMessage = "New file created"
    }

    fun openFile(file: File) {
        try {
            if (!file.exists()) return
            val content = file.readText()
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
