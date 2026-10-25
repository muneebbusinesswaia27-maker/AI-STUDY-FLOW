package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "focus_sessions")
data class FocusEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subject: String = "General Study",
    val durationMinutes: Int,
    val sessionType: String = "FOCUS", // "FOCUS", "SHORT_BREAK", "LONG_BREAK"
    val timestampMillis: Long = System.currentTimeMillis(),
    val dateString: String // "YYYY-MM-DD"
)
