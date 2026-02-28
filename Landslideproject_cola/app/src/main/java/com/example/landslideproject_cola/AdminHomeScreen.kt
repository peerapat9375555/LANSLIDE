package com.example.landslideproject_cola

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.launch

@Composable
fun AdminHomeScreen(navController: NavHostController, viewModel: EarthquakeViewModel) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    // Load first pending alert for dashboard preview
    LaunchedEffect(Unit) {
        viewModel.getPendingAlerts()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = { AdminDrawer(navController = navController, onClose = { scope.launch { drawerState.close() } }) }
    ) {
        Scaffold(
            topBar = { AdminTopBar(title = "Admin Dashboard", onMenuClick = { scope.launch { drawerState.open() } }) },
            bottomBar = { AdminBottomNavigationBar(navController, currentRoute) },
            containerColor = AppGrey
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val alerts = viewModel.pendingAlerts
                if (alerts.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("ยังไม่มีข้อมูลจากการวิเคราะห์", fontSize = 16.sp, color = AppTextGrey)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("กรุณาดึงข้อมูลน้ำฝนและวิเคราะห์ก่อน", fontSize = 13.sp, color = AppTextGrey)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("ไปที่เมนู ☰ → ดึงข้อมูล GEE / น้ำฝน", fontSize = 13.sp, color = AppRed)
                        }
                    }
                } else {
                    // Show the first alert as a preview dashboard
                    val alert = alerts.first()
                    viewModel.getAlertDetails(alert.log_id)
                    val detail = viewModel.adminAlertDetail

                    AdminDashboardContent(detail = detail)
                }
            }
        }
    }
}

// ====== Shared Dashboard Content for AdminHome and AdminVerify ======
@Composable
fun AdminDashboardContent(detail: AdminAlertDetail?, showVerifyButtons: Boolean = false, onApprove: () -> Unit = {}, onReject: () -> Unit = {}) {
    if (detail == null) {
        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AppRed)
        }
        return
    }

    // Location Info
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("📍 ข้อมูลพื้นที่", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AppTextDark)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("ตำบล", fontSize = 12.sp, color = AppTextGrey)
                    Text(detail.tambon ?: "-", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AppTextDark)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("อำเภอ", fontSize = 12.sp, color = AppTextGrey)
                    Text(detail.district ?: "-", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AppTextDark)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("ระดับความเสี่ยง", fontSize = 12.sp, color = AppTextGrey)
                    val riskColor = when (detail.risk_level) { "High" -> Color.Red; "Medium" -> Color(0xFFFF9800); else -> AppGreen }
                    Text(detail.risk_level, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = riskColor)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("ความน่าจะเป็น", fontSize = 12.sp, color = AppTextGrey)
                    Text("${String.format("%.1f", detail.probability * 100)}%", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AppTextDark)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Lat: ${detail.latitude}, Lon: ${detail.longitude}", fontSize = 11.sp, color = AppTextGrey)
        }
    }

    Spacer(modifier = Modifier.height(4.dp))

    // Rainfall Chart (CHIRPS 10 days)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("🌧️ แนวโน้มน้ำฝน 10 วัน (CHIRPS)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AppTextDark)
            Spacer(modifier = Modifier.height(12.dp))

            val features = detail.features_json ?: emptyMap()
            val rainDays = (1..10).map { features["CHIRPS_Day_$it"] ?: 0f }
            val maxRain = rainDays.maxOrNull()?.takeIf { it > 0 } ?: 1f

            Row(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                rainDays.forEachIndexed { index, rain ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${rain.toInt()}", fontSize = 9.sp, color = AppTextGrey)
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .height(((rain / maxRain) * 80).dp.coerceAtLeast(4.dp))
                                .background(
                                    if (rain > maxRain * 0.7f) AppRed else if (rain > maxRain * 0.3f) Color(0xFFFF9800) else AppGreen,
                                    RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                )
                        )
                        Text("D${index + 1}", fontSize = 9.sp, color = AppTextGrey)
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(4.dp))

    // All 23 ML Features
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("📊 ค่า ML Features (23 ตัว)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AppTextDark)
            Spacer(modifier = Modifier.height(8.dp))

            val features = detail.features_json ?: emptyMap()
            val displayNames = listOf(
                "CHIRPS_Day_1", "CHIRPS_Day_2", "CHIRPS_Day_3", "CHIRPS_Day_4", "CHIRPS_Day_5",
                "CHIRPS_Day_6", "CHIRPS_Day_7", "CHIRPS_Day_8", "CHIRPS_Day_9", "CHIRPS_Day_10",
                "elevation_extracted", "slope_extracted", "aspect_extracted",
                "modis_lc", "ndvi", "ndwi", "twi",
                "soil_type", "road_zone",
                "Rain_3D_Prior", "Rain_5D_Prior", "Rain_7D_Prior", "Rain_10D_Prior"
            )

            val labelMap = mapOf(
                "elevation_extracted" to "Elevation",
                "slope_extracted" to "Slope",
                "aspect_extracted" to "Aspect",
                "modis_lc" to "MODIS LC",
                "ndvi" to "NDVI",
                "ndwi" to "NDWI",
                "twi" to "TWI",
                "soil_type" to "Soil Type",
                "road_zone" to "Distance to Road"
            )

            displayNames.chunked(2).forEach { pair ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { key ->
                        val label = labelMap[key] ?: key
                        val value = features[key]
                        Column(modifier = Modifier.weight(1f)) {
                            Text(label, fontSize = 11.sp, color = AppTextGrey)
                            Text(
                                if (value != null) String.format("%.3f", value) else "-",
                                fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppTextDark
                            )
                        }
                    }
                    if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }

    // Verify Buttons (only for AdminVerifyScreen)
    if (showVerifyButtons) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onReject,
                modifier = Modifier.weight(1f).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppRed),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("❌ ไม่ยืนยัน", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onApprove,
                modifier = Modifier.weight(1f).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("✅ ยืนยันแจ้งเตือน", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
