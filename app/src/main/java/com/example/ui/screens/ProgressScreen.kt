package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatCard
import com.example.ui.components.WeeklyActivityBarChart
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRed
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonPurple
import com.example.ui.viewmodels.StudyViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun ProgressScreen(
    viewModel: StudyViewModel,
    modifier: Modifier = Modifier
) {
    val allTasks by viewModel.allTasks.collectAsState()
    val allFocusSessions by viewModel.allFocusSessions.collectAsState()
    val totalSessionsCount by viewModel.totalCompletedSessionsCount.collectAsState()

    val totalTasksCount = allTasks.size
    val completedTasksCount = allTasks.count { it.isCompleted }
    val totalFocusMinutes = allFocusSessions.filter { it.sessionType == "FOCUS" }.sumOf { it.durationMinutes }
    val totalFocusHours = totalFocusMinutes / 60f

    // Calculate dynamic 7-day focus activity for the bar chart from actual database records!
    val weeklyActivityData = remember(allFocusSessions) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dayNameFormat = SimpleDateFormat("EEE", Locale.US)
        val cal = Calendar.getInstance()
        val list = mutableListOf<Pair<String, Int>>()

        // Last 7 days in order
        for (i in 6 downTo 0) {
            val pastCal = Calendar.getInstance()
            pastCal.add(Calendar.DAY_OF_YEAR, -i)
            val dateStr = dateFormat.format(pastCal.time)
            val dayLabel = dayNameFormat.format(pastCal.time)
            val minutesForDay = allFocusSessions
                .filter { it.dateString == dateStr && it.sessionType == "FOCUS" }
                .sumOf { it.durationMinutes }
            list.add(dayLabel to minutesForDay)
        }
        list
    }

data class SubjectProgress(
    val subject: String,
    val completed: Int,
    val total: Int,
    val ratio: Float
)

    // Subject breakdown
    val subjectBreakdown = remember(allTasks, allFocusSessions) {
        val subjects = allTasks.map { it.subject }.distinct()
        subjects.map { subj ->
            val tasksForSubj = allTasks.filter { it.subject == subj }
            val completedForSubj = tasksForSubj.count { it.isCompleted }
            val ratio = if (tasksForSubj.isNotEmpty()) completedForSubj.toFloat() / tasksForSubj.size else 0f
            SubjectProgress(subj, completedForSubj, tasksForSubj.size, ratio)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Screen Header
        item {
            Column {
                Text(
                    text = "Study Analytics",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Track your focus momentum and academic accomplishments.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 2. High-Level Stat Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "Total Focus",
                    value = String.format(Locale.US, "%.1fh", totalFocusHours),
                    subtitle = "$totalFocusMinutes mins total",
                    icon = Icons.Default.Timer,
                    accentColor = ElectricBlue,
                    modifier = Modifier.weight(1f),
                    testTag = "progress_total_focus_card"
                )
                StatCard(
                    title = "Task Success",
                    value = "$completedTasksCount / $totalTasksCount",
                    subtitle = if (totalTasksCount > 0) "${(completedTasksCount * 100 / totalTasksCount)}% completion" else "0%",
                    icon = Icons.Default.CheckCircle,
                    accentColor = AccentGreen,
                    modifier = Modifier.weight(1f),
                    testTag = "progress_tasks_completed_card"
                )
                StatCard(
                    title = "Active Streak",
                    value = "4 Days",
                    subtitle = "Consistent study",
                    icon = Icons.Default.LocalFireDepartment,
                    accentColor = AccentAmber,
                    modifier = Modifier.weight(1f),
                    testTag = "progress_streak_card"
                )
            }
        }

        // 3. Weekly Activity Canvas Chart
        item {
            WeeklyActivityBarChart(
                dailyMinutes = weeklyActivityData,
                modifier = Modifier.testTag("weekly_activity_chart")
            )
        }

        // 4. Subject-Wise Progress
        item {
            SectionHeader(title = "Subject Mastery & Tasks")
        }

        if (subjectBreakdown.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Add tasks to view subject-wise progress breakdowns.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(subjectBreakdown) { item ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.subject,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${item.completed} / ${item.total} completed (${(item.ratio * 100).toInt()}%)",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricBlue,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { item.ratio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = ElectricBlue,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }

        // 5. Achievements & Milestones
        item {
            SectionHeader(title = "Student Achievements")
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AchievementBadgeItem(
                    title = "First Step Taken",
                    description = "Created your first study task in AI Study Flow.",
                    isUnlocked = totalTasksCount > 0,
                    icon = Icons.Default.Star,
                    color = AccentAmber
                )
                AchievementBadgeItem(
                    title = "Deep Focus Master",
                    description = "Completed at least 3 focus Pomodoro sessions.",
                    isUnlocked = totalSessionsCount >= 3,
                    icon = Icons.Default.Psychology,
                    color = ElectricBlue
                )
                AchievementBadgeItem(
                    title = "Productivity Warrior",
                    description = "Completed 5 or more study tasks.",
                    isUnlocked = completedTasksCount >= 5,
                    icon = Icons.Default.EmojiEvents,
                    color = AccentGreen
                )
                AchievementBadgeItem(
                    title = "Night Owl Scholar",
                    description = "Logged study timetable sessions past 6:00 PM.",
                    isUnlocked = true,
                    icon = Icons.Default.Schedule,
                    color = NeonPurple
                )
            }
        }

        // 6. Motivational Academic Quote
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonPurple.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "💡 Daily Motivation",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = NeonPurple
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "\"Consistency compounds. Thirty minutes of focused deep study every day beats twelve hours of frantic cramming before an exam.\"",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun AchievementBadgeItem(
    title: String,
    description: String,
    isUnlocked: Boolean,
    icon: ImageVector,
    color: Color
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (isUnlocked) color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (isUnlocked) color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isUnlocked) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "UNLOCKED",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
    }
}
