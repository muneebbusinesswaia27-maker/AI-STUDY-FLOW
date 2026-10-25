package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusDao {
    @Query("SELECT * FROM focus_sessions ORDER BY timestampMillis DESC")
    fun getAllFocusSessions(): Flow<List<FocusEntity>>

    @Query("SELECT * FROM focus_sessions WHERE dateString = :dateString")
    fun getFocusSessionsForDate(dateString: String): Flow<List<FocusEntity>>

    @Query("SELECT SUM(durationMinutes) FROM focus_sessions WHERE dateString = :dateString AND sessionType = 'FOCUS'")
    fun getFocusMinutesForDate(dateString: String): Flow<Int?>

    @Query("SELECT SUM(durationMinutes) FROM focus_sessions WHERE sessionType = 'FOCUS'")
    fun getTotalFocusMinutes(): Flow<Int?>

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE sessionType = 'FOCUS'")
    fun getTotalCompletedSessionsCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFocusSession(session: FocusEntity): Long

    @Query("DELETE FROM focus_sessions")
    suspend fun clearAll()
}
