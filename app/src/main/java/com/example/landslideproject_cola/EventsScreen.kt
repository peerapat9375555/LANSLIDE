package com.example.landslideproject_cola

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import kotlinx.coroutines.launch

@Composable
fun EventsScreen(
    navController: NavHostController,
    viewModel: EarthquakeViewModel
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { viewModel.getEvents() }
    val events = viewModel.events

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(navController = navController, onClose = { scope.launch { drawerState.close() } })
        }
    ) {
        Scaffold(
            topBar = {
                GreenTopBar(title = "เหตุการณ์ดินถล่ม") { scope.launch { drawerState.open() } }
            },
            bottomBar = { AppBottomNav(navController) },
            containerColor = AppGrey
        ) { padding ->
            if (events.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("ไม่มีข้อมูลเหตุการณ์", color = AppTextGrey)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(events) { event ->
                        EventListCard(event)
                    }
                }
            }
        }
    }
}

@Composable
fun EventListCard(event: LandslideEvent) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppWhite),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(
                        if (event.verified == 1) AppGreen else AppRed,
                        CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.district ?: "ไม่ระบุพื้นที่",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = AppTextDark
                )
                Text(
                    text = event.occurred_at?.take(16) ?: "-",
                    fontSize = 12.sp,
                    color = AppTextGrey
                )
                event.level_name?.let {
                    Text(text = "ระดับ: $it", fontSize = 12.sp, color = AppRed)
                }
            }
            Text(
                text = if (event.verified == 1) "✓ ยืนยัน" else "⚠ รอยืนยัน",
                fontSize = 11.sp,
                color = if (event.verified == 1) AppGreen else Color(0xFFFF9800),
                fontWeight = FontWeight.Medium
            )
        }
    }
}
