package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.configureAppCheck
import com.example.ui.screens.AiPlannerScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ProgressScreen
import com.example.ui.screens.ScheduleScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.screens.TimerScreen
import com.example.ui.theme.AIStudyFlowTheme
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.NeonPurple
import com.example.ui.viewmodels.StudyViewModel

enum class NavigationTab(val title: String, val icon: ImageVector, val tag: String) {
    HOME("Home", Icons.Default.Home, "nav_home"),
    TASKS("Tasks", Icons.Default.Checklist, "nav_tasks"),
    AI_PLANNER("AI Planner", Icons.Default.AutoAwesome, "nav_ai_planner"),
    TIMER("Timer", Icons.Default.Timer, "nav_timer"),
    SCHEDULE("Timetable", Icons.Default.CalendarMonth, "nav_schedule"),
    PROGRESS("Analytics", Icons.Default.BarChart, "nav_progress"),
    PROFILE("Profile", Icons.Default.Person, "nav_profile")
}

class MainActivity : ComponentActivity() {

    private val viewModel: StudyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mandatory App Check configuration with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsState()
            var showSplash by remember { mutableStateOf(true) }

            AIStudyFlowTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (showSplash) {
                        com.example.ui.screens.SplashScreen(
                            onSplashFinished = { showSplash = false }
                        )
                    } else if (!isOnboardingCompleted) {
                        OnboardingScreen(
                            onComplete = { name, level, dailyGoal ->
                                viewModel.completeOnboarding(name, level, dailyGoal)
                            }
                        )
                    } else {
                        MainAppContent(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(viewModel: StudyViewModel) {
    var currentTab by remember { mutableStateOf(NavigationTab.HOME) }

    // Wire BackHandler to return to HOME if currently on a sub-screen
    BackHandler(enabled = currentTab != NavigationTab.HOME) {
        currentTab = NavigationTab.HOME
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (currentTab) {
                            NavigationTab.HOME -> "AI Study Flow"
                            NavigationTab.TASKS -> "Task Management"
                            NavigationTab.AI_PLANNER -> "AI Study Planner"
                            NavigationTab.TIMER -> "Focus Timer"
                            NavigationTab.SCHEDULE -> "Study Timetable"
                            NavigationTab.PROGRESS -> "Study Analytics"
                            NavigationTab.PROFILE -> "User Profile"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                actions = {
                    IconButton(
                        onClick = { currentTab = NavigationTab.PROGRESS },
                        modifier = Modifier.testTag("topbar_analytics_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "Analytics",
                            tint = if (currentTab == NavigationTab.PROGRESS) ElectricBlue else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { currentTab = NavigationTab.PROFILE },
                        modifier = Modifier.testTag("topbar_profile_button")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(NeonPurple.copy(alpha = 0.2f))
                                .border(
                                    1.dp,
                                    if (currentTab == NavigationTab.PROFILE) ElectricBlue else Color.Transparent,
                                    CircleShape
                                ),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = if (currentTab == NavigationTab.PROFILE) ElectricBlue else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            val bottomNavItems = listOf(
                NavigationTab.HOME,
                NavigationTab.TASKS,
                NavigationTab.AI_PLANNER,
                NavigationTab.TIMER,
                NavigationTab.SCHEDULE,
                NavigationTab.PROFILE
            )

            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                bottomNavItems.forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF070B19),
                            selectedTextColor = ElectricBlue,
                            indicatorColor = ElectricBlue,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag(tab.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Crossfade(
            targetState = currentTab,
            label = "ScreenTransition",
            modifier = Modifier.padding(innerPadding)
        ) { tab ->
            when (tab) {
                NavigationTab.HOME -> HomeScreen(
                    viewModel = viewModel,
                    onNavigateToTasks = { currentTab = NavigationTab.TASKS },
                    onNavigateToAiPlanner = { currentTab = NavigationTab.AI_PLANNER },
                    onNavigateToTimer = { currentTab = NavigationTab.TIMER },
                    onNavigateToSchedule = { currentTab = NavigationTab.SCHEDULE }
                )
                NavigationTab.TASKS -> TasksScreen(viewModel = viewModel)
                NavigationTab.AI_PLANNER -> AiPlannerScreen(
                    viewModel = viewModel,
                    onNavigateToTasks = { currentTab = NavigationTab.TASKS },
                    onNavigateToSchedule = { currentTab = NavigationTab.SCHEDULE }
                )
                NavigationTab.TIMER -> TimerScreen(viewModel = viewModel)
                NavigationTab.SCHEDULE -> ScheduleScreen(viewModel = viewModel)
                NavigationTab.PROGRESS -> ProgressScreen(viewModel = viewModel)
                NavigationTab.PROFILE -> ProfileScreen(viewModel = viewModel)
            }
        }
    }
}
