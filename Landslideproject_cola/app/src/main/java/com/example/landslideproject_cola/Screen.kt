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

    data object AdminAlerts : Screen(
        route = "admin_alerts_screen",
        name = "รายชื่อเหตุแจ้งเตือน"
    )

    data object AdminAnalysis : Screen(
        route = "admin_analysis_screen",
        name = "การวิเคราะห์และดึงข้อมูล"
    )

    data object AdminVerify : Screen(
        route = "admin_verify_screen/{logId}",
        name = "ยืนยันแจ้งเตือน"
    ) {
        fun createRoute(logId: String) = "admin_verify_screen/$logId"
    }

    data object AdminHome : Screen(
        route = "admin_home_screen",
        name = "Admin Dashboard"
    )

    data object AdminMap : Screen(
        route = "admin_map_screen",
        name = "แผนที่ความเสี่ยง"
    )

    data object AdminEmergency : Screen(
        route = "admin_emergency_screen",
        name = "แก้ไขเบอร์ฉุกเฉิน"
    )

    data object AdminNotificationHistory : Screen(
        route = "admin_notification_history_screen",
        name = "ประวัติการแจ้งเตือน"
    )

    data object PinDashboard : Screen(
        route = "pin_dashboard_screen/{pinId}",
        name = "กราฟวิเคราะห์น้ำฝนจุดปักหมุด"
    ) {
        fun createRoute(pinId: String) = "pin_dashboard_screen/$pinId"
    }

    data object SetLocation : Screen(
        route = "set_location_screen",
        name = "ปักหมุดที่อยู่"
    )

    // หน้าแสดงรายละเอียดแจ้งเตือนสำหรับ User (แสดงกราฟ)
    data object UserAlertDetail : Screen(
        route = "user_alert_detail/{logId}",
        name = "รายละเอียดแจ้งเตือน"
    ) {
        fun createRoute(logId: String) = "user_alert_detail/$logId"
    }

    // หน้าส่งรายงาน user → admin
    data object UserReport : Screen(
        route = "user_report_screen",
        name = "ขอความช่วยเหลือ"
    )

    // หน้าแอดมินดูรายงานจาก user
    data object AdminReports : Screen(
        route = "admin_reports_screen",
        name = "รายงานจากผู้ใช้"
    )

    // หน้าประวัติการช่วยเหลือ
    data object AdminReportHistory : Screen(
        route = "admin_report_history_screen",
        name = "ประวัติการช่วยเหลือ"
    )
}
