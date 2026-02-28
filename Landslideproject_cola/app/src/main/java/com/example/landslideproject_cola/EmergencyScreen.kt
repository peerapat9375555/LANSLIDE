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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun EmergencyScreen(
    navController: NavHostController,
    viewModel: EarthquakeViewModel
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { viewModel.getEmergencyServices() }
    val services = viewModel.emergencyServices

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(navController = navController, onClose = { scope.launch { drawerState.close() } })
        }
    ) {
        Scaffold(
            topBar = {
                GreenTopBar(title = "เบอร์โทรฉุกเฉิน") { scope.launch { drawerState.open() } }
            },
            bottomBar = { AppBottomNav(navController) },
            containerColor = AppGrey
        ) { padding ->
            val defaultServices = listOf(
                Triple("สายด่วนป้องกันภัยพิบัติ", "1784", "กรมป้องกันและบรรเทาสาธารณภัย"),
                Triple("สายด่วนตำรวจ", "191", "สำนักงานตำรวจแห่งชาติ"),
                Triple("สายด่วนฉุกเฉินการแพทย์", "1669", "สถาบันการแพทย์ฉุกเฉิน"),
                Triple("สายด่วนดับเพลิง", "199", "กรมป้องกันและบรรเทาสาธารณภัย"),
                Triple("สายด่วนกู้ภัย", "1646", "มูลนิธิกู้ภัย")
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (services.isEmpty()) {
                    items(defaultServices) { (name, phone, district) ->
                        EmergencyContactCard(
                            name = name,
                            phone = phone,
                            subtitle = district,
                            imgUrl = null
                        ) {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                            context.startActivity(intent)
                        }
                    }
                } else {
                    items(services) { s ->
                        EmergencyContactCard(
                            name = s.service_name,
                            phone = s.phone_number,
                            subtitle = s.district ?: "",
                            imgUrl = s.img_url
                        ) {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${s.phone_number}"))
                            context.startActivity(intent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmergencyContactCard(
    name: String,
    phone: String,
    subtitle: String,
    imgUrl: String?,
    onCall: () -> Unit
) {
    val baseUrl = EarthquakeClient.BASE_URL

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppWhite),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ---- Icon / Image ----
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(AppRed.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                val hasImage = !imgUrl.isNullOrBlank()
                if (hasImage) {
                    val fullUrl = if (imgUrl!!.startsWith("http")) imgUrl
                                  else "$baseUrl${imgUrl.trimStart('/')}"
                    AsyncImage(
                        model = fullUrl,
                        contentDescription = "รูป$name",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = null,
                        tint = AppRed,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AppTextDark)
                if (subtitle.isNotBlank()) {
                    Text(subtitle, fontSize = 12.sp, color = AppTextGrey)
                }
            }

            Button(
                onClick = onCall,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppRed),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(phone, color = AppWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
