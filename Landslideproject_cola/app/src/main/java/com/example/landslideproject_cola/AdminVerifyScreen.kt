package com.example.landslideproject_cola

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminVerifyScreen(
    navController: NavHostController,
    viewModel: EarthquakeViewModel,
    logId: String
) {
    val context = LocalContext.current
    val alertDetail = viewModel.adminAlertDetail
    val isLoading = viewModel.isLoading

    LaunchedEffect(logId) {
        if (logId.isNotEmpty()) {
            viewModel.getAlertDetails(logId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ยืนยันการแจ้งเตือน", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
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
        if (isLoading || alertDetail == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AppRed)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AdminDashboardContent(
                    detail = alertDetail,
                    showVerifyButtons = true,
                    onApprove = {
                        viewModel.verifyAlert(context, logId, "approve") {
                            navController.popBackStack()
                        }
                    },
                    onReject = {
                        viewModel.verifyAlert(context, logId, "reject") {
                            navController.popBackStack()
                        }
                    }
                )
            }
        }
    }
}
