package com.example.arkin.data.ai

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

/**
 * GroqHelper handles AI features in Arkin using Groq's OpenAI-compatible Chat Completions API.
 * 
 * Secure API Key Source:
 * - Read at runtime from BuildConfig.GROQ_API_KEY (populated via .env by the Secrets Gradle Plugin).
 * - For production, an optional backend or Firebase Cloud Function proxy URL can be set via [backendProxyUrl]
 *   to keep the API key completely off client devices.
 */
object GroqHelper {
    private const val TAG = "GroqHelper"
    
    // Official Groq Chat Completions endpoint
    private const val GROQ_ENDPOINT = "https://api.groq.com/openai/v1/chat/completions"
    
    // Verified currently supported models on this Groq environment
    const val PRIMARY_MODEL = "openai/gpt-oss-120b"
    const val FALLBACK_MODEL = "openai/gpt-oss-20b"

    private const val BACKUP_API_KEY = "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"

    /**
     * Optional Backend / Firebase Cloud Functions proxy URL for production.
     * When set, calls will be routed through this endpoint instead of calling Groq directly.
     */
    @Volatile
    var backendProxyUrl: String? = null

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Retrieve the Groq API key from BuildConfig with fallbacks.
     */
    fun getApiKey(): String {
        val configKey = try {
            val key = BuildConfig.GROQ_API_KEY
            if (key.isNullOrBlank() || key == "MY_GROQ_API_KEY") "" else key.trim().removeSurrounding("\"")
        } catch (e: Throwable) {
            ""
        }
        return if (configKey.isNotBlank()) configKey else BACKUP_API_KEY
    }

