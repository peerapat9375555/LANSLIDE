package com.example.landslideproject_cola

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Phone
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
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.launch

@Composable
fun AdminEmergencyScreen(navController: NavHostController, viewModel: EarthquakeViewModel) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    var showEditDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingService by remember { mutableStateOf<EmergencyService?>(null) }
    var editName by remember { mutableStateOf("") }
    var editPhone by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.getEmergencyServices()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = { AdminDrawer(navController = navController, onClose = { scope.launch { drawerState.close() } }) }
    ) {
        Scaffold(
            topBar = { AdminTopBar(title = "แก้ไขเบอร์ฉุกเฉิน", onMenuClick = { scope.launch { drawerState.open() } }) },
            bottomBar = { AdminBottomNavigationBar(navController, currentRoute) },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        editName = ""
                        editPhone = ""
                        showAddDialog = true
                    },
                    containerColor = AppRed
                ) {
                    Icon(Icons.Default.Add, contentDescription = "เพิ่มเบอร์", tint = Color.White)
                }
            },
            containerColor = AppGrey
        ) { padding ->
            val services = viewModel.emergencyServices

            if (services.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ยังไม่มีเบอร์ฉุกเฉินในระบบ", fontSize = 16.sp, color = AppTextGrey)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("กด + เพื่อเพิ่มเบอร์ใหม่", fontSize = 14.sp, color = AppRed)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(services) { service ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(service.service_name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AppTextDark)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(service.phone_number, fontSize = 14.sp, color = AppRed, fontWeight = FontWeight.SemiBold)
                                    if (service.district != null) {
                                        Text(service.district, fontSize = 12.sp, color = AppTextGrey)
                                    }
                                }
                                IconButton(onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${service.phone_number}"))
                                    context.startActivity(intent)
                                }) {
                                    Icon(Icons.Default.Phone, contentDescription = "โทร", tint = AppGreen)
                                }
                                IconButton(onClick = {
                                    editingService = service
                                    editName = service.service_name
                                    editPhone = service.phone_number
                                    showEditDialog = true
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "แก้ไข", tint = AppRed)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Dialog
    if (showEditDialog && editingService != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("แก้ไขเบอร์ฉุกเฉิน", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("ชื่อหน่วยงาน") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = editPhone, onValueChange = { editPhone = it }, label = { Text("เบอร์โทร") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateEmergencyService(context, editingService!!.service_id, editName, editPhone) {
                            viewModel.getEmergencyServices()
                        }
                        showEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppRed)
                ) { Text("บันทึก", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("ยกเลิก") }
            }
        )
    }

    // Add Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("เพิ่มเบอร์ฉุกเฉิน", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("ชื่อหน่วยงาน") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = editPhone, onValueChange = { editPhone = it }, label = { Text("เบอร์โทร") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addEmergencyService(context, editName, editPhone) {
                            viewModel.getEmergencyServices()
                        }
                        showAddDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppGreen)
                ) { Text("เพิ่ม", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("ยกเลิก") }
            }
        )
    }
}
