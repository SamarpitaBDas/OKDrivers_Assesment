package com.example.okdrivers.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.okdrivers.data.local.entity.SafetyBaselineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SafetyBaselineDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(baseline: SafetyBaselineEntity)
    @Query("""
        SELECT * FROM safety_baselines
        WHERE driverId = :driverId
        AND vehicleId = :vehicleId
        LIMIT 1
    """)
    suspend fun getBaseline(
        driverId: String,
        vehicleId: String
    ): SafetyBaselineEntity?

    @Query("""
        SELECT * FROM safety_baselines
        WHERE driverId = :driverId
        AND vehicleId = :vehicleId
        LIMIT 1
    """)
    fun observeBaseline(
        driverId: String,
        vehicleId: String
    ): Flow<SafetyBaselineEntity?>
}