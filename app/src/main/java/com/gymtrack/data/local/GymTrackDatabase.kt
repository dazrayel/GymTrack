package com.gymtrack.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gymtrack.data.local.dao.ExerciseDao
import com.gymtrack.data.local.dao.WorkoutDao
import com.gymtrack.data.local.dao.WorkoutExerciseDao
import com.gymtrack.data.local.dao.WorkoutSessionDao
import com.gymtrack.data.local.dao.WorkoutSessionExerciseDao
import com.gymtrack.data.local.dao.WorkoutSetDao
import com.gymtrack.data.local.entity.ExerciseEntity
import com.gymtrack.data.local.entity.WorkoutEntity
import com.gymtrack.data.local.entity.WorkoutExerciseEntity
import com.gymtrack.data.local.entity.WorkoutSessionEntity
import com.gymtrack.data.local.entity.WorkoutSessionExerciseEntity
import com.gymtrack.data.local.entity.WorkoutSetEntity

@Database(
    entities = [
        ExerciseEntity::class,
        WorkoutEntity::class,
        WorkoutExerciseEntity::class,
        WorkoutSessionEntity::class,
        WorkoutSessionExerciseEntity::class,
        WorkoutSetEntity::class,
    ],
    version = 6,
    exportSchema = true,
)
abstract class GymTrackDatabase : RoomDatabase() {

    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun workoutExerciseDao(): WorkoutExerciseDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun workoutSessionExerciseDao(): WorkoutSessionExerciseDao
    abstract fun workoutSetDao(): WorkoutSetDao

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

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `workout_sessions` " +
                        "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`workoutId` INTEGER, " +
                        "`workoutName` TEXT NOT NULL, " +
                        "`workoutDescription` TEXT NOT NULL, " +
                        "`startedAtMillis` INTEGER NOT NULL, " +
                        "`endedAtMillis` INTEGER, " +
                        "`status` TEXT NOT NULL, " +
                        "FOREIGN KEY(`workoutId`) REFERENCES `workouts`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE SET NULL )",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_workout_sessions_status` " +
                        "ON `workout_sessions` (`status`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_workout_sessions_workoutId` " +
                        "ON `workout_sessions` (`workoutId`)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `workout_session_exercises` " +
                        "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`sessionId` INTEGER NOT NULL, " +
                        "`exerciseId` INTEGER, " +
                        "`position` INTEGER NOT NULL, " +
                        "`exerciseName` TEXT NOT NULL, " +
                        "`muscleGroup` TEXT NOT NULL, " +
                        "`equipmentType` TEXT NOT NULL, " +
                        "`plannedSets` INTEGER NOT NULL, " +
                        "`minRepetitions` INTEGER NOT NULL, " +
                        "`maxRepetitions` INTEGER NOT NULL, " +
                        "`plannedWeight` REAL NOT NULL, " +
                        "`restSeconds` INTEGER NOT NULL, " +
                        "`notes` TEXT NOT NULL, " +
                        "FOREIGN KEY(`sessionId`) REFERENCES `workout_sessions`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE , " +
                        "FOREIGN KEY(`exerciseId`) REFERENCES `exercises`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE SET NULL )",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_workout_session_exercises_sessionId` " +
                        "ON `workout_session_exercises` (`sessionId`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_workout_session_exercises_exerciseId` " +
                        "ON `workout_session_exercises` (`exerciseId`)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `workout_sets` " +
                        "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`sessionExerciseId` INTEGER NOT NULL, " +
                        "`setIndex` INTEGER NOT NULL, " +
                        "`reps` INTEGER NOT NULL, " +
                        "`weight` REAL NOT NULL, " +
                        "`completedAtMillis` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`sessionExerciseId`) REFERENCES `workout_session_exercises`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE )",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_workout_sets_sessionExerciseId` " +
                        "ON `workout_sets` (`sessionExerciseId`)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_workout_sets_sessionExerciseId_setIndex` " +
                        "ON `workout_sets` (`sessionExerciseId`, `setIndex`)",
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `workout_sessions` ADD COLUMN `restEndsAtMillis` INTEGER")
                db.execSQL(
                    "ALTER TABLE `workout_sessions` ADD COLUMN `restPausedRemainingMillis` INTEGER",
                )
                db.execSQL(
                    "ALTER TABLE `workout_sessions` ADD COLUMN `restSessionExerciseId` INTEGER",
                )
                db.execSQL("ALTER TABLE `workout_sessions` ADD COLUMN `restAfterSetIndex` INTEGER")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `workout_sessions` ADD COLUMN `inProgressLock` INTEGER")
                db.execSQL(
                    "DELETE FROM `workout_sets` WHERE `sessionExerciseId` IN (" +
                        "SELECT `se`.`id` FROM `workout_session_exercises` AS `se` " +
                        "INNER JOIN `workout_sessions` AS `s` ON `se`.`sessionId` = `s`.`id` " +
                        "WHERE `s`.`status` = 'IN_PROGRESS' AND `s`.`id` > (" +
                        "SELECT MIN(`id`) FROM `workout_sessions` WHERE `status` = 'IN_PROGRESS'))",
                )
                db.execSQL(
                    "DELETE FROM `workout_session_exercises` WHERE `sessionId` IN (" +
                        "SELECT `id` FROM `workout_sessions` " +
                        "WHERE `status` = 'IN_PROGRESS' AND `id` > (" +
                        "SELECT MIN(`id`) FROM `workout_sessions` WHERE `status` = 'IN_PROGRESS'))",
                )
                db.execSQL(
                    "DELETE FROM `workout_sessions` WHERE `status` = 'IN_PROGRESS' AND `id` NOT IN (" +
                        "SELECT `keep_id` FROM (" +
                        "SELECT MIN(`id`) AS `keep_id` FROM `workout_sessions` " +
                        "WHERE `status` = 'IN_PROGRESS'))",
                )
                db.execSQL(
                    "UPDATE `workout_sessions` SET `inProgressLock` = 1 " +
                        "WHERE `status` = 'IN_PROGRESS'",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_workout_sessions_inProgressLock` " +
                        "ON `workout_sessions` (`inProgressLock`)",
                )
            }
        }
    }
}
