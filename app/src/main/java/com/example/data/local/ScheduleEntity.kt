package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedule_sessions")
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val subject: String,
    val dateString: String, // "YYYY-MM-DD"
    val startTime: String,  // "HH:mm" 24h
    val endTime: String,    // "HH:mm" 24h
    val notes: String = "",
    val isCompleted: Boolean = false,
    val reminderEnabled: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis()
)
