package com.example.data.db

import androidx.room.*
import com.example.data.model.Candidate
import kotlinx.coroutines.flow.Flow

@Dao
interface CandidateDao {
    @Query("SELECT * FROM candidates ORDER BY createdAt DESC")
    fun getAllCandidates(): Flow<List<Candidate>>

    @Query("SELECT * FROM candidates WHERE id = :id")
    suspend fun getCandidateById(id: Int): Candidate?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCandidate(candidate: Candidate): Long

    @Update
    suspend fun updateCandidate(candidate: Candidate)

    @Delete
    suspend fun deleteCandidate(candidate: Candidate)

    @Query("DELETE FROM candidates WHERE id = :id")
    suspend fun deleteCandidateById(id: Int)
}
