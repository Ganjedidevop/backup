package com.example.arkin.ui.screens

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.CalendarToday
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.window.Dialog
import com.example.arkin.data.model.ReminderEntity
import com.example.arkin.ui.components.AppHeader
import com.example.arkin.ui.components.ConfirmationDialog
import com.example.arkin.ui.components.ReminderRow
import com.example.arkin.viewmodel.ArkinViewModel
import com.example.ui.theme.AccentYellowBg
import com.example.ui.theme.AccentYellowText
import com.example.ui.theme.ArkinPrimary

@Composable
fun RemindersScreen(viewModel: ArkinViewModel) {
    val reminders by viewModel.allReminders.collectAsState()
    var isAddDialogOpen by remember { mutableStateOf(false) }
    var reminderToDelete by remember { mutableStateOf<ReminderEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("reminders_screen_root")
    ) {
        AppHeader(
            title = "Reminders",
            onBackClick = { viewModel.goBack() },
            trailingAction = {
                Surface(
                    onClick = { isAddDialogOpen = true },
                    shape = CircleShape,
                    color = AccentYellowBg,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("add_reminder_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Reminder",
                            tint = AccentYellowText,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        )

        if (reminders.isEmpty()) {
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
                                imageVector = Icons.Rounded.NotificationsActive,
                                contentDescription = "No Reminders",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "No active reminders",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Add reminders for tuition fee dates, library book returns, and project milestones.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                    )
                    Button(
                        onClick = { isAddDialogOpen = true },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ArkinPrimary)
                    ) {
                        Text("Add Reminder")
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(reminders, key = { it.id }) { reminder ->
                    ReminderRow(
                        reminder = reminder,
                        onDelete = { reminderToDelete = reminder }
                    )
                }
            }
        }
    }

    if (isAddDialogOpen) {
        AddReminderDialog(
            onDismiss = { isAddDialogOpen = false },
            onAdd = { title, dueText, iconKey, colorKey ->
                viewModel.addReminder(title, dueText, iconKey, colorKey)
                isAddDialogOpen = false
            }
        )
    }

    reminderToDelete?.let { reminder ->
        ConfirmationDialog(
            title = "Delete Reminder?",
            message = "Are you sure you want to delete '${reminder.title}'?",
            confirmButtonText = "Delete",
            onConfirm = {
                viewModel.deleteReminder(reminder)
                reminderToDelete = null
            },
            onDismiss = { reminderToDelete = null }
        )
    }
}

@Composable
private fun AddReminderDialog(
    onDismiss: () -> Unit,
    onAdd: (title: String, dueText: String, iconKey: String, colorKey: String) -> Unit
) {
    val context = LocalContext.current
    val calendar = remember {
        Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
    }
    val dateTimeFormatter = remember { SimpleDateFormat("dd/MM/yyyy, hh:mm a", Locale.getDefault()) }
    var dueText by remember { mutableStateOf(dateTimeFormatter.format(calendar.time)) }
    var title by remember { mutableStateOf("") }
    var titleError by remember { mutableStateOf<String?>(null) }
    var iconKey by remember { mutableStateOf("bell") }

    val timePicker = remember {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                dueText = dateTimeFormatter.format(calendar.time)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
        )
    }

    val datePicker = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                timePicker.show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = System.currentTimeMillis()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("add_reminder_dialog_surface")
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "New Reminder 🔔",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.isNotBlank()) titleError = null
                    },
                    label = { Text("Reminder Title *") },
                    placeholder = { Text("e.g. Return library book, Fee payment") },
                    isError = titleError != null,
                    supportingText = titleError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ArkinPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("reminder_title_input")
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 18.dp)
                ) {
                    OutlinedTextField(
                        value = dueText,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("When (Date & Time) *") },
                        placeholder = { Text("Tap to select date & time") },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { datePicker.show() }) {
                                Icon(
                                    imageVector = Icons.Rounded.CalendarToday,
                                    contentDescription = "Select Date & Time",
                                    tint = ArkinPrimary
                                )
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ArkinPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reminder_time_input")
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { datePicker.show() }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                titleError = "Please enter a reminder title"
                                return@Button
                            }
                            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                                android.widget.Toast.makeText(
                                    context,
                                    "Please select a future date and time",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }
                            onAdd(title.trim(), dueText, iconKey, "yellow")
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ArkinPrimary),
                        modifier = Modifier.testTag("submit_reminder_button")
                    ) {
                        Text("Add Reminder", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
