package com.example.landslideproject_cola

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: EarthquakeViewModel
) {
    val context = LocalContext.current
    val sharedPref = SharedPreferencesManager(context)
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.getPredictions()
        viewModel.getEvents()
    }

    val predictions = viewModel.predictions

    // ====== Drawer + Main ======
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                navController = navController,
                onClose = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            topBar = {
                GreenTopBar(
                    title = "Dashboard",
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            },
            bottomBar = { AppBottomNav(navController) },
            containerColor = AppGrey
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ---- Analysis Chart Card ----
                AnalysisCard()

                // ---- Rainfall Card ----
                RainfallCard()

                // ---- Subscribe Button ----
                Button(
                    onClick = { navController.navigate(Screen.Notifications.route) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppGreen)
                ) {
                    Text("รับการแจ้งเตือนแผนที่ ●", color = AppWhite, fontWeight = FontWeight.Bold)
                }

                // ---- Section: รายชื่อเหตุแจ้งเตือน ----
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "รายชื่อเหตุแจ้งเตือน",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = AppTextDark
                    )
                    TextButton(onClick = { navController.navigate(Screen.Notifications.route) }) {
                        Text("ดูทั้งหมด", fontSize = 13.sp, color = AppGreen)
                    }
                }

                if (predictions.isEmpty()) {
                    // Loading / Empty state
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = AppWhite),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "ไม่มีข้อมูลแจ้งเตือน",
                                color = AppTextGrey,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    // แสดงรายการ predictions (สูงสุด 10 รายการ)
                    predictions.take(10).forEach { pred ->
                        AlertNotifCard(
                            prediction = pred,
                            onClick = {
                                navController.navigate(
                                    Screen.NotificationDetail.createRoute(pred.prediction_id)
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// ====== Alert Notification Card ======
@Composable
fun AlertNotifCard(prediction: LandslidePrediction, onClick: () -> Unit) {
    val riskLevel = prediction.risk_level ?: "Low"
    val dotColor = when (riskLevel.lowercase()) {
        "high" -> Color(0xFFE53935)
        "medium" -> Color(0xFFFF9800)
        else -> Color(0xFF4CAF50)
    }
    val conf = prediction.confidence ?: 0f
    val confText = "${String.format("%.1f", conf * 100)}%"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppWhite),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // สีจุดบ่งบอกระดับความเสี่ยง
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(dotColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // ชื่อ: ระดับความเสี่ยง
                val riskLabel = when (riskLevel.lowercase()) {
                    "high" -> "สูง"
                    "medium" -> "ปานกลาง"
                    else -> "ต่ำ"
                }
                Text(
                    "ความเสี่ยง: $riskLabel (รอยืนยัน)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = AppTextDark
                )
                Spacer(modifier = Modifier.height(3.dp))

                // ตำบล อำเภอ
                if (!prediction.district.isNullOrEmpty()) {
                    Text(
                        prediction.district,
                        fontSize = 13.sp,
                        color = AppTextGrey
                    )
                }

                // Lat / Lon
                val lat = prediction.latitude ?: 0.0
                val lon = prediction.longitude ?: 0.0
                Text(
                    "ละติจูด: ${String.format("%.4f", lat)}, ลองจิจูด: ${String.format("%.4f", lon)}",
                    fontSize = 12.sp,
                    color = AppTextGrey
                )

                // ความน่าจะเป็น
                Text(
                    "ความน่าจะเป็น: $confText",
                    fontSize = 12.sp,
                    color = AppTextGrey
                )

                // วันที่
                if (!prediction.analyzed_at.isNullOrEmpty()) {
                    Text(
                        prediction.analyzed_at.take(19).replace("T", " "),
                        fontSize = 11.sp,
                        color = AppTextGrey
                    )
                }
            }

            // ลูกศรชี้ขวา
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = AppTextGrey,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}


@Composable
fun AnalysisCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppWhite),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Landslide Analysis", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AppTextDark)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Analysis", fontSize = 13.sp, color = AppTextGrey)
            Spacer(modifier = Modifier.height(12.dp))

            // Simple chart placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(AppGrey, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("📈 กราฟวิเคราะห์ดินถล่ม", color = AppTextGrey, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun RainfallCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppWhite),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("7-Day accumulate", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AppTextDark)
                Text("Rain fall", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AppTextDark)
                Text("220 mm", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = AppGreen)
            }
            Text("🌧️", fontSize = 52.sp)
        }
    }
}

