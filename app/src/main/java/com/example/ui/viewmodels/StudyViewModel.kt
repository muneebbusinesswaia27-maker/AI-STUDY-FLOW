package com.example.ui.viewmodels

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiPlannerService
import com.example.data.ai.GeneratedStudyPlan
import com.example.data.ai.PlanSessionItem
import com.example.data.local.FocusEntity
import com.example.data.local.ScheduleEntity
import com.example.data.local.StudyFlowDatabase
import com.example.data.local.StudyPlanEntity
import com.example.data.local.TaskEntity
import com.example.data.preferences.UserPreferences
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class TaskFilter {
    ALL, PENDING, COMPLETED, HIGH_PRIORITY
}

enum class TimerStatus {
    STOPPED, RUNNING, PAUSED
}

enum class PomodoroMode(val defaultMinutes: Int, val label: String) {
    FOCUS(25, "Focus Session"),
    SHORT_BREAK(5, "Short Break"),
    LONG_BREAK(15, "Long Break")
}

sealed interface AiPlannerUiState {
    data object Idle : AiPlannerUiState
    data object Loading : AiPlannerUiState
    data class Success(val plan: GeneratedStudyPlan) : AiPlannerUiState
    data class Error(val message: String) : AiPlannerUiState
}

class StudyViewModel(application: Application) : AndroidViewModel(application) {

    private val db = StudyFlowDatabase.getDatabase(application)
    private val taskDao = db.taskDao()
    private val scheduleDao = db.scheduleDao()
    private val focusDao = db.focusDao()
    private val studyPlanDao = db.studyPlanDao()
    val userPrefs = UserPreferences(application)
    private val aiService = AiPlannerService()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val todayDateString: String = dateFormat.format(Date())

    // --- User Profile State ---
    val userName = MutableStateFlow(userPrefs.userName)
    val educationLevel = MutableStateFlow(userPrefs.educationLevel)
    val dailyGoalHours = MutableStateFlow(userPrefs.dailyGoalHours)
    val isDarkTheme = MutableStateFlow(userPrefs.isDarkTheme)
    val isOnboardingCompleted = MutableStateFlow(userPrefs.isOnboardingCompleted)

    // --- Tasks State & Filtering ---
    val allTasks = taskDao.getAllTasks().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private val _taskFilter = MutableStateFlow(TaskFilter.ALL)
    val taskFilter: StateFlow<TaskFilter> = _taskFilter.asStateFlow()

    private val _subjectFilter = MutableStateFlow<String?>(null)
    val subjectFilter: StateFlow<String?> = _subjectFilter.asStateFlow()

    private val _taskSearchQuery = MutableStateFlow("")
    val taskSearchQuery: StateFlow<String> = _taskSearchQuery.asStateFlow()