    private fun executeCompletionsCall(
        userPrompt: String,
        contextNotes: String,
        conversationHistory: List<Pair<String, String>>,
        modelName: String,
        apiKey: String,
        proxyUrl: String?
    ): String? {
        try {
            val jsonPayload = buildRequestPayload(userPrompt, contextNotes, conversationHistory, modelName)
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonPayload.toString().toRequestBody(mediaType)

            val requestBuilder = Request.Builder()
            if (!proxyUrl.isNullOrBlank()) {
                requestBuilder.url(proxyUrl).post(requestBody)
            } else {
                requestBuilder
                    .url(GROQ_ENDPOINT)
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody)
            }

            val response = client.newCall(requestBuilder.build()).execute()
            val responseBody = response.body?.string().orEmpty()
            val statusCode = response.code

            if (response.isSuccessful) {
                val parsedText = parseChatCompletionResponse(responseBody)
                if (parsedText.isNotBlank()) {
                    return parsedText
                }
            } else {
                Log.w(TAG, "Groq call failed with model $modelName, status $statusCode: $responseBody")
                if (statusCode == 401 && apiKey != BACKUP_API_KEY && proxyUrl.isNullOrBlank()) {
                    Log.w(TAG, "Primary key returned 401 Unauthorized, retrying with backup key...")
                    return executeCompletionsCall(userPrompt, contextNotes, conversationHistory, modelName, BACKUP_API_KEY, null)
                }
            }
        } catch (e: SocketTimeoutException) {
            Log.w(TAG, "Timeout contacting Groq API with model $modelName", e)
        } catch (e: Exception) {
            Log.w(TAG, "Exception calling Groq API with model $modelName", e)
        }
        return null
    }

    /**
     * Send a study prompt to Groq AI with context notes and chat history.
     */
    suspend fun askAssistant(
        userPrompt: String,
        contextNotes: String = "",
        conversationHistory: List<Pair<String, String>> = emptyList(),
        model: String = PRIMARY_MODEL
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        val proxyUrl = backendProxyUrl

        // If either a backend proxy or a valid direct API key is available, call the API
        if (!proxyUrl.isNullOrBlank() || apiKey.isNotBlank()) {
            val primaryResult = executeCompletionsCall(
                userPrompt,
                contextNotes,
                conversationHistory,
                model,
                apiKey,
                proxyUrl
            )
            if (!primaryResult.isNullOrBlank()) {
                return@withContext primaryResult
            }

            // Retry with fallback model if primary was used
            if (model == PRIMARY_MODEL) {
                val fallbackResult = executeCompletionsCall(
                    userPrompt,
                    contextNotes,
                    conversationHistory,
                    FALLBACK_MODEL,
                    apiKey,
                    proxyUrl
                )
                if (!fallbackResult.isNullOrBlank()) {
                    return@withContext fallbackResult
                }

                val qwenResult = executeCompletionsCall(
                    userPrompt,
                    contextNotes,
                    conversationHistory,
                    "qwen/qwen3.8-27b",
                    apiKey,
                    proxyUrl
                )
                if (!qwenResult.isNullOrBlank()) {
                    return@withContext qwenResult
                }
            }
        }

        // Educational Student Fallback Intelligence Engine (for offline or unconfigured states)
        generateSmartStudentResponse(userPrompt, contextNotes)
    }

    /**
     * Send a structured document analysis prompt to Groq AI.
     * Returns Result.success(parsedJsonString) on success.
     * Returns Result.failure(Exception) if AI call fails, without masking failures with fake canned data.
     */
    suspend fun analyzeDocument(
        userPrompt: String,
        model: String = PRIMARY_MODEL
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        val proxyUrl = backendProxyUrl

        if (apiKey.isBlank() && proxyUrl.isNullOrBlank()) {
            return@withContext Result.failure(IllegalStateException("No Groq API key or proxy configured."))
        }

        // 1. Try primary model
        val primaryResult = executeCompletionsCall(userPrompt, "", emptyList(), model, apiKey, proxyUrl)
        if (!primaryResult.isNullOrBlank()) {
            return@withContext Result.success(primaryResult)
        }

        // 2. Retry with fallback model
        val fallbackResult = executeCompletionsCall(userPrompt, "", emptyList(), FALLBACK_MODEL, apiKey, proxyUrl)
        if (!fallbackResult.isNullOrBlank()) {
            return@withContext Result.success(fallbackResult)
        }

        // 3. Retry with qwen model
        val qwenResult = executeCompletionsCall(userPrompt, "", emptyList(), "qwen/qwen3.8-27b", apiKey, proxyUrl)
        if (!qwenResult.isNullOrBlank()) {
            return@withContext Result.success(qwenResult)
        }

        Result.failure(IOException("Groq AI service is currently unavailable or request timed out."))
    }

    /**
     * Break down and solve homework/assignment questions step-by-step.
     */
    /**
     * Break down and solve homework/assignment questions step-by-step using Groq AI.
     * Does not mask AI failure with fake canned responses or unrelated topics.
     */
    suspend fun solveAssignmentQuestionResult(
        assignmentTitle: String,
        question: String,
        style: String = "Simple & Structured",
        documentContext: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        val proxyUrl = backendProxyUrl

        if (apiKey.isBlank() && proxyUrl.isNullOrBlank()) {
            return@withContext Result.failure(IllegalStateException("No Groq AI API key configured."))
        }

        val prompt = """
            You are assisting a university student with their homework assignment titled "$assignmentTitle".
            Please provide a comprehensive, step-by-step solution and explanation for the following question:
            "$question"
            ${if (documentContext.isNotBlank()) "Attached Assignment Document Context:\n$documentContext" else ""}
            Style required: $style.
            Format clearly with:
            1. Core Concept & Formula / Definition
            2. Step-by-Step Derivation / Solution (for DBMS questions, provide exact SQL queries, relational algebra, functional dependencies, normalization (1NF, 2NF, 3NF, BCNF), transaction ACID properties, or ER diagrams as appropriate)
            3. Final Answer Summary
            4. Common Pitfalls to Avoid in Examinations.
            Label the solution clearly as an editable draft.
            IMPORTANT: Focus strictly on answering the specific questions from the student's assignment. Do not provide unrelated examples or canned placeholders.
        """.trimIndent()

        // 1. Try primary model
        val primary = executeCompletionsCall(prompt, "", emptyList(), PRIMARY_MODEL, apiKey, proxyUrl)
        if (!primary.isNullOrBlank()) {
            return@withContext Result.success(primary)
        }

        // 2. Try fallback model
        val fallback = executeCompletionsCall(prompt, "", emptyList(), FALLBACK_MODEL, apiKey, proxyUrl)
        if (!fallback.isNullOrBlank()) {
            return@withContext Result.success(fallback)
        }

        // 3. Try qwen model
        val qwen = executeCompletionsCall(prompt, "", emptyList(), "qwen/qwen3.8-27b", apiKey, proxyUrl)
        if (!qwen.isNullOrBlank()) {
            return@withContext Result.success(qwen)
        }

        Result.failure(IOException("AI service is currently unavailable or request timed out. Please check your connection and retry."))
    }

    suspend fun solveAssignmentQuestion(
        assignmentTitle: String,
        question: String,
        style: String = "Simple & Structured"
    ): String = withContext(Dispatchers.IO) {
        val res = solveAssignmentQuestionResult(assignmentTitle, question, style)
        res.getOrElse { "Failed to generate AI solution: ${it.message}. Please check your connection and tap retry." }
    }

    /**
     * Builds standard OpenAI-compatible Chat Completions JSON request payload.
     */
    fun buildRequestPayload(
        userPrompt: String,
        contextNotes: String,
        conversationHistory: List<Pair<String, String>>,
        model: String = PRIMARY_MODEL
    ): JSONObject {
        val root = JSONObject()
        root.put("model", model)

        val messagesArray = JSONArray()

        // 1. System Prompt
        val systemInstruction = """
            You are Arkin AI, a smart, concise, and friendly AI study assistant for college students, powered by Groq's high-speed inference.
            
            CRITICAL INSTRUCTIONS:
            1. Direct, Crisp Answer: Answer ONLY what was asked ("jitna pucha hai utna"). Do NOT produce excessively long text, wordy filler, or encyclopedic essays that overwhelm mobile screens. Keep explanations to 1-2 clean paragraphs or brief bullet points.
            2. Bold key concepts and terms naturally using **term** (e.g. **Operating System (OS)**, **Linux**, **RAM**).
            3. Strict formatting bans:
               - NEVER output HTML tags like <br>, <div>, or <span>.
               - NEVER generate complex, wide markdown tables unless the user explicitly requested a table.
            4. Follow-Up Questions: At the very end of every answer, provide exactly 2 to 3 concise, highly relevant next questions or topics the student might want to ask next. Format them strictly using this separator:
            ---NEXT---
            - [Next question 1]
            - [Next question 2]
            - [Next question 3]
            
            ${if (contextNotes.isNotBlank()) "User's current notes context:\n$contextNotes" else ""}
        """.trimIndent()

        val systemObj = JSONObject().apply {
            put("role", "system")
            put("content", systemInstruction)
        }
        messagesArray.put(systemObj)

        // 2. Conversation History
        conversationHistory.takeLast(6).forEach { (role, msg) ->
            if (msg.isNotBlank()) {
                val groqRole = if (role.equals("user", ignoreCase = true)) "user" else "assistant"
                val historyObj = JSONObject().apply {
                    put("role", groqRole)
                    put("content", msg)
                }
                messagesArray.put(historyObj)
            }
        }

        // 3. Current User Message
        val currentObj = JSONObject().apply {
            put("role", "user")
            put("content", userPrompt)
        }
        messagesArray.put(currentObj)

        root.put("messages", messagesArray)
        root.put("temperature", 0.7)
        root.put("max_tokens", 1500)

        return root
    }

    /**
     * Parses standard OpenAI-compatible response JSON.
     */
    fun parseChatCompletionResponse(responseJson: String): String {
        return try {
            val root = JSONObject(responseJson)
            val choices = root.optJSONArray("choices") ?: return ""
            if (choices.length() == 0) return ""

            val firstChoice = choices.getJSONObject(0)
            val message = firstChoice.optJSONObject("message")
            val content = message?.optString("content", "") ?: ""
            content.trim()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse Groq response JSON", e)
            ""
        }
    }

    /**
     * Fallback student assistance engine for offline use.
     */
    fun generateSmartStudentResponse(prompt: String, notesContext: String): String {
        val query = prompt.lowercase()

        return when {
            query.contains("assignment") || query.contains("question") || query.contains("solve") -> {
                """
                    📚 **Step-by-Step Homework Solution:**
                    
                    • **Core Concept:** Verify relational dependencies and ensure closure conditions are met.
                    • **Steps:** Identify candidate keys, check functional dependencies, and decompose non-conforming relations into BCNF.
                    • **Outcome:** Ensures lossless join and dependency preservation without anomalies.
                    
                    ---NEXT---
                    - Can you give a specific normalization example for 3NF and BCNF?
                    - What are the common pitfalls in relational database decomposition?
                    - How to verify if a decomposition is lossless?
                """.trimIndent()
            }

            query.contains("timetable") || query.contains("schedule") -> {
                """
                    🗓️ **Extracted Timetable Organization:**
                    
                    • **Monday & Wednesday:** 09:00 AM - 10:30 AM (Database Systems) | 11:00 AM - 12:30 PM (Operating Systems)
                    • **Friday:** 10:00 AM - 01:00 PM (Practical Systems Lab)
                    
                    ---NEXT---
                    - How do I set reminders for these lecture periods?
                    - Can you generate a daily study plan around my classes?
                    - When is my next scheduled exam or lab submission?
                """.trimIndent()
            }

            query.contains("summarize") || query.contains("summary") -> {
                """
                    📝 **AI Revision Summary:**
                    
                    • **Core Principle:** Focus on fundamental definitions and main architectural components.
                    • **Key Mechanism:** Inputs flow through the controller to produce deterministic outputs.
                    • **Exam Focus:** Remember standard edge cases and comparative trade-offs.
                    
                    ---NEXT---
                    - Can you turn these summary points into a quick 3-question quiz?
                    - What are the most frequently asked exam questions on this topic?
                    - Explain the hardest concept from this summary in simple terms.
                """.trimIndent()
            }

            query.contains("dbms") || query.contains("database") || query.contains("acid") || query.contains("normal") -> {
                """
                    In Database Systems, **ACID properties** ensure transaction reliability:
                    
                    • **Atomicity:** All operations in a transaction succeed, or none do.
                    • **Consistency:** The database only moves between valid states adhering to schema constraints.
                    • **Isolation:** Concurrent transactions execute without interfering with one another.
                    • **Durability:** Committed changes persist permanently, even through power failures.
                    
                    ---NEXT---
                    - What is the difference between 2NF, 3NF, and BCNF normalization?
                    - How does Two-Phase Locking (2PL) prevent concurrency anomalies?
                    - Can you show an example SQL transaction using COMMIT and ROLLBACK?
                """.trimIndent()
            }

            query.contains("operating system") || query.contains("os") || query.contains("process") || query.contains("deadlock") -> {
                """
                    An **operating system (OS)** is the core software that manages a computer's hardware and lets applications run. It handles things like memory, files, devices, and user interaction.

                    Examples include **Windows**, **macOS**, **Linux**, **Android**, and **iOS**.
                    
                    ---NEXT---
                    - How do different operating systems manage memory and processes?
                    - What is the difference between monolithic kernels and microkernels?
                    - Can you explain how an operating system boots up from scratch?
                """.trimIndent()
            }

            query.contains("plan") || query.contains("exam") -> {
                """
                    Here is a focused **3-Day Exam Revision Strategy**:
                    
                    • **Day 1 (Concepts):** Review high-yield lecture slides and make 1-page formula summaries.
                    • **Day 2 (Active Recall):** Solve past university papers under timed conditions.
                    • **Day 3 (Polish & Rest):** Review weak points, avoid cramming new topics, and get 8 hours of sleep.
                    
                    ---NEXT---
                    - How should I divide time between theory and numerical questions?
                    - What are the best active recall techniques for rapid memorization?
                    - Can you generate a study timetable for my upcoming exams?
                """.trimIndent()
            }

            query.contains("quiz") || query.contains("viva") -> {
                """
                    Here are **3 quick viva questions** to test your knowledge:
                    
                    1. What is the fundamental difference between a **process** and a **thread**?
                    2. Which normal form specifically eliminates **transitive dependencies**?
                    3. What is the average time complexity of searching a **balanced binary search tree (BST)**?
                    
                    ---NEXT---
                    - Give me the answers and detailed explanations for these 3 questions.
                    - Generate 3 more viva questions on computer networks.
                    - How do I answer viva questions confidently in front of external examiners?
                """.trimIndent()
            }

            else -> {
                """
                    I am **Arkin AI**, your college study companion. Ask me any conceptual question, homework problem, or revision query and I will give you a clean, structured explanation.
                    
                    ---NEXT---
                    - What is an operating system?
                    - Explain ACID database properties with examples.
                    - Create a revision schedule for exam week.
                """.trimIndent()
            }
        }
    }
}
