package com.atharvakale.facerecognition.di

import android.content.Context
import androidx.room.Room
import com.atharvakale.facerecognition.data.db.AppDatabase
import com.atharvakale.facerecognition.data.db.FaceEmbeddingDao
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
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "face_recognition_db"
        ).build()
    }

    @Provides
    fun provideFaceEmbeddingDao(database: AppDatabase): FaceEmbeddingDao {
        return database.faceEmbeddingDao()
    }
}
