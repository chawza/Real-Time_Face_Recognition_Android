package com.atharvakale.facerecognition.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onFinished: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(2500)
        onFinished()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val compositionResult = rememberLottieComposition(
            LottieCompositionSpec.RawRes(com.atharvakale.facerecognition.R.raw.facialrecognition1)
        )
        LottieAnimation(
            composition = compositionResult.value,
            iterations = LottieConstants.IterateForever,
            modifier = Modifier.fillMaxSize()
        )
    }
}
