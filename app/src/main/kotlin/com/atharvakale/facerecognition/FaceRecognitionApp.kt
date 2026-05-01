package com.atharvakale.facerecognition

import android.app.Application
import com.atharvakale.facerecognition.ml.FaceEmbeddingExtractor
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class FaceRecognitionApp : Application() {

    @Inject
    lateinit var embeddingExtractor: FaceEmbeddingExtractor

    override fun onCreate() {
        super.onCreate()
        embeddingExtractor.initialize(this)
    }
}
