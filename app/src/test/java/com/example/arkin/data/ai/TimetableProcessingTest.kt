package com.example.arkin.data.ai

import com.example.arkin.model.ParsedTimetableClass
import com.example.arkin.util.DocumentTextExtractor
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TimetableProcessingTest {

    @Test
    fun testParseTimetableAiResponse_validMarkdownJson() {
        val aiResponse = """
            Here is the organized timetable schedule:
            ```json
            {
              "summary": "Weekly schedule consists of 4 core classes across Monday, Wednesday, and Friday.",
              "classes": [
                {
                  "day": "Monday",
                  "subject": "Distributed Systems",
                  "timeSlot": "09:00 AM - 10:30 AM",
                  "startTime": "09:00 AM",
                  "endTime": "10:30 AM",
                  "room": "Room 401",
                  "faculty": "Prof. Rao",
                  "isLab": false,
                  "colorKey": "purple"
                },
                {
                  "day": "Mon",
                  "subject": "Lunch Break",
                  "timeSlot": "12:30 PM - 01:30 PM",
                  "room": "Cafeteria",
                  "isLab": false
                },
                {
                  "day": "Wednesday",
                  "subject": "Database Systems Lab",
                  "timeSlot": "10:00 AM - 12:00 PM",
                  "startTime": "10:00 AM",
                  "endTime": "12:00 PM",
                  "room": "Lab 2",
                  "isLab": true,
                  "colorKey": "teal"
                },
                {
                  "day": "Friday",
                  "subject": "Computer Networks",
                  "timeSlot": "02:00 PM - 03:30 PM",
                  "startTime": "02:00 PM",
                  "endTime": "03:30 PM",
                  "room": "Hall B",
                  "isLab": false,
                  "colorKey": "blue"
                },
                {
                  "day": "Friday",
                  "subject": "Recess / Free Period",
                  "timeSlot": "03:30 PM - 04:00 PM",
                  "room": "",
                  "isLab": false
                }
              ]
            }
            ```
        """.trimIndent()

        val parsed = parseTimetableFromResponse(aiResponse)
        val classes = parsed.second

        // "Lunch Break" and "Recess / Free Period" must be excluded
        assertEquals(3, classes.size)
        assertEquals("Mon", classes[0].day)
        assertEquals("Distributed Systems", classes[0].subject)
        assertEquals("Wed", classes[1].day)
        assertEquals("Database Systems Lab", classes[1].subject)
        assertTrue(classes[1].isLab)
        assertEquals("Fri", classes[2].day)
        assertEquals("Computer Networks", classes[2].subject)
    }

    @Test
    fun testParseTimetableAiResponse_deduplicatesIdenticalEntries() {
        val json = """
            {
              "summary": "Schedule with duplicates",
              "classes": [
                {
                  "day": "Mon",
                  "subject": "Operating Systems",
                  "timeSlot": "09:00 AM - 10:30 AM",
                  "room": "Room 101"
                },
                {
                  "day": "Mon",
                  "subject": "Operating Systems",
                  "timeSlot": "09:00 AM - 10:30 AM",
                  "room": "Room 101"
                },
                {
                  "day": "Tue",
                  "subject": "Compiler Design",
                  "timeSlot": "11:00 AM - 12:30 PM",
                  "room": "Room 202"
                }
              ]
            }
        """.trimIndent()

        val parsed = parseTimetableFromResponse(json)
        val classes = parsed.second

        // The duplicate Operating Systems class on Monday should be deduplicated to 1
        assertEquals(2, classes.size)
        assertEquals("Operating Systems", classes[0].subject)
        assertEquals("Compiler Design", classes[1].subject)
    }

    @Test
    fun testParseTimetableAiResponse_emptyOrNonJsonHandledSafely() {
        // Non-JSON response (e.g. conversational text without schedule)
        val plainText = "I could not find any schedule in the image provided."
        val parsed = parseTimetableFromResponse(plainText)

        assertEquals(0, parsed.second.size)
        assertTrue(parsed.first.contains("No timetable classes"))

        // Malformed JSON
        val malformed = "{ this is not valid json }"
        val parsedMalformed = parseTimetableFromResponse(malformed)
        assertEquals(0, parsedMalformed.second.size)
        assertTrue(parsedMalformed.first.contains("No timetable classes"))
    }

    @Test
    fun testDocumentTextExtractor_supportedFormatsValidation() {
        // Supported formats
        assertTrue(DocumentTextExtractor.isSupportedFile("timetable.pdf", "application/pdf"))
        assertTrue(DocumentTextExtractor.isSupportedFile("schedule.png", "image/png"))
        assertTrue(DocumentTextExtractor.isSupportedFile("photo.jpg", "image/jpeg"))
        assertTrue(DocumentTextExtractor.isSupportedFile("scan.webp", "image/webp"))
        assertTrue(DocumentTextExtractor.isSupportedFile("notes.txt", "text/plain"))
        assertTrue(DocumentTextExtractor.isSupportedFile("data.csv", "text/csv"))

        // Unsupported formats
        assertFalse(DocumentTextExtractor.isSupportedFile("document.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
        assertFalse(DocumentTextExtractor.isSupportedFile("archive.zip", "application/zip"))
        assertFalse(DocumentTextExtractor.isSupportedFile("lecture.mp4", "video/mp4"))
        assertFalse(DocumentTextExtractor.isSupportedFile("app.exe", "application/octet-stream"))
    }

    private fun parseTimetableFromResponse(rawResponse: String): Pair<String, List<ParsedTimetableClass>> {
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

            val arr = json.optJSONArray("classes")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val dayRaw = obj.optString("day", "Mon").trim()
                    val dayFormatted = when (dayRaw.lowercase().take(3)) {
                        "mon" -> "Mon"
                        "tue" -> "Tue"
                        "wed" -> "Wed"
                        "thu" -> "Thu"
                        "fri" -> "Fri"
                        "sat" -> "Sat"
                        "sun" -> "Sun"
                        else -> "Mon"
                    }
                    val subject = obj.optString("subject", "").trim()
                    val timeSlot = obj.optString("timeSlot", "10:00 AM - 11:30 AM").trim()
                    val room = obj.optString("room", "").trim()
                    val faculty = obj.optString("faculty", "").trim()
                    val isLab = obj.optBoolean("isLab", false)
                    val colorKey = obj.optString("colorKey", if (isLab) "teal" else "purple")

                    if (subject.isNotBlank() && !isBreakOrHeading(subject)) {
                        rawClassesList.add(
                            ParsedTimetableClass(
                                day = dayFormatted,
                                subject = subject,
                                timeSlot = timeSlot,
                                room = room,
                                faculty = faculty,
                                colorKey = colorKey,
                                isLab = isLab
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {
            summaryText = ""
        }

        val deduplicatedClasses = rawClassesList.distinctBy {
            "${it.day.lowercase()}_${it.subject.lowercase().trim()}_${it.timeSlot.lowercase().trim()}"
        }

        val finalSummary = when {
            deduplicatedClasses.isNotEmpty() && summaryText.isNotBlank() -> summaryText
            deduplicatedClasses.isNotEmpty() -> {
                "Found ${deduplicatedClasses.size} classes."
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
            "lunch", "break", "recess", "interval", "tea break", "free period",
            "heading", "timetable", "schedule", "period", "class", "day", "time"
        )
        return nonClassKeywords.any { s == it || s.startsWith("$it ") || s.endsWith(" $it") } ||
                !subject.any { it.isLetter() }
    }
}
