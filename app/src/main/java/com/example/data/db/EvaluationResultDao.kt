package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.EvaluationResult
import kotlinx.coroutines.flow.Flow

@Dao
interface EvaluationResultDao {
    @Query("SELECT * FROM evaluation_results ORDER BY score DESC, timestamp DESC")
    fun getAllEvaluations(): Flow<List<EvaluationResult>>

    @Query("SELECT * FROM evaluation_results WHERE candidateId = :candidateId ORDER BY timestamp DESC")
    fun getEvaluationsForCandidate(candidateId: Int): Flow<List<EvaluationResult>>

    @Query("SELECT * FROM evaluation_results WHERE jobTitle = :jobTitle AND requiredSkills = :skills AND minExperience = :minExp ORDER BY score DESC")
    fun getEvaluationsForCriteria(jobTitle: String, skills: String, minExp: Double): Flow<List<EvaluationResult>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvaluation(evaluation: EvaluationResult): Long

    @Delete
    suspend fun deleteEvaluation(evaluation: EvaluationResult)

    @Query("DELETE FROM evaluation_results")
    suspend fun clearAllEvaluations()

    @Query("DELETE FROM evaluation_results WHERE jobTitle = :jobTitle")
    suspend fun clearEvaluationsByJob(jobTitle: String)
}
