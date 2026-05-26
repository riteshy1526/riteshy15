package com.example.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<Content>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val responseMimeType: String? = null,
    val temperature: Double? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<CandidateWrapper>? = null
)

@JsonClass(generateAdapter = true)
data class CandidateWrapper(
    val content: Content? = null
)

// --- Match Result Models returned by Gemini JSON ---

@JsonClass(generateAdapter = true)
data class CandidateEvaluationJson(
    val candidateId: Int,
    val score: Int, // Overall ranking score (0-100)
    val matchedSkills: String, // Comma-separated matched skills
    val missingSkills: String, // Comma-separated missing skills
    val experienceAnalysis: String, // Detailed explanation of experience score
    val summary: String // General feedback summary
)

@JsonClass(generateAdapter = true)
data class GeminiEvaluationListResponse(
    val evaluations: List<CandidateEvaluationJson>
)
