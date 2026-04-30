package com.atharvakale.facerecognition

import android.app.Application
import com.atharvakale.facerecognition.data.FaceRepository
import com.atharvakale.facerecognition.ml.FaceEmbeddingExtractor
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class FaceRecognitionApp : Application() {

    @Inject
    lateinit var embeddingExtractor: FaceEmbeddingExtractor

    @Inject
    lateinit var faceRepository: FaceRepository

    override fun onCreate() {
        super.onCreate()
        embeddingExtractor.initialize(this)

        CoroutineScope(Dispatchers.IO).launch {
            val sharedPrefs = getSharedPreferences("migrated", MODE_PRIVATE)
            if (!sharedPrefs.getBoolean("room_migrated", false)) {
                faceRepository.migrateFromSharedPrefs()
                sharedPrefs.edit().putBoolean("room_migrated", true).apply()
            }
        }
    }
}
