package com.example.mad_mini_project.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_files")
data class RecentFile(
    @PrimaryKey val filePath: String,
    val fileName: String,
    val lastOpened: Long = System.currentTimeMillis()
)
