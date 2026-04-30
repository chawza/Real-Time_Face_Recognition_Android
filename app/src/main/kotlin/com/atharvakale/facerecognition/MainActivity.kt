package com.atharvakale.facerecognition

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.atharvakale.facerecognition.ui.navigation.FaceRecognitionNavGraph
import com.atharvakale.facerecognition.ui.theme.FaceRecognitionTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FaceRecognitionTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    FaceRecognitionNavGraph()
                }
            }
        }
    }
}
