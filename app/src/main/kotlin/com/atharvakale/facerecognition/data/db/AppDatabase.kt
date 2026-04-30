package com.atharvakale.facerecognition.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [FaceEmbeddingEntity::class], version = 1, exportSchema = false)
@TypeConverters(FaceEmbeddingTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun faceEmbeddingDao(): FaceEmbeddingDao
}
