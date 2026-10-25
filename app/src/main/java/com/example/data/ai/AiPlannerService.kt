package com.example.data.ai

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private const val TAG = "AiPlannerService"

class AiPlannerService {

    private val planSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string(description = "A motivating title for this study plan"),
            "overview" to Schema.string(description = "Executive summary and strategy for the upcoming exam"),
            "dailyHours" to Schema.double(description = "Allocated daily study hours"),
            "breakRecommendation" to Schema.string(description = "Recommended study block duration and break strategy"),
            "revisionStrategy" to Schema.array(
                items = Schema.string(),
                description = "Actionable revision and active recall techniques"
            ),
            "sessions" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "dayOffset" to Schema.integer(description = "Day offset: 0 for today, 1 for tomorrow, etc."),
                        "subject" to Schema.string(description = "Subject name"),
                        "topic" to Schema.string(description = "Specific topic to master"),
                        "goals" to Schema.array(items = Schema.string(), description = "Actionable learning goals"),
                        "durationMinutes" to Schema.integer(description = "Duration in minutes"),
                        "suggestedStartTime" to Schema.string(description = "Suggested start time format HH:mm"),
                        "suggestedEndTime" to Schema.string(description = "Suggested end time format HH:mm"),
                        "sessionType" to Schema.string(description = "DEEP_FOCUS, REVISION, or PRACTICE")
                    )
                ),
                description = "Daily and weekly study sessions"
            )
        )
    )

    private val model by lazy {
        try {
            Firebase.ai.generativeModel(
                modelName = "gemini-3.8-flash",
                generationConfig = generationConfig {
                    responseMimeType = "application/json"
                    responseSchema = planSchema
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase generative model", e)
            null
        }
    }

    suspend fun generatePlan(
        educationLevel: String,
        subjects: List<String>,
        examDate: String,
        dailyHours: Float,
        difficultSubjects: List<String>,
        goalsAndWeaknesses: String,
        studyBlockMinutes: Int,
        breakMinutes: Int
    ): GeneratedStudyPlan = withContext(Dispatchers.IO) {
        val subjectListStr = subjects.joinToString(", ")
        val difficultListStr = if (difficultSubjects.isEmpty()) "None specified" else difficultSubjects.joinToString(", ")

        val prompt = """
            You are an elite academic productivity strategist. Create a comprehensive, realistic, and highly motivating personalized daily and weekly study timetable.
            
            Student Profile:
            - Education Level: $educationLevel
            - Subjects: $subjectListStr
            - Target Exam Date: $examDate
            - Daily Available Study Hours: $dailyHours hours/day
            - High Difficulty / Priority Subjects: $difficultListStr
            - Specific Goals & Notes: $goalsAndWeaknesses
            - Preferred Study Block Duration: $studyBlockMinutes minutes
            - Preferred Break Duration: $breakMinutes minutes
            
            Generate a 7-day schedule (dayOffset from 0 to 6) distributing the available daily hours across subjects, prioritizing difficult subjects earlier in the day, incorporating spaced revision, practical practice tasks, and specific learning outcomes for each session.
        """.trimIndent()

        try {
            val generativeModel = model
            if (generativeModel != null) {
                val response = withTimeout(60_000L) {
                    generativeModel.generateContent(prompt)
                }
                val jsonString = response.text
                if (!jsonString.isNullOrBlank()) {
                    return@withContext parseJsonPlan(jsonString, dailyHours)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Firebase AI model call failed or not provisioned, switching to smart algorithmic curriculum generator: ${e.message}")
        }

        // Algorithmic smart curriculum generator ensures 100% reliable functionality
        return@withContext generateSmartAlgorithmicPlan(
            educationLevel, subjects, examDate, dailyHours, difficultSubjects,
            goalsAndWeaknesses, studyBlockMinutes, breakMinutes
        )
    }

    private fun parseJsonPlan(jsonString: String, fallbackDailyHours: Float): GeneratedStudyPlan {
        val root = JSONObject(jsonString)
        val title = root.optString("title", "Personalized Academic Mastery Plan")
        val overview = root.optString("overview", "Optimized study schedule aligned with exam milestones.")
        val dailyHours = root.optDouble("dailyHours", fallbackDailyHours.toDouble()).toFloat()
        val breakRec = root.optString("breakRecommendation", "Study in 25-45 min intervals with 5-10 min refreshing breaks.")

        val revisionList = mutableListOf<String>()
        val revArray = root.optJSONArray("revisionStrategy")
        if (revArray != null) {
            for (i in 0 until revArray.length()) {
                revisionList.add(revArray.optString(i))
            }
        }
        if (revisionList.isEmpty()) {
            revisionList.add("Use Feynman technique: teach concepts simply to test true mastery.")
            revisionList.add("Active recall: close notes and write down key formulas from memory.")
            revisionList.add("Spaced repetition: review today's notes after 24 hours, 3 days, and 7 days.")
        }

        val sessionList = mutableListOf<PlanSessionItem>()
        val sessArray = root.optJSONArray("sessions")
        if (sessArray != null) {
            for (i in 0 until sessArray.length()) {
                val sObj = sessArray.optJSONObject(i) ?: continue
                val goals = mutableListOf<String>()
                val gArr = sObj.optJSONArray("goals")
                if (gArr != null) {
                    for (g in 0 until gArr.length()) {
                        goals.add(gArr.optString(g))
                    }
                }
                sessionList.add(
                    PlanSessionItem(
                        dayOffset = sObj.optInt("dayOffset", 0),
                        subject = sObj.optString("subject", "Core Subject"),
                        topic = sObj.optString("topic", "Foundational Concepts"),
                        goals = if (goals.isEmpty()) listOf("Master core theory", "Solve 5 practice problems") else goals,
                        durationMinutes = sObj.optInt("durationMinutes", 45),
                        suggestedStartTime = sObj.optString("suggestedStartTime", "09:00"),
                        suggestedEndTime = sObj.optString("suggestedEndTime", "10:00"),
                        sessionType = sObj.optString("sessionType", "DEEP_FOCUS")
                    )
                )
            }
        }

        return GeneratedStudyPlan(
            title = title,
            overview = overview,
            dailyHours = dailyHours,
            sessions = sessionList,
            revisionStrategy = revisionList,
            breakRecommendation = breakRec
        )
    }

    private fun generateSmartAlgorithmicPlan(
        educationLevel: String,
        subjects: List<String>,
        examDate: String,
        dailyHours: Float,
        difficultSubjects: List<String>,
        notes: String,
        blockMinutes: Int,
        breakMinutes: Int
    ): GeneratedStudyPlan {
        val safeSubjects = if (subjects.isEmpty()) listOf("Mathematics", "Science", "Literature") else subjects
        val primaryDifficulty = difficultSubjects.firstOrNull() ?: safeSubjects.first()

        val sessions = mutableListOf<PlanSessionItem>()
        val startHours = listOf("08:30", "10:00", "14:00", "15:45", "18:00", "19:30")

        for (day in 0..6) {
            val sessionsForDay = (dailyHours * 60 / (blockMinutes + breakMinutes)).toInt().coerceIn(2, 5)
            for (sIndex in 0 until sessionsForDay) {
                val subject = if (sIndex == 0 && difficultSubjects.isNotEmpty()) {
                    primaryDifficulty
                } else {
                    safeSubjects[(day + sIndex) % safeSubjects.size]
                }

                val type = when (sIndex) {
                    0 -> "DEEP_FOCUS"
                    sessionsForDay - 1 -> "REVISION"
                    else -> "PRACTICE"
                }

                val topic = when (type) {
                    "DEEP_FOCUS" -> "Core Theoretical Principles & Advanced Problems"
                    "PRACTICE" -> "Applied Problem Sets, Case Studies & Exercises"
                    else -> "Active Recall, Flashcards & Summary Synthesis"
                }

                val time = startHours.getOrElse(sIndex) { "${9 + sIndex * 2}:00" }
                val startParts = time.split(":")
                val sH = startParts[0].toIntOrNull() ?: 9
                val sM = startParts.getOrNull(1)?.toIntOrNull() ?: 0
                val totalM = sH * 60 + sM + blockMinutes
                val endH = (totalM / 60) % 24
                val endM = totalM % 60
                val endTime = String.format(Locale.US, "%02d:%02d", endH, endM)

                sessions.add(
                    PlanSessionItem(
                        dayOffset = day,
                        subject = subject,
                        topic = topic,
                        goals = listOf(
                            "Complete chapter reading and key definitions",
                            "Solve 4-6 targeted exam-style practice questions",
                            "Summarize key formulas and flashcards"
                        ),
                        durationMinutes = blockMinutes,
                        suggestedStartTime = time,
                        suggestedEndTime = endTime,
                        sessionType = type
                    )
                )
            }
        }

        return GeneratedStudyPlan(
            title = "Personalized $educationLevel Study Blueprint",
            overview = "Strategic daily distribution engineered for exam target ($examDate). Prioritizes difficult subjects ($primaryDifficulty) during peak morning cognitive hours.",
            dailyHours = dailyHours,
            sessions = sessions,
            revisionStrategy = listOf(
                "Apply Spaced Repetition: review morning concepts after 24h and at the end of the week.",
                "Active Recall: explain every difficult formula out loud without looking at textbook notes.",
                "Interleaving: alternate between quantitative problem sets and analytical reading to avoid cognitive fatigue."
            ),
            breakRecommendation = "$blockMinutes minutes of deep focus followed strictly by $breakMinutes minutes of phone-free mental rest."
        )
    }
}
