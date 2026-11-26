package com.example.arkin.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExamSubjectValidatorTest {

    @Test
    fun validateSubject_acceptsRealSubjectsAndAbbreviations() {
        val validSubjects = listOf(
            "SST",
            "sst",
            "Science",
            "HTML",
            "html",
            "Python",
            "Operating Systems",
            "Data Structures & Algorithms",
            "C++",
            "C#",
            "DBMS",
            "Computer Networks",
            "AI",
            "ML",
            "Java",
            "Mathematics II",
            "Physics 101",
            "Social Studies",
            "EVS"
        )
        validSubjects.forEach { subject ->
            val result = ExamSubjectValidator.validateSubject(subject)
            assertTrue("Expected '$subject' to be valid, got: ${result.errorMessage}", result.isValid)
        }
    }

    @Test
    fun validateSubject_rejectsDummyAndGibberishInputs() {
        val invalidSubjects = listOf(
            "",
            "   ",
            "jjk",
            "snjdv",
            "jjsjdhd",
            "asdf",
            "qwer",
            "zxcv",
            "qwerty",
            "aaaa",
            "xyz",
            "test",
            "dummy",
            "12345",
            "ab",
            "bcdfg"
        )
        invalidSubjects.forEach { subject ->
            val result = ExamSubjectValidator.validateSubject(subject)
            assertFalse("Expected '$subject' to be rejected", result.isValid)
        }
    }

    @Test
    fun parseDateTimeToEpoch_handlesNarrowNoBreakSpaceAndStandardFormats() {
        val epoch1 = AlarmScheduler.parseDateTimeToEpoch("26/11/2026, 10:10\u202Fam")
        val epoch2 = AlarmScheduler.parseDateTimeToEpoch("26/11/2026, 10:10 AM")
        assertNotNull("Expected narrow no-break space datetime to parse", epoch1)
        assertNotNull("Expected standard datetime to parse", epoch2)
    }
}
