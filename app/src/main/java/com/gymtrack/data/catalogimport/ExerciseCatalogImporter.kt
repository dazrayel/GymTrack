package com.gymtrack.data.catalogimport

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.room.withTransaction
import com.gymtrack.data.local.GymTrackDatabase
import com.gymtrack.data.local.dao.ExerciseDao
import com.gymtrack.data.local.dao.ExerciseSecondaryMuscleDao
import com.gymtrack.data.local.entity.ExerciseEntity
import com.gymtrack.data.local.entity.ExerciseSecondaryMuscleEntity
import com.gymtrack.domain.catalogimport.ExerciseCatalogImportResult
import com.gymtrack.domain.catalogimport.ExerciseCatalogPackException
import com.gymtrack.domain.catalogimport.ImportableCatalogExercise
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExerciseCatalogImporter @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val database: GymTrackDatabase,
    private val exerciseDao: ExerciseDao,
    private val secondaryMuscleDao: ExerciseSecondaryMuscleDao,
) {

    @VisibleForTesting
    var failAfterPersistedCount: Int? = null

    suspend fun importDefaultAsset(): ExerciseCatalogImportResult = importJson(readDefaultAsset())

    suspend fun importJson(json: String): ExerciseCatalogImportResult = withContext(Dispatchers.IO) {
        val records = NormalizedExerciseCatalogParser.parse(json)
        ExerciseCatalogImportPolicy.validatePack(records)
        val classified = ExerciseCatalogImportPolicy.classify(records)
        val sourceLabel = classified.importable.firstOrNull()?.externalSource
            ?: classified.rejected.firstOrNull()?.externalSource
            ?: ExerciseCatalogImportPolicy.SOURCE_FREE_EXERCISE_DB

        database.withTransaction {
            var inserted = 0
            var updated = 0
            var unchanged = 0
            var persisted = 0
            for (item in classified.importable) {
                when (persist(item)) {
                    PersistKind.INSERTED -> inserted++
                    PersistKind.UPDATED -> updated++
                    PersistKind.UNCHANGED -> unchanged++
                }
                persisted++
                val failAt = failAfterPersistedCount
                if (failAt != null && persisted >= failAt) {
                    throw ExerciseCatalogPackException(
                        "Forced import failure after $persisted persist(s)",
                    )
                }
            }
            ExerciseCatalogImportResult(
                source = sourceLabel,
                inputCount = records.size,
                importableCount = classified.importable.size,
                inserted = inserted,
                updated = updated,
                unchanged = unchanged,
                rejected = classified.rejected.size,
                errors = 0,
                rejections = classified.rejected,
            )
        }
    }

    fun readDefaultAsset(): String =
        context.assets.open(ASSET_PATH).bufferedReader().use { it.readText() }

    private fun persist(item: ImportableCatalogExercise): PersistKind {
        val existing = exerciseDao.getByExternalIdentity(item.externalSource, item.externalId)
        if (existing == null) {
            val id = exerciseDao.insertImported(
                ExerciseEntity(
                    name = item.name,
                    muscleGroup = item.muscleGroup,
                    equipmentType = item.equipmentType,
                    externalSource = item.externalSource,
                    externalId = item.externalId,
                ),
            )
            replaceSecondaries(id, item.secondaryMuscles)
            return PersistKind.INSERTED
        }
        val currentSecondaries = secondaryMuscleDao.getMusclesOnce(existing.id)
        val samePayload =
            existing.name == item.name &&
                existing.muscleGroup == item.muscleGroup &&
                existing.equipmentType == item.equipmentType &&
                currentSecondaries == item.secondaryMuscles &&
                existing.externalSource == item.externalSource &&
                existing.externalId == item.externalId
        if (samePayload) {
            return PersistKind.UNCHANGED
        }
        val rows = exerciseDao.updateImported(
            id = existing.id,
            externalSource = existing.externalSource!!,
            externalId = existing.externalId!!,
            name = item.name,
            muscleGroup = item.muscleGroup,
            equipmentType = item.equipmentType,
        )
        check(rows == 1) { "Expected to update imported exercise ${existing.id}" }
        replaceSecondaries(existing.id, item.secondaryMuscles)
        return PersistKind.UPDATED
    }

    private fun replaceSecondaries(exerciseId: Long, muscles: List<String>) {
        secondaryMuscleDao.deleteByExerciseId(exerciseId)
        if (muscles.isNotEmpty()) {
            secondaryMuscleDao.insertAll(
                muscles.map { muscle ->
                    ExerciseSecondaryMuscleEntity(exerciseId = exerciseId, muscle = muscle)
                },
            )
        }
    }

    private enum class PersistKind { INSERTED, UPDATED, UNCHANGED }

    companion object {
        const val ASSET_PATH = "exercises/gymtrack-exercises.json"
    }
}
