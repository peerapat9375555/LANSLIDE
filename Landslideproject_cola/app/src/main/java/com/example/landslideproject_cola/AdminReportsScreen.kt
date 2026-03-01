package com.example.landslideproject_cola

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

// ============================================================
//  ADMIN REPORTS SCREEN  (รายงานรอดำเนินการ)
// ============================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminReportsScreen(
    navController: NavHostController,
    viewModel: EarthquakeViewModel
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val reports = viewModel.userReports
    val isLoading = viewModel.isLoading

    LaunchedEffect(Unit) { viewModel.getAdminReports() }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AdminDrawer(navController = navController, onClose = { scope.launch { drawerState.close() } })
        }
    ) {
        Scaffold(
            topBar = {
                AdminTopBar(
                    title = "รายงานจากผู้ใช้ (${reports.size})",
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            },
            bottomBar = { AdminBottomNavigationBar(navController, currentRoute) },
            floatingActionButton = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // ปุ่มไปหน้า History
                    SmallFloatingActionButton(
                        onClick = { navController.navigate(Screen.AdminReportHistory.route) },
                        containerColor = AppGreen,
                        contentColor = Color.White
                    ) {
                        Icon(Icons.Default.History, contentDescription = "ประวัติ", modifier = Modifier.size(20.dp))
                    }
                    // ปุ่ม Refresh
                    FloatingActionButton(
                        onClick = { viewModel.getAdminReports() },
                        containerColor = AppRed,
                        contentColor = Color.White,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "รีเฟรช", modifier = Modifier.size(22.dp))
                    }
                }
            },
            containerColor = AppGrey
        ) { paddingValues ->
            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = AppRed)
                            Spacer(Modifier.height(8.dp))
                            Text("กำลังโหลด...", color = AppTextGrey, fontSize = 14.sp)
                        }
                    }
                }
                reports.isEmpty() -> {
                    Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Inbox, null, modifier = Modifier.size(72.dp), tint = Color.LightGray)
                            Spacer(Modifier.height(12.dp))
                            Text("ไม่มีรายงานที่รอดำเนินการ", color = AppTextGrey, fontSize = 16.sp)
                            Spacer(Modifier.height(4.dp))
                            TextButton(onClick = { navController.navigate(Screen.AdminReportHistory.route) }) {
                                Icon(Icons.Default.History, null, tint = AppGreen, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("ดูประวัติการช่วยเหลือ", color = AppGreen)
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        items(reports, key = { it.report_id }) { report ->
                            AdminReportCard(
                                report = report,
                                onComplete = {
                                    viewModel.completeReport(context, report.report_id) {}
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
//  REPORT CARD  (ใช้ร่วมกันระหว่าง Pending & History)
// ============================================================
@Composable
fun AdminReportCard(
    report: UserReportItem,
    onComplete: (() -> Unit)? = null,   // null = หน้า history ไม่แสดงปุ่ม
    showCompletedBadge: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // ---- รูปภาพ ----
            if (!report.img_url.isNullOrBlank()) {
                AsyncImage(
                    model = report.img_url,
                    contentDescription = "รูปประกอบ",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Column(modifier = Modifier.padding(14.dp)) {

                // ---- หัวข้อ + วันที่ + badge ----
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Flag, null, tint = AppRed, modifier = Modifier.size(18.dp))
                        Text(report.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AppTextDark)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(report.created_at?.take(10) ?: "", fontSize = 11.sp, color = AppTextGrey)
                        if (showCompletedBadge) {
                            Spacer(Modifier.height(2.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = AppGreen.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    "✓ ช่วยเหลือแล้ว",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppGreen
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                // ---- ผู้รายงาน ----
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Person, null, tint = AppTextGrey, modifier = Modifier.size(14.dp))
                    Text(report.user_name ?: "ผู้ใช้ไม่ระบุ", fontSize = 12.sp, color = AppTextGrey)
                    Text("•", fontSize = 12.sp, color = Color.LightGray)
                    Text(report.created_at?.replace("T", " ")?.take(16) ?: "", fontSize = 12.sp, color = AppTextGrey)
                }

                Spacer(Modifier.height(10.dp))

                // ---- ข้อความ ----
                Text(report.message, fontSize = 14.sp, color = AppTextDark, lineHeight = 20.sp)

                // ---- พิกัด ----
                Spacer(Modifier.height(10.dp))
                Divider(color = Color(0xFFEEEEEE))
                Spacer(Modifier.height(8.dp))

                val hasCoords = report.latitude != null && report.longitude != null
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (hasCoords) AppGreen.copy(alpha = 0.08f) else Color(0xFFF5F5F5),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.LocationOn, null,
                        tint = if (hasCoords) AppGreen else Color.LightGray,
                        modifier = Modifier.size(20.dp))
                    Column {
                        if (hasCoords) {
                            val locationLabel = when {
                                !report.tambon.isNullOrBlank() && !report.district.isNullOrBlank() ->
                                    "ต.${report.tambon}  อ.${report.district}"
                                !report.district.isNullOrBlank() -> "อ.${report.district}"
                                !report.tambon.isNullOrBlank() -> "ต.${report.tambon}"
                                else -> null
                            }
                            if (locationLabel != null)
                                Text(locationLabel, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppTextDark)
                            Text(
                                "${"%.5f".format(report.latitude)},  ${"%.5f".format(report.longitude)}",
                                fontSize = 12.sp, color = AppTextGrey
                            )
                        } else {
                            Text("ไม่พบพิกัดของผู้ใช้", fontSize = 13.sp, color = Color.Gray)
                        }
                    }
                }

                // ---- ปุ่มช่วยเหลือเสร็จสิ้น (เฉพาะหน้า pending) ----
                if (onComplete != null) {
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onComplete,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppGreen)
                    ) {
                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp), tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("ช่วยเหลือเสร็จสิ้น", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }

                // ---- วันที่ช่วยเหลือเสร็จ (เฉพาะ history) ----
                if (showCompletedBadge && !report.completed_at.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = AppGreen, modifier = Modifier.size(14.dp))
                        Text(
                            "ช่วยเหลือเสร็จ: ${report.completed_at.replace("T", " ").take(16)}",
                            fontSize = 12.sp, color = AppGreen
                        )
                    }
                }
            }
        }
    }
}
