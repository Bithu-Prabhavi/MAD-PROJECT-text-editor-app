package com.example.mad_mini_project.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(viewModel: EditorViewModel) {
    var showMenu by remember { mutableStateOf(false) }
    var showSaveAsDialog by remember { mutableStateOf(false) }
    var showOpenDialog by remember { mutableStateOf(false) }
    var showRecentDialog by remember { mutableStateOf(false) }
    var saveAsName by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.statusMessage) {
        viewModel.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.statusMessage = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = viewModel.currentFileName,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("New File") },
                            onClick = {
                                showMenu = false
                                viewModel.newFile()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Open File") },
                            onClick = {
                                showMenu = false
                                showOpenDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Save") },
                            onClick = {
                                showMenu = false
                                if (viewModel.currentFilePath == null) {
                                    saveAsName = viewModel.currentFileName
                                    showSaveAsDialog = true
                                } else {
                                    viewModel.saveFile()
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Save As...") },
                            onClick = {
                                showMenu = false
                                saveAsName = viewModel.currentFileName
                                showSaveAsDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Recent Files") },
                            onClick = {
                                showMenu = false
                                viewModel.loadRecentFiles()
                                showRecentDialog = true
                            }
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Editor Body
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                OutlinedTextField(
                    value = viewModel.textContent,
                    onValueChange = { viewModel.onTextChange(it) },
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.fillMaxSize(),
                    placeholder = { Text("Type text here...") }
                )
            }
        }
    }

    // Save As Dialog
    if (showSaveAsDialog) {
        AlertDialog(
            onDismissRequest = { showSaveAsDialog = false },
            title = { Text("Save As") },
            text = {
                OutlinedTextField(
                    value = saveAsName,
                    onValueChange = { saveAsName = it },
                    label = { Text("Filename") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (saveAsName.isNotBlank()) {
                            viewModel.saveFile(saveAsName.trim())
                            showSaveAsDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveAsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Open File Dialog
    if (showOpenDialog) {
        val files = remember { viewModel.listAppFiles() }
        AlertDialog(
            onDismissRequest = { showOpenDialog = false },
            title = { Text("Open File") },
            text = {
                if (files.isEmpty()) {
                    Text("No files saved in app storage.")
                } else {
                    LazyColumn {
                        items(files) { file ->
                            TextButton(
                                onClick = {
                                    viewModel.openFile(file)
                                    showOpenDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(file.name, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showOpenDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Recent Files Dialog (Room)
    if (showRecentDialog) {
        val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
        AlertDialog(
            onDismissRequest = { showRecentDialog = false },
            title = { Text("Recent Files") },
            text = {
                if (viewModel.recentFiles.isEmpty()) {
                    Text("No recent files found.")
                } else {
                    LazyColumn {
                        items(viewModel.recentFiles) { recent ->
                            Card(
                                onClick = {
                                    val file = File(recent.filePath)
                                    if (file.exists()) {
                                        viewModel.openFile(file)
                                        showRecentDialog = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(recent.fileName, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        dateFormat.format(Date(recent.lastOpened)),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showRecentDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
