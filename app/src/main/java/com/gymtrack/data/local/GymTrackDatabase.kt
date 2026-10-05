package com.gymtrack.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gymtrack.data.local.dao.ExerciseDao
import com.gymtrack.data.local.dao.ExerciseSecondaryMuscleDao
import com.gymtrack.data.local.dao.WeeklyWorkoutPlanDao
import com.gymtrack.data.local.dao.WorkoutBlockDao
import com.gymtrack.data.local.dao.WorkoutDao
import com.gymtrack.data.local.dao.WorkoutExerciseDao
import com.gymtrack.data.local.dao.WorkoutSessionDao
import com.gymtrack.data.local.dao.WorkoutSessionExerciseDao
import com.gymtrack.data.local.dao.WorkoutSetDao
import com.gymtrack.data.local.entity.ExerciseEntity
import com.gymtrack.data.local.entity.ExerciseSecondaryMuscleEntity
import com.gymtrack.data.local.entity.WeeklyWorkoutPlanEntity
import com.gymtrack.data.local.entity.WorkoutBlockEntity
import com.gymtrack.data.local.entity.WorkoutEntity
import com.gymtrack.data.local.entity.WorkoutExerciseEntity
import com.gymtrack.data.local.entity.WorkoutSessionEntity
import com.gymtrack.data.local.entity.WorkoutSessionExerciseEntity
import com.gymtrack.data.local.entity.WorkoutSetEntity

@Database(
    entities = [
        ExerciseEntity::class,
        ExerciseSecondaryMuscleEntity::class,
        WorkoutEntity::class,
        WorkoutBlockEntity::class,
        WorkoutExerciseEntity::class,
        WorkoutSessionEntity::class,
        WorkoutSessionExerciseEntity::class,
        WorkoutSetEntity::class,
        WeeklyWorkoutPlanEntity::class,
    ],
    version = 12,
    exportSchema = true,
)
abstract class GymTrackDatabase : RoomDatabase() {

