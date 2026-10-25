package com.example

import com.example.data.ai.AiPlannerService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StudyFlowUnitTest {

    @Test
    fun testAiPlannerServiceGeneratesStructuredPlan() = runBlocking {
        val service = AiPlannerService()
        val plan = service.generatePlan(
            educationLevel = "Undergraduate",
            subjects = listOf("Mathematics", "Physics", "Computer Science"),
            examDate = "December 15, 2026",
            dailyHours = 4.0f,
            difficultSubjects = listOf("Physics"),
            goalsAndWeaknesses = "Master core concepts and solve problems",
            studyBlockMinutes = 45,
            breakMinutes = 10
        )

        assertNotNull(plan)
        assertTrue(plan.title.isNotBlank())
        assertTrue(plan.sessions.isNotEmpty())
        assertTrue(plan.revisionStrategy.isNotEmpty())
        assertTrue(plan.breakRecommendation.isNotBlank())

        val physicsSession = plan.sessions.firstOrNull { it.subject == "Physics" }
        assertNotNull("Physics session should be generated", physicsSession)
    }

    @Test
    fun testOverlapCalculationLogic() {
        // Session 1: 09:00 - 10:30 (540 to 630 mins)
        // Session 2: 10:00 - 11:00 (600 to 660 mins) -> Should overlap!
        val s1Start = 9 * 60
        val s1End = 10 * 60 + 30
        val s2Start = 10 * 60
        val s2End = 11 * 60

        val overlaps = maxOf(s1Start, s2Start) < minOf(s1End, s2End)
        assertTrue("Sessions overlapping between 10:00 and 10:30 must be detected", overlaps)

        // Session 3: 11:00 - 12:00 -> Does NOT overlap with Session 1
        val s3Start = 11 * 60
        val s3End = 12 * 60
        val overlaps3 = maxOf(s1Start, s3Start) < minOf(s1End, s3End)
        assertFalse("Sessions with distinct times must not overlap", overlaps3)
    }
}
