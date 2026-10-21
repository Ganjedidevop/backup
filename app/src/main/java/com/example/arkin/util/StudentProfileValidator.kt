package com.example.arkin.util

data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)

object StudentProfileValidator {

    val COMMON_PROGRAMS = listOf(
        "BCA", "BBA", "B.Tech", "B.Sc", "B.Com", "BA", "MCA", "MBA", "M.Tech"
    )

    fun getMaxSemesters(program: String): Int {
        val p = program.trim().uppercase()
        return when {
            p == "B.TECH" || p.contains("B.TECH") || p.contains("BTECH") || p.contains("ENGINEERING") -> 8
            p in listOf("MCA", "MBA", "M.TECH") || p.contains("MCA") || p.contains("MBA") || p.contains("M.TECH") -> 4
            p in listOf("BCA", "BBA", "B.SC", "B.COM", "BA") -> 6
            else -> 10 // Safe fallback for other programs (e.g., dual degrees, medical, law, custom)
        }
    }

    fun getDepartmentOptions(program: String): List<String> {
        val p = program.trim().uppercase()
        return when {
            p.contains("B.TECH") || p.contains("M.TECH") || p.contains("ENGINEERING") -> listOf(
                "Computer Science & Engineering",
                "Information Technology",
                "Artificial Intelligence & Machine Learning",
                "Data Science",
                "Electronics & Communication",
                "Mechanical Engineering",
                "Civil Engineering",
                "Electrical & Electronics"
            )
            p.contains("BCA") || p.contains("MCA") -> listOf(
                "Computer Applications",
                "Software Development",
                "Cloud Computing & DevOps",
                "Cyber Security",
                "Data Analytics",
                "Artificial Intelligence"
            )
            p.contains("BBA") || p.contains("MBA") -> listOf(
                "Finance & Banking",
                "Marketing Management",
                "Human Resource Management",
                "International Business",
                "Operations & Supply Chain",
                "Business Analytics"
            )
            p.contains("B.SC") -> listOf(
                "Computer Science",
                "Mathematics",
                "Physics",
                "Chemistry",
                "Biotechnology",
                "Statistics"
            )
            p.contains("B.COM") -> listOf(
                "Accounting & Finance",
                "Banking & Insurance",
                "Taxation",
                "Financial Markets"
            )
            p.contains("BA") -> listOf(
                "English Literature",
                "Economics",
                "Psychology",
                "Political Science",
                "History",
                "Journalism & Mass Comm"
            )
            else -> listOf(
                "Computer Science",
                "Business Administration",
                "General Studies",
                "Information Technology"
            )
        }
    }

    /**
     * Validate college / university name:
     * - Trim leading and trailing whitespace.
     * - Reject empty, whitespace-only, symbol-only, and clearly malformed values.
     * - Accept legitimate names containing letters, numbers, spaces, and punctuation (.,'-&()).
     */
    fun validateCollege(college: String): ValidationResult {
        val trimmed = college.trim()
        if (trimmed.isBlank()) {
            return ValidationResult(false, "College / University name cannot be empty.")
        }
        if (trimmed.length < 3) {
            return ValidationResult(false, "College name must be at least 3 characters long.")
        }
        // Must contain at least one letter
        val hasLetter = trimmed.any { it.isLetter() }
        if (!hasLetter) {
            return ValidationResult(false, "College name must contain letters.")
        }
        // Reject if consists only of symbols and numbers
        val validCharsRegex = Regex("^[a-zA-Z0-9\\s.,'\\-&()]+$")
        if (!validCharsRegex.matches(trimmed)) {
            return ValidationResult(false, "College name contains invalid characters.")
        }
        // Count letters vs symbols - must be predominantly legitimate text
        val letterCount = trimmed.count { it.isLetter() }
        if (letterCount < 2) {
            return ValidationResult(false, "Please enter a valid college or university name.")
        }
        return ValidationResult(true)
    }

