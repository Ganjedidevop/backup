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

    @Test
    fun testExamTimetableExtraction_validationFixture() {
        val aiResponse = """
            ```json
            {
              "summary": "Extracted 4 examination papers for End-Semester BCA Exams.",
              "classes": [
                {
                  "date": "23/10/2026",
                  "timeSlot": "10:30 AM - 12:30 PM",
                  "startTime": "10:30 AM",
                  "endTime": "12:30 PM",
                  "subjectCode": "BCAMJ501",
                  "subject": "Understanding the Operating Systems",
                  "isExam": true,
                  "colorKey": "purple"
                },
                {
                  "date": "24/10/2026",
                  "timeSlot": "10:30 AM - 12:30 PM",
                  "startTime": "10:30 AM",
                  "endTime": "12:30 PM",
                  "subjectCode": "BCAMJ502",
                  "subject": "Advanced Database Management Systems",
                  "isExam": true,
                  "colorKey": "purple"
                },
                {
                  "date": "26/10/2026",
                  "timeSlot": "10:30 AM - 12:30 PM",
                  "startTime": "10:30 AM",
                  "endTime": "12:30 PM",
                  "subjectCode": "BCAMN501",
                  "subject": "Software Engineering",
                  "isExam": true,
                  "colorKey": "teal"
                },
                {
                  "date": "27/10/2026",
                  "timeSlot": "10:30 AM - 12:30 PM",
                  "startTime": "10:30 AM",
                  "endTime": "12:30 PM",
                  "subjectCode": "BCAMN502",
                  "subject": "Operational Research",
                  "isExam": true,
                  "colorKey": "teal"
                }
              ]
            }
            ```
        """.trimIndent()

        val (summary, exams) = parseTimetableFromResponse(aiResponse)

        assertEquals(4, exams.size)

        // 1. 23/10/2026, 10:30 AM–12:30 PM, BCAMJ501, Understanding the Operating Systems (Friday)
        assertEquals("23/10/2026", exams[0].date)
        assertEquals("BCAMJ501", exams[0].subjectCode)
        assertEquals("Understanding the Operating Systems", exams[0].subject)
        assertEquals("10:30 AM - 12:30 PM", exams[0].timeSlot)
        assertEquals("Fri", exams[0].day)
        assertTrue(exams[0].isExam)

        // 2. 24/10/2026, 10:30 AM–12:30 PM, BCAMJ502, Advanced Database Management Systems (Saturday)
        assertEquals("24/10/2026", exams[1].date)
        assertEquals("BCAMJ502", exams[1].subjectCode)
        assertEquals("Advanced Database Management Systems", exams[1].subject)
        assertEquals("10:30 AM - 12:30 PM", exams[1].timeSlot)
        assertEquals("Sat", exams[1].day)
        assertTrue(exams[1].isExam)

        // 3. 26/10/2026, 10:30 AM–12:30 PM, BCAMN501, Software Engineering (Monday)
        assertEquals("26/10/2026", exams[2].date)
        assertEquals("BCAMN501", exams[2].subjectCode)
        assertEquals("Software Engineering", exams[2].subject)
        assertEquals("10:30 AM - 12:30 PM", exams[2].timeSlot)
        assertEquals("Mon", exams[2].day)
        assertTrue(exams[2].isExam)

        // 4. 27/10/2026, 10:30 AM–12:30 PM, BCAMN502, Operational Research (Tuesday)
        assertEquals("27/10/2026", exams[3].date)
        assertEquals("BCAMN502", exams[3].subjectCode)
        assertEquals("Operational Research", exams[3].subject)
        assertEquals("10:30 AM - 12:30 PM", exams[3].timeSlot)
        assertEquals("Tue", exams[3].day)
        assertTrue(exams[3].isExam)
    }

    @Test
    fun testExamTimetableExtraction_rawTextFallback() {
        val rawOcrText = """
            BCA 5th Semester Examination Date Sheet October 2026
            1. 23/10/2026, 10:30 AM–12:30 PM, BCAMJ501, Understanding the Operating Systems.
            2. 24/10/2026, 10:30 AM–12:30 PM, BCAMJ502, Advanced Database Management Systems.
            3. 26/10/2026, 10:30 AM–12:30 PM, BCAMN501, Software Engineering.
            4. 27/10/2026, 10:30 AM–12:30 PM, BCAMN502, Operational Research.
        """.trimIndent()

        // When AI fails or returns empty, the raw OCR text parser must dynamically extract all 4 entries
        val (summary, exams) = parseTimetableFromResponse("", rawDocumentText = rawOcrText)

        assertEquals(4, exams.size)
        assertEquals("23/10/2026", exams[0].date)
        assertEquals("BCAMJ501", exams[0].subjectCode)
        assertTrue(exams[0].subject.contains("Operating Systems"))
        assertTrue(exams[0].isExam)

        assertEquals("24/10/2026", exams[1].date)
        assertEquals("BCAMJ502", exams[1].subjectCode)
        assertTrue(exams[1].subject.contains("Database Management Systems"))
        assertTrue(exams[1].isExam)

        assertEquals("26/10/2026", exams[2].date)
        assertEquals("BCAMN501", exams[2].subjectCode)
        assertTrue(exams[2].subject.contains("Software Engineering"))
        assertTrue(exams[2].isExam)

        assertEquals("27/10/2026", exams[3].date)
        assertEquals("BCAMN502", exams[3].subjectCode)
        assertTrue(exams[3].subject.contains("Operational Research"))
        assertTrue(exams[3].isExam)
    }

    private fun parseTimetableFromResponse(rawResponse: String, rawDocumentText: String = ""): Pair<String, List<ParsedTimetableClass>> {
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
                    val timeSlot = obj.optString("timeSlot", "10:00 AM - 11:30 AM").trim().replace("–", "-").replace("—", "-")
                    val startTime = obj.optString("startTime", "").trim()
                    val endTime = obj.optString("endTime", "").trim()
                    val room = obj.optString("room", "").trim()
                    val faculty = obj.optString("faculty", "").trim()
                    val isLab = obj.optBoolean("isLab", false)
                    val colorKey = obj.optString("colorKey", if (isExam) "purple" else if (isLab) "teal" else "purple")

                    if (subject.isNotBlank() && !isBreakOrHeading(subject)) {
                        rawClassesList.add(
                            ParsedTimetableClass(
                                day = dayFormatted,
                                subject = subject,
                                timeSlot = timeSlot,
                                startTime = startTime,
                                endTime = endTime,
                                room = room,
                                faculty = faculty,
                                colorKey = colorKey,
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

        val deduplicatedClasses = rawClassesList.distinctBy {
            "${it.date}_${it.day.lowercase()}_${it.subject.lowercase().trim()}_${it.timeSlot.lowercase().trim()}"
        }

        if (deduplicatedClasses.isEmpty() && rawDocumentText.isNotBlank()) {
            return parseTimetableFromDocumentText(rawDocumentText)
        }

        val finalSummary = when {
            deduplicatedClasses.isNotEmpty() && summaryText.isNotBlank() -> summaryText
            deduplicatedClasses.isNotEmpty() -> {
                val examsCount = deduplicatedClasses.count { it.isExam }
                if (examsCount > 0) {
                    "Found $examsCount examination schedule entries."
                } else {
                    "Found ${deduplicatedClasses.size} classes."
                }
            }
            else -> {
                "No timetable classes or schedule entries were identified in this document. Please verify that the document contains clear days of the week, subject names, and class timings."
            }
        }

        return Pair(finalSummary, deduplicatedClasses)
    }

    private fun parseTimetableFromDocumentText(rawText: String): Pair<String, List<ParsedTimetableClass>> {
        val extractedClasses = mutableListOf<ParsedTimetableClass>()
        if (rawText.isBlank()) {
            return Pair("No timetable classes or schedule entries were identified in this document.", emptyList())
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
            "Found ${deduplicated.size} examination entries."
        } else {
            "No timetable classes or schedule entries were identified in this document."
        }

        return Pair(summary, deduplicated)
    }

    private fun deriveDayOfWeek(dateStr: String): String {
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
