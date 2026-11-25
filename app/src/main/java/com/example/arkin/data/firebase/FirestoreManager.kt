package com.example.arkin.data.firebase

import com.example.arkin.data.ArkinDatabase
import com.example.arkin.data.model.ExamResultEntity
import com.example.arkin.data.model.HomeworkEntity
import com.example.arkin.data.model.NoteEntity
import com.example.arkin.data.model.ReminderEntity
import com.example.arkin.data.model.StudentProfileEntity
import com.example.arkin.data.model.TaskEntity
import com.example.arkin.data.model.TimetableEntity
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import android.content.Context
import com.example.arkin.data.model.VaultItemEntity
import java.io.File
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FirestoreManager {

    val firestore: FirebaseFirestore?
        get() = try {
            FirebaseFirestore.getInstance()
        } catch (e: Throwable) {
            null
        }

    private fun userDoc(uid: String): DocumentReference =
        firestore?.collection("users")?.document(uid)
            ?: throw IllegalStateException("Firebase Firestore is not initialized")

    fun mapFirestoreException(e: Exception): Exception {
        val msg = e.message ?: ""
        return when {
            msg.contains("PERMISSION_DENIED", ignoreCase = true) ||
            msg.contains("Missing or insufficient permissions", ignoreCase = true) ->
                Exception("PERMISSION_DENIED: Missing or insufficient permissions. Please deploy the user-scoped Firestore security rules from 'firestore.rules' in Firebase Console for project 'arkin-22b82'.")
            msg.contains("UNAVAILABLE", ignoreCase = true) || msg.contains("network", ignoreCase = true) ->
                Exception("Network unavailable. Firestore offline persistence is enabled; data will synchronize automatically when connection is restored.")
            else -> e
        }
    }

    // ==========================================
    // Student Profile
    // ==========================================

    suspend fun saveProfile(profile: StudentProfileEntity, uidOverride: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = uidOverride ?: FirebaseAuthHelper.currentUserId ?: return@withContext Result.failure(IllegalStateException("No authenticated user"))
        try {
            val data = hashMapOf<String, Any>(
                "id" to profile.id,
                "name" to profile.name,
                "email" to profile.email,
                "major" to profile.major,
                "university" to profile.university,
                "collegeName" to profile.collegeName,
                "course" to profile.course,
                "semester" to profile.semester,
                "academicYear" to profile.academicYear,
                "currentStreak" to profile.currentStreak,
                "isDarkMode" to profile.isDarkMode,
                "cloudSyncEnabled" to profile.cloudSyncEnabled,
                "lastBackupDate" to profile.lastBackupDate,
                "aiExplanationStyle" to profile.aiExplanationStyle,
                "aiAnswerLength" to profile.aiAnswerLength,
                "updatedAt" to System.currentTimeMillis()
            )
            userDoc(uid).set(data, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchProfile(uidOverride: String? = null): StudentProfileEntity? = withContext(Dispatchers.IO) {
        val uid = uidOverride ?: FirebaseAuthHelper.currentUserId ?: return@withContext null
        try {
            val snapshot = userDoc(uid).get().await()
            if (snapshot.exists()) {
                StudentProfileEntity(
                    id = 1,
                    name = snapshot.getString("name") ?: "",
                    email = snapshot.getString("email") ?: "",
                    major = snapshot.getString("major") ?: "",
                    university = snapshot.getString("university") ?: "",
                    collegeName = snapshot.getString("collegeName") ?: "",
                    course = snapshot.getString("course") ?: "",
                    semester = snapshot.getString("semester") ?: "",
                    academicYear = snapshot.getString("academicYear") ?: "",
                    lockerPin = "",
                    currentStreak = snapshot.getLong("currentStreak")?.toInt() ?: 0,
                    isDarkMode = snapshot.getBoolean("isDarkMode") ?: false,
                    cloudSyncEnabled = snapshot.getBoolean("cloudSyncEnabled") ?: true,
                    lastBackupDate = snapshot.getLong("lastBackupDate") ?: 0L,
                    aiExplanationStyle = snapshot.getString("aiExplanationStyle") ?: "Simple & Structured",
                    aiAnswerLength = snapshot.getString("aiAnswerLength") ?: "Standard"
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    // ==========================================
    // Notes
    // ==========================================

    suspend fun saveNote(note: NoteEntity): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = FirebaseAuthHelper.currentUserId ?: return@withContext Result.failure(IllegalStateException("No user"))
        try {
            val data = hashMapOf<String, Any?>(
                "id" to note.id,
                "title" to note.title,
                "content" to note.content,
                "subject" to note.subject,
                "semester" to note.semester,
                "category" to note.category,
                "tag" to note.tag,
                "colorKey" to note.colorKey,
                "timestamp" to note.timestamp,
                "isSecure" to note.isSecure,
                "isPinned" to note.isPinned,
                "isFavorite" to note.isFavorite,
                "isArchived" to note.isArchived,
                "attachmentUri" to note.attachmentUri,
                "attachmentName" to note.attachmentName
            )
            userDoc(uid).collection("notes").document(note.id.toString()).set(data, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteNote(noteId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = FirebaseAuthHelper.currentUserId ?: return@withContext Result.failure(IllegalStateException("No user"))
        try {
            userDoc(uid).collection("notes").document(noteId.toString()).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // Tasks
    // ==========================================

    suspend fun saveTask(task: TaskEntity): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = FirebaseAuthHelper.currentUserId ?: return@withContext Result.failure(IllegalStateException("No user"))
        try {
            val data = hashMapOf<String, Any?>(
                "id" to task.id,
                "title" to task.title,
                "description" to task.description,
                "subject" to task.subject,
                "isCompleted" to task.isCompleted,
                "priority" to task.priority,
                "dueDate" to task.dueDate,
                "dueTime" to task.dueTime,
                "startDate" to task.startDate,
                "linkedAssignmentId" to task.linkedAssignmentId,
                "createdAt" to task.createdAt
            )
            userDoc(uid).collection("tasks").document(task.id.toString()).set(data, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTask(taskId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = FirebaseAuthHelper.currentUserId ?: return@withContext Result.failure(IllegalStateException("No user"))
        try {
            userDoc(uid).collection("tasks").document(taskId.toString()).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // Homework
    // ==========================================

    suspend fun saveHomework(hw: HomeworkEntity): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = FirebaseAuthHelper.currentUserId ?: return@withContext Result.failure(IllegalStateException("No user"))
        try {
            val data = hashMapOf<String, Any?>(
                "id" to hw.id,
                "subject" to hw.subject,
                "title" to hw.title,
                "description" to hw.description,
                "teacherName" to hw.teacherName,
                "receivedDate" to hw.receivedDate,
                "dueDate" to hw.dueDate,
                "dueTime" to hw.dueTime,
                "priority" to hw.priority,
                "status" to hw.status,
                "isDone" to hw.isDone,
                "colorKey" to hw.colorKey,
                "reminderEnabled" to hw.reminderEnabled,
                "reminderMinutesBefore" to hw.reminderMinutesBefore,
                "attachmentUri" to hw.attachmentUri,
                "attachmentName" to hw.attachmentName,
                "attachmentSize" to hw.attachmentSize,
                "extractedQuestionsJson" to hw.extractedQuestionsJson,
                "aiDraftAnswer" to hw.aiDraftAnswer
            )
            userDoc(uid).collection("homework").document(hw.id.toString()).set(data, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteHomework(hwId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = FirebaseAuthHelper.currentUserId ?: return@withContext Result.failure(IllegalStateException("No user"))
        try {
            userDoc(uid).collection("homework").document(hwId.toString()).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // Timetable
    // ==========================================

    suspend fun saveTimetableClass(tt: TimetableEntity): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = FirebaseAuthHelper.currentUserId ?: return@withContext Result.failure(IllegalStateException("No user"))
        try {
            val data = hashMapOf<String, Any?>(
                "id" to tt.id,
                "dayOfWeek" to tt.dayOfWeek,
                "subject" to tt.subject,
                "timeSlot" to tt.timeSlot,
                "startTime" to tt.startTime,
                "endTime" to tt.endTime,
                "room" to tt.room,
                "facultyName" to tt.facultyName,
                "requiredMaterials" to tt.requiredMaterials,
                "isLab" to tt.isLab,
                "reminderMinutesBefore" to tt.reminderMinutesBefore,
                "colorKey" to tt.colorKey,
                "examDate" to tt.examDate,
                "courseCode" to tt.courseCode,
                "isExam" to tt.isExam
            )
            userDoc(uid).collection("timetable").document(tt.id.toString()).set(data, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTimetableClass(ttId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = FirebaseAuthHelper.currentUserId ?: return@withContext Result.failure(IllegalStateException("No user"))
        try {
            userDoc(uid).collection("timetable").document(ttId.toString()).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // Exam Results
    // ==========================================

    suspend fun saveExamResult(exam: ExamResultEntity): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = FirebaseAuthHelper.currentUserId ?: return@withContext Result.failure(IllegalStateException("No user"))
        try {
            val data = hashMapOf<String, Any?>(
                "id" to exam.id,
                "subject" to exam.subject,
                "examName" to exam.examName,
                "marksObtained" to exam.marksObtained.toDouble(),
                "totalMarks" to exam.totalMarks.toDouble(),
                "semester" to exam.semester,
                "dateRecorded" to exam.dateRecorded
            )
            userDoc(uid).collection("exam_results").document(exam.id.toString()).set(data, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteExamResult(examId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = FirebaseAuthHelper.currentUserId ?: return@withContext Result.failure(IllegalStateException("No user"))
        try {
            userDoc(uid).collection("exam_results").document(examId.toString()).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // Reminders
    // ==========================================

    suspend fun saveReminder(reminder: ReminderEntity): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = FirebaseAuthHelper.currentUserId ?: return@withContext Result.failure(IllegalStateException("No user"))
        try {
            val data = hashMapOf<String, Any?>(
                "id" to reminder.id,
                "title" to reminder.title,
                "dueText" to reminder.dueText,
                "iconKey" to reminder.iconKey,
                "colorKey" to reminder.colorKey,
                "isCompleted" to reminder.isCompleted
            )
            userDoc(uid).collection("reminders").document(reminder.id.toString()).set(data, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteReminder(reminderId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = FirebaseAuthHelper.currentUserId ?: return@withContext Result.failure(IllegalStateException("No user"))
        try {
            userDoc(uid).collection("reminders").document(reminderId.toString()).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // Vault Items
    // ==========================================

    suspend fun saveVaultItem(item: VaultItemEntity, context: Context? = null): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = FirebaseAuthHelper.currentUserId ?: return@withContext Result.failure(IllegalStateException("No user"))
        try {
            var encryptedBase64: String? = null
            if (context != null && !item.attachmentUri.isNullOrBlank()) {
                val encFile = if (item.attachmentUri.startsWith("/")) {
                    File(item.attachmentUri)
                } else {
                    File(context.filesDir, item.attachmentUri)
                }
                if (encFile.exists() && encFile.length() in 1..(800 * 1024)) {
                    val bytes = encFile.readBytes()
                    encryptedBase64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                }
            }

            val data = hashMapOf<String, Any?>(
                "id" to item.id,
                "title" to item.title,
                "category" to item.category,
                "content" to item.content,
                "attachmentUri" to item.attachmentUri,
                "attachmentName" to item.attachmentName,
                "fileType" to item.fileType,
                "fileSize" to item.fileSize,
                "mimeType" to item.mimeType,
                "timestamp" to item.timestamp,
                "encryptedPayloadBase64" to encryptedBase64
            )
            userDoc(uid).collection("vault_items").document(item.id.toString()).set(data, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteVaultItem(vaultItemId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = FirebaseAuthHelper.currentUserId ?: return@withContext Result.failure(IllegalStateException("No user"))
        try {
            userDoc(uid).collection("vault_items").document(vaultItemId.toString()).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveVaultSecurityConfig(context: Context, uid: String) = withContext(Dispatchers.IO) {
        try {
            val prefs = context.getSharedPreferences("arkin_vault_prefs", Context.MODE_PRIVATE)
            val pinHash = prefs.getString("vault_pin_hash", null)
            val pinSalt = prefs.getString("vault_pin_salt", null)
            val question = prefs.getString("vault_security_question", null)
            val ansHash = prefs.getString("vault_security_ans_hash", null)
            val ansSalt = prefs.getString("vault_security_ans_salt", null)
            val email = prefs.getString("vault_recovery_email", null)

            if (!pinHash.isNullOrBlank() && !pinSalt.isNullOrBlank()) {
                val secData = hashMapOf<String, Any?>(
                    "pinHash" to pinHash,
                    "pinSalt" to pinSalt,
                    "question" to question,
                    "ansHash" to ansHash,
                    "ansSalt" to ansSalt,
                    "recoveryEmail" to email,
                    "updatedAt" to System.currentTimeMillis()
                )
                userDoc(uid).collection("security").document("vault_config").set(secData, SetOptions.merge()).await()
            }
        } catch (_: Exception) {}
    }

    suspend fun restoreVaultSecurityConfig(context: Context, uid: String) = withContext(Dispatchers.IO) {
        try {
            val prefs = context.getSharedPreferences("arkin_vault_prefs", Context.MODE_PRIVATE)
            // Only restore if local vault PIN is not set yet (e.g. fresh install or deleted app)
            if (prefs.getString("vault_pin_hash", null) == null) {
                val doc = userDoc(uid).collection("security").document("vault_config").get().await()
                if (doc.exists()) {
                    val pinHash = doc.getString("pinHash")
                    val pinSalt = doc.getString("pinSalt")
                    val question = doc.getString("question")
                    val ansHash = doc.getString("ansHash")
                    val ansSalt = doc.getString("ansSalt")
                    val email = doc.getString("recoveryEmail")

                    if (!pinHash.isNullOrBlank() && !pinSalt.isNullOrBlank()) {
                        prefs.edit().apply {
                            putString("vault_pin_hash", pinHash)
                            putString("vault_pin_salt", pinSalt)
                            if (!question.isNullOrBlank()) putString("vault_security_question", question)
                            if (!ansHash.isNullOrBlank()) putString("vault_security_ans_hash", ansHash)
                            if (!ansSalt.isNullOrBlank()) putString("vault_security_ans_salt", ansSalt)
                            if (!email.isNullOrBlank()) putString("vault_recovery_email", email)
                            apply()
                        }
                    }
                }
            }
        } catch (_: Exception) {}
    }

    // ==========================================
    // Bidirectional Bulk Sync
    // ==========================================

    /**
     * Upload all local Room database entries to Firestore under `users/{uid}`.
     */
    suspend fun syncLocalToFirestore(db: ArkinDatabase, context: Context? = null): Result<Int> = withContext(Dispatchers.IO) {
        val uid = FirebaseAuthHelper.currentUserId ?: return@withContext Result.failure(IllegalStateException("Please log in to sync with Firebase"))
        try {
            var itemsSynced = 0

            // 1. Profile
            val profile = db.studentProfileDao().getProfileSync()
            if (profile != null) {
                saveProfile(profile.copy(lastBackupDate = System.currentTimeMillis()), uid).getOrThrow()
                itemsSynced++
            }

            // 2. Notes
            val notes = db.noteDao().getAllNotesSync()
            notes.forEach { note ->
                saveNote(note).getOrThrow()
                itemsSynced++
            }

            // 3. Tasks
            val tasks = db.taskDao().getAllTasksSync()
            tasks.forEach { task ->
                saveTask(task).getOrThrow()
                itemsSynced++
            }

            // 4. Homework
            val homework = db.homeworkDao().getAllHomeworkSync()
            homework.forEach { hw ->
                saveHomework(hw).getOrThrow()
                itemsSynced++
            }

            // 5. Timetable
            val timetable = db.timetableDao().getAllClassesSync()
            timetable.forEach { tt ->
                saveTimetableClass(tt).getOrThrow()
                itemsSynced++
            }

            // 6. Exam Results
            val examResults = db.examResultDao().getAllExamResultsSync()
            examResults.forEach { exam ->
                saveExamResult(exam).getOrThrow()
                itemsSynced++
            }

            // 7. Reminders
            val reminders = db.reminderDao().getAllRemindersSync()
            reminders.forEach { rem ->
                saveReminder(rem).getOrThrow()
                itemsSynced++
            }

            // 8. Vault Items
            val vaultItems = db.vaultItemDao().getAllVaultItemsSync()
            vaultItems.forEach { vItem ->
                saveVaultItem(vItem, context).getOrThrow()
                itemsSynced++
            }
            if (context != null) {
                saveVaultSecurityConfig(context, uid)
            }

            Result.success(itemsSynced)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }

    /**
     * Download all cloud documents from Firestore under `users/{uid}` into local Room database.
     */
    suspend fun syncFirestoreToLocal(
        db: ArkinDatabase,
        context: Context? = null,
        merge: Boolean = true,
        replaceAll: Boolean = false
    ): Result<Int> = withContext(Dispatchers.IO) {
        val uid = FirebaseAuthHelper.currentUserId ?: return@withContext Result.failure(IllegalStateException("Please log in to restore from Firebase"))
        try {
            var restoredCount = 0

            if (replaceAll) {
                db.noteDao().clearAllNotes()
                db.taskDao().clearAllTasks()
                db.homeworkDao().clearAllHomework()
                db.timetableDao().clearAllClasses()
                db.examResultDao().clearAllExamResults()
                db.reminderDao().clearAllReminders()
                db.vaultItemDao().clearAllVaultItems()
            }

            // 1. Restore Profile
            val remoteProfile = fetchProfile(uid)
            if (remoteProfile != null) {
                db.studentProfileDao().updateProfile(remoteProfile)
                restoredCount++
            }

            // 2. Notes
            val notesSnap = userDoc(uid).collection("notes").get().await()
            if (!notesSnap.isEmpty) {
                val notes = notesSnap.documents.mapNotNull { doc ->
                    try {
                        NoteEntity(
                            id = if (merge) (doc.getLong("id") ?: 0L) else 0L,
                            title = doc.getString("title") ?: "Untitled Note",
                            content = doc.getString("content") ?: "",
                            subject = doc.getString("subject") ?: "General",
                            semester = doc.getString("semester") ?: "Semester 4",
                            category = doc.getString("category") ?: "Class Notes",
                            tag = doc.getString("tag") ?: "Study",
                            colorKey = doc.getString("colorKey") ?: "purple",
                            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                            isSecure = doc.getBoolean("isSecure") ?: false,
                            isPinned = doc.getBoolean("isPinned") ?: false,
                            isFavorite = doc.getBoolean("isFavorite") ?: false,
                            isArchived = doc.getBoolean("isArchived") ?: false,
                            attachmentUri = doc.getString("attachmentUri"),
                            attachmentName = doc.getString("attachmentName")
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                if (notes.isNotEmpty()) {
                    db.noteDao().insertNotes(notes)
                    restoredCount += notes.size
                }
            }

            // 3. Tasks
            val tasksSnap = userDoc(uid).collection("tasks").get().await()
            if (!tasksSnap.isEmpty) {
                val tasks = tasksSnap.documents.mapNotNull { doc ->
                    try {
                        TaskEntity(
                            id = if (merge) (doc.getLong("id") ?: 0L) else 0L,
                            title = doc.getString("title") ?: "Task",
                            description = doc.getString("description") ?: "",
                            subject = doc.getString("subject") ?: "",
                            isCompleted = doc.getBoolean("isCompleted") ?: false,
                            priority = doc.getString("priority") ?: "Medium",
                            dueDate = doc.getString("dueDate") ?: "Today",
                            dueTime = doc.getString("dueTime") ?: "5:00 PM",
                            startDate = doc.getString("startDate") ?: "Today",
                            linkedAssignmentId = doc.getLong("linkedAssignmentId"),
                            createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                if (tasks.isNotEmpty()) {
                    db.taskDao().insertTasks(tasks)
                    restoredCount += tasks.size
                }
            }

            // 4. Homework
            val hwSnap = userDoc(uid).collection("homework").get().await()
            if (!hwSnap.isEmpty) {
                val hwList = hwSnap.documents.mapNotNull { doc ->
                    try {
                        HomeworkEntity(
                            id = if (merge) (doc.getLong("id") ?: 0L) else 0L,
                            subject = doc.getString("subject") ?: "General",
                            title = doc.getString("title") ?: "Assignment",
                            description = doc.getString("description") ?: "",
                            teacherName = doc.getString("teacherName") ?: "",
                            receivedDate = doc.getString("receivedDate") ?: "Recent",
                            dueDate = doc.getString("dueDate") ?: "Today",
                            dueTime = doc.getString("dueTime") ?: "11:59 PM",
                            priority = doc.getString("priority") ?: "Medium",
                            status = doc.getString("status") ?: "Not Started",
                            isDone = doc.getBoolean("isDone") ?: false,
                            colorKey = doc.getString("colorKey") ?: "purple",
                            reminderEnabled = doc.getBoolean("reminderEnabled") ?: true,
                            reminderMinutesBefore = doc.getLong("reminderMinutesBefore")?.toInt() ?: 1440,
                            attachmentUri = doc.getString("attachmentUri"),
                            attachmentName = doc.getString("attachmentName"),
                            attachmentSize = doc.getString("attachmentSize"),
                            extractedQuestionsJson = doc.getString("extractedQuestionsJson"),
                            aiDraftAnswer = doc.getString("aiDraftAnswer")
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                if (hwList.isNotEmpty()) {
                    db.homeworkDao().insertHomeworkList(hwList)
                    restoredCount += hwList.size
                }
            }

            // 5. Timetable
            val ttSnap = userDoc(uid).collection("timetable").get().await()
            if (!ttSnap.isEmpty) {
                val ttList = ttSnap.documents.mapNotNull { doc ->
                    try {
                        TimetableEntity(
                            id = if (merge) (doc.getLong("id") ?: 0L) else 0L,
                            dayOfWeek = doc.getString("dayOfWeek") ?: "Mon",
                            subject = doc.getString("subject") ?: "Class",
                            timeSlot = doc.getString("timeSlot") ?: "10:00 AM - 11:00 AM",
                            startTime = doc.getString("startTime") ?: "",
                            endTime = doc.getString("endTime") ?: "",
                            room = doc.getString("room") ?: "Room 101",
                            facultyName = doc.getString("facultyName") ?: "",
                            requiredMaterials = doc.getString("requiredMaterials") ?: "",
                            isLab = doc.getBoolean("isLab") ?: false,
                            reminderMinutesBefore = doc.getLong("reminderMinutesBefore")?.toInt() ?: 15,
                            colorKey = doc.getString("colorKey") ?: "purple",
                            examDate = doc.getString("examDate") ?: "",
                            courseCode = doc.getString("courseCode") ?: "",
                            isExam = doc.getBoolean("isExam") ?: false
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                if (ttList.isNotEmpty()) {
                    db.timetableDao().insertClasses(ttList)
                    restoredCount += ttList.size
                }
            }

            // 6. Exam Results
            val examSnap = userDoc(uid).collection("exam_results").get().await()
            if (!examSnap.isEmpty) {
                val examList = examSnap.documents.mapNotNull { doc ->
                    try {
                        ExamResultEntity(
                            id = if (merge) (doc.getLong("id") ?: 0L) else 0L,
                            subject = doc.getString("subject") ?: "General",
                            examName = doc.getString("examName") ?: "Midterm",
                            marksObtained = (doc.getDouble("marksObtained") ?: 0.0).toFloat(),
                            totalMarks = (doc.getDouble("totalMarks") ?: 100.0).toFloat(),
                            semester = doc.getString("semester") ?: "Semester 4",
                            dateRecorded = doc.getLong("dateRecorded") ?: System.currentTimeMillis()
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                if (examList.isNotEmpty()) {
                    db.examResultDao().insertExamResults(examList)
                    restoredCount += examList.size
                }
            }

            // 7. Reminders
            val remSnap = userDoc(uid).collection("reminders").get().await()
            if (!remSnap.isEmpty) {
                val remList = remSnap.documents.mapNotNull { doc ->
                    try {
                        ReminderEntity(
                            id = if (merge) (doc.getLong("id") ?: 0L) else 0L,
                            title = doc.getString("title") ?: "Reminder",
                            dueText = doc.getString("dueText") ?: "Today",
                            iconKey = doc.getString("iconKey") ?: "bell",
                            colorKey = doc.getString("colorKey") ?: "yellow",
                            isCompleted = doc.getBoolean("isCompleted") ?: false
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                if (remList.isNotEmpty()) {
                    db.reminderDao().insertReminders(remList)
                    restoredCount += remList.size
                }
            }

            // 8. Vault Items
            val vaultSnap = userDoc(uid).collection("vault_items").get().await()
            if (!vaultSnap.isEmpty) {
                for (doc in vaultSnap.documents) {
                    try {
                        val id = if (merge) (doc.getLong("id") ?: 0L) else 0L
                        val title = doc.getString("title") ?: "Vault Item"
                        val category = doc.getString("category") ?: "Private Note"
                        val content = doc.getString("content") ?: ""
                        val fileType = doc.getString("fileType") ?: "TEXT"
                        val attachmentName = doc.getString("attachmentName")
                        val fileSize = doc.getLong("fileSize") ?: 0L
                        val mimeType = doc.getString("mimeType")
                        val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()

                        var localAttachmentUri = doc.getString("attachmentUri")
                        val base64Payload = doc.getString("encryptedPayloadBase64")
                        if (!base64Payload.isNullOrBlank() && context != null) {
                            try {
                                val bytes = android.util.Base64.decode(base64Payload, android.util.Base64.DEFAULT)
                                val vaultDir = File(context.filesDir, "vault").apply { mkdirs() }
                                val encFileName = "vault_cloud_${doc.id}.enc"
                                val localEncFile = File(vaultDir, encFileName)
                                localEncFile.writeBytes(bytes)
                                localAttachmentUri = "vault/$encFileName"
                            } catch (_: Exception) {}
                        }

                        val entity = VaultItemEntity(
                            id = id,
                            title = title,
                            category = category,
                            content = content,
                            attachmentUri = localAttachmentUri,
                            attachmentName = attachmentName,
                            fileType = fileType,
                            fileSize = fileSize,
                            mimeType = mimeType,
                            timestamp = timestamp
                        )
                        db.vaultItemDao().insertVaultItem(entity)
                        restoredCount++
                    } catch (_: Exception) {}
                }
            }
            if (context != null) {
                restoreVaultSecurityConfig(context, uid)
            }

            Result.success(restoredCount)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }

    /**
     * Purge all documents under `users/{uid}` in Firestore collections.
     */
    suspend fun clearUserCloudData(uidOverride: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = uidOverride ?: FirebaseAuthHelper.currentUserId
            ?: return@withContext Result.failure(IllegalStateException("No authenticated user"))
        try {
            val collections = listOf("notes", "tasks", "homework", "timetable", "exam_results", "reminders", "vault_items")
            for (col in collections) {
                val snapshot = userDoc(uid).collection(col).get().await()
                for (doc in snapshot.documents) {
                    doc.reference.delete().await()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }
}
