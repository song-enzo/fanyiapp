package com.ocrtranslator.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY createdAt DESC LIMIT 100")
    suspend fun latest(): List<HistoryEntity>

    @Insert
    suspend fun insert(entity: HistoryEntity)

    @Delete
    suspend fun delete(entity: HistoryEntity)

    @Query("DELETE FROM history")
    suspend fun clear()

    @Query("DELETE FROM history WHERE id NOT IN (SELECT id FROM history ORDER BY createdAt DESC LIMIT 100)")
    suspend fun trimToLimit()
}
