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
        route = "predictions_screen?lat={lat}&lon={lon}",
        name = "การทำนาย"
    ) {
        fun createRoute(lat: Float? = null, lon: Float? = null): String {
            return if (lat != null && lon != null) {
                "predictions_screen?lat=$lat&lon=$lon"
            } else {
                "predictions_screen"
            }
        }
    }

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



    data object AdminHome : Screen(
        route = "admin_home_screen?logId={logId}",
        name = "Admin Dashboard"
    ) {
        fun createRoute(logId: String? = null): String {
            return if (logId != null) {
                "admin_home_screen?logId=$logId"
            } else {
                "admin_home_screen"
            }
        }
    }

    data object AdminMap : Screen(
        route = "admin_map_screen?lat={lat}&lon={lon}",
        name = "แผนที่ความเสี่ยง"
    ) {
        fun createRoute(lat: Float? = null, lon: Float? = null): String {
            return if (lat != null && lon != null) {
                "admin_map_screen?lat=$lat&lon=$lon"
            } else {
                "admin_map_screen"
            }
        }
    }

    data object AdminEmergency : Screen(
        route = "admin_emergency_screen",
        name = "แก้ไขเบอร์ฉุกเฉิน"
    )

    data object AdminNotificationHistory : Screen(
        route = "admin_notification_history_screen",
        name = "ประวัติการยืนยัน"
    )

    data object AdminSentNotificationHistory : Screen(
        route = "admin_sent_notification_history_screen",
        name = "ประวัติการแจ้งเตือน"
    )

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
