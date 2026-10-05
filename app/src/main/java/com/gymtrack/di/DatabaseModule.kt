package com.gymtrack.di

import android.content.Context
import androidx.room.Room
import com.gymtrack.data.local.GymTrackDatabase
import com.gymtrack.data.local.dao.ExerciseDao
import com.gymtrack.data.local.dao.ExerciseSecondaryMuscleDao
import com.gymtrack.data.local.dao.WeeklyWorkoutPlanDao
import com.gymtrack.data.local.dao.WorkoutBlockDao
import com.gymtrack.data.local.dao.WorkoutDao
import com.gymtrack.data.local.dao.WorkoutExerciseDao
import com.gymtrack.data.local.dao.WorkoutSessionDao
import com.gymtrack.data.local.dao.WorkoutSessionExerciseDao
import com.gymtrack.data.local.dao.WorkoutSetDao
import com.gymtrack.data.catalog.CatalogDemoOptionsProvider
import com.gymtrack.data.repository.ExerciseRepositoryImpl
import com.gymtrack.data.repository.WeeklyPlanRepositoryImpl
import com.gymtrack.data.repository.WorkoutRepositoryImpl
import com.gymtrack.data.repository.WorkoutSessionRepositoryImpl
import com.gymtrack.domain.exercise.CatalogDemoOptionsSource
import com.gymtrack.domain.repository.ExerciseRepository
import com.gymtrack.domain.repository.WeeklyPlanRepository
import com.gymtrack.domain.repository.WorkoutRepository
import com.gymtrack.domain.repository.WorkoutSessionRepository
import com.gymtrack.domain.time.SystemTimeProvider
import com.gymtrack.domain.time.TimeProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): GymTrackDatabase {
        return Room.databaseBuilder(
            context,
            GymTrackDatabase::class.java,
            GymTrackDatabase.DATABASE_NAME,
        )
            .addMigrations(
                GymTrackDatabase.MIGRATION_1_2,
                GymTrackDatabase.MIGRATION_2_3,
                GymTrackDatabase.MIGRATION_3_4,
                GymTrackDatabase.MIGRATION_4_5,
                GymTrackDatabase.MIGRATION_5_6,
                GymTrackDatabase.MIGRATION_6_7,
                GymTrackDatabase.MIGRATION_7_8,
                GymTrackDatabase.MIGRATION_8_9,
                GymTrackDatabase.MIGRATION_9_10,
                GymTrackDatabase.MIGRATION_10_11,
                GymTrackDatabase.MIGRATION_11_12,
                GymTrackDatabase.MIGRATION_12_13,
            )
            .build()
    }

    @Provides
    @Singleton
    fun provideTimeProvider(): TimeProvider = SystemTimeProvider()

    @Provides
    @Singleton
    fun provideCatalogDemoOptionsSource(
        provider: CatalogDemoOptionsProvider,
    ): CatalogDemoOptionsSource = provider

    @Provides
    fun provideExerciseDao(database: GymTrackDatabase): ExerciseDao {
        return database.exerciseDao()
    }

    @Provides
    fun provideExerciseSecondaryMuscleDao(database: GymTrackDatabase): ExerciseSecondaryMuscleDao {
        return database.exerciseSecondaryMuscleDao()
    }

    @Provides
    @Singleton
    fun provideExerciseRepository(
        database: GymTrackDatabase,
        dao: ExerciseDao,
        secondaryMuscleDao: ExerciseSecondaryMuscleDao,
    ): ExerciseRepository {
        return ExerciseRepositoryImpl(database, dao, secondaryMuscleDao)
    }

    @Provides
    fun provideWorkoutDao(database: GymTrackDatabase): WorkoutDao {
        return database.workoutDao()
    }

    @Provides
    fun provideWorkoutBlockDao(database: GymTrackDatabase): WorkoutBlockDao {
        return database.workoutBlockDao()
    }

    @Provides
    fun provideWorkoutExerciseDao(database: GymTrackDatabase): WorkoutExerciseDao {
        return database.workoutExerciseDao()
    }

    @Provides
    @Singleton
    fun provideWorkoutRepository(
        database: GymTrackDatabase,
        workoutDao: WorkoutDao,
        workoutBlockDao: WorkoutBlockDao,
        workoutExerciseDao: WorkoutExerciseDao,
    ): WorkoutRepository {
        return WorkoutRepositoryImpl(database, workoutDao, workoutBlockDao, workoutExerciseDao)
    }

    @Provides
    fun provideWorkoutSessionDao(database: GymTrackDatabase): WorkoutSessionDao {
        return database.workoutSessionDao()
    }

    @Provides
    fun provideWorkoutSessionExerciseDao(database: GymTrackDatabase): WorkoutSessionExerciseDao {
        return database.workoutSessionExerciseDao()
    }

    @Provides
    fun provideWorkoutSetDao(database: GymTrackDatabase): WorkoutSetDao {
        return database.workoutSetDao()
    }

    @Provides
    fun provideWeeklyWorkoutPlanDao(database: GymTrackDatabase): WeeklyWorkoutPlanDao {
        return database.weeklyWorkoutPlanDao()
    }

    @Provides
    @Singleton
    fun provideWeeklyPlanRepository(
        planDao: WeeklyWorkoutPlanDao,
    ): WeeklyPlanRepository {
        return WeeklyPlanRepositoryImpl(planDao)
    }

    @Provides
    @Singleton
    fun provideWorkoutSessionRepository(
        database: GymTrackDatabase,
        sessionDao: WorkoutSessionDao,
        sessionExerciseDao: WorkoutSessionExerciseDao,
        setDao: WorkoutSetDao,
        workoutRepository: WorkoutRepository,
        exerciseRepository: ExerciseRepository,
        timeProvider: TimeProvider,
    ): WorkoutSessionRepository {
        return WorkoutSessionRepositoryImpl(
            database,
            sessionDao,
            sessionExerciseDao,
            setDao,
            workoutRepository,
            exerciseRepository,
            timeProvider,
        )
    }
}
