package com.example.arkin.util

/**
 * Validates exam subject names to accept genuine academic subjects and standard abbreviations
 * (e.g., "SST", "Science", "HTML", "Python", "Operating Systems", "C++") while rejecting
 * random keyboard mashing or dummy strings (e.g., "jjk", "snjdv", "jjsjdhd", "asdf").
 */
object ExamSubjectValidator {

    private val KNOWN_SUBJECT_ACRONYMS = setOf(
        "c", "c++", "c#", "r", "go",
        "ai", "ml", "dl", "os", "cn", "se", "cs", "it", "ds", "cg", "gk", "pe", "pt",
        "ui", "ux", "cd", "db", "js", "ts", "hr", "pr",
        "sst", "evs", "dsa", "oop", "coa", "toc", "iot", "nlp", "css", "sql", "php",
        "xml", "cad", "cam", "cnc", "cnn", "rnn", "llm", "hdl", "tcp",
        "html", "dbms", "rdbms", "oops", "java", "vhdl", "vlsi", "hvac", "bfsi",
        "mern", "mean", "rest", "json", "ajax", "jdbc", "http", "ospf", "unix", "perl",
        "ruby", "rust", "dart", "math", "chem", "phys", "econ", "mgmt", "acct", "stat", "comm",
        "bio", "eng", "pol", "geo", "his", "phy", "eco", "acc", "bst", "art"
    )

    private val ROMAN_NUMERALS = setOf("i", "ii", "iii", "iv", "v", "vi", "vii", "viii", "ix", "x")

    private val DUMMY_WORDS = setOf(
        "asdf", "qwer", "zxcv", "hjkl", "uiop", "fdsa", "rewq", "vcxz",
        "qwerty", "asdfgh", "zxcvbn", "abcdef", "abcd", "abc", "xyz",
        "test", "testing", "dummy", "sample", "example", "none", "nothing",
        "null", "nil", "na", "n/a", "temp", "xyz123", "abc123", "jjk"
    )

    private val KEYBOARD_MASH_RUNS = listOf(
        "qwert", "werty", "ertyu", "rtyui", "tyuio", "yuiop",
        "asdfg", "sdfgh", "dfghj", "fghjk", "ghjkl",
        "zxcvb", "xcvbn", "cvbnm",
        "poiu", "oiuy", "lkjh", "kjhg", "mnbv", "nbvc"
    )

    private val ALLOWED_CHARS_REGEX = Regex("^[a-zA-Z0-9\\s.,'&/+()\\-:#]+$")

    fun validateSubject(subject: String): ValidationResult {
        val trimmed = subject.trim().replace(Regex("\\s+"), " ")
        if (trimmed.isBlank()) {
            return ValidationResult(false, "Please enter an exam subject.")
        }
        if (!ALLOWED_CHARS_REGEX.matches(trimmed)) {
            return ValidationResult(false, "Subject contains invalid characters.")
        }
        if (!trimmed.any { it.isLetter() }) {
            return ValidationResult(false, "Subject must contain letters (e.g., Science, Python, SST, HTML).")
        }

        val normalizedWhole = trimmed.lowercase()
        if (normalizedWhole in KNOWN_SUBJECT_ACRONYMS) {
            return ValidationResult(true)
        }
        if (normalizedWhole in DUMMY_WORDS) {
            return ValidationResult(false, "Please enter a real subject name (e.g., SST, Science, HTML, Python).")
        }

        val tokens = trimmed.split(" ")
        var meaningfulTokenCount = 0

        for (rawToken in tokens) {
            val tokenLower = rawToken.lowercase().trim(',', '.', ':', ';', '(', ')', '-')
            if (tokenLower.isBlank()) continue

            if (tokenLower in DUMMY_WORDS || KEYBOARD_MASH_RUNS.any { tokenLower.contains(it) }) {
                return ValidationResult(false, "Please enter a valid academic subject (e.g., SST, Science, HTML, Python).")
            }

            if (tokenLower in KNOWN_SUBJECT_ACRONYMS || tokenLower in ROMAN_NUMERALS) {
                if (tokenLower in KNOWN_SUBJECT_ACRONYMS) meaningfulTokenCount++
                continue
            }

            val lettersOnly = tokenLower.filter { it.isLetter() }
            if (lettersOnly.isEmpty()) {
                // Numeric suffix like "101" or "2" is fine when paired with a valid subject word
                continue
            }

            // Reject 3+ identical consecutive letters (e.g. "aaa", "jjj", "xxx")
            if (Regex("([a-z])\\1\\1+").containsMatchIn(lettersOnly)) {
                return ValidationResult(false, "Please enter a real subject name (e.g., SST, Science, HTML, Python).")
            }

            // Single-word subject must be at least 3 letters unless in KNOWN_SUBJECT_ACRONYMS
            if (tokens.size == 1 && lettersOnly.length < 3) {
                return ValidationResult(false, "Subject is too short. Enter a valid subject like SST, Science, HTML, or Python.")
            }

            // Reject 3+ letter words with no vowels at all (catches "jjk", "snjdv", "jjsjdhd", "bcdf")
            if (lettersOnly.length >= 3 && !lettersOnly.any { it in "aeiouy" }) {
                return ValidationResult(false, "Invalid subject '$rawToken'. Enter a proper subject like SST, Science, HTML, or Python.")
            }

            // Reject gibberish consonant clusters (> 4 consecutive consonants, e.g. "jjsjdhd", "snjdve")
            if (hasLongConsonantRun(lettersOnly, maxRun = 4)) {
                return ValidationResult(false, "Invalid subject '$rawToken'. Enter a proper subject like SST, Science, HTML, or Python.")
            }

            // Reject very low vowel density on longer single words (e.g., "bcdfghja")
            if (lettersOnly.length >= 6) {
                val vowelCount = lettersOnly.count { it in "aeiouy" }
                if (vowelCount == 1 && lettersOnly.length >= 7) {
                    return ValidationResult(false, "Please enter a valid subject name (e.g., SST, Science, HTML, Python).")
                }
            }

            meaningfulTokenCount++
        }

        if (meaningfulTokenCount == 0) {
            return ValidationResult(false, "Please enter a valid academic subject (e.g., SST, Science, HTML, Python).")
        }

        return ValidationResult(true)
    }

    private fun hasLongConsonantRun(lettersOnly: String, maxRun: Int): Boolean {
        var run = 0
        for (ch in lettersOnly) {
            if (ch in "aeiouy") {
                run = 0
            } else {
                run++
                if (run > maxRun) return true
            }
        }
        return false
    }
}
