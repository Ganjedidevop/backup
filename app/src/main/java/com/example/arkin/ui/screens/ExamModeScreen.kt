package com.example.arkin.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.arkin.data.model.TimetableEntity
import com.example.arkin.model.AiExamPrepState
import com.example.arkin.model.AppScreen
import com.example.arkin.model.DocUploadType
import com.example.arkin.model.RevisionPlanItem
import com.example.arkin.ui.components.AppHeader
import com.example.arkin.ui.components.ConfirmationDialog
import com.example.arkin.ui.components.MarkdownText
import com.example.arkin.util.AlarmScheduler
import com.example.arkin.viewmodel.ArkinViewModel
import com.example.ui.theme.AccentBlueBg
import com.example.ui.theme.AccentBlueText
import com.example.ui.theme.AccentOrangeBg
import com.example.ui.theme.AccentOrangeText
import com.example.ui.theme.AccentPurpleBg
import com.example.ui.theme.AccentPurpleText
import com.example.ui.theme.AccentTealBg
import com.example.ui.theme.AccentTealText
import com.example.ui.theme.ArkinPrimary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class ExamModeTab {
    SCHEDULE,
    PLANNER,
    AI_PREP
}

@Composable
fun ExamModeScreen(viewModel: ArkinViewModel) {
    val exams by viewModel.allExams.collectAsState()
    val prepState by viewModel.aiExamPrepState.collectAsState()
    val completedRevisionIds by viewModel.completedRevisionTaskIds.collectAsState()
    val studyHours by viewModel.selectedStudyHoursPerDay.collectAsState()
    val customSyllabusMap by viewModel.examCustomSyllabus.collectAsState()

    var currentTab by remember { mutableStateOf(ExamModeTab.SCHEDULE) }
    var isCalendarView by remember { mutableStateOf(false) }
    var selectedCalendarDate by remember { mutableStateOf<String?>(null) }

    var isAddExamDialogOpen by remember { mutableStateOf(false) }
    var examBeingEdited by remember { mutableStateOf<TimetableEntity?>(null) }
    var examToDelete by remember { mutableStateOf<TimetableEntity?>(null) }
    var selectedExamForPrep by remember { mutableStateOf<TimetableEntity?>(null) }

    // If an exam is selected for prep or planner, maintain reference
    val activeExam = selectedExamForPrep ?: exams.firstOrNull()

    val nextExamInfo = remember(exams) {
        val now = System.currentTimeMillis()
        exams.firstOrNull { exam ->
            val epoch = AlarmScheduler.parseDateTimeToEpoch("${exam.examDate} ${exam.startTime.ifBlank { exam.timeSlot }}")
            epoch != null && epoch >= now - (3 * 3600 * 1000L) // Include exams happening today
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("exam_mode_root")
    ) {
        // App Header
        AppHeader(
            title = "Exam Mode 🎯",
            onBackClick = { viewModel.goBack() },
            trailingAction = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        onClick = {
                            viewModel.setUploadDocType(DocUploadType.TIMETABLE)
                            viewModel.navigateTo(AppScreen.AI_DOC_ORGANIZER)
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF3E8FF),
                        modifier = Modifier
                            .height(40.dp)
                            .testTag("exam_ai_upload_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        ) {
                            Text("🪄 Import Date Sheet", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9333EA))
                        }
                    }

                    Surface(
                        onClick = { isAddExamDialogOpen = true },
                        shape = CircleShape,
                        color = Color(0xFFFEE2E2),
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("add_exam_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Exam",
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        )

        // Hero Countdown Banner
        NextExamCountdownBanner(
            nextExam = nextExamInfo,
            onStartPrep = { exam ->
                selectedExamForPrep = exam
                currentTab = ExamModeTab.AI_PREP
            }
        )

        // Mode Navigation Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TabButton(
                title = "Schedule (${exams.size})",
                icon = Icons.Rounded.CalendarToday,
                isSelected = currentTab == ExamModeTab.SCHEDULE,
                onClick = { currentTab = ExamModeTab.SCHEDULE },
                modifier = Modifier.weight(1f)
            )
            TabButton(
                title = "Revision Plan",
                icon = Icons.Rounded.Checklist,
                isSelected = currentTab == ExamModeTab.PLANNER,
                onClick = { currentTab = ExamModeTab.PLANNER },
                modifier = Modifier.weight(1f)
            )
            TabButton(
                title = "AI Prep 🪄",
                icon = Icons.Rounded.Psychology,
                isSelected = currentTab == ExamModeTab.AI_PREP,
                onClick = { currentTab = ExamModeTab.AI_PREP },
                modifier = Modifier.weight(1f)
            )
        }

        // Tab Content
        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            when (currentTab) {
                ExamModeTab.SCHEDULE -> {
                    ExamScheduleTab(
                        exams = exams,
                        isCalendarView = isCalendarView,
                        selectedCalendarDate = selectedCalendarDate,
                        onToggleView = { isCalendarView = !isCalendarView },
                        onSelectCalendarDate = { selectedCalendarDate = it },
                        onAddExamClick = { isAddExamDialogOpen = true },
                        onEditExam = { examBeingEdited = it },
                        onDeleteExam = { examToDelete = it },
                        onStartPrep = { exam ->
                            selectedExamForPrep = exam
                            currentTab = ExamModeTab.AI_PREP
                        }
                    )
                }

                ExamModeTab.PLANNER -> {
                    RevisionPlannerTab(
                        exams = exams,
                        selectedExam = activeExam,
                        studyHours = studyHours,
                        completedTaskIds = completedRevisionIds,
                        customSyllabus = activeExam?.let { customSyllabusMap[it.id] }.orEmpty(),
                        onSelectExam = { selectedExamForPrep = it },
                        onStudyHoursChanged = { viewModel.setStudyHours(it) },
                        onToggleTask = { viewModel.toggleRevisionTask(it) },
                        onSyllabusChanged = { syllabus ->
                            activeExam?.let { viewModel.setExamSyllabus(it.id, syllabus) }
                        },
                        onNavigateToAiPrep = {
                            currentTab = ExamModeTab.AI_PREP
                        }
                    )
                }

                ExamModeTab.AI_PREP -> {
                    AiExamPrepTab(
                        viewModel = viewModel,
                        exams = exams,
                        selectedExam = activeExam,
                        prepState = prepState,
                        onSelectExam = { selectedExamForPrep = it },
                        onGeneratePrep = { exam, mode ->
                            viewModel.generateExamPreparation(exam, mode)
                        },
                        onResetPrep = { viewModel.resetAiExamPrepState() }
                    )
                }
            }
        }
    }

    // Add Exam Dialog
    if (isAddExamDialogOpen) {
        ExamEditorDialog(
            title = "Add Exam Schedule 🎯",
            initialExam = null,
            onDismiss = { isAddExamDialogOpen = false },
            onSave = { subject, courseCode, examDate, timeSlot, room ->
                viewModel.addExam(
                    subject = subject,
                    courseCode = courseCode,
                    examDate = examDate,
                    timeSlot = timeSlot,
                    room = room
                )
                isAddExamDialogOpen = false
            }
        )
    }

    // Edit Exam Dialog
    examBeingEdited?.let { exam ->
        ExamEditorDialog(
            title = "Edit Exam Schedule 📝",
            initialExam = exam,
            onDismiss = { examBeingEdited = null },
            onSave = { subject, courseCode, examDate, timeSlot, room ->
                val updated = exam.copy(
                    subject = subject,
                    courseCode = courseCode,
                    examDate = examDate,
                    timeSlot = timeSlot,
                    room = room
                )
                viewModel.updateExam(updated)
                examBeingEdited = null
            }
        )
    }

    // Delete Exam Confirmation
    examToDelete?.let { exam ->
        ConfirmationDialog(
            title = "Remove Exam?",
            message = "Are you sure you want to delete '${exam.subject}' scheduled on ${exam.examDate}?",
            confirmButtonText = "Delete Exam",
            onConfirm = {
                viewModel.deleteExam(exam)
                examToDelete = null
            },
            onDismiss = { examToDelete = null }
        )
    }
}

