package com.atharvakale.facerecognition.data

import com.atharvakale.facerecognition.data.model.RegisteredFace
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FaceRepository @Inject constructor(
    private val storage: FaceEmbeddingStorage
) {

    fun getRegisteredFaces(): Flow<List<RegisteredFace>> = storage.faces

    suspend fun getAllOnce(): List<RegisteredFace> = storage.getAll()

    suspend fun registerFace(name: String, embedding: FloatArray) {
        storage.save(name, embedding)
    }

    suspend fun deleteFace(name: String) = storage.delete(name)

    suspend fun clearAll() = storage.clearAll()
}
