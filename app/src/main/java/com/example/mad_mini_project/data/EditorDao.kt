package com.example.mad_mini_project.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface EditorDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentFile(file: RecentFile): Long

    @Query("SELECT * FROM recent_files ORDER BY lastOpened DESC LIMIT 20")
    suspend fun getRecentFiles(): List<RecentFile>

    @Query("DELETE FROM recent_files WHERE filePath NOT IN (SELECT filePath FROM recent_files ORDER BY lastOpened DESC LIMIT 20)")
    suspend fun trimRecentFiles()
}
