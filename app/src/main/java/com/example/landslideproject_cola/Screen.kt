package com.example.landslideproject_cola

sealed class Screen(val route: String, val name: String) {

    data object Login : Screen(
        route = "login_screen",
        name = "เข้าสู่ระบบ"
    )

    data object Register : Screen(
        route = "register_screen",
        name = "สมัครสมาชิก"
    )

    data object Home : Screen(
        route = "home_screen",
        name = "หน้าหลัก"
    )

    data object Predictions : Screen(
        route = "predictions_screen",
        name = "การทำนาย"
    )

    data object Events : Screen(
        route = "events_screen",
        name = "เหตุการณ์"
    )

    data object Notifications : Screen(
        route = "notifications_screen",
        name = "การแจ้งเตือน"
    )

    data object Emergency : Screen(
        route = "emergency_screen",
        name = "ฉุกเฉิน"
    )

    data object Profile : Screen(
        route = "profile_screen",
        name = "โปรไฟล์"
    )
}
