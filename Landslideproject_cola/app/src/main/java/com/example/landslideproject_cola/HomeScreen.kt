package com.example.landslideproject_cola

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import kotlin.math.pow
import android.widget.Toast

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
        val userId = sharedPref.getSavedUserId()
        Toast.makeText(context, "Debug ID: '$userId'", android.widget.Toast.LENGTH_SHORT).show()
        if (userId.isNotEmpty()) {
            viewModel.getUserPins(userId)
            viewModel.getUserLocation(userId) // โหลดหมุดจากโปรไฟล์ด้วย
        }
    }

    // 1. หมุดแรกของ user (หาจาก map pins ก่อน ถ้าไม่มีค่อยเอาจาก profile)
    val userPins = viewModel.userPins
    val userLocation = viewModel.userLocation
    
    // สร้าง object กลางเพื่อใช้งานร่วมกัน
    val activePinLat = userPins.firstOrNull()?.latitude ?: userLocation?.latitude
    val activePinLon = userPins.firstOrNull()?.longitude ?: userLocation?.longitude
    val hasAnyPin = activePinLat != null && activePinLon != null && (activePinLat != 0.0 || activePinLon != 0.0)

    LaunchedEffect(userPins.size, userLocation) {
        if (userPins.isNotEmpty() || userLocation != null) {
            Toast.makeText(context, "โหลดข้อมูลหมุด/ตำแหน่งสำเร็จ", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // 2. โหลดกราฟน้ำฝน (rain_trend) - ตอนนี้ getPinDashboard รองรับแค่ pin_id จากแผนที่
    LaunchedEffect(userPins.firstOrNull()?.pin_id) {
        userPins.firstOrNull()?.pin_id?.let { viewModel.getPinDashboard(it) }
    }
    val pinDashboard = viewModel.pinDashboard

    // 3. หา prediction (จุดพยากรณ์) ที่ใกล้หมุดผู้ใช้ที่สุด
    val predictions = viewModel.predictions
    val nearestPrediction = remember(activePinLat, activePinLon, predictions) {
        if (hasAnyPin && predictions.isNotEmpty()) {
            predictions.minByOrNull { pred ->
                val dLat = pred.latitude - activePinLat!!
                val dLon = pred.longitude - activePinLon!!
                dLat * dLat + dLon * dLon
            }
        } else null
    }

    val recentEvents = viewModel.events.take(3)

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
                    .fillMaxWidth()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ---- ข้อมูลพื้นที่ใกล้หมุด (อิงจาก Prediction ที่ใกล้ที่สุด) ----
                if (hasAnyPin) {
                    NearbyAlertSection(
                        prediction = nearestPrediction,
                        pinData = pinDashboard,
                        userLocation = userLocation,
                        fallbackLat = activePinLat ?: 0.0,
                        fallbackLon = activePinLon ?: 0.0,
                        hasValidPin = hasAnyPin
                    )
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = AppWhite),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("📍", fontSize = 32.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("ยังไม่มีหมุดพื้นที่ส่วนตัว", fontWeight = FontWeight.Bold, color = AppTextDark)
                            Text("ไปที่แผนที่เพื่อเพิ่มบนหน้าสรุปนี้", fontSize = 12.sp, color = AppTextGrey)
                        }
                    }
                }
            }
        }
    }
}