// ====== Green Top Bar (TopAppBar handles status bar insets automatically) ======
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GreenTopBar(title: String, onMenuClick: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = AppTextDark
            )
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(
                    Icons.Default.Menu,
                    contentDescription = "เมนู",
                    tint = AppGreenDark,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = AppWhite
        )
    )
}


// ====== Navigation Drawer ======
@Composable
fun AppDrawer(navController: NavHostController, onClose: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPref = SharedPreferencesManager(context)

    ModalDrawerSheet(
        drawerContainerColor = AppWhite,
        modifier = Modifier.width(260.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        val items = listOf(
            Triple(Icons.Default.Home,          "Dashboard",          Screen.Home),
            Triple(Icons.Default.LocationOn,    "Map",                Screen.Events),
            Triple(Icons.Default.Notifications, "แจ้งเตือนดินถล่ม",  Screen.Notifications),
            Triple(Icons.Default.Person,        "แก้ไขข้อมูลผู้ใช้", Screen.Profile),
            Triple(Icons.Default.Phone,         "เบอร์โทรฉุกเฉิน",   Screen.Emergency)
        )

        items.forEach { (icon, label, screen) ->
            NavigationDrawerItem(
                icon = { Icon(icon, null, tint = AppTextDark) },
                label = { Text(label, color = AppTextDark, fontSize = 15.sp) },
                selected = false,
                onClick = {
                    onClose()
                    navController.navigate(screen.route) { launchSingleTop = true }
                },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }

        // ดัน logout ลงล่างสุด
        Spacer(modifier = Modifier.weight(1f))

        Divider(modifier = Modifier.padding(horizontal = 16.dp), color = Color.LightGray)
        Spacer(modifier = Modifier.height(8.dp))

        // ====== ปุ่ม Logout สีแดง ======
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.ExitToApp, null, tint = AppRed) },
            label = {
                Text(
                    "ออกจากระบบ",
                    color = AppRed,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            selected = false,
            onClick = {
                onClose()
                sharedPref.logout()
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            colors = NavigationDrawerItemDefaults.colors(
                unselectedContainerColor = AppRed.copy(alpha = 0.08f)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}


// ====== Bottom Navigation ======
@Composable
fun AppBottomNav(navController: NavHostController) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    NavigationBar(containerColor = AppWhite) {
        NavigationBarItem(
            selected = currentRoute == Screen.Home.route,
            onClick = { navController.navigate(Screen.Home.route) { launchSingleTop = true } },
            icon = { Icon(Icons.Default.Home, null) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = AppGreen,
                unselectedIconColor = AppTextGrey,
                indicatorColor = Color.Transparent
            )
        )
        NavigationBarItem(
            selected = currentRoute == Screen.Predictions.route,
            onClick = { navController.navigate(Screen.Predictions.route) { launchSingleTop = true } },
            icon = { Icon(Icons.Default.LocationOn, null) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = AppGreen,
                unselectedIconColor = AppTextGrey,
                indicatorColor = Color.Transparent
            )
        )
        NavigationBarItem(
            selected = currentRoute == Screen.Notifications.route,
            onClick = { navController.navigate(Screen.Notifications.route) { launchSingleTop = true } },
            icon = { Icon(Icons.Default.Notifications, null) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = AppGreen,
                unselectedIconColor = AppTextGrey,
                indicatorColor = Color.Transparent
            )
        )
    }
}
