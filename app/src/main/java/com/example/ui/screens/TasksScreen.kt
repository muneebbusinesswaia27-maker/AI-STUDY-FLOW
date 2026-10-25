package com.example.ui.screens

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.TaskEntity
import com.example.ui.components.PriorityBadge
import com.example.ui.components.SubjectChip
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRed
import com.example.ui.theme.ElectricBlue
import com.example.ui.viewmodels.StudyViewModel
import com.example.ui.viewmodels.TaskFilter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    viewModel: StudyViewModel,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.filteredTasks.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()
    val currentFilter by viewModel.taskFilter.collectAsState()
    val currentSubjectFilter by viewModel.subjectFilter.collectAsState()
    val searchQuery by viewModel.taskSearchQuery.collectAsState()

    var showAddEditDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<TaskEntity?>(null) }
    var taskToDelete by remember { mutableStateOf<TaskEntity?>(null) }

    val distinctSubjects = remember(allTasks) {
        allTasks.map { it.subject }.distinct()
    }

    val nowMillis = System.currentTimeMillis()
    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    taskToEdit = null
                    showAddEditDialog = true
                },
                containerColor = ElectricBlue,
                contentColor = Color(0xFF070B19),
                shape = CircleShape,
                modifier = Modifier.testTag("add_task_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setTaskSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_search_input"),
                placeholder = { Text("Search tasks or subjects...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setTaskSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = ElectricBlue,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Chips (All, Pending, Completed, High Priority)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(TaskFilter.values()) { filter ->
                    val isSelected = currentFilter == filter
                    val label = when (filter) {
                        TaskFilter.ALL -> "All"
                        TaskFilter.PENDING -> "Pending"
                        TaskFilter.COMPLETED -> "Completed"
                        TaskFilter.HIGH_PRIORITY -> "High Priority"
                    }
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { viewModel.setTaskFilter(filter) }
                            .border(
                                1.dp,
                                if (isSelected) ElectricBlue else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                RoundedCornerShape(20.dp)
                            )
                            .testTag("filter_chip_${filter.name}"),
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) ElectricBlue.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) ElectricBlue else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Subject Filter Bar
            if (distinctSubjects.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        SubjectChip(
                            subject = "All Subjects",
                            isSelected = currentSubjectFilter == null,
                            onClick = { viewModel.setSubjectFilter(null) }
                        )
                    }
                    items(distinctSubjects) { subj ->
                        SubjectChip(
                            subject = subj,
                            isSelected = currentSubjectFilter == subj,
                            onClick = {
                                viewModel.setSubjectFilter(if (currentSubjectFilter == subj) null else subj)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Task List
            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No tasks found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap + to add a study task or generate an AI plan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(tasks, key = { it.id }) { task ->
                        val isOverdue = !task.isCompleted && task.dueDateMillis > 0 && task.dueDateMillis < nowMillis

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("task_item_${task.id}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (task.isCompleted) {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            ),
                            border = if (isOverdue) androidx.compose.foundation.BorderStroke(1.dp, AccentRed) else null
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        IconButton(
                                            onClick = { viewModel.toggleTaskCompletion(task) },
                                            modifier = Modifier.size(32.dp).testTag("toggle_task_${task.id}")
                                        ) {
                                            Icon(
                                                imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                                contentDescription = if (task.isCompleted) "Completed" else "Mark Done",
                                                tint = if (task.isCompleted) AccentGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Column {
                                            Text(
                                                text = task.title,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                            )
                                            if (task.description.isNotBlank()) {
                                                Text(
                                                    text = task.description,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 2
                                                )
                                            }
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                taskToEdit = task
                                                showAddEditDialog = true
                                            },
                                            modifier = Modifier.size(32.dp).testTag("edit_task_${task.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { taskToDelete = task },
                                            modifier = Modifier.size(32.dp).testTag("delete_task_${task.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = AccentRed.copy(alpha = 0.8f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Footer with Subject, Priority, Duration, and Overdue Tag
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    PriorityBadge(priority = task.priority)

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = ElectricBlue.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = task.subject,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ElectricBlue,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontSize = 11.sp
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "${task.estimatedDurationMinutes}m",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.weight(1f))

                                    if (isOverdue) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = AccentRed.copy(alpha = 0.2f)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ErrorOutline,
                                                    contentDescription = "Overdue",
                                                    tint = AccentRed,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(
                                                    text = "OVERDUE",
                                                    color = AccentRed,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                    } else if (task.dueDateMillis > 0) {
                                        Text(
                                            text = "Due ${dateFormat.format(Date(task.dueDateMillis))}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (taskToDelete != null) {
        val target = taskToDelete!!
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = {
                Text(
                    text = "Delete Study Task?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete \"${target.title}\"? This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTask(target)
                        taskToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                    modifier = Modifier.testTag("confirm_delete_task_button")
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add / Edit Dialog
    if (showAddEditDialog) {
        AddEditTaskDialog(
            task = taskToEdit,
            onDismiss = { showAddEditDialog = false },
            onSave = { title, desc, subject, priority, duration, dueMillis ->
                if (taskToEdit == null) {
                    viewModel.addTask(
                        title = title,
                        description = desc,
                        subject = subject,
                        priority = priority,
                        dueDateMillis = dueMillis,
                        estimatedDurationMinutes = duration
                    )
                } else {
                    viewModel.updateTask(
                        taskToEdit!!.copy(
                            title = title,
                            description = desc,
                            subject = subject,
                            priority = priority,
                            dueDateMillis = dueMillis,
                            estimatedDurationMinutes = duration
                        )
                    )
                }
                showAddEditDialog = false
            }
        )
    }
}

@Composable
fun AddEditTaskDialog(
    task: TaskEntity?,
    onDismiss: () -> Unit,
    onSave: (title: String, desc: String, subject: String, priority: String, duration: Int, dueMillis: Long) -> Unit
) {
    val now = System.currentTimeMillis()
    var title by remember { mutableStateOf(task?.title ?: "") }
    var description by remember { mutableStateOf(task?.description ?: "") }
    var subject by remember { mutableStateOf(task?.subject ?: "Mathematics") }
    var customSubject by remember { mutableStateOf("") }
    var isCustomSubjectSelected by remember { mutableStateOf(false) }
    var priority by remember { mutableStateOf(task?.priority ?: "MEDIUM") }
    var durationMinutes by remember { mutableIntStateOf(task?.estimatedDurationMinutes ?: 30) }
    var selectedDueOffsetDays by remember { mutableIntStateOf(1) } // 0: Today, 1: Tomorrow, 3: In 3 Days, 7: In 1 Week

    val presetSubjects = listOf("Mathematics", "Physics", "Chemistry", "Computer Science", "Biology", "Literature", "History")
    val priorities = listOf("LOW", "MEDIUM", "HIGH")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (task == null) "Create Study Task" else "Edit Task",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title *") },
                    placeholder = { Text("e.g. Chapter 4 Practice Problems") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_task_title_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Notes") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_task_desc_input")
                )

                Text(
                    text = "Subject",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(presetSubjects) { subj ->
                        SubjectChip(
                            subject = subj,
                            isSelected = !isCustomSubjectSelected && subject == subj,
                            onClick = {
                                isCustomSubjectSelected = false
                                subject = subj
                            }
                        )
                    }
                    item {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { isCustomSubjectSelected = true }
                                .border(
                                    1.dp,
                                    if (isCustomSubjectSelected) ElectricBlue else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                    RoundedCornerShape(12.dp)
                                ),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCustomSubjectSelected) ElectricBlue.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "+ Custom",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isCustomSubjectSelected) ElectricBlue else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isCustomSubjectSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                if (isCustomSubjectSelected) {
                    OutlinedTextField(
                        value = customSubject,
                        onValueChange = { customSubject = it },
                        label = { Text("Custom Subject Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Due Date Quick Selector
                Text(
                    text = "Due Date",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val dueOptions = listOf(0 to "Today", 1 to "Tomorrow", 3 to "3 Days", 7 to "1 Week")
                    dueOptions.forEach { (offset, label) ->
                        val isSel = selectedDueOffsetDays == offset
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedDueOffsetDays = offset }
                                .border(
                                    1.dp,
                                    if (isSel) ElectricBlue else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    RoundedCornerShape(8.dp)
                                ),
                            color = if (isSel) ElectricBlue.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) ElectricBlue else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "Priority",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    priorities.forEach { p ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { priority = p }
                                .border(
                                    1.dp,
                                    if (priority == p) ElectricBlue else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    RoundedCornerShape(8.dp)
                                ),
                            color = if (priority == p) ElectricBlue.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = p,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = if (priority == p) ElectricBlue else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Text(
                    text = "Estimated Duration: ${durationMinutes}m",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(15, 25, 30, 45, 60, 90).forEach { mins ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { durationMinutes = mins },
                            color = if (durationMinutes == mins) ElectricBlue else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "${mins}m",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (durationMinutes == mins) Color(0xFF070B19) else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalSubject = if (isCustomSubjectSelected && customSubject.isNotBlank()) {
                        customSubject.trim()
                    } else subject

                    if (title.isNotBlank()) {
                        val computedDueMillis = if (task != null && selectedDueOffsetDays == 1 && task.dueDateMillis > 0) {
                            task.dueDateMillis
                        } else {
                            now + (selectedDueOffsetDays * 86_400_000L).coerceAtLeast(8 * 3600_000L)
                        }
                        onSave(title, description, finalSubject, priority, durationMinutes, computedDueMillis)
                    }
                },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue, contentColor = Color(0xFF070B19)),
                modifier = Modifier.testTag("dialog_save_task_button")
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
