package com.gymtrack.di

import android.content.Context
import androidx.room.Room
import com.gymtrack.data.local.GymTrackDatabase
import com.gymtrack.data.local.dao.ExerciseDao
import com.gymtrack.data.local.dao.WorkoutDao
import com.gymtrack.data.local.dao.WorkoutExerciseDao
import com.gymtrack.data.repository.ExerciseRepositoryImpl
import com.gymtrack.data.repository.WorkoutRepositoryImpl
import com.gymtrack.domain.repository.ExerciseRepository
import com.gymtrack.domain.repository.WorkoutRepository
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
            .addMigrations(GymTrackDatabase.MIGRATION_1_2, GymTrackDatabase.MIGRATION_2_3)
            .build()
    }

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
}
