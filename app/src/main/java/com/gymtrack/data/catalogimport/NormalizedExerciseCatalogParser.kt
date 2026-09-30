package com.gymtrack.data.catalogimport

import com.gymtrack.domain.catalogimport.ExerciseCatalogPackException
import com.gymtrack.domain.catalogimport.NormalizedCatalogRecord
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

object NormalizedExerciseCatalogParser {

    fun parse(json: String): List<NormalizedCatalogRecord> {
        val root = try {
            JSONArray(json)
        } catch (e: JSONException) {
            throw ExerciseCatalogPackException("Catalog JSON must be an array: ${e.message}")
        }
        val records = ArrayList<NormalizedCatalogRecord>(root.length())
        for (index in 0 until root.length()) {
            val item = root.optJSONObject(index)
                ?: throw ExerciseCatalogPackException("Catalog item at index $index is not an object")
            records += parseObject(item)
        }
        return records
    }

    private fun parseObject(obj: JSONObject): NormalizedCatalogRecord {
        val sourceData = obj.optJSONObject("sourceData")
        return NormalizedCatalogRecord(
            source = optionalString(obj, "source"),
            externalId = optionalString(obj, "externalId"),
            name = optionalString(obj, "name"),
            muscleGroup = optionalString(obj, "muscleGroup"),
            equipmentType = optionalString(obj, "equipmentType"),
            secondaryMuscles = stringList(obj.optJSONArray("secondaryMuscles")),
            sourceEquipment = sourceData?.let { optionalString(it, "equipment") },
            sourcePrimaryMuscles = stringList(sourceData?.optJSONArray("primaryMuscles")),
        )
    }

    private fun optionalString(obj: JSONObject, key: String): String? {
        if (!obj.has(key) || obj.isNull(key)) return null
        val value = obj.get(key)
        if (value !is String) {
            throw ExerciseCatalogPackException("Field '$key' must be a string or null")
        }
        return value
    }

    private fun stringList(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        val out = ArrayList<String>(array.length())
        for (i in 0 until array.length()) {
            if (array.isNull(i)) continue
            val value = array.get(i)
            if (value is String) out += value
        }
        return out
    }
}
