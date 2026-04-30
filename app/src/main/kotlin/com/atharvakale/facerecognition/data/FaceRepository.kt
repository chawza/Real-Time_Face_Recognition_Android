package com.atharvakale.facerecognition.data

import android.content.Context
import android.content.SharedPreferences
import com.atharvakale.facerecognition.data.db.FaceEmbeddingDao
import com.atharvakale.facerecognition.data.db.FaceEmbeddingEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FaceRepository @Inject constructor(
    private val dao: FaceEmbeddingDao,
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "HashMap"
        private const val PREFS_KEY = "map"
        private const val OUTPUT_SIZE = 192
    }

    fun getRegisteredFaces(): Flow<List<FaceEmbeddingEntity>> = dao.getAll()

    suspend fun getAllOnce(): List<FaceEmbeddingEntity> = dao.getAllOnce()

    suspend fun registerFace(name: String, embedding: FloatArray) {
        dao.insert(FaceEmbeddingEntity(name, embedding.toList()))
    }

    suspend fun deleteFace(name: String) = dao.delete(name)

    suspend fun clearAll() = dao.deleteAll()

    suspend fun migrateFromSharedPrefs() {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val defValue = Gson().toJson(HashMap<String, Any>())
        val json = sharedPreferences.getString(PREFS_KEY, defValue) ?: return

        val type = object : TypeToken<HashMap<String, Any>>() {}.type
        val map: HashMap<String, Any> = Gson().fromJson(json, type)
        if (map.isEmpty()) return

        for ((name, value) in map) {
            try {
                @Suppress("UNCHECKED_CAST")
                val recognitionMap = value as? Map<String, Any?> ?: continue
                val extra = recognitionMap["extra"] ?: continue
                @Suppress("UNCHECKED_CAST")
                val outerList = extra as? ArrayList<ArrayList<Double>> ?: continue
                if (outerList.isEmpty()) continue
                val innerList = outerList[0]
                val embedding = FloatArray(OUTPUT_SIZE) { i ->
                    if (i < innerList.size) innerList[i].toFloat() else 0f
                }
                dao.insert(FaceEmbeddingEntity(name, embedding.toList()))
            } catch (_: Exception) {
                continue
            }
        }
    }
}
