package com.example.landslideproject_cola

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationDetailScreen(
    navController: NavHostController,
    viewModel: EarthquakeViewModel,
    predictionId: String
) {
    // หา prediction ที่ตรงกับ predictionId
    val prediction = viewModel.predictions.find { it.prediction_id == predictionId }

    // ค่า CHIRPS Day 1-10 จาก ML Features (ถ้ามีข้อมูลจริง ดึงจาก prediction)
    // ในที่นี้ใช้ค่า mock ที่สอดคล้องกับ risk score
    val riskScore = prediction?.risk_score ?: 0f
    val chirpsValues = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0.2f, riskScore.coerceAtMost(2f), 0f, 0f)
    val maxChirps = chirpsValues.maxOrNull()?.coerceAtLeast(1f) ?: 1f

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "ข้อมูลพื้นที่",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = AppTextDark
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "ย้อนกลับ",
                            tint = AppTextDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppWhite)
            )
        },
        containerColor = AppGrey
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ====== Section 1: ข้อมูลพื้นที่ ======
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = AppWhite),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📍", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "ข้อมูลพื้นที่",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = AppTextDark
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = Color.LightGray)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Row: ตำบล / อำเภอ
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("ตำบล", fontSize = 12.sp, color = AppTextGrey)
                            Spacer(modifier = Modifier.height(2.dp))
                            val districtParts = prediction?.district?.split(" ") ?: listOf("-")
                            Text(
                                districtParts.firstOrNull() ?: "-",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = AppTextDark
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("อำเภอ", fontSize = 12.sp, color = AppTextGrey)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                districtParts.drop(1).joinToString(" ").ifEmpty { "-" },
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = AppTextDark
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Row: ระดับความเสี่ยง / ความน่าจะเป็น
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("ระดับความเสี่ยง", fontSize = 12.sp, color = AppTextGrey)
                            Spacer(modifier = Modifier.height(2.dp))
                            val riskLevel = prediction?.risk_level ?: "Low"
                            val riskColor = when (riskLevel.lowercase()) {
                                "high" -> Color(0xFFE53935)
                                "medium" -> Color(0xFFFF9800)
                                else -> Color(0xFF4CAF50)
                            }
                            Text(
                                riskLevel,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = riskColor
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("ความน่าจะเป็น", fontSize = 12.sp, color = AppTextGrey)
                            Spacer(modifier = Modifier.height(2.dp))
                            val conf = prediction?.confidence ?: 0f
                            Text(
                                "${String.format("%.1f", conf * 100)}%",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = AppTextDark
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Lat / Lon
                    val lat = prediction?.latitude ?: 0.0
                    val lon = prediction?.longitude ?: 0.0
                    Text(
                        "Lat: $lat, Lon: $lon",
                        fontSize = 12.sp,
                        color = AppTextGrey
                    )
                }
            }

            // ====== Section 2: แนวโน้มน้ำฝน 10 วัน ======
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = AppWhite),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🌧️", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "แนวโน้มน้ำฝน 10 วัน (CHIRPS)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = AppTextDark
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // Bar Chart
                    val barMaxHeight = 100.dp
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        chirpsValues.forEachIndexed { index, value ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.weight(1f)
                            ) {
                                // Value label on top
                                Text(
                                    text = if (value == 0f) "0" else String.format("%.1f", value),
                                    fontSize = 9.sp,
                                    color = AppTextGrey
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                // Bar
                                val barHeightFraction = (value / maxChirps).coerceIn(0.02f, 1f)
                                val barColor = if (value > 0.5f) Color(0xFFE53935) else Color(0xFF4CAF50)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.6f)
                                        .height(barMaxHeight * barHeightFraction)
                                        .background(barColor, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                // Day label
                                Text(
                                    "D${index + 1}",
                                    fontSize = 9.sp,
                                    color = AppTextGrey
                                )
                            }
                        }
                    }
                }
            }

            // ====== Section 3: ค่า ML Features ======
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = AppWhite),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📊", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "ค่า ML Features (23 ตัว)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = AppTextDark
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = Color.LightGray)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Feature list - 2 columns grid
                    val features = listOf(
                        "CHIRPS_Day_1" to "0.000",
                        "CHIRPS_Day_2" to "0.000",
                        "CHIRPS_Day_3" to "0.000",
                        "CHIRPS_Day_4" to "0.000",
                        "CHIRPS_Day_5" to "0.000",
                        "CHIRPS_Day_6" to "0.000",
                        "CHIRPS_Day_7" to String.format("%.3f", chirpsValues[6]),
                        "CHIRPS_Day_8" to String.format("%.3f", chirpsValues[7]),
                        "CHIRPS_Day_9" to "0.000",
                        "CHIRPS_Day_10" to "0.000",
                        "Elevation" to if (riskScore > 0) String.format("%.1f", riskScore * 100) else "0.0",
                        "Slope" to if (riskScore > 0) String.format("%.1f", riskScore * 20) else "0.0",
                        "Aspect" to "180.0",
                        "Curvature" to "0.000",
                        "NDVI" to "0.450",
                        "LULC" to "1.000",
                        "Soil_Type" to "3.000",
                        "TWI" to String.format("%.3f", riskScore * 5),
                        "SPI" to "0.000",
                        "STI" to "0.000",
                        "Distance_to_Road" to "500.0",
                        "Distance_to_Fault" to "2000.0",
                        "Risk_Score" to String.format("%.3f", riskScore.toDouble())
                    )

                    features.chunked(2).forEach { pair ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            pair.forEach { (name, value) ->
                                Column(modifier = Modifier.weight(1f).padding(vertical = 6.dp)) {
                                    Text(name, fontSize = 11.sp, color = AppTextGrey)
                                    Text(
                                        value,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = AppTextDark
                                    )
                                }
                            }
                            // ถ้า pair มีแค่ 1 item ให้ใส่ spacer
                            if (pair.size < 2) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        Divider(color = Color.LightGray.copy(alpha = 0.3f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// Extension helper สำหรับดึงส่วนตำบล/อำเภอออกจาก district string
private val String.districtParts get() = this.split(" ")