    val filteredTasks = combine(allTasks, taskFilter, subjectFilter, taskSearchQuery) { tasks, filter, subject, query ->
        val now = System.currentTimeMillis()
        tasks.filter { task ->
            val matchesFilter = when (filter) {
                TaskFilter.ALL -> true
                TaskFilter.PENDING -> !task.isCompleted
                TaskFilter.COMPLETED -> task.isCompleted
                TaskFilter.HIGH_PRIORITY -> task.priority.equals("HIGH", ignoreCase = true)
            }
            val matchesSubject = subject == null || task.subject.equals(subject, ignoreCase = true)
            val matchesQuery = query.isBlank() || task.title.contains(query, ignoreCase = true) ||
                    task.subject.contains(query, ignoreCase = true)
            matchesFilter && matchesSubject && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Schedule State ---
    val allScheduleSessions = scheduleDao.getAllSessions().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private val _selectedScheduleDate = MutableStateFlow(todayDateString)
    val selectedScheduleDate: StateFlow<String> = _selectedScheduleDate.asStateFlow()

    val sessionsForSelectedDate = combine(allScheduleSessions, selectedScheduleDate) { sessions, date ->
        sessions.filter { it.dateString == date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Focus History & Stats ---
    val allFocusSessions = focusDao.getAllFocusSessions().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val todayFocusMinutes = focusDao.getFocusMinutesForDate(todayDateString).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        0
    )

    val totalCompletedSessionsCount = focusDao.getTotalCompletedSessionsCount().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        0
    )

    val savedStudyPlans = studyPlanDao.getAllStudyPlans().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // --- Focus Timer State ---
    private val _pomodoroMode = MutableStateFlow(PomodoroMode.FOCUS)
    val pomodoroMode: StateFlow<PomodoroMode> = _pomodoroMode.asStateFlow()

    private val _timerDurationMinutes = MutableStateFlow(25)
    val timerDurationMinutes: StateFlow<Int> = _timerDurationMinutes.asStateFlow()

    private val _timerRemainingSeconds = MutableStateFlow(25 * 60)
    val timerRemainingSeconds: StateFlow<Int> = _timerRemainingSeconds.asStateFlow()

    private val _timerStatus = MutableStateFlow(TimerStatus.STOPPED)
    val timerStatus: StateFlow<TimerStatus> = _timerStatus.asStateFlow()

    private val _timerSubject = MutableStateFlow("Mathematics")
    val timerSubject: StateFlow<String> = _timerSubject.asStateFlow()

    private var timerJob: Job? = null

    // --- AI Planner State ---
    private val _aiPlannerState = MutableStateFlow<AiPlannerUiState>(AiPlannerUiState.Idle)
    val aiPlannerState: StateFlow<AiPlannerUiState> = _aiPlannerState.asStateFlow()

    // --- Pre-populate Sample Data on First Run for Demo ---
    init {
        viewModelScope.launch {
            if (!userPrefs.isOnboardingCompleted) {
                // Populate realistic initial starter tasks and sessions
                prepopulateInitialData()
            }
        }
    }

    private suspend fun prepopulateInitialData() {
        val now = System.currentTimeMillis()
        val oneDayMillis = 86_400_000L
        val sampleTasks = listOf(
            TaskEntity(
                title = "Calculus II: Review Integration by Parts",
                description = "Complete exercises 14 to 28 on page 142.",
                subject = "Mathematics",
                priority = "HIGH",
                dueDateMillis = now + oneDayMillis,
                estimatedDurationMinutes = 45,
                isCompleted = false
            ),
            TaskEntity(
                title = "Physics: Quantum Mechanics Problem Set",
                description = "Wave functions and Schrödinger equations walkthrough.",
                subject = "Physics",
                priority = "HIGH",
                dueDateMillis = now + 2 * oneDayMillis,
                estimatedDurationMinutes = 60,
                isCompleted = false
            ),
            TaskEntity(
                title = "Data Structures: Binary Search Tree Implementation",
                description = "Implement insertion, deletion, and in-order traversal.",
                subject = "Computer Science",
                priority = "MEDIUM",
                dueDateMillis = now + 3 * oneDayMillis,
                estimatedDurationMinutes = 50,
                isCompleted = true,
                completedAtMillis = now - 3600_000L
            ),
            TaskEntity(
                title = "Organic Chemistry: Synthesis Mechanisms Summary",
                description = "Flashcard review for nucleophilic substitutions.",
                subject = "Chemistry",
                priority = "LOW",
                dueDateMillis = now + 4 * oneDayMillis,
                estimatedDurationMinutes = 30,
                isCompleted = false
            )
        )
        taskDao.insertTasks(sampleTasks)

        val sampleSchedule = listOf(
            ScheduleEntity(
                title = "Deep Focus: Calculus Problem Solving",
                subject = "Mathematics",
                dateString = todayDateString,
                startTime = "09:00",
                endTime = "10:30",
                notes = "Focus on step-by-step proofs",
                isCompleted = true
            ),
            ScheduleEntity(
                title = "Physics Lab Prep & Formula Sheet",
                subject = "Physics",
                dateString = todayDateString,
                startTime = "14:00",
                endTime = "15:30",
                notes = "Verify error calculations",
                isCompleted = false
            ),
            ScheduleEntity(
                title = "Evening Revision & Recall",
                subject = "Computer Science",
                dateString = todayDateString,
                startTime = "18:00",
                endTime = "19:00",
                notes = "Test memory without checking code",
                isCompleted = false
            )
        )
        scheduleDao.insertSessions(sampleSchedule)

        // Seed 2 sample completed focus sessions
        focusDao.insertFocusSession(
            FocusEntity(
                subject = "Mathematics",
                durationMinutes = 25,
                sessionType = "FOCUS",
                timestampMillis = now - 7200_000L,
                dateString = todayDateString
            )
        )
        focusDao.insertFocusSession(
            FocusEntity(
                subject = "Physics",
                durationMinutes = 25,
                sessionType = "FOCUS",
                timestampMillis = now - 3600_000L,
                dateString = todayDateString
            )
        )
    }

    // --- Onboarding Completion ---
    fun completeOnboarding(name: String, level: String, dailyGoal: Float) {
        userPrefs.userName = name
        userPrefs.educationLevel = level
        userPrefs.dailyGoalHours = dailyGoal
        userPrefs.isOnboardingCompleted = true

        userName.value = name
        educationLevel.value = level
        dailyGoalHours.value = dailyGoal
        isOnboardingCompleted.value = true
    }

    fun updateProfile(name: String, level: String, dailyGoal: Float, darkTheme: Boolean) {
        userPrefs.userName = name
        userPrefs.educationLevel = level
        userPrefs.dailyGoalHours = dailyGoal
        userPrefs.isDarkTheme = darkTheme

        userName.value = name
        educationLevel.value = level
        dailyGoalHours.value = dailyGoal
        isDarkTheme.value = darkTheme
    }

    // --- Task Actions ---
    fun setTaskFilter(filter: TaskFilter) {
        _taskFilter.value = filter
    }

    fun setSubjectFilter(subject: String?) {
        _subjectFilter.value = subject
    }

    fun setTaskSearchQuery(query: String) {
        _taskSearchQuery.value = query
    }

    fun addTask(
        title: String,
        description: String,
        subject: String,
        priority: String,
        dueDateMillis: Long,
        estimatedDurationMinutes: Int
    ) {
        viewModelScope.launch {
            val task = TaskEntity(
                title = title,
                description = description,
                subject = subject,
                priority = priority,
                dueDateMillis = dueDateMillis,
                estimatedDurationMinutes = estimatedDurationMinutes,
                isCompleted = false
            )
            taskDao.insertTask(task)
        }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch {
            taskDao.updateTask(task)
        }
    }

    fun toggleTaskCompletion(task: TaskEntity) {
        viewModelScope.launch {
            val updated = task.copy(
                isCompleted = !task.isCompleted,
                completedAtMillis = if (!task.isCompleted) System.currentTimeMillis() else 0L
            )
            taskDao.updateTask(updated)
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            taskDao.deleteTask(task)
        }
    }

    // --- Schedule Actions ---
    fun selectScheduleDate(dateString: String) {
        _selectedScheduleDate.value = dateString
    }

    // Overlap validation helper
    fun checkSessionOverlap(dateString: String, startTime: String, endTime: String, excludeId: Long = 0): ScheduleEntity? {
        val existing = allScheduleSessions.value.filter { it.dateString == dateString && it.id != excludeId }
        val newStart = parseTimeToMinutes(startTime)
        val newEnd = parseTimeToMinutes(endTime)
        for (item in existing) {
            val itemStart = parseTimeToMinutes(item.startTime)
            val itemEnd = parseTimeToMinutes(item.endTime)
            // Overlap condition: max(newStart, itemStart) < min(newEnd, itemEnd)
            if (maxOf(newStart, itemStart) < minOf(newEnd, itemEnd)) {
                return item
            }
        }
        return null
    }

    private fun parseTimeToMinutes(timeStr: String): Int {
        val parts = timeStr.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return h * 60 + m
    }

    fun addScheduleSession(
        title: String,
        subject: String,
        dateString: String,
        startTime: String,
        endTime: String,
        notes: String = ""
    ) {
        viewModelScope.launch {
            val session = ScheduleEntity(
                title = title,
                subject = subject,
                dateString = dateString,
                startTime = startTime,
                endTime = endTime,
                notes = notes
            )
            scheduleDao.insertSession(session)
        }
    }

    fun deleteScheduleSession(session: ScheduleEntity) {
        viewModelScope.launch {
            scheduleDao.deleteSession(session)
        }
    }

    fun toggleScheduleCompleted(session: ScheduleEntity) {
        viewModelScope.launch {
            scheduleDao.updateSession(session.copy(isCompleted = !session.isCompleted))
        }
    }

    // --- Timer Actions ---
    fun setPomodoroMode(mode: PomodoroMode) {
        if (_timerStatus.value == TimerStatus.RUNNING) {
            pauseTimer()
        }
        _pomodoroMode.value = mode
        _timerDurationMinutes.value = mode.defaultMinutes
        _timerRemainingSeconds.value = mode.defaultMinutes * 60
        _timerStatus.value = TimerStatus.STOPPED
    }

    fun setCustomTimerDuration(minutes: Int) {
        if (_timerStatus.value == TimerStatus.RUNNING) {
            pauseTimer()
        }
        _timerDurationMinutes.value = minutes
        _timerRemainingSeconds.value = minutes * 60
        _timerStatus.value = TimerStatus.STOPPED
    }

    fun setTimerSubject(subject: String) {
        _timerSubject.value = subject
    }

    fun startTimer() {
        if (_timerStatus.value == TimerStatus.RUNNING) return
        _timerStatus.value = TimerStatus.RUNNING

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_timerRemainingSeconds.value > 0 && _timerStatus.value == TimerStatus.RUNNING) {
                delay(1000L)
                _timerRemainingSeconds.value -= 1
            }
            if (_timerRemainingSeconds.value <= 0) {
                onTimerCompleted()
            }
        }
    }

