package com.gymtrack.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gymtrack.data.local.dao.ExerciseDao
import com.gymtrack.data.local.dao.WorkoutDao
import com.gymtrack.data.local.dao.WorkoutExerciseDao
import com.gymtrack.data.local.entity.ExerciseEntity
import com.gymtrack.data.local.entity.WorkoutEntity
import com.gymtrack.data.local.entity.WorkoutExerciseEntity

@Database(
    entities = [
        ExerciseEntity::class,
        WorkoutEntity::class,
        WorkoutExerciseEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class GymTrackDatabase : RoomDatabase() {

    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun workoutExerciseDao(): WorkoutExerciseDao

    companion object {

        const val DATABASE_NAME = "gymtrack.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `app_metadata`")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `exercises` " +
                        "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`muscleGroup` TEXT NOT NULL, " +
                        "`equipmentType` TEXT NOT NULL)",
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `workouts` " +
                        "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`description` TEXT NOT NULL)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `workout_exercises` " +
                        "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`workoutId` INTEGER NOT NULL, " +
                        "`exerciseId` INTEGER NOT NULL, " +
                        "`position` INTEGER NOT NULL, " +
                        "`sets` INTEGER NOT NULL, " +
                        "`minRepetitions` INTEGER NOT NULL, " +
                        "`maxRepetitions` INTEGER NOT NULL, " +
                        "`weight` REAL NOT NULL, " +
                        "`restSeconds` INTEGER NOT NULL, " +
                        "`notes` TEXT NOT NULL, " +
                        "FOREIGN KEY(`workoutId`) REFERENCES `workouts`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE , " +
                        "FOREIGN KEY(`exerciseId`) REFERENCES `exercises`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE )",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_workout_exercises_workoutId` " +
                        "ON `workout_exercises` (`workoutId`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_workout_exercises_exerciseId` " +
                        "ON `workout_exercises` (`exerciseId`)",
                )
            }
        }
    }
}
