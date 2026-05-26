package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "evaluation_results")
data class EvaluationResult(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val candidateId: Int,
    val jobTitle: String,
    val requiredSkills: String,
    val minExperience: Double,
    val score: Int, // 0 - 100 percentage score
    val matchedSkills: String, // Comma-separated matching skills
    val missingSkills: String, // Comma-separated missing skills
    val experienceAnalysis: String, // Gemini text explaining experience matching
    val summary: String, // Overall feedback summary
    val timestamp: Long = System.currentTimeMillis()
)
