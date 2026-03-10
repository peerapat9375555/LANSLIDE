package com.example.landslideproject_cola

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
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
import kotlinx.coroutines.launch

@Composable
fun NotificationsScreen(
    navController: NavHostController,
    viewModel: EarthquakeViewModel
) {
    val context = LocalContext.current
    val sharedPref = SharedPreferencesManager(context)
    val userId = sharedPref.getSavedUserId()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }

    val tabs = listOf("ส่วนตัว", "ประวัติยืนยัน")
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        if (userId.isNotEmpty()) {
            viewModel.getNotifications(userId)
        }
        viewModel.getAlertHistory()
    }

    val notifications = viewModel.notifications

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(navController = navController, onClose = { scope.launch { drawerState.close() } })
        }
    ) {
        Scaffold(
            topBar = {
                GreenTopBar(title = "การแจ้งเตือน") { scope.launch { drawerState.open() } }
            },
            bottomBar = { AppBottomNav(navController) },
            containerColor = AppWhite
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("ค้นหา...") },
                    shape = RoundedCornerShape(24.dp),
                    leadingIcon = {
                        Icon(Icons.Default.Search, null, tint = AppTextGrey)
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppGreen,
                        unfocusedBorderColor = Color.LightGray,
                        cursorColor = AppGreen
                    )
                )

                val history = viewModel.alertHistory
                if (history.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("ไม่มีการแจ้งเตือนในขณะนี้", color = AppTextGrey, fontSize = 16.sp)
                    }
                } else {
                    val filtered = history.filter {
                        searchQuery.isEmpty() ||
                        (it.district?.contains(searchQuery, ignoreCase = true) == true) ||
                        (it.tambon?.contains(searchQuery, ignoreCase = true) == true) ||
                        (it.risk_level.contains(searchQuery, ignoreCase = true))
                    }

                    if (filtered.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("ไม่พบผลการค้นหา", color = AppTextGrey)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 16.dp, top = 8.dp)
                        ) {
                            items(filtered) { alert ->
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
                                            val riskColor = when (alert.risk_level) { 
                                                "High" -> Color.Red; 
                                                "Medium" -> Color(0xFFFF9800); 
                                                else -> AppGreen 
                                            }
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
}

@Composable
fun NotificationItemCard(notification: NotificationItem, onClick: () -> Unit) {
    val isRead = notification.is_read == 1

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(10.dp)
                    .background(if (isRead) Color.Transparent else AppRed, CircleShape)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.title,
                    fontSize = 15.sp,
                    fontWeight = if (isRead) FontWeight.Normal else FontWeight.Bold,
                    color = AppTextDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = notification.message,
                    fontSize = 13.sp,
                    color = AppTextDark.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notification.sent_at?.take(16)?.replace("T", " ") ?: "",
                    fontSize = 11.sp,
                    color = AppTextGrey
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Divider(color = Color.LightGray.copy(alpha = 0.4f), thickness = 0.5.dp)
    }
}
