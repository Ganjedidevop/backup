package com.example.arkin

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.arkin.data.ArkinDatabase
import com.example.arkin.data.model.TaskEntity
import com.example.arkin.ui.components.ConfirmationDialog
import com.example.arkin.ui.components.TaskItemRow
import com.example.arkin.util.NotificationHelper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TodoHomeworkReminderTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun taskItemRow_displaysDescriptionCreatedTimeAndCompletedTime() {
        val createdAt = 1764130000000L
        val completedAt = 1764133600000L
        val fmt = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        val expectedCreated = "Created: ${fmt.format(Date(createdAt))}"
        val expectedCompleted = "Completed: ${fmt.format(Date(completedAt))}"

        val task = TaskEntity(
            id = 7L,
            title = "Revise OS Deadlocks",
            description = "Cover Banker's algorithm and resource allocation graphs",
            isCompleted = true,
            createdAt = createdAt,
            completedAt = completedAt
        )

        composeRule.setContent {
            MaterialTheme {
                TaskItemRow(
                    task = task,
                    onToggle = {},
                    onDelete = {}
                )
            }
        }

        composeRule.onNodeWithText("Revise OS Deadlocks").assertIsDisplayed()
        composeRule.onNodeWithText("Cover Banker's algorithm and resource allocation graphs").assertIsDisplayed()
        composeRule.onNodeWithText(expectedCreated).assertIsDisplayed()
        composeRule.onNodeWithText(expectedCompleted).assertIsDisplayed()
    }

    @Test
    fun confirmationDialog_confirmsTaskOrHomeworkCompletion() {
        var confirmed = false
        composeRule.setContent {
            MaterialTheme {
                ConfirmationDialog(
                    title = "Mark Task Done?",
                    message = "Are you sure you have completed 'Submit Lab Report'?",
                    confirmButtonText = "Yes, Done",
                    dismissButtonText = "Not Yet",
                    isDestructive = false,
                    icon = Icons.Rounded.CheckCircle,
                    onConfirm = { confirmed = true },
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithText("Mark Task Done?").assertIsDisplayed()
        composeRule.onNodeWithText("Yes, Done").performClick()
        assertTrue(confirmed)
    }

    @Test
    fun roomMigration4To5_addsCompletedAtAndPersistsTaskTimestamps() {
        runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration_4_5_test.db"
        context.deleteDatabase(dbName)

        // Create a v4-style tasks table without completedAt
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(4) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS tasks (
                                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                title TEXT NOT NULL,
                                description TEXT NOT NULL,
                                subject TEXT NOT NULL,
                                isCompleted INTEGER NOT NULL,
                                priority TEXT NOT NULL,
                                dueDate TEXT NOT NULL,
                                dueTime TEXT NOT NULL,
                                startDate TEXT NOT NULL,
                                linkedAssignmentId INTEGER,
                                createdAt INTEGER NOT NULL
                            )
                            """.trimIndent()
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
        )
        val writable = helper.writableDatabase
        writable.execSQL(
            "INSERT INTO tasks (title, description, subject, isCompleted, priority, dueDate, dueTime, startDate, linkedAssignmentId, createdAt) VALUES ('Old Task', 'Old Desc', '', 0, 'Medium', 'Today', '5:00 PM', 'Today', NULL, 1000)"
        )
        writable.close()

        // Apply MIGRATION_4_5 on the raw SQLite database and verify column exists
        val raw = SQLiteDatabase.openDatabase(context.getDatabasePath(dbName).path, null, SQLiteDatabase.OPEN_READWRITE)
        raw.execSQL("ALTER TABLE tasks ADD COLUMN completedAt INTEGER")
        val cursor = raw.rawQuery("PRAGMA table_info(tasks)", null)
        val columns = mutableListOf<String>()
        while (cursor.moveToNext()) {
            columns.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
        }
        cursor.close()
        raw.close()
        assertTrue(columns.contains("completedAt"))

        // Also test in-memory Room DB insert & read with description, createdAt, completedAt
        val memDb = Room.inMemoryDatabaseBuilder(context, ArkinDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val id = memDb.taskDao().insertTask(
            TaskEntity(
                title = "DBMS Normalization",
                description = "Finish 3NF and BCNF examples",
                isCompleted = true,
                createdAt = 1764130000000L,
                completedAt = 1764133600000L
            )
        )
        val loaded = memDb.taskDao().getAllTasksSync().first { it.id == id }
        assertEquals("Finish 3NF and BCNF examples", loaded.description)
        assertEquals(1764130000000L, loaded.createdAt)
        assertEquals(1764133600000L, loaded.completedAt)
        memDb.close()
        context.deleteDatabase(dbName)
        }
    }

    @Test
    fun notificationHelper_postsReminderNotificationWithReminderId() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        NotificationHelper.createNotificationChannels(context)
        NotificationHelper.showReminderNotification(
            context = context,
            notificationId = 42,
            title = "Reminder: Fee Deadline",
            message = "Due: Today, 10:00 AM",
            reminderId = 42L
        )

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowNm = shadowOf(nm)
        val posted = shadowNm.allNotifications.firstOrNull()
        assertNotNull("Expected reminder notification to be posted", posted)
        val contentIntent = posted!!.contentIntent
        assertNotNull("Expected notification tap PendingIntent", contentIntent)
        val shadowPi = shadowOf(contentIntent)
        assertEquals(42L, shadowPi.savedIntent.getLongExtra("EXTRA_REMINDER_ID", -1L))
    }
}
