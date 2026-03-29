package com.sr.fixit106.utils

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.TextPart
import com.google.ai.client.generativeai.type.generationConfig

class GeminiApiClient(apiKey: String) {
    private val safetySettings = listOf(
        SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.MEDIUM_AND_ABOVE),
        SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.MEDIUM_AND_ABOVE)
    )

    private val generationConfig = generationConfig {
        temperature = 0.2f
        topK = 1
        topP = 0.8f
        maxOutputTokens = 128
    }

    private val model = GenerativeModel(
        modelName = "gemini-2.5-flash-lite",
        apiKey = apiKey,
        generationConfig = generationConfig,
        safetySettings = safetySettings,
    )

    suspend fun generateTags(title: String, description: String): List<String> {
        return try {
            val prompt = """
                You generate tags for municipal issue reports.

                Return ONLY short tags for this issue.
                Rules:
                - Return between 1 and 5 tags
                - Each tag must be 1 or 2 words only
                - No hashtags
                - No numbering
                - No explanations
                - Prefer plain category words like: sanitation, lighting, road, sidewalk, water leak, parking
                - Separate tags with commas only

                Title: $title
                Description: $description
            """.trimIndent()

            val content = Content(parts = listOf(TextPart(prompt)))
            val response = model.generateContent(content)
            val responseText = response.text.orEmpty()

            Log.d("GeminiApiClient", "Raw Gemini tags response: $responseText")

            parseTags(responseText)
        } catch (e: Exception) {
            Log.e("GeminiApiClient", "Failed to generate tags", e)
            emptyList()
        }
    }

    private fun parseTags(responseText: String): List<String> {
        if (responseText.isBlank()) return emptyList()

        return responseText
            .replace("\n", ",")
            .split(",")
            .map { it.trim() }
            .map { it.removePrefix("#").trim() }
            .map { it.removePrefix("-").trim() }
            .map { it.removePrefix("*").trim() }
            .map { raw ->
                raw.substringAfter(": ", raw).trim()
            }
            .filter { it.isNotBlank() }
            .map { normalizeTag(it) }
            .filter { it.isNotBlank() }
            .distinct()
            .take(5)
    }

    private fun normalizeTag(tag: String): String {
        return tag
            .trim()
            .removePrefix("\"")
            .removeSuffix("\"")
            .removePrefix("'")
            .removeSuffix("'")
            .trim()
    }
}