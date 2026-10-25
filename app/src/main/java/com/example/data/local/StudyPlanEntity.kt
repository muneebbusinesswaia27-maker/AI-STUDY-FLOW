package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_plans")
data class StudyPlanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val educationLevel: String,
    val subjects: String,
    val examDate: String,
    val dailyHours: Float,
    val planJson: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)
