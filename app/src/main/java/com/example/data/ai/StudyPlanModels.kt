package com.example.data.ai

data class PlanSessionItem(
    val dayOffset: Int = 0, // 0 = today, 1 = tomorrow, etc.
    val subject: String,
    val topic: String,
    val goals: List<String>,
    val durationMinutes: Int,
    val suggestedStartTime: String,
    val suggestedEndTime: String,
    val sessionType: String // "DEEP_FOCUS", "REVISION", "PRACTICE"
)

data class GeneratedStudyPlan(
    val title: String,
    val overview: String,
    val dailyHours: Float,
    val sessions: List<PlanSessionItem>,
    val revisionStrategy: List<String>,
    val breakRecommendation: String
)
