package com.example.landslideproject_cola

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
fun AdminAlertsScreen(
    navController: NavHostController,
    viewModel: EarthquakeViewModel
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val alerts = viewModel.pendingAlerts
    val isLoading = viewModel.isLoading

    LaunchedEffect(Unit) {
        viewModel.getPendingAlerts()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = { AdminDrawer(navController = navController, onClose = { scope.launch { drawerState.close() } }) }
    ) {
        Scaffold(
            topBar = { AdminTopBar(title = "รายชื่อเหตุแจ้งเตือน", onMenuClick = { scope.launch { drawerState.open() } }) },
            bottomBar = { AdminBottomNavigationBar(navController, currentRoute) },
            containerColor = AppGrey
        ) { paddingValues ->
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AppRed)
                }
            } else if (alerts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Text("ไม่มีเหตุการณ์ที่ต้องยืนยันในขณะนี้", color = AppTextGrey, fontSize = 16.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(alerts) { alert ->
                        AdminAlertCard(alert = alert) {
                            navController.navigate(Screen.AdminVerify.createRoute(alert.log_id))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminAlertCard(alert: PendingAlert, onClick: () -> Unit) {
    val riskColor = when (alert.risk_level) { "High" -> Color.Red; "Medium" -> Color(0xFFFF9800); else -> AppGreen }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(riskColor, shape = RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ความเสี่ยง: ${if (alert.risk_level == "High") "สูง" else "ปานกลาง"} (รอยืนยัน)",
                    fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AppTextDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "ต.${alert.tambon ?: "-"} อ.${alert.district ?: "-"}",
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppTextDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "ละติจูด: ${"%.4f".format(alert.latitude)}, ลองจิจูด: ${"%.4f".format(alert.longitude)}",
                    fontSize = 12.sp, color = AppTextGrey
                )
                Text(
                    "ความน่าจะเป็น: ${(alert.probability * 100).toInt()}%",
                    fontSize = 12.sp, color = AppTextGrey
                )
                Text(text = alert.timestamp ?: "", fontSize = 11.sp, color = Color.Gray)
            }
        }
    }
}