    fun pauseTimer() {
        _timerStatus.value = TimerStatus.PAUSED
        timerJob?.cancel()
    }

    fun resumeTimer() {
        startTimer()
    }

    fun resetTimer() {
        timerJob?.cancel()
        _timerStatus.value = TimerStatus.STOPPED
        _timerRemainingSeconds.value = _timerDurationMinutes.value * 60
    }

    private fun onTimerCompleted() {
        _timerStatus.value = TimerStatus.STOPPED
        val completedDuration = _timerDurationMinutes.value
        val mode = _pomodoroMode.value

        // Trigger haptic vibration notification
        triggerVibration()

        // Record focus session persistently if it was a FOCUS session
        if (mode == PomodoroMode.FOCUS) {
            viewModelScope.launch {
                focusDao.insertFocusSession(
                    FocusEntity(
                        subject = _timerSubject.value,
                        durationMinutes = completedDuration,
                        sessionType = "FOCUS",
                        dateString = todayDateString
                    )
                )
            }
        }

        // Switch to appropriate break mode
        if (mode == PomodoroMode.FOCUS) {
            setPomodoroMode(PomodoroMode.SHORT_BREAK)
        } else {
            setPomodoroMode(PomodoroMode.FOCUS)
        }
    }

    private fun triggerVibration() {
        try {
            val app = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 300, 200, 300), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 300, 200, 300), -1)
            }
        } catch (_: Exception) {}
    }

    // --- AI Planner Actions ---
    fun generateAiPlan(
        subjects: List<String>,
        examDate: String,
        dailyHours: Float,
        difficultSubjects: List<String>,
        goalsAndNotes: String,
        studyBlockMinutes: Int = 45,
        breakMinutes: Int = 10
    ) {
        viewModelScope.launch {
            _aiPlannerState.value = AiPlannerUiState.Loading
            try {
                val plan = aiService.generatePlan(
                    educationLevel = educationLevel.value,
                    subjects = subjects,
                    examDate = examDate,
                    dailyHours = dailyHours,
                    difficultSubjects = difficultSubjects,
                    goalsAndWeaknesses = goalsAndNotes,
                    studyBlockMinutes = studyBlockMinutes,
                    breakMinutes = breakMinutes
                )
                _aiPlannerState.value = AiPlannerUiState.Success(plan)
            } catch (e: Exception) {
                _aiPlannerState.value = AiPlannerUiState.Error(
                    e.localizedMessage ?: "Failed to generate AI study plan."
                )
            }
        }
    }

    fun saveCurrentPlan(plan: GeneratedStudyPlan) {
        viewModelScope.launch {
            val jsonRoot = JSONObject()
            jsonRoot.put("title", plan.title)
            jsonRoot.put("overview", plan.overview)
            jsonRoot.put("dailyHours", plan.dailyHours)
            jsonRoot.put("breakRecommendation", plan.breakRecommendation)

            val revArray = JSONArray()
            plan.revisionStrategy.forEach { revArray.put(it) }
            jsonRoot.put("revisionStrategy", revArray)

            val sessArray = JSONArray()
            plan.sessions.forEach { s ->
                val sObj = JSONObject()
                sObj.put("dayOffset", s.dayOffset)
                sObj.put("subject", s.subject)
                sObj.put("topic", s.topic)
                sObj.put("durationMinutes", s.durationMinutes)
                sObj.put("suggestedStartTime", s.suggestedStartTime)
                sObj.put("suggestedEndTime", s.suggestedEndTime)
                sObj.put("sessionType", s.sessionType)
                val gArr = JSONArray()
                s.goals.forEach { gArr.put(it) }
                sObj.put("goals", gArr)
                sessArray.put(sObj)
            }
            jsonRoot.put("sessions", sessArray)

            val entity = StudyPlanEntity(
                title = plan.title,
                educationLevel = educationLevel.value,
                subjects = plan.sessions.map { it.subject }.distinct().joinToString(", "),
                examDate = "Upcoming Milestone",
                dailyHours = plan.dailyHours,
                planJson = jsonRoot.toString()
            )
            studyPlanDao.insertPlan(entity)
        }
    }

    fun deleteSavedPlan(plan: StudyPlanEntity) {
        viewModelScope.launch {
            studyPlanDao.deletePlan(plan)
        }
    }

    fun convertPlanSessionsToTasks(sessions: List<PlanSessionItem>) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val cal = Calendar.getInstance()
            val tasks = sessions.map { session ->
                cal.timeInMillis = now
                cal.add(Calendar.DAY_OF_YEAR, session.dayOffset)
                cal.set(Calendar.HOUR_OF_DAY, 20)
                cal.set(Calendar.MINUTE, 0)
                TaskEntity(
                    title = "${session.subject}: ${session.topic}",
                    description = session.goals.joinToString(" • "),
                    subject = session.subject,
                    priority = if (session.sessionType == "DEEP_FOCUS") "HIGH" else "MEDIUM",
                    dueDateMillis = cal.timeInMillis,
                    estimatedDurationMinutes = session.durationMinutes,
                    isCompleted = false
                )
            }
            taskDao.insertTasks(tasks)
        }
    }

    fun convertPlanSessionsToSchedule(sessions: List<PlanSessionItem>) {
        viewModelScope.launch {
            val cal = Calendar.getInstance()
            val newSessions = sessions.map { session ->
                cal.time = Date()
                cal.add(Calendar.DAY_OF_YEAR, session.dayOffset)
                val dateStr = dateFormat.format(cal.time)

                ScheduleEntity(
                    title = session.topic,
                    subject = session.subject,
                    dateString = dateStr,
                    startTime = session.suggestedStartTime,
                    endTime = session.suggestedEndTime,
                    notes = session.goals.joinToString("; ")
                )
            }
            scheduleDao.insertSessions(newSessions)
        }
    }

    // --- Reset All Data ---
    fun resetAllUserData() {
        viewModelScope.launch {
            taskDao.clearAll()
            scheduleDao.clearAll()
            focusDao.clearAll()
            studyPlanDao.clearAll()
            userPrefs.clearAll()
            userName.value = "Student"
            educationLevel.value = "Undergraduate"
            dailyGoalHours.value = 4.0f
            isOnboardingCompleted.value = false
            _timerStatus.value = TimerStatus.STOPPED
            timerJob?.cancel()
        }
    }
}
