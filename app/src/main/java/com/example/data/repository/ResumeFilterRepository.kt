package com.example.data.repository

import com.example.BuildConfig
import com.example.data.db.CandidateDao
import com.example.data.db.EvaluationResultDao
import com.example.data.model.Candidate
import com.example.data.model.EvaluationResult
import com.example.data.network.Content
import com.example.data.network.GeminiRequest
import com.example.data.network.GenerationConfig
import com.example.data.network.Part
import com.example.data.network.RetrofitClient
import com.example.data.network.GeminiEvaluationListResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ResumeFilterRepository(
    private val candidateDao: CandidateDao,
    private val evaluationResultDao: EvaluationResultDao
) {
    val allCandidates: Flow<List<Candidate>> = candidateDao.getAllCandidates()
    val allEvaluations: Flow<List<EvaluationResult>> = evaluationResultDao.getAllEvaluations()

    suspend fun getCandidateById(id: Int): Candidate? = candidateDao.getCandidateById(id)

    suspend fun insertCandidate(candidate: Candidate): Long = candidateDao.insertCandidate(candidate)

    suspend fun updateCandidate(candidate: Candidate) = candidateDao.updateCandidate(candidate)

    suspend fun deleteCandidate(candidate: Candidate) = candidateDao.deleteCandidate(candidate)

    suspend fun deleteCandidateById(id: Int) = candidateDao.deleteCandidateById(id)

    suspend fun insertEvaluation(evaluation: EvaluationResult): Long =
        evaluationResultDao.insertEvaluation(evaluation)

    suspend fun clearAllEvaluations() = evaluationResultDao.clearAllEvaluations()

    suspend fun clearEvaluationsByJob(jobTitle: String) = evaluationResultDao.clearEvaluationsByJob(jobTitle)

    /**
     * Sends active candidates along with the filtering criteria to the Gemini AI API,
     * parses the structured evaluation response, and saves the matches to the database.
     * Use customApiKey if user enters one in the UI. Otherwise, fallback to BuildConfig.GEMINI_API_KEY.
     */
    suspend fun filterCandidatesWithGemini(
        candidates: List<Candidate>,
        jobTitle: String,
        requiredSkills: String,
        minExperience: Double,
        additionalNotes: String = "",
        customApiKey: String? = null
    ): List<EvaluationResult> = withContext(Dispatchers.IO) {
        val apiKey = if (!customApiKey.isNullOrBlank()) customApiKey else BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalArgumentException("API Key is missing or default placeholder! Please provide an API key in the configuration panel.")
        }
        if (candidates.isEmpty()) {
            return@withContext emptyList()
        }

        // 1. Construct the system Recruiter instructions
        val systemMessage = """
            You are an expert HR Talent Acquisition Specialist and Technical Recruiter.
            You must evaluate candidates carefully and rank them based on how well they fit the target criteria.
            Your analysis must be thorough, objective, and realistic. 
            Evaluate the Candidate's background details (skills list, title, experience years, resume texts) against the requirements.
            You MUST return a JSON object wrapping an array of evaluations, which matches EXACTLY this JSON structure:
            {
              "evaluations": [
                {
                  "candidateId": <integer candidate ID precisely matching input>,
                  "score": <integer between 0 and 100 representing percentage matching suitability>,
                  "matchedSkills": "<comma-separated list of actual skills they possess matching requirements>",
                  "missingSkills": "<comma-separated list of required skills not found in resume>",
                  "experienceAnalysis": "<brief explanation comparing candidate experience with min experience required>",
                  "summary": "<2-sentence comprehensive Recruiter verdict summary of strengths and deficiencies>"
                }
              ]
            }
            Do NOT include any markdown markup like ```json, comments, or extra text. Output ONLY the raw valid JSON string.
        """.trimIndent()

        // 2. Build the candidate list prompt details
        val candidPromptBuilder = StringBuilder()
        candidPromptBuilder.append("TARGET JOB EVALUATION CRITERIA:\n")
        candidPromptBuilder.append("- Position Title: $jobTitle\n")
        candidPromptBuilder.append("- Key Required Skills: $requiredSkills\n")
        candidPromptBuilder.append("- Minimum Experience Years: $minExperience\n")
        candidPromptBuilder.append("- Extra Recruiting Notes: $additionalNotes\n\n")
        candidPromptBuilder.append("CANDIDATES TO ANALYZE:\n")

        candidates.forEach { candidate ->
            candidPromptBuilder.append("----------------------------\n")
            candidPromptBuilder.append("Candidate ID: ${candidate.id}\n")
            candidPromptBuilder.append("Name: ${candidate.name}\n")
            candidPromptBuilder.append("Title: ${candidate.title}\n")
            candidPromptBuilder.append("Experience: ${candidate.experienceYears} Years\n")
            candidPromptBuilder.append("Skills declared: ${candidate.skills}\n")
            candidPromptBuilder.append("Resume Text:\n${candidate.resumeText}\n")
        }

        candidPromptBuilder.append("\nRun candidate evaluations and output the JSON response containing rankings for all candidates.")

        // 3. Assemble Gemini Request
        val request = GeminiRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = candidPromptBuilder.toString())))
            ),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.2
            ),
            systemInstruction = Content(parts = listOf(Part(text = systemMessage)))
        )

        // 4. Perform API call
        val serviceResponse = RetrofitClient.geminiService.generateContent(apiKey, request)
        val textResponse = serviceResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw IllegalStateException("Gemini API did not return any candidate content candidate evaluations. Try again.")

        // 5. Parse the extracted result with Moshi
        val cleanedText = cleanJsonText(textResponse)
        val parser = RetrofitClient.moshiParser.adapter(GeminiEvaluationListResponse::class.java)
        val listResponse = parser.fromJson(cleanedText) 
            ?: throw IllegalStateException("Could not parse JSON response from Gemini API: $cleanedText")

        // 6. Map parsed evaluations to Database entities and save them
        val evaluationEntities = listResponse.evaluations.map { jsonResult ->
            EvaluationResult(
                candidateId = jsonResult.candidateId,
                jobTitle = jobTitle,
                requiredSkills = requiredSkills,
                minExperience = minExperience,
                score = jsonResult.score,
                matchedSkills = jsonResult.matchedSkills,
                missingSkills = jsonResult.missingSkills,
                experienceAnalysis = jsonResult.experienceAnalysis,
                summary = jsonResult.summary
            )
        }

        // Save new evaluations to database
        evaluationEntities.forEach { entity ->
            evaluationResultDao.insertEvaluation(entity)
        }

        evaluationEntities
    }

    /**
     * Clean up potential markdown formatting code blocks like ```json ... ``` surrounding the response.
     */
    private fun cleanJsonText(rawText: String): String {
        var clean = rawText.trim()
        if (clean.startsWith("```json")) {
            clean = clean.removePrefix("```json")
            if (clean.endsWith("```")) {
                clean = clean.removeSuffix("```")
            }
        } else if (clean.startsWith("```")) {
            clean = clean.removePrefix("```")
            if (clean.endsWith("```")) {
                clean = clean.removeSuffix("```")
            }
        }
        return clean.trim()
    }
}
