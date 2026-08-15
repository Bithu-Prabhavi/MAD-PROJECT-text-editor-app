package com.example.mad_mini_project.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.mad_mini_project.util.SyntaxVisualTransformation
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(viewModel: EditorViewModel) {
    var showMenu by remember { mutableStateOf(false) }
    var showSearchMenu by remember { mutableStateOf(false) }
    var showSaveAsDialog by remember { mutableStateOf(false) }
    var showOpenDialog by remember { mutableStateOf(false) }
    var showRecentDialog by remember { mutableStateOf(false) }
    var showVersionDialog by remember { mutableStateOf(false) }
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
                    Column {
                        Text(
                            text = viewModel.currentFileName,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (viewModel.isReadOnly) {
                            Text(
                                text = "Read-Only",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.undo() }, enabled = !viewModel.isReadOnly) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                    }
                    IconButton(onClick = { viewModel.redo() }, enabled = !viewModel.isReadOnly) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                    }
                    Box {
                        IconButton(onClick = { showSearchMenu = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search Options")
                        }
                        DropdownMenu(
                            expanded = showSearchMenu,
                            onDismissRequest = { showSearchMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Search") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                onClick = {
                                    showSearchMenu = false
                                    viewModel.openSearch(replaceMode = false)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Search & Replace") },
                                leadingIcon = { Icon(Icons.Default.FindReplace, contentDescription = null) },
                                onClick = {
                                    showSearchMenu = false
                                    viewModel.openSearch(replaceMode = true)
                                }
                            )
                        }
                    }
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
                        DropdownMenuItem(
                            text = { Text("Version History (Diff)") },
                            onClick = {
                                showMenu = false
                                viewModel.loadVersionHistory()
                                showVersionDialog = true
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(if (viewModel.isReadOnly) "Disable Read-Only" else "Enable Read-Only") },
                            onClick = {
                                showMenu = false
                                viewModel.isReadOnly = !viewModel.isReadOnly
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (viewModel.isWordWrap) "Disable Word Wrap" else "Enable Word Wrap") },
                            onClick = {
                                showMenu = false
                                viewModel.isWordWrap = !viewModel.isWordWrap
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
            // Search and Replace Bar
            if (viewModel.isSearchOpen) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        if (!viewModel.isReplaceMode) {
                            // Search Mode Only
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = viewModel.searchQuery,
                                    onValueChange = {
                                        viewModel.searchQuery = it
                                        viewModel.searchMatchIndex = 0
                                    },
                                    label = { Text("Search") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = { viewModel.isSearchOpen = false }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close Search")
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            ) {
                                if (viewModel.searchQuery.isNotEmpty()) {
                                    Text(
                                        text = "Match ${viewModel.currentMatchNumber} of ${viewModel.matchCount}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { viewModel.previousMatch() },
                                            enabled = viewModel.matchCount > 0
                                        ) {
                                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous Match")
                                        }
                                        IconButton(
                                            onClick = { viewModel.nextMatch() },
                                            enabled = viewModel.matchCount > 0
                                        ) {
                                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next Match")
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "Type to search",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            // Search and Replace Mode (Stacked vertically for clean mobile layout)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Search & Replace",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(onClick = { viewModel.isSearchOpen = false }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close Search")
                                }
                            }

                            OutlinedTextField(
                                value = viewModel.searchQuery,
                                onValueChange = {
                                    viewModel.searchQuery = it
                                    viewModel.searchMatchIndex = 0
                                },
                                label = { Text("Search") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            OutlinedTextField(
                                value = viewModel.replaceQuery,
                                onValueChange = { viewModel.replaceQuery = it },
                                label = { Text("Replace with") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (viewModel.searchQuery.isNotEmpty()) {
                                        Text(
                                            text = "${viewModel.currentMatchNumber}/${viewModel.matchCount}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        IconButton(
                                            onClick = { viewModel.previousMatch() },
                                            enabled = viewModel.matchCount > 0
                                        ) {
                                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous Match")
                                        }
                                        IconButton(
                                            onClick = { viewModel.nextMatch() },
                                            enabled = viewModel.matchCount > 0
                                        ) {
                                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next Match")
                                        }
                                    } else {
                                        Text(
                                            text = "Type to search",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Button(
                                        onClick = { viewModel.replaceNext() },
                                        enabled = !viewModel.isReadOnly && viewModel.matchCount > 0,
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("Replace")
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Button(
                                        onClick = { viewModel.replaceAll() },
                                        enabled = !viewModel.isReadOnly && viewModel.matchCount > 0,
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("Replace All")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Editor Body
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                val visualTransformation = remember(viewModel.searchQuery, viewModel.searchMatchIndex, viewModel.isSearchOpen) {
                    SyntaxVisualTransformation(
                        searchQuery = if (viewModel.isSearchOpen) viewModel.searchQuery else "",
                        currentMatchIndex = if (viewModel.matchCount > 0) (viewModel.searchMatchIndex % viewModel.matchCount) else 0
                    )
                }

                if (viewModel.isWordWrap) {
                    OutlinedTextField(
                        value = viewModel.textContent,
                        onValueChange = { viewModel.onTextChange(it) },
                        readOnly = viewModel.isReadOnly,
                        visualTransformation = visualTransformation,
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.fillMaxSize(),
                        placeholder = { Text("Type Kotlin code or Markdown text here...") }
                    )
                } else {
                    val horizontalScrollState = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .horizontalScroll(horizontalScrollState)
                    ) {
                        OutlinedTextField(
                            value = viewModel.textContent,
                            onValueChange = { viewModel.onTextChange(it) },
                            readOnly = viewModel.isReadOnly,
                            visualTransformation = visualTransformation,
                            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
                            modifier = Modifier
                                .fillMaxHeight()
                                .widthIn(min = 800.dp),
                            placeholder = { Text("Type Kotlin code or Markdown text here...") }
                        )
                    }
                }
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

    // Version History Dialog (Room & Diff)
    if (showVersionDialog) {
        DiffViewerDialog(
            versions = viewModel.versionHistory,
            onDismiss = { showVersionDialog = false },
            onRestoreVersion = { ver ->
                viewModel.restoreVersion(ver)
            }
        )
    }
}