@Composable
private fun NextExamCountdownBanner(
    nextExam: TimetableEntity?,
    onStartPrep: (TimetableEntity) -> Unit
) {
    if (nextExam == null) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFFF3F4F6)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFE5E7EB),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarToday,
                            contentDescription = null,
                            tint = Color(0xFF6B7280),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "No Upcoming Exams Scheduled",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                    Text(
                        text = "Import your exam date sheet via AI or tap + to schedule an exam.",
                        fontSize = 11.sp,
                        color = Color(0xFF6B7280)
                    )
                }
            }
        }
        return
    }

    val countdownText = remember(nextExam) {
        val epoch = AlarmScheduler.parseDateTimeToEpoch("${nextExam.examDate} ${nextExam.startTime.ifBlank { nextExam.timeSlot }}")
        if (epoch == null) "Scheduled: ${nextExam.examDate}"
        else {
            val diffMs = epoch - System.currentTimeMillis()
            if (diffMs <= 0) "Today! Starts at ${nextExam.timeSlot}"
            else {
                val days = TimeUnit.MILLISECONDS.toDays(diffMs)
                val hours = TimeUnit.MILLISECONDS.toHours(diffMs) % 24
                when {
                    days > 0 -> "$days days, $hours hrs remaining"
                    hours > 0 -> "$hours hours remaining"
                    else -> "Starts in less than an hour!"
                }
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .testTag("next_exam_hero_banner"),
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFFFEF2F2),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFDC2626),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = "NEXT EXAM",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = nextExam.examDate,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF991B1B)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.HourglassBottom,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = countdownText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (nextExam.courseCode.isNotBlank()) "${nextExam.courseCode}: ${nextExam.subject}" else nextExam.subject,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF111827),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "Timing: ${nextExam.timeSlot}${if (nextExam.room.isNotBlank()) " • Hall: ${nextExam.room}" else ""}",
                fontSize = 12.sp,
                color = Color(0xFF4B5563),
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = { onStartPrep(nextExam) },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Psychology,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("AI Prep & Revision 🪄", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TabButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) ArkinPrimary else MaterialTheme.colorScheme.surface,
        shadowElevation = if (isSelected) 3.dp else 1.dp,
        modifier = modifier.height(42.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ExamScheduleTab(
    exams: List<TimetableEntity>,
    isCalendarView: Boolean,
    selectedCalendarDate: String?,
    onToggleView: () -> Unit,
    onSelectCalendarDate: (String?) -> Unit,
    onAddExamClick: () -> Unit,
    onEditExam: (TimetableEntity) -> Unit,
    onDeleteExam: (TimetableEntity) -> Unit,
    onStartPrep: (TimetableEntity) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        // Controls Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isCalendarView) "Calendar View 📅" else "Upcoming Exams (${exams.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Surface(
                onClick = onToggleView,
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.height(34.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp)
                ) {
                    Icon(
                        imageVector = if (isCalendarView) Icons.Rounded.Description else Icons.Rounded.CalendarMonth,
                        contentDescription = "Toggle View",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isCalendarView) "List View" else "Calendar View",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (exams.isEmpty()) {
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
                                imageVector = Icons.Rounded.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "No Exams Added Yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Add your exam dates manually or upload your timetable image/PDF to extract automatically.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                    )
                    Button(
                        onClick = onAddExamClick,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ArkinPrimary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Exam Schedule")
                    }
                }
            }
        } else if (isCalendarView) {
            // Calendar View with Date Grouping
            val dates = remember(exams) { exams.map { it.examDate }.distinct() }

            LazyRow(
                contentPadding = PaddingValues(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        text = "All Dates",
                        isSelected = selectedCalendarDate == null,
                        onClick = { onSelectCalendarDate(null) }
                    )
                }
                items(dates) { d ->
                    FilterChip(
                        text = d,
                        isSelected = selectedCalendarDate == d,
                        onClick = { onSelectCalendarDate(d) }
                    )
                }
            }

            val displayedExams = if (selectedCalendarDate != null) {
                exams.filter { it.examDate == selectedCalendarDate }
            } else exams

            LazyColumn(
                contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(displayedExams, key = { it.id }) { exam ->
                    ExamCard(
                        exam = exam,
                        onEdit = { onEditExam(exam) },
                        onDelete = { onDeleteExam(exam) },
                        onStartPrep = { onStartPrep(exam) }
                    )
                }
            }
        } else {
            // Standard Chronological List View
            LazyColumn(
                contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(exams, key = { it.id }) { exam ->
                    ExamCard(
                        exam = exam,
                        onEdit = { onEditExam(exam) },
                        onDelete = { onDeleteExam(exam) },
                        onStartPrep = { onStartPrep(exam) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) ArkinPrimary else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.height(32.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ExamCard(
    exam: TimetableEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStartPrep: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("exam_card_${exam.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEE2E2),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = "🎯 EXAM",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    if (exam.courseCode.isNotBlank()) {
                        Text(
                            text = exam.courseCode,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Exam",
                            tint = ArkinPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Exam",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = exam.subject,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Date and Time Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarToday,
                        contentDescription = null,
                        tint = ArkinPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = exam.examDate,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = Color(0xFFEA580C),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = exam.timeSlot,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (exam.room.isNotBlank()) {
                    Text(
                        text = "• ${exam.room}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onStartPrep,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Psychology,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("AI Prep 🪄", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun RevisionPlannerTab(
    exams: List<TimetableEntity>,
    selectedExam: TimetableEntity?,
    studyHours: Int,
    completedTaskIds: Set<String>,
    customSyllabus: String,
    onSelectExam: (TimetableEntity) -> Unit,
    onStudyHoursChanged: (Int) -> Unit,
    onToggleTask: (String) -> Unit,
    onSyllabusChanged: (String) -> Unit,
    onNavigateToAiPrep: () -> Unit
) {
    if (exams.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Please add or import an exam first to create a revision plan.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val exam = selectedExam ?: exams.first()
    val scrollState = rememberScrollState()

    // Compute remaining days
    val daysUntilExam = remember(exam) {
        val epoch = AlarmScheduler.parseDateTimeToEpoch("${exam.examDate} ${exam.startTime.ifBlank { exam.timeSlot }}")
        if (epoch != null) {
            val diff = epoch - System.currentTimeMillis()
            if (diff <= 0) 1 else (TimeUnit.MILLISECONDS.toDays(diff).toInt().coerceAtLeast(1))
        } else 3
    }

    val totalHoursAvailable = daysUntilExam * studyHours

    // Parse syllabus topics or generate standard template
    val topics = remember(customSyllabus, exam) {
        if (customSyllabus.isNotBlank()) {
            customSyllabus.split("\n", ",").map { it.trim().removePrefix("-").removePrefix("*").trim() }.filter { it.isNotBlank() }
        } else {
            listOf(
                "Core Theory & Fundamentals",
                "Key Formulas & Definitions",
                "High-Weightage Problem Solving",
                "Previous Year Questions & Derivations",
                "Final Mock Test & Rapid Recall"
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        // Exam Selector Chips
        Text(
            text = "Select Exam for Revision Plan",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        LazyRow(
            contentPadding = PaddingValues(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(exams) { e ->
                FilterChip(
                    text = e.subject,
                    isSelected = e.id == exam.id,
                    onClick = { onSelectExam(e) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Planner Stats Card
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "🎯 ${exam.subject} Plan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Date: ${exam.examDate} • $daysUntilExam day(s) remaining",
                    fontSize = 12.sp,
                    color = Color(0xFFDC2626),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                // Study Hours Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Daily Study Target:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(2, 3, 4, 6).forEach { hrs ->
                            Surface(
                                onClick = { onStudyHoursChanged(hrs) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (studyHours == hrs) ArkinPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(width = 44.dp, height = 30.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${hrs}h",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (studyHours == hrs) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Progress Bar
                val completedCount = topics.indices.count { completedTaskIds.contains("${exam.id}_$it") }
                val progress = if (topics.isNotEmpty()) completedCount.toFloat() / topics.size.toFloat() else 0f

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Plan Progress: $completedCount of ${topics.size} completed",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ArkinPrimary
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ArkinPrimary
                    )
                }

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .padding(top = 4.dp),
                    color = ArkinPrimary,
                    trackColor = ArkinPrimary.copy(alpha = 0.2f)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Daily Revision Tasks Breakdown
        Text(
            text = "Daily Revision Tasks Checklist",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Mark topics complete as you finish revising each day.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        topics.forEachIndexed { index, topic ->
            val taskId = "${exam.id}_$index"
            val isDone = completedTaskIds.contains(taskId)
            val dayLabel = "Day ${index % daysUntilExam + 1}"

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDone) Color(0xFFF0FDF4) else MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isDone,
                        onCheckedChange = { onToggleTask(taskId) },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF16A34A))
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = dayLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDone) Color(0xFF16A34A) else ArkinPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• Target: ${studyHours / (topics.size.coerceAtLeast(1))} hrs",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = topic,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDone) Color(0xFF166534) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Custom Syllabus Input
        Text(
            text = "Syllabus & High-Yield Topics",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        OutlinedTextField(
            value = customSyllabus,
            onValueChange = onSyllabusChanged,
            placeholder = { Text("Paste or type exam syllabus topics (one per line) to personalize your revision plan...") },
            minLines = 3,
            maxLines = 6,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ArkinPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 14.dp)
        )

        Button(
            onClick = onNavigateToAiPrep,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ArkinPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(bottom = 20.dp)
        ) {
            Icon(Icons.Rounded.Psychology, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Generate AI Practice Material 🪄", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AiExamPrepTab(
    viewModel: ArkinViewModel,
    exams: List<TimetableEntity>,
    selectedExam: TimetableEntity?,
    prepState: AiExamPrepState,
    onSelectExam: (TimetableEntity) -> Unit,
    onGeneratePrep: (TimetableEntity, String) -> Unit,
    onResetPrep: () -> Unit
) {
    if (exams.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Please add an exam before generating AI preparation materials.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val exam = selectedExam ?: exams.first()
    var selectedMode by remember { mutableStateOf("SUMMARY") }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        // Exam Selector
        Text(
            text = "Select Exam to Prepare",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        LazyRow(
            contentPadding = PaddingValues(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(exams) { e ->
                FilterChip(
                    text = e.subject,
                    isSelected = e.id == exam.id,
                    onClick = {
                        onSelectExam(e)
                        onResetPrep()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Preparation Modes Row
        Text(
            text = "Choose Preparation Tool",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PrepModeCard(
                title = "Summary",
                subtitle = "Core Formulas",
                icon = Icons.Rounded.Description,
                isSelected = selectedMode == "SUMMARY",
                onClick = { selectedMode = "SUMMARY" },
                modifier = Modifier.weight(1f)
            )
            PrepModeCard(
                title = "Checklist",
                subtitle = "Topic Weights",
                icon = Icons.Rounded.Checklist,
                isSelected = selectedMode == "TOPICS",
                onClick = { selectedMode = "TOPICS" },
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PrepModeCard(
                title = "Questions",
                subtitle = "With Solutions",
                icon = Icons.Rounded.MenuBook,
                isSelected = selectedMode == "QUESTIONS",
                onClick = { selectedMode = "QUESTIONS" },
                modifier = Modifier.weight(1f)
            )
            PrepModeCard(
                title = "Mock Test",
                subtitle = "Timed Quiz",
                icon = Icons.Rounded.Quiz,
                isSelected = selectedMode == "MOCK_TEST",
                onClick = { selectedMode = "MOCK_TEST" },
                modifier = Modifier.weight(1f)
            )
        }

        // Generate Action Button
        val isLoading = prepState is AiExamPrepState.Loading

        Button(
            onClick = { onGeneratePrep(exam, selectedMode) },
            enabled = !isLoading,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ArkinPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("generate_exam_prep_btn")
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Analyzing with Arkin AI...", fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generate AI $selectedMode 🪄", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // State Container
        when (prepState) {
            is AiExamPrepState.Idle -> {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Psychology,
                            contentDescription = null,
                            tint = ArkinPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Personalized AI Exam Revision",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Arkin AI checks your saved notes for '${exam.subject}' to generate custom revision guides, practice questions, and mock tests.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            is AiExamPrepState.Loading -> {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = ArkinPrimary, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Generating personalized exam preparation...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Reviewing study notes, syllabus weightage & university patterns.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            is AiExamPrepState.Error -> {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFFEF2F2),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = Color(0xFFDC2626))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Unable to Generate Exam Prep",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626)
                            )
                        }
                        Text(
                            text = prepState.message,
                            fontSize = 12.sp,
                            color = Color(0xFF7F1D1D),
                            modifier = Modifier.padding(top = 6.dp, bottom = 12.dp)
                        )
                        Button(
                            onClick = { onGeneratePrep(exam, selectedMode) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry AI Request")
                        }
                    }
                }
            }

            is AiExamPrepState.Success -> {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 80.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFDCFCE7)
                            ) {
                                Text(
                                    text = if (prepState.notesUsedCount > 0)
                                        "🌿 Grounded in ${prepState.notesUsedCount} Saved Notes"
                                    else "Standard University Syllabus Guide",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF166534),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Button(
                                onClick = {
                                    viewModel.saveNote(
                                        title = "AI Prep: ${exam.subject} (${prepState.mode})",
                                        content = prepState.content,
                                        tag = "Exam",
                                        colorKey = "purple",
                                        isSecure = false
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ArkinPrimary),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Save to Notes 💾", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        MarkdownText(
                            markdown = prepState.content,
                            textColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrepModeCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) ArkinPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isSelected) ArkinPrimary else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = modifier.height(68.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (isSelected) ArkinPrimary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) ArkinPrimary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ExamEditorDialog(
    title: String,
    initialExam: TimetableEntity?,
    onDismiss: () -> Unit,
    onSave: (subject: String, courseCode: String, examDate: String, timeSlot: String, room: String) -> Unit
) {
    val context = LocalContext.current
    var subject by remember { mutableStateOf(initialExam?.subject.orEmpty()) }
    var courseCode by remember { mutableStateOf(initialExam?.courseCode.orEmpty()) }
    var examDate by remember { mutableStateOf(initialExam?.examDate.orEmpty().ifBlank {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Calendar.getInstance().time)
    }) }
    var timeSlot by remember { mutableStateOf(initialExam?.timeSlot.orEmpty().ifBlank { "10:30 AM - 12:30 PM" }) }
    var room by remember { mutableStateOf(initialExam?.room.orEmpty()) }

    val cal = remember { Calendar.getInstance() }

    fun openDatePicker() {
        val dp = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val chosen = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                examDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(chosen.time)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        dp.setTitle("Select Exam Date 📅")
        dp.show()
    }

    fun openTimePicker() {
        val tpStart = TimePickerDialog(
            context,
            { _, startH, startM ->
                val startCal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, startH)
                    set(Calendar.MINUTE, startM)
                }
                val startStr = SimpleDateFormat("hh:mm a", Locale.US).format(startCal.time)

                val tpEnd = TimePickerDialog(
                    context,
                    { _, endH, endM ->
                        val endCal = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, endH)
                            set(Calendar.MINUTE, endM)
                        }
                        val endStr = SimpleDateFormat("hh:mm a", Locale.US).format(endCal.time)
                        timeSlot = "$startStr - $endStr"
                    },
                    (startH + 2) % 24,
                    startM,
                    false
                )
                tpEnd.setTitle("Select Exam End Time")
                tpEnd.show()
            },
            10,
            30,
            false
        )
        tpStart.setTitle("Select Exam Start Time")
        tpStart.show()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
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
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Exam Subject *") },
                    placeholder = { Text("e.g. Operating Systems") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ArkinPrimary),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                )

                OutlinedTextField(
                    value = courseCode,
                    onValueChange = { courseCode = it },
                    label = { Text("Course Code (Optional)") },
                    placeholder = { Text("e.g. BCAMJ501") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ArkinPrimary),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                )

                // Exam Date with DatePicker
                OutlinedTextField(
                    value = examDate,
                    onValueChange = { examDate = it },
                    label = { Text("Exam Date (DD/MM/YYYY) *") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ArkinPrimary),
                    trailingIcon = {
                        IconButton(onClick = { openDatePicker() }) {
                            Icon(
                                imageVector = Icons.Rounded.CalendarToday,
                                contentDescription = "Pick Date",
                                tint = ArkinPrimary
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                )

                // Time Slot with TimePicker
                OutlinedTextField(
                    value = timeSlot,
                    onValueChange = { timeSlot = it },
                    label = { Text("Exam Timing *") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ArkinPrimary),
                    trailingIcon = {
                        IconButton(onClick = { openTimePicker() }) {
                            Icon(
                                imageVector = Icons.Rounded.Schedule,
                                contentDescription = "Pick Time",
                                tint = Color(0xFF6366F1)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                )

                OutlinedTextField(
                    value = room,
                    onValueChange = { room = it },
                    label = { Text("Room / Hall (Optional)") },
                    placeholder = { Text("e.g. Exam Hall 3, Block B") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ArkinPrimary),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            if (subject.isNotBlank() && examDate.isNotBlank()) {
                                onSave(subject, courseCode, examDate, timeSlot, room)
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ArkinPrimary)
                    ) {
                        Text("Save Exam Schedule", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
