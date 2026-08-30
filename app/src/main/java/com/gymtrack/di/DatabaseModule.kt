package com.gymtrack.di

import android.content.Context
import androidx.room.Room
import com.gymtrack.data.local.GymTrackDatabase
import com.gymtrack.data.local.dao.ExerciseDao
import com.gymtrack.data.local.dao.WorkoutDao
import com.gymtrack.data.local.dao.WorkoutExerciseDao
import com.gymtrack.data.local.dao.WorkoutSessionDao
import com.gymtrack.data.local.dao.WorkoutSessionExerciseDao
import com.gymtrack.data.local.dao.WorkoutSetDao
import com.gymtrack.data.repository.ExerciseRepositoryImpl
import com.gymtrack.data.repository.WorkoutRepositoryImpl
import com.gymtrack.data.repository.WorkoutSessionRepositoryImpl
import com.gymtrack.domain.repository.ExerciseRepository
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
            )
            .build()
    }

    @Provides
    @Singleton
    fun provideTimeProvider(): TimeProvider = SystemTimeProvider()

    @Provides
    fun provideExerciseDao(database: GymTrackDatabase): ExerciseDao {
        return database.exerciseDao()
    }

    @Provides
    @Singleton
    fun provideExerciseRepository(
        dao: ExerciseDao,
    ): ExerciseRepository {
        return ExerciseRepositoryImpl(dao)
    }

    @Provides
    fun provideWorkoutDao(database: GymTrackDatabase): WorkoutDao {
        return database.workoutDao()
    }

    @Provides
    fun provideWorkoutExerciseDao(database: GymTrackDatabase): WorkoutExerciseDao {
        return database.workoutExerciseDao()
    }

    @Provides
    @Singleton
    fun provideWorkoutRepository(
        workoutDao: WorkoutDao,
        workoutExerciseDao: WorkoutExerciseDao,
    ): WorkoutRepository {
        return WorkoutRepositoryImpl(workoutDao, workoutExerciseDao)
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
