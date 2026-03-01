package com.example.landslideproject_cola

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.example.landslideproject_cola.ui.theme.LandslideProjectTheme

class MainActivity : ComponentActivity() {

    // Launcher สำหรับขอ permission แจ้งเตือน (Android 13+)
    private val requestNotifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startLandslideServiceIfLoggedIn()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // ขอ permission แจ้งเตือนก่อน (จำเป็นบน Android 13+)
        requestNotificationPermissionIfNeeded()

        setContent {
            LandslideProjectTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LandslideApp()
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (granted) {
                startLandslideServiceIfLoggedIn()
            } else {
                // ขอ permission จาก user (dialog จะโชว์อัตโนมัติ)
                requestNotifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            // Android < 13 ไม่ต้องขอ permission
            startLandslideServiceIfLoggedIn()
        }
    }

    /** Start background service เฉพาะเมื่อ user login แล้ว */
    private fun startLandslideServiceIfLoggedIn() {
        val sharedPref = SharedPreferencesManager(this)
        if (sharedPref.isLoggedIn() && sharedPref.getSavedRole() == "user") {
            LandslideNotificationService.startService(this)
        }
    }
}

@Composable
fun LandslideApp() {
    val navController = rememberNavController()
    EarthquakeNavGraph(navController = navController)
}
