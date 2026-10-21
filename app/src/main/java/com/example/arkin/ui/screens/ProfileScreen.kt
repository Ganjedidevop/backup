package com.example.arkin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.lazy.items
import com.example.arkin.util.StudentProfileValidator
import com.example.arkin.model.AppScreen
import com.example.arkin.ui.components.SecurePinChangeDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.arkin.ui.components.AppHeader
import com.example.arkin.ui.components.ConfirmationDialog
import com.example.arkin.viewmodel.ArkinViewModel
import com.example.ui.theme.AccentBlueBg
import com.example.ui.theme.AccentBlueText
import com.example.ui.theme.AccentLockerBg
import com.example.ui.theme.AccentLockerText
import com.example.ui.theme.AccentOrangeBg
import com.example.ui.theme.AccentOrangeText
import com.example.ui.theme.AccentPurpleBg
import com.example.ui.theme.AccentPurpleText
import com.example.ui.theme.ArkinPrimary
import com.example.ui.theme.ArkinPrimaryLight

@Composable
fun ProfileScreen(viewModel: ArkinViewModel) {
    val profile by viewModel.studentProfile.collectAsState()
    var isEditProfileDialogOpen by remember { mutableStateOf(false) }
    var isChangePinDialogOpen by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    val name = profile?.name?.ifBlank { "Student" } ?: "Student"
    val major = profile?.major?.ifBlank { "Academic Scholar" } ?: "Academic Scholar"
    val isDark = profile?.isDarkMode ?: false

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .testTag("profile_screen_root")
    ) {
        AppHeader(
            title = "Profile & Settings",
            onBackClick = { viewModel.goBack() }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar
            Surface(
                shape = CircleShape,
                color = ArkinPrimary,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .size(92.dp)
                    .testTag("profile_avatar_circle")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString(""),
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = major,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )

            val college = profile?.collegeName?.takeIf { it.isNotBlank() }
            val sem = profile?.semester?.takeIf { it.isNotBlank() }
            val displayText = when {
                college != null && sem != null -> "$college • $sem"
                college != null -> college
                sem != null -> sem
                else -> "Tap 'Student Profile' to add university & semester"
            }

            val badgeBg = if (isDark) MaterialTheme.colorScheme.surfaceVariant else AccentBlueBg
            val badgeText = if (isDark) ArkinPrimaryLight else AccentBlueText

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = badgeBg,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = "Academic",
                        tint = badgeText,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = displayText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = badgeText
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Settings Menu Items Group
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    // Account & Academic Details
                    SettingsRow(
                        title = "Student & Academic Profile",
                        icon = Icons.Default.Person,
                        bgColor = AccentBlueBg,
                        iconColor = AccentBlueText,
                        onClick = { isEditProfileDialogOpen = true }
                    )


                    // AI Document & Timetable Organizer
                    SettingsRow(
                        title = "AI Document & Timetable Parser",
                        icon = Icons.Default.AutoAwesome,
                        bgColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF3E8FF),
                        iconColor = if (isDark) Color(0xFFC084FC) else Color(0xFF9333EA),
                        onClick = { viewModel.navigateTo(AppScreen.AI_DOC_ORGANIZER) }
                    )

                    // Change Locker PIN
                    SettingsRow(
                        title = "Change Locker PIN",
                        icon = Icons.Default.Lock,
                        bgColor = AccentLockerBg,
                        iconColor = AccentLockerText,
                        onClick = { isChangePinDialogOpen = true }
                    )

                    // Dark Mode Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = AccentPurpleBg,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.DarkMode,
                                        contentDescription = "Dark Mode",
                                        tint = AccentPurpleText,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = "Dark Mode",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Switch(
                            checked = isDark,
                            onCheckedChange = { viewModel.toggleDarkMode() },
                            colors = SwitchDefaults.colors(checkedThumbColor = ArkinPrimary)
                        )
                    }



                    // Reset Account Data (Start Fresh)
                    SettingsRow(
                        title = "Reset Account Data (Start Fresh)",
                        icon = Icons.Default.DeleteSweep,
                        bgColor = if (isDark) Color(0xFF7C2D12).copy(alpha = 0.35f) else Color(0xFFFFEDD5),
                        iconColor = if (isDark) Color(0xFFFB923C) else Color(0xFFEA580C),
                        titleColor = if (isDark) Color(0xFFFB923C) else Color(0xFFEA580C),
                        onClick = { showResetConfirmDialog = true }
                    )

                    // Logout
                    SettingsRow(
                        title = "Log Out",
                        icon = Icons.AutoMirrored.Filled.ExitToApp,
                        bgColor = if (isDark) Color(0xFF7F1D1D).copy(alpha = 0.35f) else Color(0xFFFEE2E2),
                        iconColor = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626),
                        titleColor = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626),
                        onClick = { showLogoutConfirmDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Arkin v1.0 • Powered by Firebase (arkin-22b82) & Groq AI",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    if (isEditProfileDialogOpen) {
        EditProfileDialog(
            currentName = name,
            currentMajor = major,
            currentEmail = profile?.email ?: "",
            currentCollege = profile?.collegeName ?: "",
            currentCourse = profile?.course ?: "",
            currentSemester = profile?.semester ?: "",
            currentAcademicYear = profile?.academicYear ?: "",
            onDismiss = { isEditProfileDialogOpen = false },
            onSave = { newName, newMajor, newEmail, newCollege, newCourse, newSem, newYear ->
                viewModel.updateProfile(newName, newMajor, newEmail, newCollege, newCourse, newSem, newYear)
                isEditProfileDialogOpen = false
            }
        )
    }

    if (isChangePinDialogOpen) {
        SecurePinChangeDialog(
            onDismiss = { isChangePinDialogOpen = false },
            onPinSuccessfullyChanged = { newPin ->
                viewModel.changeLockerPin(newPin)
                isChangePinDialogOpen = false
            }
        )
    }

    if (showResetConfirmDialog) {
        ConfirmationDialog(
            title = "Reset Account Data?",
            message = "This will remove all merged notes, tasks, homework, and timetable entries from this account so you can start completely fresh. Are you sure?",
            confirmButtonText = "Reset All Data",
            icon = Icons.Default.DeleteSweep,
            onConfirm = {
                showResetConfirmDialog = false
                viewModel.resetAccountData(clearCloud = true)
            },
            onDismiss = { showResetConfirmDialog = false }
        )
    }

    if (showLogoutConfirmDialog) {
        ConfirmationDialog(
            title = "Log Out?",
            message = "Are you sure you want to log out of your account? You will need to sign in again to sync your notes and data.",
            confirmButtonText = "Log Out",
            icon = Icons.AutoMirrored.Filled.ExitToApp,
            onConfirm = {
                showLogoutConfirmDialog = false
                viewModel.logout()
            },
            onDismiss = { showLogoutConfirmDialog = false }
        )
    }
}

