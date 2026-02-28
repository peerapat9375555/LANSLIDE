package com.example.landslideproject_cola

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.launch

@Composable
fun AdminNotificationHistoryScreen(navController: NavHostController, viewModel: EarthquakeViewModel) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    LaunchedEffect(Unit) {
        viewModel.getAlertHistory()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = { AdminDrawer(navController = navController, onClose = { scope.launch { drawerState.close() } }) }
    ) {
        Scaffold(
            topBar = { AdminTopBar(title = "ประวัติการแจ้งเตือน", onMenuClick = { scope.launch { drawerState.open() } }) },
            bottomBar = { AdminBottomNavigationBar(navController, currentRoute) },
            containerColor = AppGrey
        ) { padding ->
            val history = viewModel.alertHistory

            if (history.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("ยังไม่มีประวัติการแจ้งเตือน", fontSize = 16.sp, color = AppTextGrey)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(history) { alert ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("✅ ", fontSize = 18.sp)
                                    Text("แจ้งเตือนแล้ว", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AppGreen)
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
