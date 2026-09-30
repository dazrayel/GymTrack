package com.gymtrack.domain.exercise

import com.gymtrack.domain.model.Exercise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseMediaResolverTest {

    private val resolver = ExerciseMediaResolver()

    @Test
    fun resolve_manualWithoutMedia_isUnavailable() {
        val exercise = Exercise(
            name = "Meu exercício",
            muscleGroup = "Peitoral",
            equipmentType = "Barra",
            externalSource = null,
            externalId = null,
            mediaExternalSource = null,
            mediaExternalId = null,
        )
        assertSame(ExerciseMediaResolution.Unavailable, resolver.resolve(exercise))
    }

    @Test
    fun resolve_manualWithSelectedMedia_returnsCatalogPaths() {
        val exercise = Exercise(
            name = "Meu exercício",
            muscleGroup = "Peitoral",
            equipmentType = "Barra",
            mediaExternalSource = ExerciseMediaConstants.FREE_EXERCISE_DB_SOURCE,
            mediaExternalId = "Barbell_Squat",
        )
        val resolution = resolver.resolve(exercise) as ExerciseMediaResolution.Available
        assertEquals("exercises/Barbell_Squat/0.jpg", resolution.frames.frame0AssetPath)
        assertEquals("exercises/Barbell_Squat/1.jpg", resolution.frames.frame1AssetPath)
    }

    @Test
    fun resolve_importedExercise_usesExternalIdentity() {
        val exercise = catalogExercise("Barbell_Curl")
        val resolution = resolver.resolve(exercise) as ExerciseMediaResolution.Available
        assertEquals("exercises/Barbell_Curl/0.jpg", resolution.frames.frame0AssetPath)
        assertEquals("exercises/Barbell_Curl/1.jpg", resolution.frames.frame1AssetPath)
    }

    @Test
    fun resolve_manualWithSelectedMedia_doesNotRequireExternalIdentity() {
        val exercise = Exercise(
            name = "Manual com demo",
            muscleGroup = "Costas",
            equipmentType = "Barra",
            externalSource = null,
            externalId = null,
            mediaExternalSource = ExerciseMediaConstants.FREE_EXERCISE_DB_SOURCE,
            mediaExternalId = "Barbell_Squat",
        )
        assertNull(exercise.externalSource)
        assertNull(exercise.externalId)
        assertTrue(exercise.hasSelectedMedia)
        assertTrue(resolver.resolve(exercise) is ExerciseMediaResolution.Available)
    }

    @Test
    fun resolve_afterRemovingSelectedMedia_isUnavailable() {
        val withMedia = Exercise(
            name = "Manual",
            muscleGroup = "Peitoral",
            equipmentType = "Barra",
            mediaExternalSource = ExerciseMediaConstants.FREE_EXERCISE_DB_SOURCE,
            mediaExternalId = "Barbell_Squat",
        )
        val withoutMedia = withMedia.copy(mediaExternalSource = null, mediaExternalId = null)
        assertSame(ExerciseMediaResolution.Unavailable, resolver.resolve(withoutMedia))
    }

    @Test
    fun resolve_selectedMediaTakesPriorityOverImportedIdentity() {
        val exercise = Exercise(
            name = "Importado com override",
            muscleGroup = "Peitoral",
            equipmentType = "Barra",
            externalSource = ExerciseMediaConstants.FREE_EXERCISE_DB_SOURCE,
            externalId = "Barbell_Curl",
            mediaExternalSource = ExerciseMediaConstants.FREE_EXERCISE_DB_SOURCE,
            mediaExternalId = "Barbell_Squat",
        )
        val resolution = resolver.resolve(exercise) as ExerciseMediaResolution.Available
        assertEquals("exercises/Barbell_Squat/0.jpg", resolution.frames.frame0AssetPath)
        assertEquals(
            "free-exercise-db|Barbell_Squat",
            resolver.mediaIdentityKey(exercise),
        )
    }

    @Test
    fun resolve_unknownExternalId_stillReturnsPaths() {
        val exercise = catalogExercise("Does_Not_Exist_In_Assets")
        val resolution = resolver.resolve(exercise) as ExerciseMediaResolution.Available
        assertEquals("exercises/Does_Not_Exist_In_Assets/0.jpg", resolution.frames.frame0AssetPath)
    }

    @Test
    fun resolve_withMedia_returnsFrame0Path() {
        val exercise = catalogExercise("Barbell_Squat")
        val resolution = resolver.resolve(exercise) as ExerciseMediaResolution.Available
        assertEquals("exercises/Barbell_Squat/0.jpg", resolution.frames.frame0AssetPath)
    }

    @Test
    fun resolve_withMedia_returnsFrame1Path() {
        val exercise = catalogExercise("Barbell_Squat")
        val resolution = resolver.resolve(exercise) as ExerciseMediaResolution.Available
        assertEquals("exercises/Barbell_Squat/1.jpg", resolution.frames.frame1AssetPath)
    }

    @Test
    fun resolve_withoutExternalId_isUnavailable() {
        val exercise = Exercise(
            name = "Manual",
            muscleGroup = "Peitoral",
            equipmentType = "Barra",
            externalSource = "free-exercise-db",
            externalId = null,
        )
        assertSame(ExerciseMediaResolution.Unavailable, resolver.resolve(exercise))
    }

    @Test
    fun resolve_withoutExternalSource_isUnavailable() {
        val exercise = Exercise(
            name = "Manual",
            muscleGroup = "Peitoral",
            equipmentType = "Barra",
            externalSource = null,
            externalId = "Barbell_Squat",
        )
        assertSame(ExerciseMediaResolution.Unavailable, resolver.resolve(exercise))
    }

    @Test
    fun resolve_manualExercise_isUnavailable() {
        val exercise = Exercise(
            name = "Meu exercício",
            muscleGroup = "Peitoral",
            equipmentType = "Barra",
            externalSource = null,
            externalId = null,
        )
        assertSame(ExerciseMediaResolution.Unavailable, resolver.resolve(exercise))
    }

    @Test
    fun mediaIdentityKey_changesWhenExerciseChanges() {
        val a = catalogExercise("Barbell_Squat")
        val b = catalogExercise("Barbell_Curl")
        assertTrue(resolver.mediaIdentityKey(a) != resolver.mediaIdentityKey(b))
    }

    @Test
    fun mediaIdentityKey_manualExercise_isEmpty() {
        val manual = Exercise(
            name = "Manual",
            muscleGroup = "Peitoral",
            equipmentType = "Barra",
        )
        assertEquals("", resolver.mediaIdentityKey(manual))
    }

    @Test
    fun mediaIdentityKey_nullExercise_isEmpty() {
        assertEquals("", resolver.mediaIdentityKey(null))
    }

    @Test
    fun mediaIdentityKey_manualWithSelectedMedia_usesSelectedPair() {
        val exercise = Exercise(
            name = "Manual",
            muscleGroup = "Peitoral",
            equipmentType = "Barra",
            mediaExternalSource = ExerciseMediaConstants.FREE_EXERCISE_DB_SOURCE,
            mediaExternalId = "Barbell_Squat",
        )
        assertEquals("free-exercise-db|Barbell_Squat", resolver.mediaIdentityKey(exercise))
    }

    private fun catalogExercise(externalId: String): Exercise =
        Exercise(
            name = "Test",
            muscleGroup = "Quadríceps",
            equipmentType = "Barra",
            externalSource = ExerciseMediaConstants.FREE_EXERCISE_DB_SOURCE,
            externalId = externalId,
        )
}
