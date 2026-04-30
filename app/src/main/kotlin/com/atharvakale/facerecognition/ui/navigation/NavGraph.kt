package com.atharvakale.facerecognition.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.atharvakale.facerecognition.ui.screens.DatabaseListScreen
import com.atharvakale.facerecognition.ui.screens.MenuScreen
import com.atharvakale.facerecognition.ui.screens.RecognitionScreen
import com.atharvakale.facerecognition.ui.screens.VerificationScreen

sealed class Screen(val route: String) {
    data object Menu : Screen("menu")
    data object DatabaseList : Screen("database")
    data object Recognition : Screen("recognition")
    data object Verification : Screen("verification")
}

@Composable
fun FaceRecognitionNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Menu.route
    ) {
        composable(Screen.Menu.route) {
            MenuScreen(
                onNavigateToDatabase = {
                    navController.navigate(Screen.DatabaseList.route)
                },
                onNavigateToRecognition = {
                    navController.navigate(Screen.Recognition.route)
                },
                onNavigateToVerification = {
                    navController.navigate(Screen.Verification.route)
                }
            )
        }
        composable(Screen.DatabaseList.route) {
            DatabaseListScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Recognition.route) {
            RecognitionScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Verification.route) {
            VerificationScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
