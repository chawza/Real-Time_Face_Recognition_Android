package com.atharvakale.facerecognition.data

import com.atharvakale.facerecognition.data.db.FaceEmbeddingDao
import com.atharvakale.facerecognition.data.db.FaceEmbeddingEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FaceRepository @Inject constructor(
    private val dao: FaceEmbeddingDao
) {

    fun getRegisteredFaces(): Flow<List<FaceEmbeddingEntity>> = dao.getAll()

    suspend fun getAllOnce(): List<FaceEmbeddingEntity> = dao.getAllOnce()

    suspend fun registerFace(name: String, embedding: FloatArray) {
        dao.insert(FaceEmbeddingEntity(name, embedding.toList()))
    }

    suspend fun deleteFace(name: String) = dao.delete(name)

    suspend fun clearAll() = dao.deleteAll()
}
