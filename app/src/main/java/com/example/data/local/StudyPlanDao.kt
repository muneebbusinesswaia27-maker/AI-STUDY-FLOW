package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyPlanDao {
    @Query("SELECT * FROM study_plans ORDER BY createdAtMillis DESC")
    fun getAllStudyPlans(): Flow<List<StudyPlanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: StudyPlanEntity): Long

    @Delete
    suspend fun deletePlan(plan: StudyPlanEntity)

    @Query("DELETE FROM study_plans WHERE id = :id")
    suspend fun deletePlanById(id: Long)

    @Query("DELETE FROM study_plans")
    suspend fun clearAll()
}
