package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.GeneratedStudyPlan
import com.example.data.ai.PlanSessionItem
import com.example.ui.components.SectionHeader
import com.example.ui.components.SubjectChip
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRed
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.PurpleDark
import com.example.ui.viewmodels.AiPlannerUiState
import com.example.ui.viewmodels.StudyViewModel
import java.util.Locale

@Composable
fun AiPlannerScreen(
    viewModel: StudyViewModel,
    onNavigateToTasks: () -> Unit,
    onNavigateToSchedule: () -> Unit,
    modifier: Modifier = Modifier
) {
    val aiState by viewModel.aiPlannerState.collectAsState()
    val savedPlans by viewModel.savedStudyPlans.collectAsState()
    val userEduLevel by viewModel.educationLevel.collectAsState()
    val defaultDailyGoal by viewModel.dailyGoalHours.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: Generator / Active Plan, 1: Saved Plans

    // Form inputs state
    var educationLevel by remember(userEduLevel) { mutableStateOf(userEduLevel) }
    val subjectsList = remember { mutableStateListOf("Mathematics", "Physics", "Computer Science") }
    var newSubjectInput by remember { mutableStateOf("") }
    var examDateInput by remember { mutableStateOf("December 15, 2026") }
    var dailyStudyHours by remember(defaultDailyGoal) { mutableFloatStateOf(defaultDailyGoal) }
    val difficultSubjectsList = remember { mutableStateListOf("Physics") }
    var learningGoalsAndNotes by remember { mutableStateOf("Master problem solving and clear upcoming midterm with high distinction.") }
    var studyBlockMinutes by remember { mutableIntStateOf(45) }
    var breakMinutes by remember { mutableIntStateOf(10) }

    var conversionNotification by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Tabs: Plan Generator vs Saved Plans
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = ElectricBlue
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = { Text("Generate Plan", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("tab_generate_plan")
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = { Text("Saved Plans (${savedPlans.size})", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("tab_saved_plans")
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedTabIndex == 1) {
            // Saved Plans List
            if (savedPlans.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No saved study plans yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Generate a plan and tap 'Save Plan' to keep it here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(savedPlans) { saved ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth().testTag("saved_plan_${saved.id}")
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = saved.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Level: ${saved.educationLevel} • Target: ${saved.examDate}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteSavedPlan(saved) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Delete",
                                            tint = AccentRed.copy(alpha = 0.8f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Subjects: ${saved.subjects}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ElectricBlue
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Daily: ${saved.dailyHours} hours allocated per day",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Plan Generator or Plan Results
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Conversion Feedback Banner
                if (conversionNotification != null) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = AccentGreen.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AccentGreen.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = conversionNotification!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AccentGreen,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(
                                    onClick = { conversionNotification = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss",
                                        tint = AccentGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // AI Hero Card
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .border(
                                1.dp,
                                Brush.horizontalGradient(listOf(NeonPurple, ElectricBlue)),
                                RoundedCornerShape(20.dp)
                            ),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(NeonPurple.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = NeonPurple,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "AI Study Flow Engine",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Powered by Google Gemini 3.8 Flash to engineer your personalized revision syllabus.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                when (val state = aiState) {
                    is AiPlannerUiState.Loading -> {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(40.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(
                                        color = ElectricBlue,
                                        strokeWidth = 4.dp,
                                        modifier = Modifier.size(54.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Synthesizing AI Study Plan...",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Structuring spaced repetition intervals, priority topics, and cognitive breaks...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    is AiPlannerUiState.Success -> {
                        item {
                            PlanResultView(
                                plan = state.plan,
                                onSave = {
                                    viewModel.saveCurrentPlan(state.plan)
                                    conversionNotification = "Plan saved to 'Saved Plans' tab!"
                                },
                                onConvertToTasks = {
                                    viewModel.convertPlanSessionsToTasks(state.plan.sessions)
                                    conversionNotification = "${state.plan.sessions.size} sessions converted into tasks!"
                                },
                                onConvertToSchedule = {
                                    viewModel.convertPlanSessionsToSchedule(state.plan.sessions)
                                    conversionNotification = "${state.plan.sessions.size} study blocks added to your calendar!"
                                },
                                onRegenerate = {
                                    viewModel.generateAiPlan(
                                        subjects = subjectsList,
                                        examDate = examDateInput,
                                        dailyHours = dailyStudyHours,
                                        difficultSubjects = difficultSubjectsList,
                                        goalsAndNotes = learningGoalsAndNotes,
                                        studyBlockMinutes = studyBlockMinutes,
                                        breakMinutes = breakMinutes
                                    )
                                }
                            )
                        }
                    }

                    is AiPlannerUiState.Error -> {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = AccentRed.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AccentRed.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "AI Plan Generation Notice",
                                        fontWeight = FontWeight.Bold,
                                        color = AccentRed
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = state.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = {
                                            viewModel.generateAiPlan(
                                                subjects = subjectsList,
                                                examDate = examDateInput,
                                                dailyHours = dailyStudyHours,
                                                difficultSubjects = difficultSubjectsList,
                                                goalsAndNotes = learningGoalsAndNotes,
                                                studyBlockMinutes = studyBlockMinutes,
                                                breakMinutes = breakMinutes
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                                    ) {
                                        Text("Retry Generation")
                                    }
                                }
                            }
                        }
                    }

                    AiPlannerUiState.Idle -> {
                        // Form to configure AI parameters
                    }
                }

                // Configuration Form (always available so users can tweak and regenerate)
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Customize Your Study Flow",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // 1. Education Level
                            Column {
                                Text(
                                    text = "Class / Education Level",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                val levels = listOf("High School", "Undergraduate", "Graduate", "Competitive Exam")
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(levels) { lvl ->
                                        SubjectChip(
                                            subject = lvl,
                                            isSelected = educationLevel == lvl,
                                            onClick = { educationLevel = lvl }
                                        )
                                    }
                                }
                            }

                            // 2. Subjects List
                            Column {
                                Text(
                                    text = "Subjects You Study",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(subjectsList) { subj ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = ElectricBlue.copy(alpha = 0.2f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.5f))
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = subj,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = ElectricBlue,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remove",
                                                    tint = ElectricBlue,
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .clickable {
                                                            if (subjectsList.size > 1) {
                                                                subjectsList.remove(subj)
                                                                difficultSubjectsList.remove(subj)
                                                            }
                                                        }
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = newSubjectInput,
                                        onValueChange = { newSubjectInput = it },
                                        placeholder = { Text("Add subject (e.g. Biology)") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f).testTag("new_subject_input")
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            if (newSubjectInput.isNotBlank() && !subjectsList.contains(newSubjectInput.trim())) {
                                                subjectsList.add(newSubjectInput.trim())
                                                newSubjectInput = ""
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue, contentColor = Color(0xFF070B19)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.testTag("add_subject_button")
                                    ) {
                                        Text("+ Add")
                                    }
                                }
                            }

                            // 3. Difficult Subjects
                            Column {
                                Text(
                                    text = "High Priority / Difficult Subjects",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "AI will allocate peak focus hours to these subjects.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(subjectsList) { subj ->
                                        val isDiff = difficultSubjectsList.contains(subj)
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isDiff) AccentRed.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (isDiff) AccentRed else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                            ),
                                            modifier = Modifier.clickable {
                                                if (isDiff) difficultSubjectsList.remove(subj)
                                                else difficultSubjectsList.add(subj)
                                            }
                                        ) {
                                            Text(
                                                text = if (isDiff) "🔥 $subj" else subj,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isDiff) AccentRed else MaterialTheme.colorScheme.onSurface,
                                                fontWeight = if (isDiff) FontWeight.Bold else FontWeight.Normal,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // 4. Upcoming Exam Date
                            OutlinedTextField(
                                value = examDateInput,
                                onValueChange = { examDateInput = it },
                                label = { Text("Upcoming Exam / Milestone Date") },
                                singleLine = true,
                                leadingIcon = {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = ElectricBlue)
                                },
                                modifier = Modifier.fillMaxWidth().testTag("exam_date_input")
                            )

                            // 5. Daily Available Study Hours
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Available Study Hours per Day",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = String.format(Locale.US, "%.1f hrs/day", dailyStudyHours),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = ElectricBlue
                                    )
                                }
                                Slider(
                                    value = dailyStudyHours,
                                    onValueChange = { dailyStudyHours = it },
                                    valueRange = 1f..10f,
                                    steps = 17,
                                    colors = SliderDefaults.colors(
                                        thumbColor = ElectricBlue,
                                        activeTrackColor = ElectricBlue
                                    ),
                                    modifier = Modifier.testTag("daily_hours_slider")
                                )
                            }

                            // 6. Focus Block & Break Durations
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Focus Block",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf(25, 45, 60).forEach { mins ->
                                            Surface(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable { studyBlockMinutes = mins },
                                                color = if (studyBlockMinutes == mins) NeonPurple else MaterialTheme.colorScheme.surfaceVariant
                                            ) {
                                                Text(
                                                    text = "${mins}m",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (studyBlockMinutes == mins) Color.White else MaterialTheme.colorScheme.onSurface,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Break Duration",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf(5, 10, 15).forEach { mins ->
                                            Surface(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable { breakMinutes = mins },
                                                color = if (breakMinutes == mins) ElectricBlue else MaterialTheme.colorScheme.surfaceVariant
                                            ) {
                                                Text(
                                                    text = "${mins}m",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (breakMinutes == mins) Color(0xFF070B19) else MaterialTheme.colorScheme.onSurface,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 7. Goals and Weaknesses Notes
                            OutlinedTextField(
                                value = learningGoalsAndNotes,
                                onValueChange = { learningGoalsAndNotes = it },
                                label = { Text("Learning Goals & Focus Topics") },
                                maxLines = 3,
                                modifier = Modifier.fillMaxWidth().testTag("goals_and_notes_input")
                            )

                            // Generate Button
                            Button(
                                onClick = {
                                    viewModel.generateAiPlan(
                                        subjects = subjectsList,
                                        examDate = examDateInput,
                                        dailyHours = dailyStudyHours,
                                        difficultSubjects = difficultSubjectsList,
                                        goalsAndNotes = learningGoalsAndNotes,
                                        studyBlockMinutes = studyBlockMinutes,
                                        breakMinutes = breakMinutes
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("generate_ai_plan_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonPurple
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Generate AI Study Plan",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlanResultView(
    plan: GeneratedStudyPlan,
    onSave: () -> Unit,
    onConvertToTasks: () -> Unit,
    onConvertToSchedule: () -> Unit,
    onRegenerate: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().testTag("ai_plan_result_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = plan.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${plan.dailyHours} hrs/day • ${plan.sessions.size} structured study sessions",
                        style = MaterialTheme.typography.bodySmall,
                        color = ElectricBlue,
                        fontWeight = FontWeight.Medium
                    )
                }

                IconButton(onClick = onRegenerate, modifier = Modifier.testTag("regenerate_plan_button")) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Regenerate",
                        tint = ElectricBlue
                    )
                }
            }

            Text(
                text = plan.overview,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Break & Strategy Callouts
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = PurpleDark.copy(alpha = 0.25f),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonPurple.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = NeonPurple,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = plan.breakRecommendation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Conversion Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onConvertToTasks,
                    modifier = Modifier.weight(1f).testTag("convert_to_tasks_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue, contentColor = Color(0xFF070B19)),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.FormatListBulleted, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add to Tasks", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onConvertToSchedule,
                    modifier = Modifier.weight(1f).testTag("convert_to_schedule_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add to Schedule", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onSave,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("save_plan_button"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Bookmark, contentDescription = "Save", tint = ElectricBlue, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Session Syllabus Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Display Sessions
            plan.sessions.forEachIndexed { idx, session ->
                SessionItemCard(session = session, index = idx + 1)
            }

            if (plan.revisionStrategy.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Key Revision Strategies",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                plan.revisionStrategy.forEach { tip ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(text = "• ", color = ElectricBlue, fontWeight = FontWeight.Bold)
                        Text(
                            text = tip,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SessionItemCard(session: PlanSessionItem, index: Int) {
    val dayLabel = when (session.dayOffset) {
        0 -> "Today"
        1 -> "Tomorrow"
        else -> "Day ${session.dayOffset + 1}"
    }

    val typeColor = when (session.sessionType) {
        "DEEP_FOCUS" -> AccentRed
        "REVISION" -> AccentAmber
        else -> ElectricBlue
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = typeColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = session.sessionType.replace("_", " "),
                            style = MaterialTheme.typography.labelSmall,
                            color = typeColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$dayLabel • ${session.suggestedStartTime} - ${session.suggestedEndTime}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "${session.durationMinutes}m",
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricBlue,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${session.subject}: ${session.topic}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (session.goals.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                session.goals.forEach { goal ->
                    Row(
                        modifier = Modifier.padding(vertical = 1.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "✓ ",
                            color = AccentGreen,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = goal,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
