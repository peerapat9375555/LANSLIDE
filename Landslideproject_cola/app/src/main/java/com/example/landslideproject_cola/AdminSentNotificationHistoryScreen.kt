package com.example.landslideproject_cola

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import java.util.Calendar

@Composable
fun AdminSentNotificationHistoryScreen(navController: NavHostController, viewModel: EarthquakeViewModel) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = { AdminDrawer(navController = navController, onClose = { scope.launch { drawerState.close() } }) }
    ) {
        Scaffold(
            topBar = { AdminTopBar(title = "ประวัติการแจ้งเตือน", onMenuClick = { scope.launch { drawerState.open() } }) },
            bottomBar = { AdminBottomNavigationBar(navController, currentRoute) },
            containerColor = AppGrey
        ) { padding ->
            
            var startDate by remember { mutableStateOf<String?>(null) }
            var endDate by remember { mutableStateOf<String?>(null) }
            val history = viewModel.sentNotificationHistory
            val context = LocalContext.current
            
            // Calendar instance for DatePicker
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val startDatePickerDialog = DatePickerDialog(
                context,
                { _, selectedYear, selectedMonth, selectedDay ->
                    startDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                }, year, month, day
            )

            val endDatePickerDialog = DatePickerDialog(
                context,
                { _, selectedYear, selectedMonth, selectedDay ->
                    endDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                }, year, month, day
            )

            LaunchedEffect(startDate, endDate) {
                viewModel.getSentNotificationHistory(startDate, endDate)
            }

            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                
                // Date Filter UI
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("ตัวกรองวันที่", fontSize = 12.sp, color = AppTextGrey)
                            val dateText = if (startDate != null && endDate != null) {
                                "$startDate ถึง $endDate"
                            } else if (startDate != null) {
                                "ตั้งแต่ $startDate"
                            } else if (endDate != null) {
                                "ถึง $endDate"
                            } else {
                                "ทั้งหมด (ไม่ต้องกรอง)"
                            }
                            Text(dateText, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppTextDark)
                        }
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (startDate != null || endDate != null) {
                                TextButton(onClick = { 
                                    startDate = null
                                    endDate = null 
                                }) {
                                    Text("เคลียร์", color = AppRed, fontSize = 12.sp)
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Button(
                                    onClick = { startDatePickerDialog.show() },
                                    colors = ButtonDefaults.buttonColors(containerColor = AppRed),
                                    modifier = Modifier.height(30.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Text("เริ่ม", fontSize = 11.sp)
                                }
                                Button(
                                    onClick = { endDatePickerDialog.show() },
                                    colors = ButtonDefaults.buttonColors(containerColor = AppRed),
                                    modifier = Modifier.height(30.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Text("สิ้นสุด", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                if (history.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("ยังไม่มีประวัติการแจ้งเตือน", fontSize = 16.sp, color = AppTextGrey)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        items(history) { alert ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                onClick = {
                                    viewModel.currentDashboardLogId = alert.log_id
                                    navController.navigate(Screen.AdminHome.createRoute()) {
                                        popUpTo(Screen.AdminHome.route) { inclusive = false }
                                    }
                                },
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val displayRiskText = if (alert.status == "approved") "✅ โดนยืนยันการแจ้งเตือน" else "⚠️ ตรวจพบความเสี่ยงจากโมเดล"
                                        val displayColor = if (alert.status == "approved") AppGreen else Color(0xFFE65100) // Darker Orange
                                        
                                        Text(displayRiskText, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = displayColor)
                                        Spacer(modifier = Modifier.weight(1f))
                                        val riskColor = when (alert.risk_level) { "High" -> Color.Red; "Medium" -> Color(0xFFFF9800); else -> AppGreen }
                                        Text(alert.risk_level, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = riskColor)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("ต.${alert.tambon ?: "-"} อ.${alert.district ?: "-"}", fontSize = 13.sp, color = AppTextDark)
                                    Text("ความน่าจะเป็น: ${String.format("%.1f", alert.probability * 100)}%", fontSize = 12.sp, color = AppTextGrey)
                                    Text("เวลา: ${alert.timestamp ?: "-"}", fontSize = 11.sp, color = AppTextGrey)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
