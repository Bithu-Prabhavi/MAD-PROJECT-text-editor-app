package com.example.mad_mini_project.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "document_versions")
data class DocumentVersion(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val filePath: String,
    val timestamp: Long = System.currentTimeMillis(),
    val patchString: String
)
