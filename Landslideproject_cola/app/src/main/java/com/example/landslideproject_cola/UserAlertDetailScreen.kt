package com.example.landslideproject_cola

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserAlertDetailScreen(
    navController: NavHostController,
    viewModel: EarthquakeViewModel,
    logId: String
) {
    val detail = viewModel.adminAlertDetail
    val isLoading = viewModel.isLoading

    LaunchedEffect(logId) {
        if (logId.isNotEmpty()) {
            viewModel.getAlertDetails(logId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("รายละเอียดการแจ้งเตือน", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppRed)
            )
        },
        containerColor = AppGrey
    ) { paddingValues ->
        if (isLoading || detail == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AppRed)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("📍 พื้นที่ที่ได้รับผลกระทบ", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("ต.${detail.tambon ?: "-"} อ.${detail.district ?: "-"}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)

                        val riskColor = if(detail.risk_level == "High") AppRed else Color(0xFFFF9800)
                        Text("ระดับความเสี่ยง: ${detail.risk_level}", color = riskColor, fontWeight = FontWeight.Bold)

                        Text("เวลาที่ตรวจพบ: ${detail.timestamp ?: "-"}", fontSize = 12.sp, color = Color.Gray)
                    }
                }

                // Rain Chart
                Text("🌧️ แนวโน้มปริมาณน้ำฝน (CHIRPS)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Card(
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    val features = detail.features_json ?: emptyMap()
                    val rainDays = (1..10).map { features["CHIRPS_Day_$it"] ?: 0f }
                    val maxRain = rainDays.maxOrNull()?.coerceAtLeast(1f) ?: 1f

                    Row(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        rainDays.forEachIndexed { index, rain ->
                            val heightPercent = rain / maxRain
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("%.0f".format(rain), fontSize = 9.sp, color = Color.Gray)
                                Box(
                                    modifier = Modifier
                                        .width(20.dp)
                                        .height((140 * heightPercent).dp.coerceAtLeast(4.dp))
                                        .background(
                                            if (rain > 100) AppRed else if (rain > 50) Color(0xFFFF9800) else AppGreen,
                                            RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                        )
                                )
                                Text("D${index + 1}", fontSize = 9.sp)
                            }
                        }
                    }
                }

                // Warning Note
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AppRed.copy(alpha = 0.1f))
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("⚠️ คำแนะนำ: โปรดเฝ้าระวังและติดตามสถานการณ์อย่างใกล้ชิด หากมีฝนตกหนักต่อเนื่องควรเตรียมพร้อมอพยพ", fontSize = 14.sp, color = AppRed, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
