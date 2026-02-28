package com.example.landslideproject_cola

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun EarthquakeNavGraph(navController: NavHostController) {

    val viewModel: EarthquakeViewModel = viewModel()
    val context = LocalContext.current
    val sharedPref = SharedPreferencesManager(context)

    // ถ้า login อยู่แล้ว ให้ไปหน้า Home หรือ AdminHome
    val startDest = if (sharedPref.isLoggedIn()) {
        if (sharedPref.getSavedRole() == "admin") Screen.AdminHome.route else Screen.Home.route
    } else {
        Screen.Login.route
    }

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
        // ===== ADMIN SCREENS =====
        composable(Screen.AdminHome.route) {
            AdminHomeScreen(navController, viewModel)
        }
        composable(Screen.AdminAlerts.route) {
            AdminAlertsScreen(navController, viewModel)
        }
        composable(Screen.AdminMap.route) {
            AdminMapScreen(navController, viewModel)
        }
        composable(Screen.AdminAnalysis.route) {
            AdminAnalysisScreen(navController, viewModel)
        }
        composable(Screen.AdminEmergency.route) {
            AdminEmergencyScreen(navController, viewModel)
        }
        composable(Screen.AdminNotificationHistory.route) {
            AdminNotificationHistoryScreen(navController, viewModel)
        }
        composable(Screen.AdminVerify.route) { backStackEntry ->
            val logId = backStackEntry.arguments?.getString("logId") ?: ""
            AdminVerifyScreen(navController, viewModel, logId)
        }
        composable(Screen.PinDashboard.route) { backStackEntry ->
            val pinId = backStackEntry.arguments?.getString("pinId") ?: ""
            PinDashboardScreen(navController, viewModel, pinId)
        }
    }
}