    abstract fun exerciseDao(): ExerciseDao
    abstract fun exerciseSecondaryMuscleDao(): ExerciseSecondaryMuscleDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun workoutBlockDao(): WorkoutBlockDao
    abstract fun workoutExerciseDao(): WorkoutExerciseDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun workoutSessionExerciseDao(): WorkoutSessionExerciseDao
    abstract fun workoutSetDao(): WorkoutSetDao
    abstract fun weeklyWorkoutPlanDao(): WeeklyWorkoutPlanDao

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

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `exercise_secondary_muscles` " +
                        "(`exerciseId` INTEGER NOT NULL, " +
                        "`muscle` TEXT NOT NULL, " +
                        "PRIMARY KEY(`exerciseId`, `muscle`), " +
                        "FOREIGN KEY(`exerciseId`) REFERENCES `exercises`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE )",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_exercise_secondary_muscles_exerciseId` " +
                        "ON `exercise_secondary_muscles` (`exerciseId`)",
                )
                db.execSQL(
                    "ALTER TABLE `workout_session_exercises` " +
                        "ADD COLUMN `secondaryMuscles` TEXT NOT NULL DEFAULT ''",
                )
                db.execSQL(
                    "UPDATE `exercises` SET `muscleGroup` = 'Peitoral' " +
                        "WHERE lower(`muscleGroup`) IN ('peito', 'chest', 'peitoral')",
                )
                db.execSQL(
                    "UPDATE `exercises` SET `muscleGroup` = 'Costas' " +
                        "WHERE lower(`muscleGroup`) IN ('back', 'costas')",
                )
                db.execSQL(
                    "UPDATE `exercises` SET `muscleGroup` = 'Ombros' " +
                        "WHERE lower(`muscleGroup`) IN ('shoulders', 'ombros')",
                )
                db.execSQL(
                    "UPDATE `exercises` SET `muscleGroup` = 'Bíceps' " +
                        "WHERE lower(`muscleGroup`) IN ('biceps', 'bíceps')",
                )
                db.execSQL(
                    "UPDATE `exercises` SET `muscleGroup` = 'Tríceps' " +
                        "WHERE lower(`muscleGroup`) IN ('triceps', 'tríceps')",
                )
                db.execSQL(
                    "UPDATE `exercises` SET `muscleGroup` = 'Antebraço' " +
                        "WHERE lower(`muscleGroup`) IN ('forearm', 'forearms', 'antebraço', 'antebraços')",
                )
                db.execSQL(
                    "UPDATE `exercises` SET `muscleGroup` = 'Quadríceps' " +
                        "WHERE lower(`muscleGroup`) IN ('quadriceps', 'quads', 'quadríceps')",
                )
                db.execSQL(
                    "UPDATE `exercises` SET `muscleGroup` = 'Posteriores' " +
                        "WHERE lower(`muscleGroup`) IN ('hamstrings', 'posteriores', 'posteriores de coxa')",
                )
                db.execSQL(
                    "UPDATE `exercises` SET `muscleGroup` = 'Glúteos' " +
                        "WHERE lower(`muscleGroup`) IN ('glutes', 'glúteos')",
                )
                db.execSQL(
                    "UPDATE `exercises` SET `muscleGroup` = 'Panturrilhas' " +
                        "WHERE lower(`muscleGroup`) IN ('calves', 'panturrilhas')",
                )
                db.execSQL(
                    "UPDATE `exercises` SET `muscleGroup` = 'Lombar' " +
                        "WHERE lower(`muscleGroup`) IN ('lower back', 'lombar')",
                )
                db.execSQL(
                    "UPDATE `exercises` SET `muscleGroup` = 'Trapézio' " +
                        "WHERE lower(`muscleGroup`) IN ('traps', 'trapézio')",
                )
                db.execSQL(
                    "UPDATE `exercises` SET `muscleGroup` = 'Abdômen' " +
                        "WHERE lower(`muscleGroup`) IN ('abs', 'abdomen', 'abdômen')",
                )
                db.execSQL(
                    "UPDATE `exercises` SET `equipmentType` = 'Barra' " +
                        "WHERE lower(`equipmentType`) IN ('barra', 'barbell')",
                )
                db.execSQL(
                    "UPDATE `exercises` SET `equipmentType` = 'Halteres' " +
                        "WHERE lower(`equipmentType`) IN ('halteres', 'dumbbell', 'dumbbells')",
                )
                db.execSQL(
                    "UPDATE `exercises` SET `equipmentType` = 'Máquina' " +
                        "WHERE lower(`equipmentType`) IN ('máquina', 'maquina', 'machine')",
                )
                db.execSQL(
                    "UPDATE `exercises` SET `equipmentType` = 'Smith' " +
                        "WHERE lower(`equipmentType`) IN ('smith', 'smith machine')",
                )
                db.execSQL(
                    "UPDATE `exercises` SET `equipmentType` = 'Cabos' " +
                        "WHERE lower(`equipmentType`) IN ('cabo', 'cabos', 'cable', 'cables')",
                )
                db.execSQL(
                    "UPDATE `exercises` SET `equipmentType` = 'Peso corporal' " +
                        "WHERE lower(`equipmentType`) IN ('peso corporal', 'bodyweight')",
                )
                db.execSQL(
                    "UPDATE `exercises` SET `equipmentType` = 'Kettlebell' " +
                        "WHERE lower(`equipmentType`) = 'kettlebell'",
                )
                db.execSQL(
                    "UPDATE `exercises` SET `equipmentType` = 'Elástico' " +
                        "WHERE lower(`equipmentType`) IN ('elástico', 'elastico', 'band', 'bands')",
                )
                db.execSQL(
                    "UPDATE `exercises` SET `equipmentType` = 'Outro' " +
                        "WHERE lower(`equipmentType`) IN ('outro', 'other')",
                )
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `workout_session_exercises` " +
                        "ADD COLUMN `status` TEXT NOT NULL DEFAULT 'PENDING'",
                )
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `exercises` ADD COLUMN `externalSource` TEXT")
                db.execSQL("ALTER TABLE `exercises` ADD COLUMN `externalId` TEXT")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS " +
                        "`index_exercises_externalSource_externalId` " +
                        "ON `exercises` (`externalSource`, `externalId`)",
                )
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `exercises` ADD COLUMN `mediaExternalSource` TEXT")
                db.execSQL("ALTER TABLE `exercises` ADD COLUMN `mediaExternalId` TEXT")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create workout_blocks table.
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `workout_blocks` " +
                        "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`workoutId` INTEGER NOT NULL, " +
                        "`position` INTEGER NOT NULL, " +
                        "`type` TEXT NOT NULL, " +
                        "`rounds` INTEGER NOT NULL, " +
                        "`restSeconds` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`workoutId`) REFERENCES `workouts`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE )",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_workout_blocks_workoutId` " +
                        "ON `workout_blocks` (`workoutId`)",
                )

                // 2. For each existing workout_exercises row, insert a SINGLE block.
                db.execSQL(
                    "INSERT INTO `workout_blocks` (`workoutId`, `position`, `type`, `rounds`, `restSeconds`) " +
                        "SELECT `workoutId`, `position`, 'SINGLE', `sets`, `restSeconds` " +
                        "FROM `workout_exercises`",
                )

                // 3. Create new workout_exercises table with updated schema.
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `workout_exercises_new` " +
                        "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`blockId` INTEGER NOT NULL, " +
                        "`exerciseId` INTEGER NOT NULL, " +
                        "`positionInBlock` INTEGER NOT NULL, " +
                        "`minRepetitions` INTEGER NOT NULL, " +
                        "`maxRepetitions` INTEGER NOT NULL, " +
                        "`weight` REAL NOT NULL, " +
                        "`notes` TEXT NOT NULL, " +
                        "FOREIGN KEY(`blockId`) REFERENCES `workout_blocks`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE , " +
                        "FOREIGN KEY(`exerciseId`) REFERENCES `exercises`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE )",
                )

                // Copy exercises, linking each to its corresponding new SINGLE block.
                // The block was inserted with the same workoutId + position, so we join on those.
                db.execSQL(
                    "INSERT INTO `workout_exercises_new` (`blockId`, `exerciseId`, `positionInBlock`, " +
                        "`minRepetitions`, `maxRepetitions`, `weight`, `notes`) " +
                        "SELECT wb.`id`, we.`exerciseId`, 0, " +
                        "we.`minRepetitions`, we.`maxRepetitions`, we.`weight`, we.`notes` " +
                        "FROM `workout_exercises` AS we " +
                        "INNER JOIN `workout_blocks` AS wb " +
                        "ON wb.`workoutId` = we.`workoutId` AND wb.`position` = we.`position`",
                )

                // 4. Replace old table.
                db.execSQL("DROP TABLE `workout_exercises`")
                db.execSQL("ALTER TABLE `workout_exercises_new` RENAME TO `workout_exercises`")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_workout_exercises_blockId` " +
                        "ON `workout_exercises` (`blockId`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_workout_exercises_exerciseId` " +
                        "ON `workout_exercises` (`exerciseId`)",
                )

                // 5. Add block tracking columns to workout_session_exercises.
                db.execSQL(
                    "ALTER TABLE `workout_session_exercises` " +
                        "ADD COLUMN `blockType` TEXT NOT NULL DEFAULT 'SINGLE'",
                )
                db.execSQL(
                    "ALTER TABLE `workout_session_exercises` " +
                        "ADD COLUMN `blockPosition` INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL(
                    "ALTER TABLE `workout_session_exercises` " +
                        "ADD COLUMN `positionInBlock` INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL(
                    "UPDATE `workout_session_exercises` SET " +
                        "`blockType` = 'SINGLE', " +
                        "`blockPosition` = `position`, " +
                        "`positionInBlock` = 0",
                )
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `weekly_workout_plans` " +
                        "(`dayOfWeek` INTEGER NOT NULL, " +
                        "`workoutId` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`dayOfWeek`), " +
                        "FOREIGN KEY(`workoutId`) REFERENCES `workouts`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE )",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_weekly_workout_plans_workoutId` " +
                        "ON `weekly_workout_plans` (`workoutId`)",
                )
            }
        }
    }
}
