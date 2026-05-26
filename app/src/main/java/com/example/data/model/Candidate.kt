package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "candidates")
data class Candidate(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val title: String,
    val email: String = "",
    val phone: String = "",
    val experienceYears: Double,
    val skills: String, // Comma-separated or free form list
    val resumeText: String, // Full resume content
    val createdAt: Long = System.currentTimeMillis()
)
