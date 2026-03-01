package com.example.landslideproject_cola

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminReportHistoryScreen(
    navController: NavHostController,
    viewModel: EarthquakeViewModel
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val reports = viewModel.completedReports
    val isLoading = viewModel.isLoading

    LaunchedEffect(Unit) { viewModel.getReportHistory() }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AdminDrawer(navController = navController, onClose = { scope.launch { drawerState.close() } })
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "ประวัติการช่วยเหลือ (${reports.size})",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "ย้อนกลับ", tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.getReportHistory() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "รีเฟรช", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = AppRed)
                )
            },
            bottomBar = { AdminBottomNavigationBar(navController, currentRoute) },
            containerColor = AppGrey
        ) { paddingValues ->
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = AppGreen)
                            Spacer(Modifier.height(8.dp))
                            Text("กำลังโหลด...", color = AppTextGrey, fontSize = 14.sp)
                        }
                    }
                }
                reports.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(72.dp),
                                tint = Color.LightGray
                            )
                            Spacer(Modifier.height(12.dp))
                            Text("ยังไม่มีประวัติการช่วยเหลือ", color = AppTextGrey, fontSize = 16.sp)
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        // Summary banner
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = AppGreen.copy(alpha = 0.1f)),
                                elevation = CardDefaults.cardElevation(0.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, null, tint = AppGreen, modifier = Modifier.size(28.dp))
                                    Column {
                                        Text(
                                            "ดำเนินการแล้วทั้งหมด ${reports.size} ราย",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = AppGreen
                                        )
                                        Text("รายการที่กดช่วยเหลือเสร็จสิ้นแล้ว", fontSize = 12.sp, color = AppTextGrey)
                                    }
                                }
                            }
                        }

                        items(reports, key = { it.report_id }) { report ->
                            AdminReportCard(
                                report = report,
                                onComplete = null,
                                showCompletedBadge = true
                            )
                        }
                    }
                }
            }
        }
    }
}
