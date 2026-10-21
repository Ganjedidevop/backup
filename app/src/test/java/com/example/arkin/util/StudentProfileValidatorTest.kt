package com.example.arkin.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudentProfileValidatorTest {

    @Test
    fun testCollegeValidation_rejectsEmptyAndWhitespace() {
        assertFalse(StudentProfileValidator.validateCollege("").isValid)
        assertFalse(StudentProfileValidator.validateCollege("   ").isValid)
        assertFalse(StudentProfileValidator.validateCollege("\t\n").isValid)
    }

    @Test
    fun testCollegeValidation_rejectsSymbolsAndDigitsOnly() {
        assertFalse(StudentProfileValidator.validateCollege("!@#$%^&*()").isValid)
        assertFalse(StudentProfileValidator.validateCollege("----").isValid)
        assertFalse(StudentProfileValidator.validateCollege("123456").isValid)
        assertFalse(StudentProfileValidator.validateCollege("ab").isValid) // Too short (<3 chars)
        assertFalse(StudentProfileValidator.validateCollege("?!!").isValid)
    }

    @Test
    fun testCollegeValidation_acceptsLegitimateCollegeNames() {
        // Valid Indian colleges & universities
        assertTrue(StudentProfileValidator.validateCollege("IIT Delhi").isValid)
        assertTrue(StudentProfileValidator.validateCollege("BITS Pilani, Goa Campus").isValid)
        assertTrue(StudentProfileValidator.validateCollege("St. Xavier's College").isValid)
        assertTrue(StudentProfileValidator.validateCollege("Delhi University").isValid)
        assertTrue(StudentProfileValidator.validateCollege("National Institute of Technology, Trichy").isValid)
        assertTrue(StudentProfileValidator.validateCollege("SRM Institute of Science and Technology").isValid)
        assertTrue(StudentProfileValidator.validateCollege("Manipal Academy of Higher Education (MAHE)").isValid)
    }

    @Test
    fun testProgramValidation_commonAndCustomPrograms() {
        assertTrue(StudentProfileValidator.validateProgram("B.Tech").isValid)
        assertTrue(StudentProfileValidator.validateProgram("BCA").isValid)
        assertTrue(StudentProfileValidator.validateProgram("MCA").isValid)
        assertTrue(StudentProfileValidator.validateProgram("MBA").isValid)
        assertTrue(StudentProfileValidator.validateProgram("B.Sc").isValid)

        // Custom legitimate programs (under 'Other')
        assertTrue(StudentProfileValidator.validateProgram("MBBS").isValid)
        assertTrue(StudentProfileValidator.validateProgram("LLB").isValid)
        assertTrue(StudentProfileValidator.validateProgram("Bachelor of Design").isValid)

        // Invalid custom programs
        assertFalse(StudentProfileValidator.validateProgram("").isValid)
        assertFalse(StudentProfileValidator.validateProgram("   ").isValid)
        assertFalse(StudentProfileValidator.validateProgram("!@#$").isValid)
        assertFalse(StudentProfileValidator.validateProgram("123").isValid)
    }

    @Test
    fun testCourseDurationAndMaxSemesters() {
        assertEquals(8, StudentProfileValidator.getMaxSemesters("B.Tech"))
        assertEquals(8, StudentProfileValidator.getMaxSemesters("Computer Science Engineering"))
        assertEquals(4, StudentProfileValidator.getMaxSemesters("MCA"))
        assertEquals(4, StudentProfileValidator.getMaxSemesters("MBA"))
        assertEquals(4, StudentProfileValidator.getMaxSemesters("M.Tech"))
        assertEquals(6, StudentProfileValidator.getMaxSemesters("BCA"))
        assertEquals(6, StudentProfileValidator.getMaxSemesters("BBA"))
        assertEquals(6, StudentProfileValidator.getMaxSemesters("B.Sc"))
        assertEquals(6, StudentProfileValidator.getMaxSemesters("B.Com"))
        assertEquals(6, StudentProfileValidator.getMaxSemesters("BA"))
        assertEquals(10, StudentProfileValidator.getMaxSemesters("Other"))
        assertEquals(10, StudentProfileValidator.getMaxSemesters("Custom Dual Degree"))
    }

    @Test
    fun testSemesterValidation_againstCourseDuration() {
        // Valid semesters
        assertTrue(StudentProfileValidator.validateSemester("Semester 4", "B.Tech").isValid)
        assertTrue(StudentProfileValidator.validateSemester("Semester 8", "B.Tech").isValid)
        assertTrue(StudentProfileValidator.validateSemester("4", "MCA").isValid)
        assertTrue(StudentProfileValidator.validateSemester("Semester 6", "BCA").isValid)

        // Out-of-bounds semesters
        assertFalse(StudentProfileValidator.validateSemester("Semester 5", "MCA").isValid) // MCA is max 4 sems
        assertFalse(StudentProfileValidator.validateSemester("Semester 6", "MBA").isValid) // MBA is max 4 sems
        assertFalse(StudentProfileValidator.validateSemester("Semester 9", "B.Tech").isValid) // B.Tech is max 8 sems
        assertFalse(StudentProfileValidator.validateSemester("Semester 0", "B.Tech").isValid)
        assertFalse(StudentProfileValidator.validateSemester("Semester -1", "B.Tech").isValid)
        assertFalse(StudentProfileValidator.validateSemester("", "B.Tech").isValid)
        assertFalse(StudentProfileValidator.validateSemester("RandomText", "B.Tech").isValid)
    }

    @Test
    fun testAcademicYearValidation() {
        // Valid formats
        assertTrue(StudentProfileValidator.validateAcademicYear("2026-2027").isValid)
        assertTrue(StudentProfileValidator.validateAcademicYear("2024-2025").isValid)
        assertTrue(StudentProfileValidator.validateAcademicYear("2025-2026").isValid)

        // Invalid formats & malformed values
        assertFalse(StudentProfileValidator.validateAcademicYear("").isValid)
        assertFalse(StudentProfileValidator.validateAcademicYear("   ").isValid)
        assertFalse(StudentProfileValidator.validateAcademicYear("random").isValid)
        assertFalse(StudentProfileValidator.validateAcademicYear("ABCD-EFGH").isValid)
        assertFalse(StudentProfileValidator.validateAcademicYear("2026").isValid)
        assertFalse(StudentProfileValidator.validateAcademicYear("2024-2024").isValid) // End year must be start + 1
        assertFalse(StudentProfileValidator.validateAcademicYear("2026-2025").isValid) // Reversed
        assertFalse(StudentProfileValidator.validateAcademicYear("2024-2026").isValid) // Gap of 2
        assertFalse(StudentProfileValidator.validateAcademicYear("1990-1991").isValid) // Out of range (<2000)
    }

    @Test
    fun testDepartmentValidation() {
        // Legitimate departments
        assertTrue(StudentProfileValidator.validateMajor("Computer Science & Engineering").isValid)
        assertTrue(StudentProfileValidator.validateMajor("Information Technology").isValid)
        assertTrue(StudentProfileValidator.validateMajor("Finance & Banking").isValid)
        assertTrue(StudentProfileValidator.validateMajor("AI & Data Science").isValid)
        assertTrue(StudentProfileValidator.validateMajor("Physics").isValid)

        // Invalid / symbol-only / empty
        assertFalse(StudentProfileValidator.validateMajor("").isValid)
        assertFalse(StudentProfileValidator.validateMajor("   ").isValid)
        assertFalse(StudentProfileValidator.validateMajor("!@#$%").isValid)
        assertFalse(StudentProfileValidator.validateMajor("12345").isValid)
        assertFalse(StudentProfileValidator.validateMajor("a").isValid) // < 2 chars
    }
}
