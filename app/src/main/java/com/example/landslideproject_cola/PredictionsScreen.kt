package com.example.landslideproject_cola

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch

// หน้าแผนที่แสดงพื้นที่เสี่ยง (Risk Map - Mockup style)
@Composable
fun PredictionsScreen(
    navController: NavHostController,
    viewModel: EarthquakeViewModel
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(navController = navController, onClose = { scope.launch { drawerState.close() } })
        }
    ) {
        Scaffold(
            topBar = {
                GreenTopBar(title = "Map") { scope.launch { drawerState.open() } }
            },
            bottomBar = { AppBottomNav(navController) },
            containerColor = AppWhite
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // แผนที่ placeholder (สีน้ำตาลอ่อนแสดงพื้นที่)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp)
                        .background(Color(0xFFE8D5B7), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🗺️", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("แผนที่พื้นที่เสี่ยงดินถล่ม", color = AppTextGrey, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Red dot markers
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(14.dp).background(Color.Red, CircleShape))
                            Box(modifier = Modifier.size(14.dp).background(Color(0xFF4CAF50), CircleShape))
                        }
                    }
                }

                // Legend
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(14.dp).background(Color(0xFF4CAF50), RoundedCornerShape(3.dp)))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("จุดที่เราอยู่", fontSize = 13.sp, color = AppTextDark)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(14.dp).background(Color.Red, RoundedCornerShape(3.dp)))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("จุดเสี่ยง", fontSize = 13.sp, color = AppTextDark)
                    }
                }
            }
        }
    }
}
