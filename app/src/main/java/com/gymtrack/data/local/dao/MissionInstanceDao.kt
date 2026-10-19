package com.gymtrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gymtrack.data.local.entity.MissionInstanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MissionInstanceDao {

    @Query("SELECT * FROM mission_instances ORDER BY startedAtMillis DESC, id DESC")
    fun observeAll(): Flow<List<MissionInstanceEntity>>

    @Query("SELECT * FROM mission_instances")
    fun getAllOnce(): List<MissionInstanceEntity>

    @Query(
        "SELECT * FROM mission_instances WHERE definitionId = :definitionId AND periodKey = :periodKey LIMIT 1",
    )
    fun getByDefinitionAndPeriod(definitionId: String, periodKey: String): MissionInstanceEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIgnore(entity: MissionInstanceEntity): Long

    @Query(
        """
        UPDATE mission_instances
        SET status = :status, completedAtMillis = :completedAtMillis
        WHERE definitionId = :definitionId AND periodKey = :periodKey AND status = :activeStatus
        """,
    )
    fun markCompletedIfActive(
        definitionId: String,
        periodKey: String,
        status: String,
        completedAtMillis: Long,
        activeStatus: String = MissionInstanceEntity.STATUS_ACTIVE,
    ): Int
}
