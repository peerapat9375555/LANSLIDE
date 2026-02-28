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

    LaunchedEffect(Unit) {
        if (userId.isNotEmpty()) {
            viewModel.getNotifications(userId)
        }
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
                    placeholder = { Text("ค้นหาการแจ้งเตือน") },
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

                if (notifications.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("ไม่มีการแจ้งเตือนในขณะนี้", color = AppTextGrey, fontSize = 16.sp)
                    }
                } else {
                    val filtered = notifications.filter {
                        searchQuery.isEmpty() ||
                                it.title.contains(searchQuery, ignoreCase = true) ||
                                it.message.contains(searchQuery, ignoreCase = true)
                    }

                    if (filtered.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("ไม่พบผลการค้นหา", color = AppTextGrey)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(filtered) { notif ->
                                NotificationItemCard(
                                    notification = notif,
                                    onClick = {
                                        // 1. Mark as read
                                        viewModel.markRead(notif.notification_id, userId)

                                        // 2. Navigate to detail screen if log_id exists
                                        val logId = notif.log_id
                                        if (logId != null && logId.isNotEmpty()) {
                                            navController.navigate(Screen.UserAlertDetail.createRoute(logId))
                                        }
                                    }
                                )
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