// ====== Section: ข้อมูลพื้นที่ + กราฟน้ำฝน CHIRPS (ของหมุด) ======
@Composable
fun NearbyAlertSection(
    prediction: PredictionResponseItem?,
    pinData: UserPinDashboard?,
    userLocation: UserLocationData?,
    fallbackLat: Double,
    fallbackLon: Double,
    hasValidPin: Boolean
) {
    val riskColor = when {
        !hasValidPin -> AppTextGrey
        prediction?.risk_level == "High" -> Color(0xFFE53935)
        prediction?.risk_level == "Medium" -> Color(0xFFFF9800)
        prediction?.risk_level == "Low" -> AppGreen
        else -> AppGreen
    }
    val riskLabel = when {
        !hasValidPin -> "รอระบุพิกัด"
        prediction?.risk_level == "High" -> "สูง"
        prediction?.risk_level == "Medium" -> "ปานกลาง"
        prediction?.risk_level == "Low" -> "ต่ำ"
        else -> "ต่ำ (ปลอดภัย)"
    }

    val displayTambon = pinData?.label?.substringBefore(" ") ?: userLocation?.tambon ?: "ไม่ได้ระบุตำบล"
    val displayDistrict = userLocation?.district ?: "ไม่ได้ระบุอำเภอ"

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // ---- พิกัดของคุณ Card (แยกออกมาด้านบนสุด) ----
        if (hasValidPin) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = AppWhite),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📌 ", fontSize = 16.sp)
                    Column {
                        Text("พิกัดปัจจุบันของคุณ", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AppTextDark)
                        Text(
                            "Lat: ${"%.6f".format(fallbackLat)}, Lon: ${"%.6f".format(fallbackLon)}",
                            fontSize = 12.sp,
                            color = AppTextGrey
                        )
                    }
                }
            }
        }

        // ---- ข้อมูลพื้นที่ (Area Info) Card ----
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = AppWhite),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📍 ", fontSize = 16.sp)
                    Text(
                        "ข้อมูลพื้นที่เสี่ยง (บริเวณใกล้เคียง)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = AppTextDark
                    )
                }
                Divider(color = Color.LightGray)
                
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("ตำบล", fontSize = 12.sp, color = AppTextGrey)
                        Text(displayTambon, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = AppTextDark)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("อำเภอ", fontSize = 12.sp, color = AppTextGrey)
                        Text(displayDistrict, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = AppTextDark)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("ระดับความเสี่ยง", fontSize = 12.sp, color = AppTextGrey)
                        Text(riskLabel, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = riskColor)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("ความน่าจะเป็น", fontSize = 12.sp, color = AppTextGrey)
                        Text(
                            if (!hasValidPin) "-"
                            else if (prediction != null) "อิงสถิติ AI" 
                            else "พื้นที่ปกติ", 
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = AppTextDark
                        )
                    }
                }
                
                if (hasValidPin) {
                    if (prediction != null) {
                        Text(
                            "อ้างอิงจุดเสี่ยง AI: ${"%.6f".format(prediction.latitude)}, ${"%.6f".format(prediction.longitude)}",
                            fontSize = 11.sp,
                            color = AppRed.copy(alpha = 0.8f)
                        )
                    } else {
                        Text(
                            "ไม่พบจุดเสี่ยงในบริเวณนี้ ยึดตามพิกัดของคุณ",
                            fontSize = 11.sp,
                            color = AppTextGrey
                        )
                    }
                } else {
                    Text(
                        "⚠️ ระบบไม่พบพิกัด กรุณาตั้งค่าตำแหน่งในหน้าโปรไฟล์",
                        fontSize = 11.sp,
                        color = AppRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // ---- กราฟน้ำฝน 10 วัน (CHIRPS) Card ----
    val rain = (pinData?.rain_trend ?: List(10) { 0f }).take(10)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppWhite),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🌧️ ", fontSize = 16.sp)
                Text(
                    "แนวโน้มน้ำฝน 10 วัน (CHIRPS)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = AppTextDark
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            // Bar Chart
            val maxRain = rain.maxOrNull()?.coerceAtLeast(1f) ?: 1f
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                rain.forEachIndexed { index, value ->
                    val heightFraction = (value / maxRain).coerceIn(0f, 1f)
                    val barColor = when {
                        value >= maxRain * 0.75f -> Color(0xFFE53935)
                        value >= maxRain * 0.4f  -> Color(0xFFFF9800)
                        else                     -> AppGreen
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (value > 0f) "%.0f".format(value) else "0",
                            fontSize = 9.sp,
                            color = AppTextGrey
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height((120 * heightFraction).coerceAtLeast(4f).dp)
                                .background(barColor, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("D${index + 1}", fontSize = 9.sp, color = AppTextGrey)
                    }
                }
            }
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

// ====== Red Top Bar (เหมือน Admin) ======
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GreenTopBar(title: String, onMenuClick: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(
                    Icons.Default.Menu,
                    contentDescription = "เมนู",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = AppRed
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
            Triple(Icons.Default.LocationOn,    "แผนที่เสี่ยงดินถล่ม",     Screen.Predictions),
            Triple(Icons.Default.Notifications, "แจ้งเตือนดินถล่ม",  Screen.Notifications),
            Triple(Icons.Default.ReportProblem, "ขอความช่วยเหลือ",   Screen.UserReport),
            Triple(Icons.Default.Person,        "แก้ไขข้อมูลผู้ใช้", Screen.Profile),
            Triple(Icons.Default.Phone,         "เบอร์โทรฉุกเฉิน",   Screen.Emergency)
        )

        items.forEachIndexed { index, (icon, label, screen) ->
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
            // ขีดคั่นระหว่าง item (ยกเว้น item สุดท้าย)
            if (index < items.lastIndex) {
                Divider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = Color(0xFFBBBBBB),
                    thickness = 1.dp
                )
            }
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
                selectedIconColor = AppRed,
                unselectedIconColor = AppTextGrey,
                indicatorColor = Color.Transparent
            )
        )
        NavigationBarItem(
            selected = currentRoute == Screen.Predictions.route,
            onClick = { navController.navigate(Screen.Predictions.route) { launchSingleTop = true } },
            icon = { Icon(Icons.Default.LocationOn, null) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = AppRed,
                unselectedIconColor = AppTextGrey,
                indicatorColor = Color.Transparent
            )
        )
        NavigationBarItem(
            selected = currentRoute == Screen.Notifications.route,
            onClick = { navController.navigate(Screen.Notifications.route) { launchSingleTop = true } },
            icon = { Icon(Icons.Default.Notifications, null) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = AppRed,
                unselectedIconColor = AppTextGrey,
                indicatorColor = Color.Transparent
            )
        )
    }
}
