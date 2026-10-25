package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedule_sessions ORDER BY dateString ASC, startTime ASC")
    fun getAllSessions(): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedule_sessions WHERE dateString = :dateString ORDER BY startTime ASC")
    fun getSessionsForDate(dateString: String): Flow<List<ScheduleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ScheduleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<ScheduleEntity>)

    @Update
    suspend fun updateSession(session: ScheduleEntity)

    @Delete
    suspend fun deleteSession(session: ScheduleEntity)

    @Query("DELETE FROM schedule_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Long)

    @Query("DELETE FROM schedule_sessions")
    suspend fun clearAll()
}
