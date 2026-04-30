package com.atharvakale.facerecognition.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.atharvakale.facerecognition.ui.screens.MainScreen
import com.atharvakale.facerecognition.ui.screens.RealtimeScreen
import com.atharvakale.facerecognition.ui.screens.SplashScreen

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Main : Screen("main")
    data object RealtimeMetrics : Screen("realtime")
}

@Composable
fun FaceRecognitionNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onFinished = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Main.route) {
            MainScreen(
                onNavigateToRealtime = {
                    navController.navigate(Screen.RealtimeMetrics.route)
                }
            )
        }
        composable(Screen.RealtimeMetrics.route) {
            RealtimeScreen()
        }
    }
}
