package com.example.arkin.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.arkin.data.ArkinDatabase
import com.example.arkin.data.ai.GroqHelper
import com.example.arkin.data.backup.BackupManager
import com.example.arkin.data.backup.BackupPreview
import com.example.arkin.data.firebase.FirebaseAuthHelper
import com.example.arkin.data.firebase.FirestoreManager
import com.example.arkin.data.model.ExamResultEntity
import com.example.arkin.data.model.HomeworkEntity
import com.example.arkin.data.model.NoteEntity
import com.example.arkin.data.model.ReminderEntity
import com.example.arkin.data.model.StudentProfileEntity
import com.example.arkin.data.model.TaskEntity
import com.example.arkin.data.model.TimetableEntity
import com.example.arkin.data.model.VaultItemEntity
import com.example.arkin.data.security.PinVerificationResult
import com.example.arkin.data.security.VaultSecurityManager
import android.net.Uri
import com.example.arkin.model.AppScreen
import com.example.arkin.model.BottomNavTab
import com.example.arkin.model.ChatMessage
import com.example.arkin.model.DocProcessingState
import com.example.arkin.model.DocUploadType
import com.example.arkin.model.ParsedTimetableClass
import com.example.arkin.util.DocumentTextExtractor
import com.example.arkin.util.ExtractionResult
import com.example.arkin.util.StudentProfileValidator
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class ArkinViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ArkinDatabase.getDatabase(application)
    private val noteDao = db.noteDao()
    private val taskDao = db.taskDao()
    private val homeworkDao = db.homeworkDao()
    private val timetableDao = db.timetableDao()
    private val reminderDao = db.reminderDao()
    private val examResultDao = db.examResultDao()
    private val vaultItemDao = db.vaultItemDao()
    private val profileDao = db.studentProfileDao()

    // Screen Navigation
    private val _currentScreen = MutableStateFlow(AppScreen.SPLASH)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _navigationStack = mutableListOf<AppScreen>()

    private val _currentBottomNav = MutableStateFlow(BottomNavTab.HOME)
    val currentBottomNav: StateFlow<BottomNavTab> = _currentBottomNav.asStateFlow()

    // Search and Filters for Notes
    val noteSearchQuery = MutableStateFlow("")
    val selectedNoteTag = MutableStateFlow("All")
    val selectedNoteCategory = MutableStateFlow("All")
    val selectedNoteSubject = MutableStateFlow("All")

    // Notes Flows
    private val allPublicNotesFlow = noteDao.getAllPublicNotes()
    val filteredPublicNotes: StateFlow<List<NoteEntity>> = combine(
        allPublicNotesFlow,
        noteSearchQuery,
        selectedNoteTag,
        selectedNoteCategory,
        selectedNoteSubject
    ) { notes, query, tag, category, subject ->
        notes.filter { note ->
            val matchesQuery = query.isBlank() ||
                    note.title.contains(query, ignoreCase = true) ||
                    note.content.contains(query, ignoreCase = true) ||
                    note.subject.contains(query, ignoreCase = true)
            val matchesTag = tag == "All" || note.tag.equals(tag, ignoreCase = true)
            val matchesCat = category == "All" || note.category.equals(category, ignoreCase = true)
            val matchesSub = subject == "All" || note.subject.equals(subject, ignoreCase = true)
            matchesQuery && matchesTag && matchesCat && matchesSub
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val secureNotes: StateFlow<List<NoteEntity>> = noteDao.getNotes(secure = true)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Tasks Flow & Planner Filters
    val taskFilter = MutableStateFlow("All") // All, Today, Upcoming, Completed
    val allTasks: StateFlow<List<TaskEntity>> = taskDao.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredTasks: StateFlow<List<TaskEntity>> = combine(
        allTasks,
        taskFilter
    ) { tasks, filter ->
        tasks.filter {
            when (filter) {
                "Today" -> !it.isCompleted && (it.dueDate.equals("Today", ignoreCase = true))
                "Upcoming" -> !it.isCompleted
                "Completed" -> it.isCompleted
                else -> true
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isPlannerCalendarView = MutableStateFlow(false)

    // Homework Flow & Selected Assignment
    val allHomework: StateFlow<List<HomeworkEntity>> = homeworkDao.getAllHomework()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedAssignment = MutableStateFlow<HomeworkEntity?>(null)

    // Timetable
    val selectedDay = MutableStateFlow("Mon")
    val classesForDay: StateFlow<List<TimetableEntity>> = combine(
        timetableDao.getAllClasses(),
        selectedDay
    ) { classes, day ->
        classes.filter { it.dayOfWeek.equals(day, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Reminders
    val allReminders: StateFlow<List<ReminderEntity>> = reminderDao.getAllReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Exam Results for Study Analyser
    val allExamResults: StateFlow<List<ExamResultEntity>> = examResultDao.getAllExamResults()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Vault Items
    val allVaultItems: StateFlow<List<VaultItemEntity>> = vaultItemDao.getAllVaultItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Profile
    val studentProfile: StateFlow<StudentProfileEntity?> = profileDao.getProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // AI Assistant Chat
    private val _chatMessages = MutableStateFlow(
        listOf(
            ChatMessage(
                sender = "ai",
                text = "Hello! 👋 I'm Arkin, your AI study assistant. I can explain complex academic concepts, solve assignment questions step-by-step, organize your timetable, or generate revision flashcards."
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    // Private Locker PIN & Security
    private val _isVaultUnlocked = MutableStateFlow(false)
    val isVaultUnlocked: StateFlow<Boolean> = _isVaultUnlocked.asStateFlow()

    private val _enteredPin = MutableStateFlow("")
    val enteredPin: StateFlow<String> = _enteredPin.asStateFlow()

    private val _isPinError = MutableStateFlow(false)
    val isPinError: StateFlow<Boolean> = _isPinError.asStateFlow()

    // Note Modal
    val isNoteDialogOpen = MutableStateFlow(false)
    val editingNote = MutableStateFlow<NoteEntity?>(null)
    val isSecureNoteDialog = MutableStateFlow(false)

    // Feedback Toast/Snackbar
    val toastMessage = MutableStateFlow<String?>(null)

    // AI Document Upload & Organizer
    val uploadedDocName = MutableStateFlow<String?>(null)
    val uploadedDocType = MutableStateFlow(DocUploadType.TIMETABLE)
    val selectedDocUri = MutableStateFlow<Uri?>(null)
    val docFileSize = MutableStateFlow<Long>(0L)
    val docProcessingState = MutableStateFlow(DocProcessingState.IDLE)
    val docProcessingStatusText = MutableStateFlow("Ready to analyze")
    val docProcessingError = MutableStateFlow<String?>(null)
    val docExtractedRawText = MutableStateFlow<String?>(null)
    val isAnalyzingDoc = MutableStateFlow(false)
    val analyzedDocResult = MutableStateFlow<String?>(null)
    val parsedClasses = MutableStateFlow<List<ParsedTimetableClass>>(emptyList())
    val parsedAssignments = MutableStateFlow<List<Triple<String, String, String>>>(emptyList())
    val parsedExamQuestions = MutableStateFlow<List<String>>(emptyList())

    // Backup & Restore
    val backupPreview = MutableStateFlow<BackupPreview?>(null)
    val isRestoringBackup = MutableStateFlow(false)

    // Firebase Authentication & Cloud State
    val firebaseUser: StateFlow<FirebaseUser?> = FirebaseAuthHelper.authStateFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FirebaseAuthHelper.currentUser)
    val isAuthLoading = MutableStateFlow(false)
    val authError = MutableStateFlow<String?>(null)
    val needsProfileSetup = MutableStateFlow(false)
    val isFirestoreSyncing = MutableStateFlow(false)
    val lastFirestoreSyncTime = MutableStateFlow<Long?>(null)

    init {
        viewModelScope.launch {
            val prof = profileDao.getProfileSync()
            if (prof == null) {
                profileDao.insertProfile(StudentProfileEntity())
            }
            // If user is already authenticated with Firebase, pull latest cloud data
            if (FirebaseAuthHelper.isUserLoggedIn) {
                val user = FirebaseAuthHelper.currentUser
                if (user != null) {
                    val userEmail = user.email
                    if (prof != null && prof.email.isNotBlank() && userEmail != null &&
                        !prof.email.equals(userEmail, ignoreCase = true)
                    ) {
                        clearLocalUserData()
                    }
                    val remoteProfile = FirestoreManager.fetchProfile(user.uid)
                    if (remoteProfile != null) {
                        profileDao.updateProfile(remoteProfile)
                    }
                    FirestoreManager.syncFirestoreToLocal(db, merge = true)
                    lastFirestoreSyncTime.value = System.currentTimeMillis()
                }
            }
        }
    }

    // Navigation Methods
    fun navigateTo(screen: AppScreen, addToStack: Boolean = true) {
        if (addToStack && _currentScreen.value != screen) {
            _navigationStack.add(_currentScreen.value)
        }
        _currentScreen.value = screen

        when (screen) {
            AppScreen.DASHBOARD -> _currentBottomNav.value = BottomNavTab.HOME
            AppScreen.NOTES -> _currentBottomNav.value = BottomNavTab.NOTES
            AppScreen.TODO -> _currentBottomNav.value = BottomNavTab.PLANNER
            AppScreen.AI_ASSISTANT -> _currentBottomNav.value = BottomNavTab.AI_ASSISTANT
            AppScreen.PROFILE -> _currentBottomNav.value = BottomNavTab.PROFILE
            else -> {}
        }

        if (screen != AppScreen.LOCKER && _isVaultUnlocked.value) {
            lockVault()
        }
    }

    fun goBack(): Boolean {
        if (_navigationStack.isNotEmpty()) {
            val prev = _navigationStack.removeAt(_navigationStack.size - 1)
            navigateTo(prev, addToStack = false)
            return true
        } else if (_currentScreen.value != AppScreen.DASHBOARD && _currentScreen.value != AppScreen.SPLASH && _currentScreen.value != AppScreen.AUTH) {
            navigateTo(AppScreen.DASHBOARD, addToStack = false)
            return true
        }
        return false
    }

    fun selectBottomNav(tab: BottomNavTab) {
        _currentBottomNav.value = tab
        when (tab) {
            BottomNavTab.HOME -> navigateTo(AppScreen.DASHBOARD)
            BottomNavTab.NOTES -> navigateTo(AppScreen.NOTES)
            BottomNavTab.PLANNER -> navigateTo(AppScreen.TODO)
            BottomNavTab.AI_ASSISTANT -> navigateTo(AppScreen.AI_ASSISTANT)
            BottomNavTab.PROFILE -> navigateTo(AppScreen.PROFILE)
        }
    }

    fun showToast(msg: String) {
        toastMessage.value = msg
    }

    fun clearToast() {
        toastMessage.value = null
    }

    // Notes Actions
    fun openAddNoteDialog(isSecure: Boolean = false) {
        editingNote.value = null
        isSecureNoteDialog.value = isSecure
        isNoteDialogOpen.value = true
    }

    fun openEditNoteDialog(note: NoteEntity) {
        editingNote.value = note
        isSecureNoteDialog.value = note.isSecure
        isNoteDialogOpen.value = true
    }

    fun closeNoteDialog() {
        isNoteDialogOpen.value = false
        editingNote.value = null
    }

    fun saveNote(
        title: String,
        content: String,
        tag: String,
        colorKey: String,
        isSecure: Boolean,
        subject: String = "General",
        category: String = "Class Notes",
        attachmentUri: String? = null,
        attachmentName: String? = null
    ) {
        viewModelScope.launch {
            val resolvedUri = if (!attachmentUri.isNullOrBlank() && attachmentUri.startsWith("content://")) {
                val (persisted, _) = com.example.arkin.util.FileViewerHelper.persistAttachment(
                    getApplication(),
                    android.net.Uri.parse(attachmentUri)
                )
                persisted
            } else {
                attachmentUri
            }

            val existing = editingNote.value
            if (existing != null) {
                val updated = existing.copy(
                    title = title,
                    content = content,
                    tag = tag,
                    colorKey = colorKey,
                    subject = subject,
                    category = category,
                    attachmentUri = resolvedUri ?: existing.attachmentUri,
                    attachmentName = attachmentName ?: existing.attachmentName,
                    timestamp = System.currentTimeMillis()
                )
                noteDao.updateNote(updated)
                if (studentProfile.value?.cloudSyncEnabled == true && FirebaseAuthHelper.isUserLoggedIn) {
                    FirestoreManager.saveNote(updated)
                }
                showToast("Note updated successfully")
            } else {
                val newNote = NoteEntity(
                    title = title,
                    content = content,
                    tag = tag,
                    colorKey = colorKey,
                    subject = subject,
                    category = category,
                    attachmentUri = resolvedUri,
                    attachmentName = attachmentName,
                    timestamp = System.currentTimeMillis(),
                    isSecure = isSecure
                )
                val id = noteDao.insertNote(newNote)
                if (studentProfile.value?.cloudSyncEnabled == true && FirebaseAuthHelper.isUserLoggedIn) {
                    FirestoreManager.saveNote(newNote.copy(id = id))
                }
                showToast(if (isSecure) "Saved to Secure Locker" else "Note created")
            }
            closeNoteDialog()
        }
    }

    fun updateNoteAttachmentUri(note: NoteEntity, newPath: String) {
        viewModelScope.launch {
            val updated = note.copy(attachmentUri = newPath)
            noteDao.updateNote(updated)
            if (studentProfile.value?.cloudSyncEnabled == true && FirebaseAuthHelper.isUserLoggedIn) {
                FirestoreManager.saveNote(updated)
            }
        }
    }

    fun toggleFavoriteNote(note: NoteEntity) {
        viewModelScope.launch {
            val updated = note.copy(isFavorite = !note.isFavorite)
            noteDao.updateNote(updated)
            if (studentProfile.value?.cloudSyncEnabled == true && FirebaseAuthHelper.isUserLoggedIn) {
                FirestoreManager.saveNote(updated)
            }
        }
    }

    fun toggleArchiveNote(note: NoteEntity) {
        viewModelScope.launch {
            val updated = note.copy(isArchived = !note.isArchived)
            noteDao.updateNote(updated)
            if (studentProfile.value?.cloudSyncEnabled == true && FirebaseAuthHelper.isUserLoggedIn) {
                FirestoreManager.saveNote(updated)
            }
            showToast(if (!note.isArchived) "Note archived" else "Note unarchived")
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            noteDao.deleteNote(note)
            if (studentProfile.value?.cloudSyncEnabled == true && FirebaseAuthHelper.isUserLoggedIn) {
                FirestoreManager.deleteNote(note.id)
            }
            showToast("Note deleted")
        }
    }

    // Tasks Actions
    fun addTask(title: String, priority: String = "Medium", description: String = "", subject: String = "", dueDate: String = "Today") {
        if (title.isBlank()) return
        viewModelScope.launch {
            val task = TaskEntity(
                title = title.trim(),
                description = description.trim(),
                subject = subject.trim(),
                priority = priority,
                dueDate = dueDate
            )
            val id = taskDao.insertTask(task)
            if (studentProfile.value?.cloudSyncEnabled == true && FirebaseAuthHelper.isUserLoggedIn) {
                FirestoreManager.saveTask(task.copy(id = id))
            }
            showToast("Task added")
        }
    }

    fun toggleTask(task: TaskEntity) {
        viewModelScope.launch {
            val updated = task.copy(isCompleted = !task.isCompleted)
            taskDao.updateTask(updated)
            if (studentProfile.value?.cloudSyncEnabled == true && FirebaseAuthHelper.isUserLoggedIn) {
                FirestoreManager.saveTask(updated)
            }
            if (updated.isCompleted) {
                showToast("Task completed! 🎉")
            }
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            taskDao.deleteTask(task)
            if (studentProfile.value?.cloudSyncEnabled == true && FirebaseAuthHelper.isUserLoggedIn) {
                FirestoreManager.deleteTask(task.id)
            }
            showToast("Task removed")
        }
    }

    // Homework Actions
    fun addHomework(
        subject: String,
        title: String,
        dueDate: String,
        dueTime: String = "11:59 PM",
        description: String = "",
        teacherName: String = "",
        priority: String = "Medium",
        colorKey: String = "purple",
        extractedQuestions: String? = null,
        attachmentUri: String? = null,
        attachmentName: String? = null,
        attachmentSize: String? = null
    ) {
        if (title.isBlank() || subject.isBlank()) return
        viewModelScope.launch {
            val resolvedUri = if (!attachmentUri.isNullOrBlank() && attachmentUri.startsWith("content://")) {
                val (persisted, _) = com.example.arkin.util.FileViewerHelper.persistAttachment(
                    getApplication(),
                    android.net.Uri.parse(attachmentUri)
                )
                persisted
            } else {
                attachmentUri
            }

            val hw = HomeworkEntity(
                subject = subject.trim(),
                title = title.trim(),
                description = description.trim(),
                teacherName = teacherName.trim(),
                dueDate = dueDate.trim(),
                dueTime = dueTime.trim(),
                priority = priority,
                status = "Not Started",
                colorKey = colorKey,
                extractedQuestionsJson = extractedQuestions,
                attachmentUri = resolvedUri,
                attachmentName = attachmentName,
                attachmentSize = attachmentSize
            )
            val id = homeworkDao.insertHomework(hw)
            val plannerTask = TaskEntity(
                title = "Assignment: $title ($subject)",
                description = description,
                subject = subject,
                priority = priority,
                dueDate = dueDate,
                dueTime = dueTime,
                linkedAssignmentId = id
            )
            val taskId = taskDao.insertTask(plannerTask)
            if (studentProfile.value?.cloudSyncEnabled == true && FirebaseAuthHelper.isUserLoggedIn) {
                FirestoreManager.saveHomework(hw.copy(id = id))
                FirestoreManager.saveTask(plannerTask.copy(id = taskId))
            }

            // Schedule local deadline notification
            try {
                com.example.arkin.util.AlarmScheduler.scheduleHomeworkNotification(
                    getApplication(),
                    id,
                    title,
                    subject,
                    dueDate,
                    dueTime
                )
            } catch (_: Exception) {}

            showToast("Homework added and synced to study planner")
        }
    }

    fun attachFileToHomework(hw: HomeworkEntity, uri: Uri) {
        viewModelScope.launch {
            val (savedPath, name) = com.example.arkin.util.FileViewerHelper.persistAttachment(getApplication(), uri)
            val file = java.io.File(savedPath)
            val sizeStr = DocumentTextExtractor.formatFileSize(if (file.exists()) file.length() else 0L)

            // Extract questions from attachment if not already present
            var questionsJson: String? = hw.extractedQuestionsJson
            try {
                val extraction = DocumentTextExtractor.extractText(getApplication(), uri)
                if (extraction is ExtractionResult.Success && extraction.document.text.isNotBlank()) {
                    val qList = extractQuestionsFromText(extraction.document.text)
                    if (qList.isNotEmpty()) {
                        val arr = org.json.JSONArray()
                        qList.forEach { arr.put(it) }
                        questionsJson = arr.toString()
                    }
                }
            } catch (_: Exception) {}

            val updated = hw.copy(
                attachmentUri = savedPath,
                attachmentName = name,
                attachmentSize = sizeStr,
                extractedQuestionsJson = questionsJson
            )
            homeworkDao.updateHomework(updated)
            selectedAssignment.value = updated
            if (studentProfile.value?.cloudSyncEnabled == true && FirebaseAuthHelper.isUserLoggedIn) {
                FirestoreManager.saveHomework(updated)
            }
            showToast("Attached: $name")
        }
    }

    fun removeHomeworkAttachment(hw: HomeworkEntity) {
        viewModelScope.launch {
            val updated = hw.copy(attachmentUri = null, attachmentName = null, attachmentSize = null)
            homeworkDao.updateHomework(updated)
            selectedAssignment.value = updated
            if (studentProfile.value?.cloudSyncEnabled == true && FirebaseAuthHelper.isUserLoggedIn) {
                FirestoreManager.saveHomework(updated)
            }
            showToast("Attachment removed")
        }
    }

    fun openAssignmentDetail(hw: HomeworkEntity) {
        selectedAssignment.value = hw
        navigateTo(AppScreen.ASSIGNMENT_DETAIL)
    }

    fun updateAssignmentStatus(hw: HomeworkEntity, newStatus: String) {
        viewModelScope.launch {
            val updated = hw.copy(status = newStatus, isDone = (newStatus == "Completed" || newStatus == "Submitted"))
            homeworkDao.updateHomework(updated)
            selectedAssignment.value = updated
            if (studentProfile.value?.cloudSyncEnabled == true && FirebaseAuthHelper.isUserLoggedIn) {
                FirestoreManager.saveHomework(updated)
            }
            if (updated.isDone) {
                try {
                    com.example.arkin.util.AlarmScheduler.cancelNotification(getApplication(), hw.id)
                    com.example.arkin.util.AlarmScheduler.cancelNotification(getApplication(), hw.id + 100000)
                } catch (_: Exception) {}
            }
            showToast("Status updated to $newStatus")
        }
    }

    fun solveAssignmentQuestionWithAi(hw: HomeworkEntity, question: String) {
        _isAiLoading.value = true
        viewModelScope.launch {
            var effectiveQuestion = question.trim()
            var docContext = ""

            // If question is a generic title/description placeholder, resolve actual questions
            if (effectiveQuestion.startsWith("Assignment topic:") && hw.description.isBlank()) {
                val questionsList = hw.extractedQuestionsJson?.let {
                    try {
                        val arr = org.json.JSONArray(it)
                        List(arr.length()) { i -> arr.getString(i) }
                    } catch (_: Exception) { emptyList() }
                } ?: emptyList()

                if (questionsList.isNotEmpty()) {
                    effectiveQuestion = questionsList.mapIndexed { idx, q -> "Question ${idx + 1}: $q" }.joinToString("\n\n")
                } else if (!hw.attachmentUri.isNullOrBlank()) {
                    try {
                        val uri = android.net.Uri.parse(hw.attachmentUri)
                        val extraction = DocumentTextExtractor.extractText(getApplication(), uri)
                        if (extraction is ExtractionResult.Success && extraction.document.text.isNotBlank()) {
                            docContext = extraction.document.text
                            effectiveQuestion = "Please provide complete step-by-step solutions for all questions in this assignment document:\n" + extraction.document.text.take(3000)
                        }
                    } catch (_: Exception) {}
                }
            }

            if (effectiveQuestion.isBlank() || (effectiveQuestion.startsWith("Assignment topic:") && hw.description.isBlank() && docContext.isBlank())) {
                _isAiLoading.value = false
                showToast("Please enter a question or attach an assignment document.")
                return@launch
            }

            val result = GroqHelper.solveAssignmentQuestionResult(hw.title, effectiveQuestion, documentContext = docContext)
            result.onSuccess { answer ->
                val updated = hw.copy(aiDraftAnswer = answer)
                homeworkDao.updateHomework(updated)
                selectedAssignment.value = updated
                if (studentProfile.value?.cloudSyncEnabled == true && FirebaseAuthHelper.isUserLoggedIn) {
                    FirestoreManager.saveHomework(updated)
                }
                _isAiLoading.value = false
                showToast("AI Solution generated! ✨")
            }.onFailure { err ->
                _isAiLoading.value = false
                showToast("AI generation failed: ${err.localizedMessage ?: err.message}. Tap to retry.")
            }
        }
    }

    fun saveAssignmentAnswerDraft(hw: HomeworkEntity, draft: String) {
        viewModelScope.launch {
            val updated = hw.copy(aiDraftAnswer = draft)
            homeworkDao.updateHomework(updated)
            selectedAssignment.value = updated
            if (studentProfile.value?.cloudSyncEnabled == true && FirebaseAuthHelper.isUserLoggedIn) {
                FirestoreManager.saveHomework(updated)
            }
            showToast("Draft saved")
        }
    }

    fun toggleHomework(hw: HomeworkEntity) {
        viewModelScope.launch {
            val updated = hw.copy(
                isDone = !hw.isDone,
                status = if (!hw.isDone) "Completed" else "In Progress"
            )
            homeworkDao.updateHomework(updated)
            if (selectedAssignment.value?.id == hw.id) {
                selectedAssignment.value = updated
            }
            if (studentProfile.value?.cloudSyncEnabled == true && FirebaseAuthHelper.isUserLoggedIn) {
                FirestoreManager.saveHomework(updated)
            }
            if (updated.isDone) {
                try {
                    com.example.arkin.util.AlarmScheduler.cancelNotification(getApplication(), hw.id)
                    com.example.arkin.util.AlarmScheduler.cancelNotification(getApplication(), hw.id + 100000)
                } catch (_: Exception) {}
            }
            showToast(if (updated.isDone) "Marked as done!" else "Marked as pending")
        }
    }

    fun deleteHomework(hw: HomeworkEntity) {
        viewModelScope.launch {
            homeworkDao.deleteHomework(hw)
            if (selectedAssignment.value?.id == hw.id) {
                goBack()
            }
            if (studentProfile.value?.cloudSyncEnabled == true && FirebaseAuthHelper.isUserLoggedIn) {
                FirestoreManager.deleteHomework(hw.id)
            }
            try {
                com.example.arkin.util.AlarmScheduler.cancelNotification(getApplication(), hw.id)
                com.example.arkin.util.AlarmScheduler.cancelNotification(getApplication(), hw.id + 100000)
            } catch (_: Exception) {}
            showToast("Homework removed")
        }
    }

    // Timetable Actions
    fun selectDay(day: String) {
        selectedDay.value = day
    }

    fun addClass(
        subject: String,
        timeSlot: String,
        room: String,
        facultyName: String = "",
        requiredMaterials: String = "",
        isLab: Boolean = false,
        colorKey: String = "purple"
    ) {
        if (subject.isBlank() || timeSlot.isBlank()) return
        viewModelScope.launch {
            val entry = TimetableEntity(
                dayOfWeek = selectedDay.value,
                subject = subject.trim(),
                timeSlot = timeSlot.trim(),
                room = room.trim(),
                facultyName = facultyName.trim(),
                requiredMaterials = requiredMaterials.trim(),
                isLab = isLab,
                colorKey = colorKey
            )
            val id = timetableDao.insertClass(entry)
            if (studentProfile.value?.cloudSyncEnabled == true && FirebaseAuthHelper.isUserLoggedIn) {
                FirestoreManager.saveTimetableClass(entry.copy(id = id))
            }
            showToast("Class added to timetable")
        }
    }

    fun deleteClass(entry: TimetableEntity) {
        viewModelScope.launch {
            timetableDao.deleteClass(entry)
            if (studentProfile.value?.cloudSyncEnabled == true && FirebaseAuthHelper.isUserLoggedIn) {
                FirestoreManager.deleteTimetableClass(entry.id)
            }
            try {
                com.example.arkin.util.AlarmScheduler.cancelNotification(getApplication(), entry.id)
                com.example.arkin.util.AlarmScheduler.cancelNotification(getApplication(), entry.id + 200000)
            } catch (_: Exception) {}
            showToast("Class removed")
        }
    }

    fun clearTimetable() {
        viewModelScope.launch {
            timetableDao.clearAllClasses()
            showToast("Timetable cleared")
        }
    }

    // Reminder Actions
    fun addReminder(title: String, dueText: String, iconKey: String = "bell", colorKey: String = "yellow") {
        if (title.isBlank()) return
        viewModelScope.launch {
            val reminder = ReminderEntity(
                title = title.trim(),
                dueText = dueText.trim(),
                iconKey = iconKey,
                colorKey = colorKey
            )
            val id = reminderDao.insertReminder(reminder)
            if (studentProfile.value?.cloudSyncEnabled == true && FirebaseAuthHelper.isUserLoggedIn) {
                FirestoreManager.saveReminder(reminder.copy(id = id))
            }

            // Schedule local alarm notification
            try {
                com.example.arkin.util.AlarmScheduler.scheduleReminderNotification(
                    getApplication(),
                    id,
                    title.trim(),
                    dueText.trim()
                )
            } catch (_: Exception) {}

            showToast("Reminder created")
        }
    }

    fun deleteReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            reminderDao.deleteReminder(reminder)
            if (studentProfile.value?.cloudSyncEnabled == true && FirebaseAuthHelper.isUserLoggedIn) {
                FirestoreManager.deleteReminder(reminder.id)
            }
            try {
                com.example.arkin.util.AlarmScheduler.cancelNotification(getApplication(), reminder.id)
            } catch (_: Exception) {}
            showToast("Reminder dismissed")
        }
    }

    // Exam Results (Study Analyser)
    fun addExamResult(subject: String, examName: String, marksObtained: Float, totalMarks: Float, semester: String = "Semester 4") {
        if (subject.isBlank() || examName.isBlank() || totalMarks <= 0f) return
        viewModelScope.launch {
            val exam = ExamResultEntity(
                subject = subject.trim(),
                examName = examName.trim(),
                marksObtained = marksObtained,
                totalMarks = totalMarks,
                semester = semester.trim()
            )
            val id = examResultDao.insertExamResult(exam)
            if (studentProfile.value?.cloudSyncEnabled == true && FirebaseAuthHelper.isUserLoggedIn) {
                FirestoreManager.saveExamResult(exam.copy(id = id))
            }
            showToast("Exam marks recorded!")
        }
    }

    fun deleteExamResult(result: ExamResultEntity) {
        viewModelScope.launch {
            examResultDao.deleteExamResult(result)
            if (studentProfile.value?.cloudSyncEnabled == true && FirebaseAuthHelper.isUserLoggedIn) {
                FirestoreManager.deleteExamResult(result.id)
            }
            showToast("Exam record deleted")
        }
    }

    // Vault Items Actions
    fun addVaultItem(title: String, category: String, content: String, attachmentName: String? = null) {
        if (title.isBlank()) return
        viewModelScope.launch {
            vaultItemDao.insertVaultItem(
                VaultItemEntity(
                    title = title.trim(),
                    category = category.trim(),
                    content = content.trim(),
                    attachmentName = attachmentName,
                    fileType = "TEXT"
                )
            )
            showToast("Item securely saved in Vault 🔒")
        }
    }

    fun addVaultAttachment(title: String, category: String, uri: Uri, fileType: String, customName: String? = null) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            val encryptResult = VaultSecurityManager.encryptAndStoreAttachment(context, uri, customName)
            encryptResult.onSuccess { enc ->
                vaultItemDao.insertVaultItem(
                    VaultItemEntity(
                        title = title.trim(),
                        category = category.trim(),
                        content = "",
                        attachmentUri = enc.encryptedRelativePath,
                        attachmentName = enc.originalFileName,
                        fileType = fileType,
                        fileSize = enc.fileSize,
                        mimeType = enc.mimeType
                    )
                )
                showToast("File encrypted & secured in Vault 🔒")
            }.onFailure { err ->
                showToast("Encryption failed: ${err.message}")
            }
        }
    }

    fun deleteVaultItem(item: VaultItemEntity) {
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            if (!item.attachmentUri.isNullOrBlank()) {
                VaultSecurityManager.deleteEncryptedFile(context, item.attachmentUri)
            }
            vaultItemDao.deleteVaultItem(item)
            showToast("Vault item deleted")
        }
    }

    // Locker PIN Actions
    fun onPinDigit(digit: String) {
        if (_enteredPin.value.length < 4) {
            val newPin = _enteredPin.value + digit
            _enteredPin.value = newPin
            _isPinError.value = false

            if (newPin.length == 4) {
                val context = getApplication<Application>().applicationContext
                if (!VaultSecurityManager.isPinSet(context)) {
                    VaultSecurityManager.setPin(context, newPin)
                    _isVaultUnlocked.value = true
                    _enteredPin.value = ""
                    showToast("PIN created & Vault unlocked 🔓")
                    return
                }

                when (val result = VaultSecurityManager.verifyCurrentPin(context, newPin)) {
                    is PinVerificationResult.Success -> {
                        _isVaultUnlocked.value = true
                        _enteredPin.value = ""
                        showToast("Vault unlocked 🔓")
                    }
                    is PinVerificationResult.Failure -> {
                        _isPinError.value = true
                        showToast("Incorrect PIN (${result.attemptsRemaining} left)")
                        viewModelScope.launch {
                            kotlinx.coroutines.delay(400)
                            _enteredPin.value = ""
                            _isPinError.value = false
                        }
                    }
                    is PinVerificationResult.LockedOut -> {
                        _isPinError.value = true
                        showToast("Vault locked. Try again in ${result.remainingSeconds}s")
                        viewModelScope.launch {
                            kotlinx.coroutines.delay(400)
                            _enteredPin.value = ""
                            _isPinError.value = false
                        }
                    }
                }
            }
        }
    }

    fun onPinBackspace() {
        if (_enteredPin.value.isNotEmpty()) {
            _enteredPin.value = _enteredPin.value.dropLast(1)
            _isPinError.value = false
        }
    }

    fun onPinClear() {
        _enteredPin.value = ""
        _isPinError.value = false
    }

    fun lockVault() {
        _isVaultUnlocked.value = false
        _enteredPin.value = ""
        _isPinError.value = false
        showToast("Vault locked 🔒")
    }

    fun changeLockerPin(newPin: String) {
        if (newPin.length == 4 && newPin.all { it.isDigit() }) {
            val context = getApplication<Application>().applicationContext
            VaultSecurityManager.setPin(context, newPin)
            showToast("Locker PIN updated successfully 🔒")
        } else {
            showToast("PIN must be 4 digits")
        }
    }

    // AI Assistant Actions
    fun sendAiMessage(promptText: String) {
        val trimmed = promptText.trim()
        if (trimmed.isBlank() || _isAiLoading.value) return

        val userMsg = ChatMessage(sender = "user", text = trimmed)
        _chatMessages.value = _chatMessages.value + userMsg
        _isAiLoading.value = true

        viewModelScope.launch {
            val history = _chatMessages.value.map { it.sender to it.text }
            val notesSummary = filteredPublicNotes.value.take(3).joinToString("\n---\n") {
                "${it.title}: ${it.content}"
            }
            val aiResponse = GroqHelper.askAssistant(trimmed, notesSummary, history)
            _chatMessages.value = _chatMessages.value + ChatMessage(sender = "ai", text = aiResponse)
            _isAiLoading.value = false
        }
    }

    fun summarizeNoteWithAi(note: NoteEntity) {
        navigateTo(AppScreen.AI_ASSISTANT)
        sendAiMessage("Please summarize this student note into structured revision bullet points and key formulas:\n\"${note.title}\"\n\nContent:\n${note.content}")
    }

    fun clearChat() {
        _chatMessages.value = listOf(
            ChatMessage(
                sender = "ai",
                text = "Chat history cleared. How else can I assist with your studies today?"
            )
        )
    }

    // AI Document Upload & Organizer
    fun setUploadDocType(type: DocUploadType) {
        uploadedDocType.value = type
        selectedDocUri.value = null
        uploadedDocName.value = null
        docFileSize.value = 0L
        docProcessingState.value = DocProcessingState.IDLE
        docProcessingStatusText.value = "Ready to analyze"
        docProcessingError.value = null
        docExtractedRawText.value = null
        analyzedDocResult.value = null
        parsedClasses.value = emptyList()
        parsedAssignments.value = emptyList()
        parsedExamQuestions.value = emptyList()
    }

    fun onDocumentSelected(uri: Uri) {
        selectedDocUri.value = uri
        val (name, size) = DocumentTextExtractor.getFileMetadata(getApplication(), uri)
        uploadedDocName.value = name
        docFileSize.value = size
        docProcessingState.value = DocProcessingState.IDLE
        docProcessingStatusText.value = "Document ready for analysis"
        docProcessingError.value = null
        docExtractedRawText.value = null
        analyzedDocResult.value = null
        parsedClasses.value = emptyList()
        parsedAssignments.value = emptyList()
        parsedExamQuestions.value = emptyList()
        showToast("Selected: $name")
    }

    fun removeSelectedDocument() {
        selectedDocUri.value = null
        uploadedDocName.value = null
        docFileSize.value = 0L
        docProcessingState.value = DocProcessingState.IDLE
        docProcessingStatusText.value = "Ready to analyze"
        docProcessingError.value = null
        docExtractedRawText.value = null
        analyzedDocResult.value = null
        parsedClasses.value = emptyList()
        parsedAssignments.value = emptyList()
        parsedExamQuestions.value = emptyList()
    }

    fun cancelDocProcessing() {
        docProcessingState.value = DocProcessingState.IDLE
        docProcessingStatusText.value = "Cancelled"
        isAnalyzingDoc.value = false
    }

    fun updateParsedClass(index: Int, updated: ParsedTimetableClass) {
        val current = parsedClasses.value.toMutableList()
        if (index in 0 until current.size) {
            current[index] = updated
            parsedClasses.value = current
        }
    }

    fun deleteParsedClass(index: Int) {
        val current = parsedClasses.value.toMutableList()
        if (index in 0 until current.size) {
            current.removeAt(index)
            parsedClasses.value = current
            showToast("Class removed from schedule review")
        }
    }

    fun addParsedClass(entry: ParsedTimetableClass) {
        parsedClasses.value = parsedClasses.value + entry
    }

    fun startDocumentAnalysis(customPrompt: String = "") {
        if (isAnalyzingDoc.value) return // Prevent repeated taps

        val uri = selectedDocUri.value
        val docName = uploadedDocName.value ?: "Document.pdf"

        if (uri == null) {
            showToast("Please select a document or scan a photo first.")
            return
        }

        docProcessingState.value = DocProcessingState.READING_DOCUMENT
        docProcessingStatusText.value = "Reading document..."
        docProcessingError.value = null
        isAnalyzingDoc.value = true

        viewModelScope.launch {
            // Step 1: Document reading & On-device OCR
            docProcessingState.value = DocProcessingState.EXTRACTING_TEXT
            val extraction = DocumentTextExtractor.extractText(getApplication(), uri) { step, _ ->
                docProcessingStatusText.value = step
            }

            val extractedDoc = when (extraction) {
                is ExtractionResult.Success -> extraction.document
                is ExtractionResult.Failure -> {
                    docProcessingState.value = DocProcessingState.FAILED
                    docProcessingError.value = extraction.errorMessage
                    docProcessingStatusText.value = "Text extraction failed"
                    isAnalyzingDoc.value = false
                    showToast(extraction.errorMessage)
                    return@launch
                }
            }

            val textContent = extractedDoc.text
            docExtractedRawText.value = textContent

            // Step 2: Send extracted text to AI for structured timetable/document analysis
            docProcessingState.value = DocProcessingState.ANALYZING_AI
            docProcessingStatusText.value = "Arkin AI is analyzing timetable & schedule..."

            val type = uploadedDocType.value
            when (type) {
                DocUploadType.TIMETABLE -> {
                    val prompt = """
                        You are Arkin AI, an expert academic schedule and timetable assistant for college students.
                        Analyze the following extracted text from a timetable or examination document ("${extractedDoc.fileName}").
                        
                        Support BOTH timetable types:
                        A. Regular class timetable: recurring classes assigned to weekdays (Mon, Tue, Wed, Thu, Fri, Sat, Sun).
                        B. Examination timetable: individual events tied to specific calendar dates (e.g. DD/MM/YYYY or DD-MM-YYYY), subject codes (e.g. BCAMJ501), subject names, course types (e.g. Major, Minor, Core), and exam timings (e.g. 10:30 AM - 12:30 PM).
                        
                        IMPORTANT INSTRUCTIONS:
                        - Extract ALL valid class lectures, practical labs, and examination events supported by the text.
                        - For examination entries:
                          * Extract exam date into "date" (e.g. "23/10/2026").
                          * Extract 3-letter weekday of the exam date into "day" (e.g. "Fri" for 23/10/2026, "Sat" for 24/10/2026, "Mon" for 26/10/2026, "Tue" for 27/10/2026).
                          * Extract subject code into "subjectCode" (e.g. "BCAMJ501").
                          * Extract subject name into "subject" (e.g. "Understanding the Operating Systems").
                          * Extract course type into "courseType" (e.g. "Major", "Minor", "Core").
                          * Set "isExam": true.
                          * Room or classroom is optional for examinations (can be empty string "").
                        - For regular classes:
                          * "day": must be standard 3-letter day: "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"
                          * "subject": course name
                          * "timeSlot": time interval string (e.g. "09:00 AM - 10:30 AM")
                          * "isLab": boolean (true if practical or lab)
                          * Set "isExam": false, "date": "".
                        - DO NOT invent subjects or events not present in the document.
                        - DO NOT include lunch breaks or general document headers as classes.
                        
                        You MUST respond with valid JSON in this exact structure:
                        {
                          "summary": "Clear student-focused Markdown summary of the timetable or examination schedule, total entries found, and dates/days scheduled.",
                          "classes": [
                             {
                               "day": "Fri",
                               "date": "23/10/2026",
                               "subjectCode": "BCAMJ501",
                               "subject": "Understanding the Operating Systems",
                               "courseType": "Major",
                               "timeSlot": "10:30 AM - 12:30 PM",
                               "startTime": "10:30 AM",
                               "endTime": "12:30 PM",
                               "room": "",
                               "faculty": "",
                               "isLab": false,
                               "isExam": true,
                               "colorKey": "purple"
                             }
                          ]
                        }
                        
                        Do not wrap with anything other than the JSON object.
                        ${if (customPrompt.isNotBlank()) "Student notes: $customPrompt" else ""}
                        
                        Extracted Document Text:
                        ---
                        $textContent
                        ---
                    """.trimIndent()

                    val aiResult = GroqHelper.analyzeDocument(prompt)
                    aiResult.onSuccess { rawResponse ->
                        val parsed = parseTimetableAiResponse(rawResponse, textContent)
                        analyzedDocResult.value = parsed.first
                        parsedClasses.value = parsed.second
                        docProcessingState.value = DocProcessingState.REVIEWING
                        if (parsed.second.isNotEmpty()) {
                            docProcessingStatusText.value = "Analysis complete (${parsed.second.size} entries found)"
                            showToast("Extracted ${parsed.second.size} schedule entries! Review and confirm below 🗓️")
                        } else {
                            docProcessingStatusText.value = "No timetable entries found"
                            showToast("No timetable entries found. Please verify document.")
                        }
                        isAnalyzingDoc.value = false
                    }.onFailure { err ->
                        // Fallback: Parse dynamic entries directly from extracted document text
                        val localParsed = parseTimetableFromDocumentText(textContent)
                        if (localParsed.second.isNotEmpty()) {
                            analyzedDocResult.value = localParsed.first
                            parsedClasses.value = localParsed.second
                            docProcessingState.value = DocProcessingState.REVIEWING
                            docProcessingStatusText.value = "Analysis complete (${localParsed.second.size} entries found)"
                            showToast("Extracted ${localParsed.second.size} schedule entries! Review and confirm below 🗓️")
                            isAnalyzingDoc.value = false
                        } else {
                            docProcessingState.value = DocProcessingState.FAILED
                            docProcessingError.value = "AI analysis failed: ${err.localizedMessage ?: err.message}. Tap to retry."
                            docProcessingStatusText.value = "Analysis failed"
                            isAnalyzingDoc.value = false
                            showToast("AI analysis failed. Please check connection and retry.")
                        }
                    }
                }

                DocUploadType.ASSIGNMENT -> {
                    val prompt = """
                        You are Arkin AI. Analyze this assignment sheet ("${extractedDoc.fileName}").
                        Extract:
                        1. Course/Subject
                        2. Assignment Title & Due Date
                        3. List of specific questions/deliverables
                        4. Step-by-step model approach
                        
                        Return JSON:
                        {
                          "summary": "Detailed Markdown explanation of the assignment deliverables and steps.",
                          "assignments": [
                             {
                               "subject": "Course Name",
                               "title": "Assignment Title",
                               "dueDate": "Tomorrow"
                             }
                          ],
                          "questions": ["Question 1 text...", "Question 2 text..."]
                        }
                        
                        Document Text:
                        $textContent
                    """.trimIndent()

                    val aiResult = GroqHelper.analyzeDocument(prompt)
                    aiResult.onSuccess { rawResponse ->
                        val parsed = parseAssignmentAiResponse(rawResponse)
                        analyzedDocResult.value = parsed.first
                        parsedAssignments.value = parsed.second
                        parsedExamQuestions.value = parsed.third
                        docProcessingState.value = DocProcessingState.REVIEWING
                        docProcessingStatusText.value = "Assignment organized"
                        isAnalyzingDoc.value = false
                        showToast("Assignment deliverables organized! 📚")
                    }.onFailure { err ->
                        docProcessingState.value = DocProcessingState.FAILED
                        docProcessingError.value = "AI analysis failed: ${err.localizedMessage ?: err.message}"
                        docProcessingStatusText.value = "Analysis failed"
                        isAnalyzingDoc.value = false
                        showToast("Analysis failed. Please retry.")
                    }
                }

                DocUploadType.QUESTION_PAPER, DocUploadType.LECTURE_NOTE -> {
                    val prompt = """
                        You are Arkin AI. Analyze this university examination paper / lecture note ("${extractedDoc.fileName}").
                        Provide a comprehensive, high-yield revision breakdown with:
                        - Key Topics & Exam Weightage
                        - Detailed Solutions & Answers for hardest questions
                        - Core Formulas and Definitions
                        - Flashcard-style summary bullets
                        
                        Document Text:
                        $textContent
                    """.trimIndent()

                    val aiResult = GroqHelper.analyzeDocument(prompt)
                    aiResult.onSuccess { rawResponse ->
                        analyzedDocResult.value = rawResponse
                        docProcessingState.value = DocProcessingState.REVIEWING
                        docProcessingStatusText.value = "Analysis complete"
                        isAnalyzingDoc.value = false
                        showToast("Document analyzed! 📝")
                    }.onFailure { err ->
                        docProcessingState.value = DocProcessingState.FAILED
                        docProcessingError.value = "AI analysis failed: ${err.localizedMessage ?: err.message}"
                        docProcessingStatusText.value = "Analysis failed"
                        isAnalyzingDoc.value = false
                        showToast("Analysis failed. Please retry.")
                    }
                }
            }
        }
    }

    fun deriveDayOfWeek(dateStr: String): String {
        val clean = dateStr.trim().replace(".", "/").replace("-", "/")
        val formats = listOf("dd/MM/yyyy", "d/M/yyyy", "yyyy/MM/dd")
        for (fmt in formats) {
            try {
                val sdf = java.text.SimpleDateFormat(fmt, java.util.Locale.US).apply { isLenient = false }
                val d = sdf.parse(clean)
                if (d != null) {
                    val cal = java.util.Calendar.getInstance().apply { time = d }
                    return when (cal.get(java.util.Calendar.DAY_OF_WEEK)) {
                        java.util.Calendar.MONDAY -> "Mon"
                        java.util.Calendar.TUESDAY -> "Tue"
                        java.util.Calendar.WEDNESDAY -> "Wed"
                        java.util.Calendar.THURSDAY -> "Thu"
                        java.util.Calendar.FRIDAY -> "Fri"
                        java.util.Calendar.SATURDAY -> "Sat"
                        java.util.Calendar.SUNDAY -> "Sun"
                        else -> "Mon"
                    }
                }
            } catch (_: Exception) {}
        }
        return "Mon"
    }

    fun extractQuestionsFromText(rawText: String): List<String> {
        if (rawText.isBlank()) return emptyList()
        val questions = mutableListOf<String>()

        val pattern = Regex("""(?m)^(?:\d+[\.\)]|Q(?:uestion)?\s*\d+[\.:\)])\s*(.+)$""")
        val matches = pattern.findAll(rawText).toList()

        if (matches.isNotEmpty()) {
            for (m in matches) {
                val q = m.groupValues[1].trim()
                if (q.length > 5) {
                    questions.add(q)
                }
            }
        }

        if (questions.isEmpty()) {
            val lines = rawText.lines().map { it.trim() }.filter { it.endsWith("?") && it.length > 10 }
            questions.addAll(lines)
        }

        return questions.distinct()
    }

    fun parseTimetableFromDocumentText(rawText: String): Pair<String, List<ParsedTimetableClass>> {
        val extractedClasses = mutableListOf<ParsedTimetableClass>()
        if (rawText.isBlank()) {
            return Pair("No timetable classes or schedule entries were identified in this document. Please verify that the document contains clear days of the week, subject names, and class timings.", emptyList())
        }

        val dateRegex = Regex("""\b(\d{1,2}[/-]\d{1,2}[/-]\d{2,4})\b""")
        val timeRegex = Regex("""(\d{1,2}:\d{2}\s*(?:AM|PM|am|pm)?\s*[-–—to]+\s*\d{1,2}:\d{2}\s*(?:AM|PM|am|pm)?)""")
        val courseCodeRegex = Regex("""\b([A-Z]{3,}[A-Z0-9_-]{2,})\b""")

        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }

        for (line in lines) {
            val dateMatch = dateRegex.find(line)
            val timeMatch = timeRegex.find(line)
            val codeMatch = courseCodeRegex.find(line)

            if (dateMatch != null && (timeMatch != null || codeMatch != null)) {
                val dateStr = dateMatch.groupValues[1]
                val timeStr = timeMatch?.groupValues?.get(1)?.replace("–", "-")?.replace("—", "-") ?: "10:30 AM - 12:30 PM"
                val codeStr = codeMatch?.groupValues?.get(1) ?: ""

                var cleanSubject = line
                    .replace(dateStr, "")
                    .replace(timeStr, "")
                if (codeStr.isNotBlank()) {
                    cleanSubject = cleanSubject.replace(codeStr, "")
                }
                cleanSubject = cleanSubject
                    .replace(Regex("""^[,\s\-–—\.]+|[,\s\-–—\.]+$"""), "")
                    .replace(Regex("""^\d+[\.\)]\s*"""), "")
                    .trim()

                if (cleanSubject.isBlank() && codeStr.isNotBlank()) {
                    cleanSubject = codeStr
                }

                if (cleanSubject.isNotBlank() && !isBreakOrHeading(cleanSubject)) {
                    val times = timeStr.split(Regex("[-–—]+")).map { it.trim() }
                    val startTime = times.getOrNull(0) ?: ""
                    val endTime = times.getOrNull(1) ?: ""
                    val dayStr = deriveDayOfWeek(dateStr)

                    extractedClasses.add(
                        ParsedTimetableClass(
                            day = dayStr,
                            date = dateStr,
                            subjectCode = codeStr,
                            subject = cleanSubject,
                            timeSlot = timeStr,
                            startTime = startTime,
                            endTime = endTime,
                            isExam = true,
                            colorKey = if (codeStr.contains("MJ") || codeStr.contains("Major")) "purple" else "teal"
                        )
                    )
                }
            }
        }

        val deduplicated = extractedClasses.distinctBy {
            "${it.date}_${it.day.lowercase()}_${it.subject.lowercase().trim()}_${it.timeSlot.lowercase().trim()}"
        }

        val summary = if (deduplicated.isNotEmpty()) {
            val datesList = deduplicated.mapNotNull { if (it.date.isNotBlank()) it.date else null }.distinct().joinToString(", ")
            "### 🗓️ Examination Schedule\n\nIdentified **${deduplicated.size} examination events** on: $datesList.\n\nReview the entries below and tap 'Apply Schedule' to save them to your timetable."
        } else {
            "No timetable classes or schedule entries were identified in this document. Please verify that the document contains clear days of the week, subject names, and class timings."
        }

        return Pair(summary, deduplicated)
    }

    fun parseTimetableAiResponse(rawResponse: String, rawDocumentText: String = ""): Pair<String, List<ParsedTimetableClass>> {
        val trimmed = rawResponse.trim()
        val firstBrace = trimmed.indexOf('{')
        val lastBrace = trimmed.lastIndexOf('}')

        val cleanJson = if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            trimmed.substring(firstBrace, lastBrace + 1)
        } else {
            trimmed.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        }

        val rawClassesList = mutableListOf<ParsedTimetableClass>()
        var summaryText = ""

        try {
            val json = JSONObject(cleanJson)
            summaryText = json.optString("summary", "").trim()

            val arr = json.optJSONArray("classes") ?: json.optJSONArray("exams") ?: json.optJSONArray("schedule")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val dateRaw = obj.optString("date", "").trim()
                    val subjectCode = obj.optString("subjectCode", obj.optString("code", "")).trim()
                    val courseType = obj.optString("courseType", "").trim()
                    val isExam = obj.optBoolean("isExam", dateRaw.isNotBlank() || subjectCode.isNotBlank())

                    val dayRaw = obj.optString("day", "").trim()
                    val dayFormatted = when {
                        dateRaw.isNotBlank() -> deriveDayOfWeek(dateRaw)
                        dayRaw.isNotBlank() -> when (dayRaw.lowercase().take(3)) {
                            "mon" -> "Mon"
                            "tue" -> "Tue"
                            "wed" -> "Wed"
                            "thu" -> "Thu"
                            "fri" -> "Fri"
                            "sat" -> "Sat"
                            "sun" -> "Sun"
                            else -> "Mon"
                        }
                        else -> "Mon"
                    }

                    val subject = obj.optString("subject", "").trim()
                    val timeSlot = obj.optString("timeSlot", "10:30 AM - 12:30 PM").trim().replace("–", "-")
                    val startTime = obj.optString("startTime", "").trim()
                    val endTime = obj.optString("endTime", "").trim()
                    val room = obj.optString("room", "").trim()
                    val faculty = obj.optString("faculty", "").trim()
                    val isLab = obj.optBoolean("isLab", false)
                    val colorKey = obj.optString("colorKey", if (isExam) "purple" else if (isLab) "teal" else "blue")

                    if (subject.isNotBlank() && !isBreakOrHeading(subject)) {
                        rawClassesList.add(
                            ParsedTimetableClass(
                                day = dayFormatted,
                                subject = subject,
                                timeSlot = timeSlot,
                                room = room,
                                faculty = faculty,
                                colorKey = colorKey,
                                startTime = startTime,
                                endTime = endTime,
                                isLab = isLab,
                                date = dateRaw,
                                subjectCode = subjectCode,
                                courseType = courseType,
                                isExam = isExam
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {
            summaryText = ""
        }

        // If AI returned 0 entries, try dynamic document text parser fallback
        if (rawClassesList.isEmpty() && rawDocumentText.isNotBlank()) {
            val textParsed = parseTimetableFromDocumentText(rawDocumentText)
            if (textParsed.second.isNotEmpty()) {
                return textParsed
            }
        }

        // Deduplicate classes (same day, date, subject, timeSlot)
        val deduplicatedClasses = rawClassesList.distinctBy {
            "${it.date}_${it.day.lowercase()}_${it.subject.lowercase().trim()}_${it.timeSlot.lowercase().trim()}"
        }

        val finalSummary = when {
            deduplicatedClasses.isNotEmpty() && summaryText.isNotBlank() -> summaryText
            deduplicatedClasses.isNotEmpty() -> {
                val hasExams = deduplicatedClasses.any { it.isExam }
                if (hasExams) {
                    val datesList = deduplicatedClasses.mapNotNull { if (it.date.isNotBlank()) it.date else null }.distinct().joinToString(", ")
                    "### 🗓️ Examination Schedule\n\nIdentified **${deduplicatedClasses.size} exam entries** on $datesList. Review the entries below and tap 'Apply Schedule' to save them to your timetable."
                } else {
                    val dayCount = deduplicatedClasses.map { it.day }.distinct().size
                    val daysList = deduplicatedClasses.map { it.day }.distinct().joinToString(", ")
                    "### 🗓️ Timetable Analysis\n\nIdentified **${deduplicatedClasses.size} classes** across **$dayCount days** ($daysList). Review the entries below and tap 'Apply Schedule' to save them to your schedule."
                }
            }
            else -> {
                "No timetable classes or schedule entries were identified in this document. Please verify that the document contains clear days of the week, subject names, and class timings."
            }
        }

        return Pair(finalSummary, deduplicatedClasses)
    }

    private fun isBreakOrHeading(subject: String): Boolean {
        val s = subject.trim().lowercase()
        if (s.length < 2) return true
        val nonClassKeywords = listOf(
            "lunch", "break", "recess", "interval", "tea break", "free period"
        )
        return nonClassKeywords.any { s == it || s.startsWith("$it ") || s.endsWith(" $it") } ||
                (!subject.any { it.isLetter() } && !subject.any { it.isDigit() })
    }

    private fun parseAssignmentAiResponse(rawResponse: String): Triple<String, List<Triple<String, String, String>>, List<String>> {
        val cleanJson = rawResponse.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()

        val assignments = mutableListOf<Triple<String, String, String>>()
        val questions = mutableListOf<String>()
        var summaryText = ""

        try {
            val json = JSONObject(cleanJson)
            summaryText = json.optString("summary", "")

            val arr = json.optJSONArray("assignments")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val sub = obj.optString("subject", "General")
                    val title = obj.optString("title", "Assignment Task")
                    val due = obj.optString("dueDate", "Tomorrow")
                    assignments.add(Triple(sub, title, due))
                }
            }

            val qArr = json.optJSONArray("questions")
            if (qArr != null) {
                for (i in 0 until qArr.length()) {
                    questions.add(qArr.getString(i))
                }
            }
        } catch (e: Exception) {
            summaryText = rawResponse
        }

        return Triple(summaryText.ifBlank { rawResponse }, assignments, questions)
    }

    fun applyParsedTimetableToSchedule() {
        val classes = parsedClasses.value
        if (classes.isEmpty()) {
            showToast("No classes found to import.")
            return
        }

        docProcessingState.value = DocProcessingState.SAVING
        docProcessingStatusText.value = "Saving schedule to timetable..."

        viewModelScope.launch {
            val existing = timetableDao.getAllClassesSync()
            val newEntities = mutableListOf<TimetableEntity>()

            classes.forEach { it ->
                // Check if identical class/exam already exists (prevent duplicates)
                val alreadyExists = existing.any { ex ->
                    if (it.isExam && it.date.isNotBlank()) {
                        ex.isExam && ex.examDate.trim() == it.date.trim() &&
                                ex.subject.equals(it.subject.trim(), ignoreCase = true)
                    } else {
                        ex.dayOfWeek.equals(it.day.trim(), ignoreCase = true) &&
                                ex.subject.equals(it.subject.trim(), ignoreCase = true) &&
                                ex.timeSlot.equals(it.timeSlot.trim(), ignoreCase = true)
                    }
                }

                if (!alreadyExists) {
                    newEntities.add(
                        TimetableEntity(
                            dayOfWeek = it.day,
                            subject = it.subject,
                            timeSlot = it.timeSlot,
                            startTime = it.startTime,
                            endTime = it.endTime,
                            room = it.room,
                            facultyName = it.faculty,
                            isLab = it.isLab,
                            colorKey = it.colorKey,
                            examDate = it.date,
                            courseCode = it.subjectCode,
                            isExam = it.isExam
                        )
                    )
                }
            }

            if (newEntities.isNotEmpty()) {
                timetableDao.insertClasses(newEntities)
                if (studentProfile.value?.cloudSyncEnabled == true && FirebaseAuthHelper.isUserLoggedIn) {
                    newEntities.forEach { FirestoreManager.saveTimetableClass(it) }
                }

                // Schedule notifications for any newly added exam events
                newEntities.filter { it.isExam && it.examDate.isNotBlank() }.forEach { exam ->
                    try {
                        com.example.arkin.util.AlarmScheduler.scheduleExamNotification(
                            getApplication(),
                            exam.id,
                            exam.subject,
                            exam.examDate,
                            exam.startTime.ifBlank { exam.timeSlot }
                        )
                    } catch (_: Exception) {}
                }

                showToast("All ${newEntities.size} entries saved to your Timetable! 🗓️")
            } else {
                showToast("All recognized entries already exist in your schedule.")
            }

            docProcessingState.value = DocProcessingState.COMPLETED
            docProcessingStatusText.value = "Schedule imported successfully"
            navigateTo(AppScreen.TIMETABLE)
        }
    }

    fun applyParsedHomework() {
        val hwItems = parsedAssignments.value
        if (hwItems.isEmpty()) return
        viewModelScope.launch {
            val questions = parsedExamQuestions.value
            val questionsJson = if (questions.isNotEmpty()) {
                val arr = org.json.JSONArray()
                questions.forEach { arr.put(it) }
                arr.toString()
            } else null

            val descriptionText = if (questions.isNotEmpty()) {
                "Assignment Questions:\n" + questions.mapIndexed { idx, q -> "${idx + 1}. $q" }.joinToString("\n")
            } else ""

            val docUriStr = selectedDocUri.value?.toString()
            val docName = uploadedDocName.value
            val docSize = DocumentTextExtractor.formatFileSize(docFileSize.value)

            hwItems.forEach { (sub, title, due) ->
                addHomework(
                    subject = sub,
                    title = title,
                    dueDate = due,
                    priority = "High",
                    description = descriptionText,
                    attachmentUri = docUriStr,
                    attachmentName = docName,
                    attachmentSize = docSize,
                    extractedQuestions = questionsJson
                )
            }
            docProcessingState.value = DocProcessingState.COMPLETED
            showToast("Assignments added to Homework & Planner! 📚")
            navigateTo(AppScreen.HOMEWORK)
        }
    }

    fun applyParsedQuestionsToNotes() {
        val content = analyzedDocResult.value ?: return
        val docName = uploadedDocName.value ?: "Question Paper Analysis"
        viewModelScope.launch {
            val note = NoteEntity(
                title = "Exam Prep: $docName",
                content = content,
                subject = "Exam Prep",
                category = "Revision Notes",
                tag = "Exam",
                colorKey = "purple",
                timestamp = System.currentTimeMillis()
            )
            val id = noteDao.insertNote(note)
            if (studentProfile.value?.cloudSyncEnabled == true && FirebaseAuthHelper.isUserLoggedIn) {
                FirestoreManager.saveNote(note.copy(id = id))
            }
            docProcessingState.value = DocProcessingState.COMPLETED
            showToast("Saved to your Study Notes! 📝")
            navigateTo(AppScreen.NOTES)
        }
    }

    // Backup & Restore Actions
    fun createBackupJson(onReady: (String) -> Unit) {
        viewModelScope.launch {
            val json = BackupManager.createBackupJson(getApplication())
            val current = studentProfile.value ?: StudentProfileEntity()
            profileDao.updateProfile(current.copy(lastBackupDate = System.currentTimeMillis()))
            onReady(json)
            showToast("Backup created successfully")
        }
    }

    fun previewBackupJson(json: String) {
        val preview = BackupManager.parsePreview(json)
        backupPreview.value = preview
    }

    fun restoreBackup(json: String, replaceAll: Boolean) {
        isRestoringBackup.value = true
        viewModelScope.launch {
            val success = BackupManager.restoreBackup(getApplication(), json, replaceAll)
            isRestoringBackup.value = false
            if (success) {
                showToast("Database restored successfully! 🎉")
                backupPreview.value = null
                navigateTo(AppScreen.DASHBOARD)
            } else {
                showToast("Failed to restore backup: Invalid format")
            }
        }
    }

    // Profile & Settings Actions
    fun updateProfile(name: String, major: String, email: String, college: String = "", course: String = "", semester: String = "", academicYear: String = "") {
        viewModelScope.launch {
            // Validate College if provided
            if (college.isNotBlank()) {
                val colVal = StudentProfileValidator.validateCollege(college)
                if (!colVal.isValid) {
                    showToast(colVal.errorMessage ?: "Invalid college name")
                    return@launch
                }
            }
            // Validate Course / Program if provided
            if (course.isNotBlank()) {
                val progVal = StudentProfileValidator.validateProgram(course)
                if (!progVal.isValid) {
                    showToast(progVal.errorMessage ?: "Invalid program name")
                    return@launch
                }
            }
            // Validate Semester if provided
            if (semester.isNotBlank()) {
                val semVal = StudentProfileValidator.validateSemester(semester, course)
                if (!semVal.isValid) {
                    showToast(semVal.errorMessage ?: "Invalid semester")
                    return@launch
                }
            }
            // Validate Academic Year if provided
            if (academicYear.isNotBlank()) {
                val yearVal = StudentProfileValidator.validateAcademicYear(academicYear)
                if (!yearVal.isValid) {
                    showToast(yearVal.errorMessage ?: "Invalid academic year")
                    return@launch
                }
            }
            // Validate Major / Department if provided
            if (major.isNotBlank()) {
                val majorVal = StudentProfileValidator.validateMajor(major)
                if (!majorVal.isValid) {
                    showToast(majorVal.errorMessage ?: "Invalid department")
                    return@launch
                }
            }

            val current = studentProfile.value ?: StudentProfileEntity()
            val updated = current.copy(
                name = name.trim(),
                major = major.trim(),
                email = email.trim(),
                collegeName = if (college.isNotBlank()) college.trim() else current.collegeName,
                course = if (course.isNotBlank()) course.trim() else current.course,
                semester = if (semester.isNotBlank()) semester.trim() else current.semester,
                academicYear = if (academicYear.isNotBlank()) academicYear.trim() else current.academicYear
            )
            profileDao.updateProfile(updated)
            if (updated.cloudSyncEnabled && FirebaseAuthHelper.isUserLoggedIn) {
                FirestoreManager.saveProfile(updated)
            }
            showToast("Profile updated")
        }
    }

    fun toggleDarkMode() {
        viewModelScope.launch {
            val current = studentProfile.value ?: StudentProfileEntity()
            val updated = current.copy(isDarkMode = !current.isDarkMode)
            profileDao.updateProfile(updated)
            if (updated.cloudSyncEnabled && FirebaseAuthHelper.isUserLoggedIn) {
                FirestoreManager.saveProfile(updated)
            }
            showToast(if (updated.isDarkMode) "Dark mode enabled" else "Light mode enabled")
        }
    }

    fun toggleCloudSync() {
        viewModelScope.launch {
            val current = studentProfile.value ?: StudentProfileEntity()
            val updated = current.copy(cloudSyncEnabled = !current.cloudSyncEnabled)
            profileDao.updateProfile(updated)
            if (updated.cloudSyncEnabled && FirebaseAuthHelper.isUserLoggedIn) {
                FirestoreManager.saveProfile(updated)
            }
            showToast(if (updated.cloudSyncEnabled) "Cloud synchronization enabled" else "Cloud sync paused (Local mode)")
        }
    }

    fun updateAiPreferences(style: String, length: String) {
        viewModelScope.launch {
            val current = studentProfile.value ?: StudentProfileEntity()
            val updated = current.copy(
                aiExplanationStyle = style,
                aiAnswerLength = length
            )
            profileDao.updateProfile(updated)
            if (updated.cloudSyncEnabled && FirebaseAuthHelper.isUserLoggedIn) {
                FirestoreManager.saveProfile(updated)
            }
            showToast("AI preferences saved")
        }
    }


    // ==========================================
    // Local Data Isolation & Reset
    // ==========================================

    suspend fun clearLocalUserData() {
        withContext(Dispatchers.IO) {
            db.noteDao().clearAllNotes()
            db.taskDao().clearAllTasks()
            db.homeworkDao().clearAllHomework()
            db.timetableDao().clearAllClasses()
            db.reminderDao().clearAllReminders()
            db.examResultDao().clearAllExamResults()
            db.vaultItemDao().clearAllVaultItems()
            profileDao.clearProfile()
            profileDao.insertProfile(StudentProfileEntity(id = 1))

            val context = getApplication<Application>().applicationContext
            VaultSecurityManager.clearVault(context)
        }

        _isVaultUnlocked.value = false
        _enteredPin.value = ""
        _isPinError.value = false
        _chatMessages.value = listOf(
            ChatMessage(
                sender = "ai",
                text = "Hello! 👋 I'm Arkin, your AI study assistant. I can explain complex academic concepts, solve assignment questions step-by-step, organize your timetable, or generate revision flashcards."
            )
        )
        selectedAssignment.value = null
        editingNote.value = null
        isNoteDialogOpen.value = false
        backupPreview.value = null
        uploadedDocName.value = null
        selectedDocUri.value = null
        parsedClasses.value = emptyList()
        parsedAssignments.value = emptyList()
        parsedExamQuestions.value = emptyList()
        authError.value = null
        needsProfileSetup.value = false
        lastFirestoreSyncTime.value = null
    }

    /**
     * Purges all data associated with this account (both local and cloud) so user can start completely fresh.
     */
    fun resetAccountData(clearCloud: Boolean = true) {
        viewModelScope.launch {
            _isAiLoading.value = true
            try {
                if (clearCloud && FirebaseAuthHelper.isUserLoggedIn) {
                    FirestoreManager.clearUserCloudData()
                }
                clearLocalUserData()
                showToast("Account data reset! Your account is now fresh and clean. ✨")
            } catch (e: Exception) {
                showToast("Reset failed: ${e.message}")
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    // ==========================================
    // Firebase Authentication Actions
    // ==========================================

    fun signUpWithEmail(
        fullName: String,
        email: String,
        password: String,
        college: String = "",
        course: String = "",
        semester: String = "",
        academicYear: String = ""
    ) {
        isAuthLoading.value = true
        authError.value = null
        viewModelScope.launch {
            // Clean any prior local data before creating a new account
            clearLocalUserData()

            val result = FirebaseAuthHelper.signUpWithEmail(email, password, fullName)
            isAuthLoading.value = false
            if (result.isSuccess) {
                val user = result.getOrNull()!!
                val newProfile = StudentProfileEntity(
                    id = 1,
                    name = fullName.trim().ifBlank { "Student" },
                    email = email.trim(),
                    major = if (course.isNotBlank() && semester.isNotBlank()) "$course, $semester" else course.ifBlank { semester },
                    collegeName = college.trim(),
                    course = course.trim(),
                    semester = semester.trim(),
                    academicYear = academicYear.trim(),
                    lastBackupDate = System.currentTimeMillis()
                )
                profileDao.updateProfile(newProfile)
                FirestoreManager.saveProfile(newProfile, user.uid)
                // Do NOT call syncLocalToFirestore(db) here! A new account starts fresh and isolated.
                showToast("Account created successfully! Welcome to Arkin, ${newProfile.name} 🎉")
                navigateTo(AppScreen.DASHBOARD)
            } else {
                val err = result.exceptionOrNull()?.message ?: "Sign up failed"
                authError.value = err
                showToast(err)
            }
        }
    }

    fun signInWithEmail(email: String, password: String) {
        isAuthLoading.value = true
        authError.value = null
        viewModelScope.launch {
            val result = FirebaseAuthHelper.signInWithEmail(email, password)
            isAuthLoading.value = false
            if (result.isSuccess) {
                val user = result.getOrNull()!!
                // Clear any leftover local data before pulling this user's data
                clearLocalUserData()

                val remoteProfile = FirestoreManager.fetchProfile(user.uid)
                if (remoteProfile != null) {
                    profileDao.updateProfile(remoteProfile)
                } else {
                    val current = StudentProfileEntity(
                        id = 1,
                        name = user.displayName ?: "Student",
                        email = user.email ?: email.trim(),
                        lastBackupDate = System.currentTimeMillis()
                    )
                    profileDao.updateProfile(current)
                    FirestoreManager.saveProfile(current, user.uid)
                }
                // Replace all local data with ONLY this user's remote cloud data
                FirestoreManager.syncFirestoreToLocal(db, replaceAll = true)
                lastFirestoreSyncTime.value = System.currentTimeMillis()
                showToast("Welcome back, ${user.displayName ?: user.email}! 👋")
                navigateTo(AppScreen.DASHBOARD)
            } else {
                val err = result.exceptionOrNull()?.message ?: "Login failed"
                authError.value = err
                showToast(err)
            }
        }
    }

    fun signInWithGoogle(context: Context) {
        isAuthLoading.value = true
        authError.value = null
        viewModelScope.launch {
            val result = FirebaseAuthHelper.signInWithGoogle(context)
            isAuthLoading.value = false
            if (result.isSuccess) {
                val user = result.getOrNull()!!
                clearLocalUserData()
                try {
                    val remoteProfile = FirestoreManager.fetchProfile(user.uid)
                    if (remoteProfile != null && remoteProfile.collegeName.isNotBlank() && remoteProfile.course.isNotBlank()) {
                        profileDao.updateProfile(remoteProfile)
                        FirestoreManager.syncFirestoreToLocal(db, replaceAll = true)
                        lastFirestoreSyncTime.value = System.currentTimeMillis()
                        showToast("Welcome back, ${user.displayName ?: user.email}! 🎉")
                        navigateTo(AppScreen.DASHBOARD)
                    } else {
                        val newProfile = StudentProfileEntity(
                            id = 1,
                            name = user.displayName?.takeIf { it.isNotBlank() } ?: "Student",
                            email = user.email ?: "",
                            lastBackupDate = System.currentTimeMillis()
                        )
                        profileDao.updateProfile(newProfile)
                        needsProfileSetup.value = true
                    }
                } catch (e: Exception) {
                    val newProfile = StudentProfileEntity(
                        id = 1,
                        name = user.displayName?.takeIf { it.isNotBlank() } ?: "Student",
                        email = user.email ?: "",
                        lastBackupDate = System.currentTimeMillis()
                    )
                    profileDao.updateProfile(newProfile)
                    needsProfileSetup.value = true
                }
            } else {
                val ex = result.exceptionOrNull()
                if (ex !is java.util.concurrent.CancellationException) {
                    val err = ex?.message ?: "Google Sign-In failed"
                    authError.value = err
                    showToast(err)
                }
            }
        }
    }

    fun completeGoogleProfileSetup(
        fullName: String,
        college: String,
        course: String,
        semester: String,
        academicYear: String
    ) {
        isAuthLoading.value = true
        viewModelScope.launch {
            val user = FirebaseAuthHelper.currentUser
            val updated = StudentProfileEntity(
                id = 1,
                name = fullName.trim().ifBlank { user?.displayName ?: "Student" },
                email = user?.email ?: "",
                major = if (course.isNotBlank() && semester.isNotBlank()) "$course, $semester" else course.ifBlank { semester },
                collegeName = college.trim(),
                course = course.trim(),
                semester = semester.trim(),
                academicYear = academicYear.trim(),
                lastBackupDate = System.currentTimeMillis()
            )
            profileDao.updateProfile(updated)
            if (user != null) {
                try {
                    FirestoreManager.saveProfile(updated, user.uid)
                } catch (e: Exception) {
                    // Ignore non-fatal sync error
                }
            }
            isAuthLoading.value = false
            needsProfileSetup.value = false
            showToast("Profile completed! Welcome to Arkin, ${updated.name} 🎉")
            navigateTo(AppScreen.DASHBOARD)
        }
    }

    fun sendPasswordReset(email: String) {
        if (email.isBlank() || !email.contains("@")) {
            showToast("Please enter a valid email address.")
            return
        }
        viewModelScope.launch {
            val result = FirebaseAuthHelper.sendPasswordReset(email)
            if (result.isSuccess) {
                showToast("Password reset link sent to $email! Please check your inbox.")
            } else {
                val err = result.exceptionOrNull()?.message ?: "Failed to send reset link"
                showToast(err)
            }
        }
    }

    fun syncToFirestore() {
        if (!FirebaseAuthHelper.isUserLoggedIn) {
            showToast("Please log in to sync with Firebase Firestore")
            return
        }
        isFirestoreSyncing.value = true
        viewModelScope.launch {
            val result = FirestoreManager.syncLocalToFirestore(db)
            isFirestoreSyncing.value = false
            if (result.isSuccess) {
                lastFirestoreSyncTime.value = System.currentTimeMillis()
                showToast("Successfully backed up ${result.getOrNull()} items to Firestore! ☁️")
            } else {
                val err = result.exceptionOrNull()?.message ?: "Sync failed"
                showToast("Firestore sync failed: $err")
            }
        }
    }

    fun syncFromFirestore() {
        if (!FirebaseAuthHelper.isUserLoggedIn) {
            showToast("Please log in to restore from Firebase Firestore")
            return
        }
        isFirestoreSyncing.value = true
        viewModelScope.launch {
            val result = FirestoreManager.syncFirestoreToLocal(db, replaceAll = true)
            isFirestoreSyncing.value = false
            if (result.isSuccess) {
                lastFirestoreSyncTime.value = System.currentTimeMillis()
                showToast("Restored ${result.getOrNull()} items from Firestore! ☁️")
            } else {
                val err = result.exceptionOrNull()?.message ?: "Restore failed"
                showToast("Firestore restore failed: $err")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            clearLocalUserData()
            FirebaseAuthHelper.signOut()
            navigateTo(AppScreen.AUTH, addToStack = false)
            showToast("Signed out. Device data cleared.")
        }
    }
}
