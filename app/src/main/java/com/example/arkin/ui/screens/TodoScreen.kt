package com.example.arkin.ui.screens

import com.example.arkin.ui.anim.animateStaggeredEntrance
import com.example.arkin.ui.anim.bouncyClickable
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arkin.data.model.TaskEntity
import com.example.arkin.ui.components.AppHeader
import com.example.arkin.ui.components.ConfirmationDialog
import com.example.arkin.ui.components.TaskItemRow
import com.example.arkin.viewmodel.ArkinViewModel
import com.example.ui.theme.ArkinPrimary

@Composable
fun TodoScreen(viewModel: ArkinViewModel) {
    val tasks by viewModel.allTasks.collectAsState()
    var taskInput by remember { mutableStateOf("") }
    var taskDescriptionInput by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") } // All, Pending, Completed
    var taskToDelete by remember { mutableStateOf<TaskEntity?>(null) }
    var taskToComplete by remember { mutableStateOf<TaskEntity?>(null) }

    val filteredTasks = tasks.filter {
        when (selectedFilter) {
            "Pending" -> !it.isCompleted
            "Completed" -> it.isCompleted
            else -> true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("todo_screen_root")
    ) {
        AppHeader(
            title = "To-Do List",
            onBackClick = { viewModel.goBack() }
        )

        // Task input form (Title + Description)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = taskInput,
                    onValueChange = { taskInput = it },
                    placeholder = { Text("Add a new task...") },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ArkinPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("todo_input_field")
                )

                Surface(
                    onClick = {
                        if (taskInput.isNotBlank()) {
                            viewModel.addTask(
                                title = taskInput,
                                description = taskDescriptionInput
                            )
                            taskInput = ""
                            taskDescriptionInput = ""
                        } else {
                            viewModel.showToast("Please enter a task title")
                        }
                    },
                    shape = RoundedCornerShape(18.dp),
                    color = ArkinPrimary,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .size(54.dp)
                        .testTag("todo_add_submit_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Task",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            OutlinedTextField(
                value = taskDescriptionInput,
                onValueChange = { taskDescriptionInput = it },
                placeholder = { Text("Add description (optional)...") },
                minLines = 1,
                maxLines = 3,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ArkinPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("todo_description_field")
            )
        }

        // Filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Pending", "Completed").forEach { filter ->
                val isSelected = selectedFilter == filter
                Surface(
                    onClick = { selectedFilter = filter },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) ArkinPrimary else MaterialTheme.colorScheme.surface,
                    shadowElevation = if (isSelected) 2.dp else 1.dp,
                    modifier = Modifier.testTag("todo_filter_$filter")
                ) {
                    Text(
                        text = filter,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Tasks list
        if (filteredTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(68.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Checklist,
                                contentDescription = "All Caught Up",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (selectedFilter == "Completed") "No completed tasks yet" else "All caught up! 🎉",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Add tasks to keep your study schedule on track.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredTasks, key = { it.id }) { task ->
                    TaskItemRow(
                        task = task,
                        onToggle = {
                            if (!task.isCompleted) {
                                taskToComplete = task
                            } else {
                                viewModel.toggleTask(task)
                            }
                        },
                        onDelete = { taskToDelete = task },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }

    taskToComplete?.let { task ->
        ConfirmationDialog(
            title = "Mark Task Done?",
            message = "Are you sure you have completed '${task.title}'?",
            confirmButtonText = "Yes, Done",
            dismissButtonText = "Not Yet",
            isDestructive = false,
            icon = Icons.Rounded.CheckCircle,
            onConfirm = {
                viewModel.toggleTask(task)
                taskToComplete = null
            },
            onDismiss = { taskToComplete = null }
        )
    }

    taskToDelete?.let { task ->
        ConfirmationDialog(
            title = "Delete Task?",
            message = "Are you sure you want to delete '${task.title}'? This action cannot be undone.",
            confirmButtonText = "Delete",
            onConfirm = {
                viewModel.deleteTask(task)
                taskToDelete = null
            },
            onDismiss = { taskToDelete = null }
        )
    }
}
