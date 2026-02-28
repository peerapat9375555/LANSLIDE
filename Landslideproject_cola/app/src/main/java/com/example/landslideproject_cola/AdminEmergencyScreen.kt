package com.example.landslideproject_cola

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

@Composable
fun AdminEmergencyScreen(navController: NavHostController, viewModel: EarthquakeViewModel) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    var showEditDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var editingService by remember { mutableStateOf<EmergencyService?>(null) }
    var deletingService by remember { mutableStateOf<EmergencyService?>(null) }
    var editName by remember { mutableStateOf("") }
    var editPhone by remember { mutableStateOf("") }

    // Upload image state per service (serviceId -> Bitmap for preview)
    var uploadingServiceId by remember { mutableStateOf<String?>(null) }
    var pendingBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pendingFilename by remember { mutableStateOf("image.jpg") }

    // Image picker launcher
    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val stream = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(stream)
                stream?.close()
                if (bitmap != null) {
                    pendingBitmap = bitmap
                    // Extract filename from URI
                    val fileName = it.lastPathSegment?.substringAfterLast('/') ?: "image.jpg"
                    pendingFilename = if (fileName.contains('.')) fileName else "$fileName.jpg"
                    // Upload immediately
                    val serviceId = uploadingServiceId
                    if (serviceId != null) {
                        val outputStream = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                        val base64Str = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                        viewModel.uploadEmergencyImage(context, serviceId, base64Str, pendingFilename) {
                            viewModel.getEmergencyServices()
                            pendingBitmap = null
                            uploadingServiceId = null
                        }
                    }
                }
            } catch (e: Exception) {
                pendingBitmap = null
            }
        }
    }

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
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(services) { service ->
                        AdminEmergencyServiceCard(
                            service = service,
                            onCall = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${service.phone_number}"))
                                context.startActivity(intent)
                            },
                            onEdit = {
                                editingService = service
                                editName = service.service_name
                                editPhone = service.phone_number
                                showEditDialog = true
                            },
                            onDelete = {
                                deletingService = service
                                showDeleteDialog = true
                            },
                            onUploadImage = {
                                uploadingServiceId = service.service_id
                                pendingBitmap = null
                                imageLauncher.launch("image/*")
                            }
                        )
                    }
                }
            }
        }
    }

    // ---- Edit Dialog ----
    if (showEditDialog && editingService != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("แก้ไขเบอร์ฉุกเฉิน", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = editName, onValueChange = { editName = it },
                        label = { Text("ชื่อหน่วยงาน") }, modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editPhone, onValueChange = { editPhone = it },
                        label = { Text("เบอร์โทร") }, modifier = Modifier.fillMaxWidth()
                    )
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

    // ---- Add Dialog ----
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("เพิ่มเบอร์ฉุกเฉิน", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = editName, onValueChange = { editName = it },
                        label = { Text("ชื่อหน่วยงาน") }, modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editPhone, onValueChange = { editPhone = it },
                        label = { Text("เบอร์โทร") }, modifier = Modifier.fillMaxWidth()
                    )
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

    // ---- Delete Confirmation Dialog ----
    if (showDeleteDialog && deletingService != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("ยืนยันการลบ", fontWeight = FontWeight.Bold) },
            text = {
                Text("คุณต้องการลบ \"${deletingService!!.service_name}\" ออกจากระบบใช่หรือไม่?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteEmergencyService(context, deletingService!!.service_id) {
                            viewModel.getEmergencyServices()
                        }
                        showDeleteDialog = false
                        deletingService = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) { Text("ลบ", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    deletingService = null
                }) { Text("ยกเลิก") }
            }
        )
    }
}

@Composable
private fun AdminEmergencyServiceCard(
    service: EmergencyService,
    onCall: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onUploadImage: () -> Unit
) {
    val baseUrl = EarthquakeClient.BASE_URL
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // ---- Thumbnail or placeholder ----
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AppRed.copy(alpha = 0.07f))
                        .border(1.dp, AppRed.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .clickable { onUploadImage() },
                    contentAlignment = Alignment.Center
                ) {
                    val imgUrl = service.img_url
                    if (!imgUrl.isNullOrBlank()) {
                        val fullUrl = if (imgUrl.startsWith("http")) imgUrl else "$baseUrl${imgUrl.trimStart('/')}"
                        AsyncImage(
                            model = fullUrl,
                            contentDescription = "รูป${service.service_name}",
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.AddPhotoAlternate,
                                contentDescription = "อัปโหลดรูป",
                                tint = AppRed.copy(alpha = 0.5f),
                                modifier = Modifier.size(24.dp)
                            )
                            Text("รูปภาพ", fontSize = 9.sp, color = AppRed.copy(alpha = 0.5f))
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // ---- Text info ----
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        service.service_name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = AppTextDark
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        service.phone_number,
                        fontSize = 13.sp,
                        color = AppRed,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (!service.district.isNullOrBlank()) {
                        Text(service.district, fontSize = 11.sp, color = AppTextGrey)
                    }
                }

                // ---- Action buttons ----
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // โทร
                    IconButton(onClick = onCall, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Phone, contentDescription = "โทร", tint = AppGreen, modifier = Modifier.size(20.dp))
                    }
                    // แก้ไข
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "แก้ไข", tint = AppRed, modifier = Modifier.size(20.dp))
                    }
                    // ลบ
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "ลบ", tint = Color(0xFFD32F2F), modifier = Modifier.size(20.dp))
                    }
                }
            }

            // ---- Upload image hint ----
            if (service.img_url.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                TextButton(
                    onClick = onUploadImage,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("อัปโหลดรูปภาพ", fontSize = 12.sp)
                }
            } else {
                Spacer(modifier = Modifier.height(6.dp))
                TextButton(
                    onClick = onUploadImage,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("เปลี่ยนรูปภาพ", fontSize = 12.sp)
                }
            }
        }
    }
}