@Composable
private fun SettingsRow(
    title: String,
    icon: ImageVector,
    bgColor: Color,
    iconColor: Color,
    titleColor: Color? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = bgColor,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = titleColor ?: MaterialTheme.colorScheme.onSurface
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Open",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun EditProfileDialog(
    currentName: String,
    currentMajor: String,
    currentEmail: String,
    currentCollege: String,
    currentCourse: String,
    currentSemester: String,
    currentAcademicYear: String,
    onDismiss: () -> Unit,
    onSave: (name: String, major: String, email: String, college: String, course: String, semester: String, academicYear: String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var email by remember { mutableStateOf(currentEmail) }
    var college by remember { mutableStateOf(currentCollege) }

    // Program selection
    val isKnownProgram = StudentProfileValidator.COMMON_PROGRAMS.any { it.equals(currentCourse.trim(), ignoreCase = true) }
    var selectedProgramOption by remember {
        mutableStateOf(
            if (isKnownProgram) {
                StudentProfileValidator.COMMON_PROGRAMS.first { it.equals(currentCourse.trim(), ignoreCase = true) }
            } else if (currentCourse.isNotBlank()) {
                "Other"
            } else {
                "B.Tech"
            }
        )
    }
    var customProgram by remember {
        mutableStateOf(if (!isKnownProgram && currentCourse.isNotBlank()) currentCourse.trim() else "")
    }
    var isProgramDropdownOpen by remember { mutableStateOf(false) }

    val effectiveProgram = if (selectedProgramOption == "Other") customProgram else selectedProgramOption

    // Semester selection
    val maxSemesters = StudentProfileValidator.getMaxSemesters(effectiveProgram)
    val parsedCurrentSem = StudentProfileValidator.extractSemesterNumber(currentSemester) ?: 1
    val initialSemNumber = parsedCurrentSem.coerceIn(1, maxSemesters)
    var selectedSemesterNumber by remember(maxSemesters) {
        mutableStateOf(initialSemNumber)
    }
    var isSemesterDropdownOpen by remember { mutableStateOf(false) }

    // Academic Year
    val currentCalYear = remember { java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) }
    val defaultAcademicYear = remember(currentCalYear) { StudentProfileValidator.formatAcademicYear(currentCalYear) }
    var academicYear by remember {
        mutableStateOf(currentAcademicYear.ifBlank { defaultAcademicYear })
    }

    // Department / Specialization
    var major by remember { mutableStateOf(currentMajor) }

    // Has user touched or attempted to submit (to show inline errors)
    var hasAttemptedSubmit by remember { mutableStateOf(false) }

    // Validations
    val collegeValidation = StudentProfileValidator.validateCollege(college)
    val programValidation = StudentProfileValidator.validateProgram(effectiveProgram)
    val semesterValidation = StudentProfileValidator.validateSemester("Semester $selectedSemesterNumber", effectiveProgram)
    val yearValidation = StudentProfileValidator.validateAcademicYear(academicYear)
    val majorValidation = StudentProfileValidator.validateMajor(major)

    val isFormValid = collegeValidation.isValid && programValidation.isValid &&
            semesterValidation.isValid && yearValidation.isValid && majorValidation.isValid

    val dialogScrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(dialogScrollState)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Student Profile 👤",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Full Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ArkinPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                )

                // Student Email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Student Email") },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ArkinPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                )

                // College / University with inline validation
                val showCollegeError = hasAttemptedSubmit && !collegeValidation.isValid
                OutlinedTextField(
                    value = college,
                    onValueChange = { college = it },
                    label = { Text("College / University *") },
                    placeholder = { Text("e.g. St. Xavier's College, IIT Delhi") },
                    isError = showCollegeError,
                    supportingText = if (showCollegeError) {
                        { Text(collegeValidation.errorMessage ?: "Invalid college", color = MaterialTheme.colorScheme.error, fontSize = 11.sp) }
                    } else null,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ArkinPrimary,
                        errorBorderColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = if (showCollegeError) 4.dp else 10.dp)
                )

                // Degree / Program Selector
                Box(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                    OutlinedTextField(
                        value = selectedProgramOption,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Degree / Program *") },
                        trailingIcon = {
                            IconButton(onClick = { isProgramDropdownOpen = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Program")
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ArkinPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isProgramDropdownOpen = true }
                    )
                    DropdownMenu(
                        expanded = isProgramDropdownOpen,
                        onDismissRequest = { isProgramDropdownOpen = false }
                    ) {
                        (StudentProfileValidator.COMMON_PROGRAMS + "Other").forEach { prog ->
                            DropdownMenuItem(
                                text = { Text(prog) },
                                onClick = {
                                    selectedProgramOption = prog
                                    isProgramDropdownOpen = false
                                }
                            )
                        }
                    }
                }

                // Custom Program Input when "Other" is selected
                if (selectedProgramOption == "Other") {
                    val showProgramError = hasAttemptedSubmit && !programValidation.isValid
                    OutlinedTextField(
                        value = customProgram,
                        onValueChange = { customProgram = it },
                        label = { Text("Specify Program Name *") },
                        placeholder = { Text("e.g. MBBS, LLB, B.Des") },
                        isError = showProgramError,
                        supportingText = if (showProgramError) {
                            { Text(programValidation.errorMessage ?: "Invalid program name", color = MaterialTheme.colorScheme.error, fontSize = 11.sp) }
                        } else null,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ArkinPrimary,
                            errorBorderColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = if (showProgramError) 4.dp else 10.dp)
                    )
                }

                // Row: Semester selector & Academic Year input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Semester Selector
                    Box(modifier = Modifier.weight(1f).padding(bottom = 10.dp)) {
                        OutlinedTextField(
                            value = "Semester $selectedSemesterNumber",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Semester *") },
                            trailingIcon = {
                                IconButton(onClick = { isSemesterDropdownOpen = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Semester")
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ArkinPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isSemesterDropdownOpen = true }
                        )
                        DropdownMenu(
                            expanded = isSemesterDropdownOpen,
                            onDismissRequest = { isSemesterDropdownOpen = false }
                        ) {
                            (1..maxSemesters).forEach { semNum ->
                                DropdownMenuItem(
                                    text = { Text("Semester $semNum") },
                                    onClick = {
                                        selectedSemesterNumber = semNum
                                        isSemesterDropdownOpen = false
                                    }
                                )
                            }
                        }
                    }

                    // Academic Year with validation
                    val showYearError = hasAttemptedSubmit && !yearValidation.isValid
                    OutlinedTextField(
                        value = academicYear,
                        onValueChange = { academicYear = it },
                        label = { Text("Academic Year *") },
                        placeholder = { Text("2026-2027") },
                        isError = showYearError,
                        supportingText = if (showYearError) {
                            { Text(yearValidation.errorMessage ?: "e.g. 2026-2027", color = MaterialTheme.colorScheme.error, fontSize = 10.sp) }
                        } else null,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ArkinPrimary,
                            errorBorderColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = if (showYearError) 4.dp else 10.dp)
                    )
                }

                // Department / Specialization
                val showMajorError = hasAttemptedSubmit && !majorValidation.isValid
                OutlinedTextField(
                    value = major,
                    onValueChange = { major = it },
                    label = { Text("Department / Specialization *") },
                    placeholder = { Text("e.g. Computer Science") },
                    isError = showMajorError,
                    supportingText = if (showMajorError) {
                        { Text(majorValidation.errorMessage ?: "Invalid department", color = MaterialTheme.colorScheme.error, fontSize = 11.sp) }
                    } else null,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ArkinPrimary,
                        errorBorderColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                )

                // Department Suggestions Chips
                val departmentSuggestions = remember(effectiveProgram) {
                    StudentProfileValidator.getDepartmentOptions(effectiveProgram).take(4)
                }
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    items(departmentSuggestions) { suggestion ->
                        Surface(
                            onClick = { major = suggestion },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = suggestion,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            hasAttemptedSubmit = true
                            if (isFormValid) {
                                onSave(
                                    name.trim(),
                                    major.trim(),
                                    email.trim(),
                                    college.trim(),
                                    effectiveProgram.trim(),
                                    "Semester $selectedSemesterNumber",
                                    academicYear.trim()
                                )
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ArkinPrimary)
                    ) {
                        Text("Save Changes", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

