package com.example.arkin.ui.screens

import com.example.arkin.ui.anim.animateStaggeredEntrance
import com.example.arkin.ui.anim.slideUpEntrance
import com.example.arkin.ui.anim.slideInFromLeft
import com.example.arkin.ui.anim.scaleEntrance
import com.example.arkin.ui.anim.bouncyClickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arkin.model.AppScreen
import com.example.arkin.ui.components.HeroBanner
import com.example.arkin.ui.components.QuickAccessButton
import com.example.arkin.ui.components.StatCard
import com.example.arkin.ui.components.getCategoryColor
import com.example.arkin.viewmodel.ArkinViewModel
import com.example.ui.theme.AccentBlueBg
import com.example.ui.theme.AccentBlueText
import com.example.ui.theme.AccentLockerBg
import com.example.ui.theme.AccentLockerText
import com.example.ui.theme.AccentOrangeBg
import com.example.ui.theme.AccentOrangeText
import com.example.ui.theme.AccentPinkBg
import com.example.ui.theme.AccentPinkText
import com.example.ui.theme.AccentPurpleBg
import com.example.ui.theme.AccentPurpleText
import com.example.ui.theme.AccentTealBg
import com.example.ui.theme.AccentTealText
import com.example.ui.theme.AccentYellowBg
import com.example.ui.theme.AccentYellowText
import com.example.ui.theme.ArkinPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.example.ui.theme.ArkinBackgroundDark
import com.example.ui.theme.ArkinPrimaryLight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import com.example.arkin.ui.components.ConfirmationDialog
import com.example.arkin.ui.components.SecurePinChangeDialog

