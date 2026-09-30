package com.gymtrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.gymtrack.data.local.entity.WorkoutExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutExerciseDao {

    @Query(
        """
        SELECT we.* FROM workout_exercises we
        INNER JOIN workout_blocks wb ON wb.id = we.blockId
        WHERE wb.workoutId = :workoutId
        ORDER BY wb.position ASC, we.positionInBlock ASC
        """,
    )
    fun getByWorkoutId(workoutId: Long): Flow<List<WorkoutExerciseEntity>>

    @Query(
        """
        SELECT we.* FROM workout_exercises we
        INNER JOIN workout_blocks wb ON wb.id = we.blockId
        WHERE wb.workoutId = :workoutId
        ORDER BY wb.position ASC, we.positionInBlock ASC
        """,
    )
    fun getByWorkoutIdOnce(workoutId: Long): List<WorkoutExerciseEntity>

    @Query(
        "SELECT * FROM workout_exercises WHERE blockId = :blockId ORDER BY positionInBlock ASC",
    )
    fun getByBlockId(blockId: Long): Flow<List<WorkoutExerciseEntity>>

    @Query(
        "SELECT * FROM workout_exercises WHERE blockId = :blockId ORDER BY positionInBlock ASC",
    )
    fun getByBlockIdOnce(blockId: Long): List<WorkoutExerciseEntity>

    @Query("SELECT * FROM workout_exercises WHERE id = :id")
    fun getByIdOnce(id: Long): WorkoutExerciseEntity?

    @Query("SELECT * FROM workout_exercises WHERE id = :id")
    fun getById(id: Long): Flow<WorkoutExerciseEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(workoutExercise: WorkoutExerciseEntity): Long

    @Query(
        "UPDATE workout_exercises SET positionInBlock = :positionInBlock WHERE id = :id",
    )
    fun updatePositionInBlock(id: Long, positionInBlock: Int): Int

    @Transaction
    fun updatePositionsInBlock(positions: Map<Long, Int>) {
        positions.forEach { (id, position) -> updatePositionInBlock(id, position) }
    }

    @Query("DELETE FROM workout_exercises WHERE id = :id")
    fun deleteById(id: Long): Int

    @Query("DELETE FROM workout_exercises WHERE blockId = :blockId")
    fun deleteByBlockId(blockId: Long): Int
}
