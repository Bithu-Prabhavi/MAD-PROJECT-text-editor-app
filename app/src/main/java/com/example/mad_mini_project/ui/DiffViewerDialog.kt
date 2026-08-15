package com.example.mad_mini_project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mad_mini_project.data.DocumentVersion
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DiffViewerDialog(
    versions: List<DocumentVersion>,
    onDismiss: () -> Unit,
    onRestoreVersion: (DocumentVersion) -> Unit
) {
    var selectedVersion by remember { mutableStateOf(versions.firstOrNull()) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Version History & Diff Viewer") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().height(400.dp)) {
                if (versions.isEmpty()) {
                    Text("No version history available for this file yet.")
                } else {
                    Text("Select Version:", style = MaterialTheme.typography.titleSmall)
                    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        items(versions) { ver ->
                            Card(
                                onClick = { selectedVersion = ver },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedVersion?.id == ver.id)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Version #${ver.id}", style = MaterialTheme.typography.bodyMedium)
                                        Text(dateFormat.format(Date(ver.timestamp)), style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    selectedVersion?.let { ver ->
                        Text("Line-by-Line Diff:", style = MaterialTheme.typography.titleSmall)
                        Surface(
                            modifier = Modifier.weight(1.5f).fillMaxWidth().padding(top = 4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small
                        ) {
                            LazyColumn(modifier = Modifier.padding(8.dp)) {
                                val lines = ver.patchString.lines()
                                items(lines) { line ->
                                    val (bgColor, textColor) = when {
                                        line.startsWith("+") && !line.startsWith("+++") -> Color(0xFFE6F4EA) to Color(0xFF137333)
                                        line.startsWith("-") && !line.startsWith("---") -> Color(0xFFFCE8E6) to Color(0xFFC5221F)
                                        line.startsWith("@@") -> Color(0xFFE8EAED) to Color(0xFF5F6368)
                                        else -> Color.Transparent to MaterialTheme.colorScheme.onSurface
                                    }
                                    Text(
                                        text = line,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = textColor,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(bgColor)
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            selectedVersion?.let { ver ->
                Button(onClick = {
                    onRestoreVersion(ver)
                    onDismiss()
                }) {
                    Text("Restore Version")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
