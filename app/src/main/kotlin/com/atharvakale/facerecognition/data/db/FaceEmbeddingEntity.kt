package com.atharvakale.facerecognition.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "face_embeddings")
data class FaceEmbeddingEntity(
    @PrimaryKey val name: String,
    val embedding: List<Float>
)
