package com.atharvakale.facerecognition.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FaceEmbeddingDao {
    @Query("SELECT * FROM face_embeddings")
    fun getAll(): Flow<List<FaceEmbeddingEntity>>

    @Query("SELECT * FROM face_embeddings")
    suspend fun getAllOnce(): List<FaceEmbeddingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(face: FaceEmbeddingEntity)

    @Query("DELETE FROM face_embeddings WHERE name = :name")
    suspend fun delete(name: String)

    @Query("DELETE FROM face_embeddings")
    suspend fun deleteAll()
}
