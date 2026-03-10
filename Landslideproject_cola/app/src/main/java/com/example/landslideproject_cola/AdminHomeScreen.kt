package com.example.landslideproject_cola

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
fun AdminHomeScreen(navController: NavHostController, viewModel: EarthquakeViewModel, logId: String? = null) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    LaunchedEffect(Unit) {
        viewModel.getPendingAlerts()
    }

    // Sync nav arg logId into ViewModel (only when first arriving from Alerts list)
    LaunchedEffect(logId) {
        if (logId != null) {
            viewModel.setDashboardLogId(logId)
        }
    }

    // Use ViewModel as single source of truth for current detail
    val activeLogId = viewModel.currentDashboardLogId

    val alerts = viewModel.pendingAlerts.sortedByDescending { it.probability }
    val isLoading = viewModel.isLoading

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = { AdminDrawer(navController = navController, onClose = { scope.launch { drawerState.close() } }) }
    ) {
        Scaffold(
            topBar = { AdminTopBar(title = "Admin Dashboard", onMenuClick = { scope.launch { drawerState.open() } }) },
            bottomBar = { AdminBottomNavigationBar(navController, currentRoute) },
            containerColor = AppGrey
        ) { padding ->
            // --- Case 1: Displaying Details for a specific Log ID ---
            if (activeLogId != null) {
                LaunchedEffect(activeLogId) {
                    // Only fetch if we don't already have the correct detail cached
                    if (viewModel.adminAlertDetail?.log_id != activeLogId) {
                        viewModel.getAlertDetails(activeLogId)
                    }
                }
                val alertDetail = viewModel.adminAlertDetail

                if (alertDetail == null || alertDetail.log_id != activeLogId) {
                    Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AppRed)
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Back button to return to list
                        TextButton(
                            onClick = {
                                viewModel.setDashboardLogId(null)
                                viewModel.getPendingAlerts()
                            }
                        ) {
                            Text("← กลับหน้ารายการ", color = AppRed, fontWeight = FontWeight.Bold)
                        }

                        AdminDashboardContent(
                            detail = alertDetail,
                            showVerifyButtons = true,
                            onApprove = {
                                viewModel.verifyAlert(context, activeLogId, "approve") {
                                    viewModel.setDashboardLogId(null)
                                    viewModel.getPendingAlerts()
                                }
                            },
                            onReject = {
                                viewModel.verifyAlert(context, activeLogId, "reject") {
                                    viewModel.setDashboardLogId(null)
                                    viewModel.getPendingAlerts()
                                }
                            },
                            onViewMap = { lat: Double, lon: Double ->
                                navController.navigate(Screen.AdminMap.createRoute(lat.toFloat(), lon.toFloat()))
                            }
                        )
                    }
                }
            } else {
                // --- Case 2: Displaying the Main Home Dashboard Summary ---
                when {
                    isLoading -> {
                        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = AppRed)
                        }
                    }
                    alerts.isEmpty() -> {
                        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                            Card(
                                modifier = Modifier.padding(24.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("ยังไม่มีข้อมูลจากการวิเคราะห์", fontSize = 16.sp, color = AppTextGrey)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("กรุณาดึงข้อมูลน้ำฝนและวิเคราะห์ก่อน", fontSize = 13.sp, color = AppTextGrey)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("ไปที่เมนู ☰ → ดึงข้อมูล GEE / น้ำฝน", fontSize = 13.sp, color = AppRed)
                                }
                            }
                        }
                    }
                else -> {
                    // --- Section Header ---
                    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "รายชื่อเหตุแจ้งเตือน",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = AppTextDark
                            )
                            Text(
                                "${alerts.size} รายการ",
                                fontSize = 13.sp,
                                color = AppTextGrey
                            )
                        }

                        // --- Alert List ---
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(alerts) { alert ->
                                val ctx = androidx.compose.ui.platform.LocalContext.current
                                AdminAlertCard(
                                    alert = alert,
                                    onClick = { viewModel.setDashboardLogId(alert.log_id) },
                                    showButtons = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
}

// ====== Shared Dashboard Content for AdminHome and AdminVerify ======
@Composable
fun AdminDashboardContent(
    detail: AdminAlertDetail?, 
    showVerifyButtons: Boolean = false, 
    onApprove: () -> Unit = {}, 
    onReject: () -> Unit = {},
    onViewMap: ((Double, Double) -> Unit)? = null
) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Lat: ${detail.latitude}, Lon: ${detail.longitude}", 
                    fontSize = 11.sp, 
                    color = AppTextGrey
                )
                if (onViewMap != null) {
                    TextButton(
                        onClick = { onViewMap(detail.latitude, detail.longitude) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("📍 ดูบนแผนที่", fontSize = 12.sp, color = AppRed, fontWeight = FontWeight.Bold)
                    }
                }
            }
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
