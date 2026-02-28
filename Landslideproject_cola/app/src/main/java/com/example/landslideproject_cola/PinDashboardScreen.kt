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
fun PinDashboardScreen(
    navController: NavHostController,
    viewModel: EarthquakeViewModel,
    pinId: String
) {
    val pinData = viewModel.pinDashboard
    val isLoading = viewModel.isLoading

    LaunchedEffect(pinId) {
        if (pinId.isNotEmpty()) {
            viewModel.getPinDashboard(pinId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ข้อมูลสำหรับจุดของคุณ", color = AppWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = AppWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppGreen)
            )
        }
    ) { paddingValues ->
        if (isLoading || pinData == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AppGreen)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppGrey)
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AppWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = pinData.label ?: "จุดปักหมุด",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = AppGreenDark
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("ละติจูด: ${pinData.latitude}", fontSize = 14.sp, color = AppTextGrey)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("ลองจิจูด: ${pinData.longitude}", fontSize = 14.sp, color = AppTextGrey)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                // Rain Trend Simple Bar Chart
                val rainValues = pinData.rain_trend ?: List(10) { 0f }
                if (rainValues.isNotEmpty()) {
                    Text("แนวโน้มปริมาณน้ำฝน (10 วัน)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AppTextDark)
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        colors = CardDefaults.cardColors(containerColor = AppWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            val maxRain = rainValues.maxOrNull()?.coerceAtLeast(1f) ?: 1f
                            rainValues.forEachIndexed { index, rain ->
                                val heightPercent = rain / maxRain
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                                    Text(text = "%.1f".format(rain), fontSize = 10.sp, color = AppTextGrey)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .width(20.dp)
                                            .height((130 * heightPercent).dp) // Scaled height
                                            .background(Color(0xFF42A5F5), RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("D${index+1}", fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
