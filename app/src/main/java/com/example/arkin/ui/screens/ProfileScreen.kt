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

import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.HorizontalDivider

@Composable
fun ProfileScreen(viewModel: ArkinViewModel) {
    val profile by viewModel.studentProfile.collectAsState()
    var isEditProfileDialogOpen by remember { mutableStateOf(false) }

    val name = profile?.name?.ifBlank { "Student" } ?: "Student"
    val major = profile?.major?.ifBlank { "Academic Scholar" } ?: "Academic Scholar"
    val email = profile?.email?.ifBlank { "student@university.edu" } ?: "student@university.edu"
    val college = profile?.collegeName?.ifBlank { "University / College" } ?: "University / College"
    val course = profile?.course?.ifBlank { "Degree Program" } ?: "Degree Program"
    val semester = profile?.semester?.ifBlank { "Semester 1" } ?: "Semester 1"
    val academicYear = profile?.academicYear?.ifBlank { "Current Academic Year" } ?: "Current Academic Year"
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
            title = "Student Profile",
            onBackClick = { viewModel.goBack() },
            trailingAction = {
                IconButton(onClick = { isEditProfileDialogOpen = true }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Profile",
                        tint = ArkinPrimary
                    )
                }
            }
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
                    .size(96.dp)
                    .testTag("profile_avatar_circle")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString(""),
                        color = Color.White,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "$course • $major",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )

            val badgeBg = if (isDark) MaterialTheme.colorScheme.surfaceVariant else AccentBlueBg
            val badgeText = if (isDark) ArkinPrimaryLight else AccentBlueText

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = badgeBg,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = "Academic",
                        tint = badgeText,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$college • $semester",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = badgeText
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Student Information Details Card (View Mode)
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    ProfileDetailItem(
                        label = "Full Name",
                        value = name,
                        icon = Icons.Default.Person,
                        iconBg = AccentBlueBg,
                        iconTint = AccentBlueText
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))

                    ProfileDetailItem(
                        label = "Student Email",
                        value = email,
                        icon = Icons.Default.Email,
                        iconBg = AccentPurpleBg,
                        iconTint = AccentPurpleText
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))

                    ProfileDetailItem(
                        label = "College / University",
                        value = college,
                        icon = Icons.Default.AccountBalance,
                        iconBg = AccentOrangeBg,
                        iconTint = AccentOrangeText
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))

                    ProfileDetailItem(
                        label = "Program / Course",
                        value = course,
                        icon = Icons.Default.School,
                        iconBg = Color(0xFFE0F2FE),
                        iconTint = Color(0xFF0284C7)
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))

                    ProfileDetailItem(
                        label = "Current Semester",
                        value = semester,
                        icon = Icons.Default.Event,
                        iconBg = Color(0xFFDCFCE7),
                        iconTint = Color(0xFF16A34A)
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))

                    ProfileDetailItem(
                        label = "Academic Year",
                        value = academicYear,
                        icon = Icons.Default.DateRange,
                        iconBg = Color(0xFFFEF3C7),
                        iconTint = Color(0xFFD97706)
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))

                    ProfileDetailItem(
                        label = "Department / Specialization",
                        value = major,
                        icon = Icons.Default.AutoStories,
                        iconBg = Color(0xFFF3E8FF),
                        iconTint = Color(0xFF9333EA)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Prominent Edit Profile Button
            Button(
                onClick = { isEditProfileDialogOpen = true },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ArkinPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Edit Profile",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Arkin Student Profile • Synced with Cloud",
                fontSize = 12.sp,
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
}

@Composable
private fun ProfileDetailItem(
    label: String,
    value: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = iconBg,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value.ifBlank { "Not set" },
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
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

