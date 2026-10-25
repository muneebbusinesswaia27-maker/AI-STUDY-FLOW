package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val subject: String,
    val priority: String = "MEDIUM", // "HIGH", "MEDIUM", "LOW"
    val dueDateMillis: Long = 0L,
    val estimatedDurationMinutes: Int = 30,
    val isCompleted: Boolean = false,
    val completedAtMillis: Long = 0L,
    val createdAtMillis: Long = System.currentTimeMillis()
)
