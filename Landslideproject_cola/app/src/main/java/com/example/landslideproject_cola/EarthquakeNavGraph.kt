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
        composable(
            route = Screen.Predictions.route,
            arguments = listOf(
                androidx.navigation.navArgument("lat") {
                    type = androidx.navigation.NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                androidx.navigation.navArgument("lon") {
                    type = androidx.navigation.NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val lat = backStackEntry.arguments?.getString("lat")?.toFloatOrNull()
            val lon = backStackEntry.arguments?.getString("lon")?.toFloatOrNull()
            PredictionsScreen(navController, viewModel, lat, lon)
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
        composable(
            route = Screen.AdminHome.route,
            arguments = listOf(
                androidx.navigation.navArgument("logId") {
                    type = androidx.navigation.NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val logId = backStackEntry.arguments?.getString("logId")
            AdminHomeScreen(navController, viewModel, logId)
        }
        composable(Screen.AdminAlerts.route) {
            AdminAlertsScreen(navController, viewModel)
        }
        composable(
            route = Screen.AdminMap.route,
            arguments = listOf(
                androidx.navigation.navArgument("lat") {
                    type = androidx.navigation.NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                androidx.navigation.navArgument("lon") {
                    type = androidx.navigation.NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val lat = backStackEntry.arguments?.getString("lat")?.toFloatOrNull()
            val lon = backStackEntry.arguments?.getString("lon")?.toFloatOrNull()
            AdminMapScreen(navController, viewModel, lat, lon)
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
        composable(Screen.AdminSentNotificationHistory.route) {
            AdminSentNotificationHistoryScreen(navController, viewModel)
        }

        composable(Screen.SetLocation.route) {
            SetLocationScreen(navController, viewModel)
        }
        // เพิ่มหน้าแสดงรายละเอียดแจ้งเตือนสำหรับ User
        composable(Screen.UserAlertDetail.route) { backStackEntry ->
            val logId = backStackEntry.arguments?.getString("logId") ?: ""
            UserAlertDetailScreen(navController, viewModel, logId)
        }
        composable(Screen.UserReport.route) {
            UserReportScreen(navController, viewModel)
        }
        composable(Screen.AdminReports.route) {
            AdminReportsScreen(navController, viewModel)
        }
        composable(Screen.AdminReportHistory.route) {
            AdminReportHistoryScreen(navController, viewModel)
        }
    }
}