@Composable
fun DashboardScreen(viewModel: ArkinViewModel) {
    val profile by viewModel.studentProfile.collectAsState()
    val tasks by viewModel.allTasks.collectAsState()
    val notes by viewModel.filteredPublicNotes.collectAsState()
    val homework by viewModel.allHomework.collectAsState()
    val timetable by viewModel.classesForDay.collectAsState()
    val reminders by viewModel.allReminders.collectAsState()

    val completedTasksCount = tasks.count { it.isCompleted }
    val totalTasksCount = tasks.size
    val pendingTasksCount = tasks.count { !it.isCompleted }
    val uncompletedRemindersCount = reminders.count { !it.isCompleted }

    val currentDateText = "Today, " + SimpleDateFormat("MMM d", Locale.getDefault()).format(Date())
    val studentFirstName = profile?.name?.trim()?.split(" ")?.firstOrNull()?.ifBlank { "Student" } ?: "Student"

    val isDark = isSystemInDarkTheme() || MaterialTheme.colorScheme.background == ArkinBackgroundDark
    val scrollState = rememberScrollState()

    var isDrawerOpen by remember { mutableStateOf(false) }
    var isChangePinDialogOpen by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    if (isChangePinDialogOpen) {
        SecurePinChangeDialog(
            onDismiss = { isChangePinDialogOpen = false },
            onPinSuccessfullyChanged = { newPin ->
                viewModel.changeLockerPin(newPin)
                isChangePinDialogOpen = false
            }
        )
    }

    if (showDeleteConfirmDialog) {
        ConfirmationDialog(
            title = "Delete Account Data?",
            message = "This will remove all merged notes, tasks, homework, and timetable entries from this account so you can start completely fresh. Are you sure?",
            confirmButtonText = "Delete All Data",
            icon = Icons.Default.DeleteSweep,
            onConfirm = {
                showDeleteConfirmDialog = false
                viewModel.resetAccountData(clearCloud = true)
            },
            onDismiss = { showDeleteConfirmDialog = false }
        )
    }

    if (showLogoutConfirmDialog) {
        ConfirmationDialog(
            title = "Log Out?",
            message = "Are you sure you want to log out of your account?",
            confirmButtonText = "Log Out",
            icon = Icons.AutoMirrored.Filled.ExitToApp,
            onConfirm = {
                showLogoutConfirmDialog = false
                viewModel.logout()
            },
            onDismiss = { showLogoutConfirmDialog = false }
        )
    }

    if (isDrawerOpen) {
        Dialog(
            onDismissRequest = { isDrawerOpen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { isDrawerOpen = false }
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(300.dp)
                        .clickable(enabled = false) {},
                    shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                    ) {
                        // Header with close button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Arkin Menu",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(onClick = { isDrawerOpen = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close Menu")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Profile Card inside drawer
                        Surface(
                            onClick = {
                                isDrawerOpen = false
                                viewModel.navigateTo(AppScreen.PROFILE)
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = ArkinPrimary.copy(alpha = 0.1f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = ArkinPrimary,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = studentFirstName.take(1),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = profile?.name?.ifBlank { "Student Profile" } ?: "Student Profile",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "View & Edit Profile",
                                        fontSize = 11.sp,
                                        color = ArkinPrimary
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = ArkinPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                        )

                        // 1. Dark Mode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.DarkMode,
                                    contentDescription = "Dark Mode",
                                    tint = ArkinPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = "Dark Mode",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Switch(
                                checked = isDark,
                                onCheckedChange = { viewModel.toggleDarkMode() },
                                colors = SwitchDefaults.colors(checkedThumbColor = ArkinPrimary)
                            )
                        }

                        // 2. Change Locker PIN
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    isDrawerOpen = false
                                    isChangePinDialogOpen = true
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Change Locker PIN",
                                tint = AccentLockerText,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = "Change Locker PIN",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // 3. Delete / Reset Account Data
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    isDrawerOpen = false
                                    showDeleteConfirmDialog = true
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Delete Account Data",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = "Delete Account Data",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFEF4444)
                            )
                        }

                        // 4. Log Out
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    isDrawerOpen = false
                                    showLogoutConfirmDialog = true
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Log Out",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = "Log Out",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
            .testTag("dashboard_screen_root")
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = { isDrawerOpen = true },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp,
                modifier = Modifier
                    .size(42.dp)
                    .testTag("dashboard_hamburger_menu")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Box(contentAlignment = Alignment.TopEnd) {
                IconButton(onClick = { viewModel.navigateTo(AppScreen.REMINDERS) }) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Reminders",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (uncompletedRemindersCount > 0) {
                    Box(
                        modifier = Modifier
                            .padding(top = 8.dp, end = 8.dp)
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444))
                            .border(1.5.dp, MaterialTheme.colorScheme.background, CircleShape)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Greeting
        Text(
            text = "Good Morning, $studentFirstName 👋",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Stay organized, stay productive.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, bottom = 20.dp)
        )

        // Hero Banner
        HeroBanner(
            completedTasks = completedTasksCount,
            totalTasks = totalTasksCount,
            currentDateText = currentDateText,
            modifier = Modifier.scaleEntrance(delayMs = 80)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // AI File & Timetable Upload Card
        val aiCardBg = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF3E8FF)
        val aiIconBg = if (isDark) MaterialTheme.colorScheme.surface else Color.White
        val aiIconTint = if (isDark) ArkinPrimaryLight else Color(0xFF9333EA)

        Surface(
            onClick = { viewModel.navigateTo(AppScreen.AI_DOC_ORGANIZER) },
            shape = RoundedCornerShape(22.dp),
            color = aiCardBg,
            shadowElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("dashboard_doc_upload_card")
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = aiIconBg,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Assignment,
                            contentDescription = "Upload Document",
                            tint = aiIconTint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Upload & Auto-Organize",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AI",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF9333EA))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "Upload a Timetable, Question Paper or Assignment to organize automatically.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Quick Access Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Quick Access",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 8-Item Quick Access Grid
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                QuickAccessButton(
                    title = "Notes", subtitle = "All your notes", icon = Icons.Rounded.Description, bgColor = AccentPurpleBg, iconColor = AccentPurpleText, modifier = Modifier.weight(1f).animateStaggeredEntrance(index = 0),
                    onClick = { viewModel.navigateTo(AppScreen.NOTES) }
                )
                QuickAccessButton(
                    title = "To-Do", subtitle = "Your tasks", icon = Icons.Rounded.Checklist, bgColor = AccentTealBg, iconColor = AccentTealText, modifier = Modifier.weight(1f).animateStaggeredEntrance(index = 1),
                    onClick = { viewModel.navigateTo(AppScreen.TODO) }
                )
                QuickAccessButton(
                    title = "Homework", subtitle = "Assignments", icon = Icons.Rounded.Book, bgColor = AccentOrangeBg, iconColor = AccentOrangeText, modifier = Modifier.weight(1f).animateStaggeredEntrance(index = 2),
                    onClick = { viewModel.navigateTo(AppScreen.HOMEWORK) }
                )
                QuickAccessButton(
                    title = "Timetable", subtitle = "Class schedule", icon = Icons.Rounded.CalendarToday, bgColor = AccentBlueBg, iconColor = AccentBlueText, modifier = Modifier.weight(1f).animateStaggeredEntrance(index = 3),
                    onClick = { viewModel.navigateTo(AppScreen.TIMETABLE) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                QuickAccessButton(
                    title = "AI Assistant", subtitle = "Ask anything", icon = Icons.Rounded.Psychology, bgColor = AccentPinkBg, iconColor = AccentPinkText, modifier = Modifier.weight(1f).animateStaggeredEntrance(index = 4),
                    hasBadge = false,
                    onClick = { viewModel.navigateTo(AppScreen.AI_ASSISTANT) }
                )
                QuickAccessButton(
                    title = "Locker", subtitle = "Secure vault", icon = Icons.Rounded.Lock, bgColor = AccentLockerBg, iconColor = AccentLockerText, modifier = Modifier.weight(1f).animateStaggeredEntrance(index = 5),
                    onClick = { viewModel.navigateTo(AppScreen.LOCKER) }
                )
                QuickAccessButton(
                    title = "Reminders", subtitle = "Stay updated", icon = Icons.Rounded.NotificationsActive, bgColor = AccentYellowBg, iconColor = AccentYellowText, modifier = Modifier.weight(1f).animateStaggeredEntrance(index = 6),
                    onClick = { viewModel.navigateTo(AppScreen.REMINDERS) }
                )
                QuickAccessButton(
                    title = "Quick Note", subtitle = "Capture ideas", icon = Icons.Rounded.EditNote, bgColor = AccentPurpleBg, iconColor = AccentPurpleText, modifier = Modifier.weight(1f).animateStaggeredEntrance(index = 7),
                    onClick = { viewModel.openAddNoteDialog(isSecure = false) }
                )
            }
        }

        Spacer(modifier = Modifier.height(26.dp))

        // Today's Overview
        Text(
            text = "Today's Overview",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            item { StatCard(count = notes.size, modifier = Modifier.animateStaggeredEntrance(index = 8),
                    title = "Notes",
                    subtitle = "Total notes",
                    icon = Icons.Rounded.Description,
                    bgColor = AccentPurpleBg,
                    iconColor = AccentPurpleText,
                    onClick = { viewModel.navigateTo(AppScreen.NOTES) }
                )
            }
            item { StatCard(count = pendingTasksCount, modifier = Modifier.animateStaggeredEntrance(index = 9),
                    title = "Tasks",
                    subtitle = "Pending tasks",
                    icon = Icons.Rounded.Checklist,
                    bgColor = AccentTealBg,
                    iconColor = AccentTealText,
                    onClick = { viewModel.navigateTo(AppScreen.TODO) }
                )
            }
            item {
                StatCard(
                    count = homework.size,
                    title = "Homework",
                    subtitle = "Due this week",
                    icon = Icons.Rounded.Book,
                    bgColor = AccentOrangeBg,
                    iconColor = AccentOrangeText,
                    onClick = { viewModel.navigateTo(AppScreen.HOMEWORK) }
                )
            }
        }

        Spacer(modifier = Modifier.height(26.dp))

        // Upcoming Timeline Preview
        Text(
            text = "Upcoming",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 100.dp)
                .testTag("dashboard_upcoming_card")
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                val previewItems = (timetable.take(2).map { "Class: ${it.subject}" to it.timeSlot } +
                        homework.take(2).map { "${it.subject} ${it.title}" to "Due ${it.dueDate}" }).take(3)

                if (previewItems.isEmpty()) {
                    Text(
                        text = "All clear for today! No upcoming deadlines.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    previewItems.forEachIndexed { index, (title, time) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (index == 0) ArkinPrimary else Color(0xFFEA580C))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = time,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ArkinPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
