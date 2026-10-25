package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatCard
import com.example.ui.components.SubjectChip
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonPurple
import com.example.ui.viewmodels.PomodoroMode
import com.example.ui.viewmodels.StudyViewModel
import com.example.ui.viewmodels.TimerStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TimerScreen(
    viewModel: StudyViewModel,
    modifier: Modifier = Modifier
) {
    val pomodoroMode by viewModel.pomodoroMode.collectAsState()
    val remainingSeconds by viewModel.timerRemainingSeconds.collectAsState()
    val totalDurationMinutes by viewModel.timerDurationMinutes.collectAsState()
    val timerStatus by viewModel.timerStatus.collectAsState()
    val selectedSubject by viewModel.timerSubject.collectAsState()

    val todayFocusMinutes by viewModel.todayFocusMinutes.collectAsState()
    val totalCompletedSessions by viewModel.totalCompletedSessionsCount.collectAsState()
    val allFocusSessions by viewModel.allFocusSessions.collectAsState()

    val totalSeconds = totalDurationMinutes * 60
    val progress = if (totalSeconds > 0) {
        remainingSeconds.toFloat() / totalSeconds.toFloat()
    } else 0f

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timeFormatted = String.format(Locale.US, "%02d:%02d", minutes, seconds)

    val subjects = listOf("Mathematics", "Physics", "Computer Science", "Chemistry", "General Study")
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Pomodoro Mode Toggle Chips (Focus, Short Break, Long Break)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PomodoroMode.values().forEach { mode ->
                    val isSelected = pomodoroMode == mode
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.setPomodoroMode(mode) }
                            .testTag("timer_mode_${mode.name}"),
                        color = if (isSelected) {
                            if (mode == PomodoroMode.FOCUS) ElectricBlue else NeonPurple
                        } else Color.Transparent
                    ) {
                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) {
                                if (mode == PomodoroMode.FOCUS) Color(0xFF070B19) else Color.White
                            } else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 10.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }

        // 2. Subject Selection Chips
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Studying Subject",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(subjects) { subj ->
                        SubjectChip(
                            subject = subj,
                            isSelected = selectedSubject == subj,
                            onClick = { viewModel.setTimerSubject(subj) }
                        )
                    }
                }
            }
        }

        // 3. Circular Countdown Timer
        item {
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .testTag("circular_timer_container"),
                contentAlignment = Alignment.Center
            ) {
                val primaryColor = if (pomodoroMode == PomodoroMode.FOCUS) ElectricBlue else NeonPurple
                val trackColor = MaterialTheme.colorScheme.surfaceVariant

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 14.dp.toPx()
                    // Background track
                    drawCircle(
                        color = trackColor,
                        style = Stroke(width = strokeWidth)
                    )
                    // Animated Arc
                    val sweepAngle = 360f * progress
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(primaryColor, NeonPurple, primaryColor)
                        ),
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.headlineLarge,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (pomodoroMode == PomodoroMode.FOCUS) selectedSubject else pomodoroMode.label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (pomodoroMode == PomodoroMode.FOCUS) ElectricBlue else NeonPurple
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = when (timerStatus) {
                            TimerStatus.RUNNING -> "Focus Active"
                            TimerStatus.PAUSED -> "Paused"
                            TimerStatus.STOPPED -> "Ready"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 4. Timer Controls (Start, Pause, Resume, Reset)
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reset Button
                OutlinedButton(
                    onClick = { viewModel.resetTimer() },
                    shape = CircleShape,
                    modifier = Modifier.size(54.dp).testTag("timer_reset_button"),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Timer",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Play / Pause Button
                Button(
                    onClick = {
                        when (timerStatus) {
                            TimerStatus.STOPPED -> viewModel.startTimer()
                            TimerStatus.RUNNING -> viewModel.pauseTimer()
                            TimerStatus.PAUSED -> viewModel.resumeTimer()
                        }
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (pomodoroMode == PomodoroMode.FOCUS) ElectricBlue else NeonPurple,
                        contentColor = if (pomodoroMode == PomodoroMode.FOCUS) Color(0xFF070B19) else Color.White
                    ),
                    modifier = Modifier.size(72.dp).testTag("timer_toggle_button"),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = if (timerStatus == TimerStatus.RUNNING) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (timerStatus == TimerStatus.RUNNING) "Pause" else "Start",
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        // 5. Custom Duration Selector
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Quick Adjust Duration",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(15, 25, 30, 45, 60).forEach { mins ->
                            val isSel = totalDurationMinutes == mins
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.setCustomTimerDuration(mins) }
                                    .testTag("custom_timer_${mins}m"),
                                color = if (isSel) ElectricBlue.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (isSel) androidx.compose.foundation.BorderStroke(1.dp, ElectricBlue) else null
                            ) {
                                Text(
                                    text = "${mins}m",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) ElectricBlue else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. Today's Focus Stats
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "Today's Focus",
                    value = "${todayFocusMinutes ?: 0}m",
                    subtitle = "Time in the zone",
                    icon = Icons.Default.Timer,
                    accentColor = ElectricBlue,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Sessions",
                    value = "$totalCompletedSessions",
                    subtitle = "Completed Pomodoros",
                    icon = Icons.Default.CheckCircle,
                    accentColor = AccentGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 7. Recent Focus Sessions Log
        item {
            SectionHeader(title = "Recent Focus History")
        }

        val recentSessions = allFocusSessions.take(4)
        if (recentSessions.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Complete your first Pomodoro session to log focus hours!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(recentSessions) { session ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(AccentGreen.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = AccentGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = session.subject,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${session.dateString} at ${timeFormat.format(Date(session.timestampMillis))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = "+${session.durationMinutes} min",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = ElectricBlue
                        )
                    }
                }
            }
        }
    }
}