    /**
     * Validate program / degree name:
     * - If "Other" or custom: must not be empty or symbol-only, must contain letters.
     */
    fun validateProgram(program: String): ValidationResult {
        val trimmed = program.trim()
        if (trimmed.isBlank()) {
            return ValidationResult(false, "Degree / Program cannot be empty.")
        }
        if (trimmed.length < 2) {
            return ValidationResult(false, "Program name must be at least 2 characters long.")
        }
        if (!trimmed.any { it.isLetter() }) {
            return ValidationResult(false, "Program name must contain letters.")
        }
        val validCharsRegex = Regex("^[a-zA-Z0-9\\s.,'\\-&()]+$")
        if (!validCharsRegex.matches(trimmed)) {
            return ValidationResult(false, "Program name contains invalid characters.")
        }
        return ValidationResult(true)
    }

    /**
     * Extract semester number from string like "Semester 4", "Sem 4", "4".
     */
    fun extractSemesterNumber(semesterStr: String): Int? {
        val trimmed = semesterStr.trim()
        val regex = Regex("(?i)(?:sem(?:ester)?)?\\s*(-?\\d+)")
        val match = regex.find(trimmed) ?: return null
        return match.groupValues[1].toIntOrNull()
    }

    /**
     * Validate semester against course duration.
     */
    fun validateSemester(semesterStr: String, program: String): ValidationResult {
        val trimmed = semesterStr.trim()
        if (trimmed.isBlank()) {
            return ValidationResult(false, "Please select a semester.")
        }
        val semNumber = extractSemesterNumber(trimmed)
            ?: return ValidationResult(false, "Invalid semester. Please select a valid semester.")

        val maxSem = getMaxSemesters(program)
        if (semNumber < 1 || semNumber > maxSem) {
            return ValidationResult(false, "Semester $semNumber is out of range for this program (1-$maxSem).")
        }
        return ValidationResult(true)
    }

    /**
     * Validate academic year format (e.g. 2026-2027):
     * - Must match YYYY-YYYY format
     * - Ending year must equal starting year + 1
     * - Starting year in realistic range
     */
    fun validateAcademicYear(academicYear: String): ValidationResult {
        val trimmed = academicYear.trim()
        if (trimmed.isBlank()) {
            return ValidationResult(false, "Academic year cannot be empty.")
        }
        val regex = Regex("^(\\d{4})\\s*-\\s*(\\d{4})$")
        val match = regex.matchEntire(trimmed)
            ?: return ValidationResult(false, "Format must be YYYY-YYYY (e.g. 2026-2027).")

        val startYear = match.groupValues[1].toIntOrNull() ?: 0
        val endYear = match.groupValues[2].toIntOrNull() ?: 0

        if (startYear < 2000 || startYear > 2099) {
            return ValidationResult(false, "Start year must be between 2000 and 2099.")
        }
        if (endYear != startYear + 1) {
            return ValidationResult(false, "Ending year must immediately follow start year (e.g. $startYear-${startYear + 1}).")
        }
        return ValidationResult(true)
    }

    /**
     * Validate department / specialization:
     * - Must not be empty
     * - Must contain letters
     * - Reject symbol-only or numbers-only
     */
    fun validateMajor(major: String): ValidationResult {
        val trimmed = major.trim()
        if (trimmed.isBlank()) {
            return ValidationResult(false, "Department / Specialization cannot be empty.")
        }
        if (trimmed.length < 2) {
            return ValidationResult(false, "Department name must be at least 2 characters.")
        }
        if (!trimmed.any { it.isLetter() }) {
            return ValidationResult(false, "Department name must contain letters.")
        }
        val validCharsRegex = Regex("^[a-zA-Z0-9\\s.,'\\-&()]+$")
        if (!validCharsRegex.matches(trimmed)) {
            return ValidationResult(false, "Department name contains invalid characters.")
        }
        return ValidationResult(true)
    }

    /**
     * Format academic year string consistently (e.g. "2026-2027").
     */
    fun formatAcademicYear(startYear: Int): String {
        return "$startYear-${startYear + 1}"
    }
}
