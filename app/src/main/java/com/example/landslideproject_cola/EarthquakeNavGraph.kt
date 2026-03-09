package com.example.landslideproject_cola

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

@Composable
fun EarthquakeNavGraph(navController: NavHostController) {

    val viewModel: EarthquakeViewModel = viewModel()
    val context = LocalContext.current
    val sharedPref = SharedPreferencesManager(context)

    // ถ้า login อยู่แล้ว ให้ไปหน้า Home เลย
    val startDest = if (sharedPref.isLoggedIn()) Screen.Home.route else Screen.Login.route

    NavHost(
        navController = navController,
        startDestination = startDest
    ) {
        composable(Screen.Login.route) {
            LoginScreen(navController, viewModel)
        }
        composable(Screen.Register.route) {
            RegisterScreen(navController, viewModel)
        }
        composable(Screen.Home.route) {
            HomeScreen(navController, viewModel)
        }
        composable(Screen.Predictions.route) {
            PredictionsScreen(navController, viewModel)
        }
        composable(Screen.Events.route) {
            EventsScreen(navController, viewModel)
        }
        composable(Screen.Notifications.route) {
            NotificationsScreen(navController, viewModel)
        }
        composable(Screen.Emergency.route) {
            EmergencyScreen(navController, viewModel)
        }
        composable(Screen.Profile.route) {
            ProfileScreen(navController, viewModel)
        }
        composable(
            route = Screen.NotificationDetail.route,
            arguments = listOf(navArgument("predictionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val predictionId = backStackEntry.arguments?.getString("predictionId") ?: ""
            NotificationDetailScreen(navController = navController, viewModel = viewModel, predictionId = predictionId)
        }
    }
}

