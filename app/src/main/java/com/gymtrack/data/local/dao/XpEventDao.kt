package com.gymtrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gymtrack.data.local.entity.XpEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface XpEventDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIgnore(event: XpEventEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIgnoreAll(events: List<XpEventEntity>): List<Long>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM xp_events")
    fun observeTotalXp(): Flow<Long>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM xp_events")
    fun getTotalXpOnce(): Long

    @Query("SELECT * FROM xp_events ORDER BY createdAtMillis DESC, id DESC")
    fun observeAll(): Flow<List<XpEventEntity>>

    @Query("SELECT COUNT(*) FROM xp_events WHERE sourceKey = :sourceKey")
    fun countBySourceKey(sourceKey: String): Int
}
