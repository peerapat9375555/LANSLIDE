package com.example.landslideproject_cola

import android.content.Context
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserReportScreen(
    navController: NavHostController,
    viewModel: EarthquakeViewModel
) {
    val context = LocalContext.current
    val sharedPref = remember { SharedPreferencesManager(context) }
    val scope = rememberCoroutineScope()

    val userId = sharedPref.getSavedUserId()
    val userLat = sharedPref.getSavedLatitude()
    val userLon = sharedPref.getSavedLongitude()

    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imageBase64 by remember { mutableStateOf<String?>(null) }
    var isSending by remember { mutableStateOf(false) }
    var sendSuccess by remember { mutableStateOf<Boolean?>(null) }

    // Launcher เลือกรูปจาก Gallery
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bmp = decodeBitmapFromUri(context, it)
            if (bmp != null) {
                selectedBitmap = bmp
                imageBase64 = bitmapToBase64(bmp)
            }
        }
    }

    // Launcher ถ่ายรูปจากกล้อง
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp: Bitmap? ->
        if (bmp != null) {
            selectedBitmap = bmp
            imageBase64 = bitmapToBase64(bmp)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("ขอความช่วยเหลือ", fontWeight = FontWeight.Bold, color = Color.White)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "ย้อนกลับ", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppRed)
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // ---- Header Card ----
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = AppRed.copy(alpha = 0.08f)),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = AppRed, modifier = Modifier.size(32.dp))
                    Column {
                        Text("ขอความช่วยเหลือ", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AppRed)
                        Text("รายงานพื้นที่เสี่ยงหรือเหตุการณ์ที่พบเห็นให้แอดมินทราบ",
                            fontSize = 12.sp, color = AppTextGrey)
                    }
                }
            }

            // ---- รูปภาพ ----
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("รูปภาพประกอบ", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = AppTextDark)
                    Spacer(modifier = Modifier.height(10.dp))

                    if (selectedBitmap != null) {
                        Box {
                            Image(
                                bitmap = selectedBitmap!!.asImageBitmap(),
                                contentDescription = "รูปที่เลือก",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            // ปุ่มลบรูป
                            IconButton(
                                onClick = { selectedBitmap = null; imageBase64 = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
                                    .size(32.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "ลบรูป", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // เลือกจาก Gallery
                        OutlinedButton(
                            onClick = { galleryLauncher.launch("image/*") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AppRed),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AppRed)
                        ) {
                            Icon(Icons.Default.Photo, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("คลัง", fontSize = 13.sp)
                        }
                        // ถ่ายรูป
                        OutlinedButton(
                            onClick = { cameraLauncher.launch(null) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AppRed),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AppRed)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ถ่ายรูป", fontSize = 13.sp)
                        }
                    }
                }
            }

            // ---- หัวข้อ ----
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("หัวข้อ *", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = AppTextDark)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("เช่น พบดินไหล บ้านห้วยทราย", color = Color.Gray, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppRed,
                            focusedLabelColor = AppRed,
                            cursorColor = AppRed
                        )
                    )
                }
            }

            // ---- ข้อความ ----
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("รายละเอียด *", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = AppTextDark)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        placeholder = { Text("อธิบายสถานการณ์ที่พบเห็น เช่น ดินเริ่มไหลทางด้านซ้ายของถนน...", color = Color.Gray, fontSize = 13.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        shape = RoundedCornerShape(8.dp),
                        maxLines = 6,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppRed,
                            focusedLabelColor = AppRed,
                            cursorColor = AppRed
                        )
                    )
                }
            }

            // ---- พิกัดที่แนบ ----
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = AppGreen, modifier = Modifier.size(24.dp))
                    Column {
                        Text("พิกัดของคุณ (จะแนบไปพร้อมรายงาน)", fontSize = 12.sp, color = AppTextGrey)
                        if (userLat != 0.0 || userLon != 0.0) {
                            Text(
                                "${"%.5f".format(userLat)}, ${"%.5f".format(userLon)}",
                                fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppTextDark
                            )
                        } else {
                            Text("ไม่พบพิกัด — กรุณาปักหมุดที่อยู่ก่อน", fontSize = 13.sp, color = AppRed)
                        }
                    }
                }
            }

            // ---- สถานะส่ง ----
            if (sendSuccess == true) {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = AppGreen.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = AppGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ส่งรายงานสำเร็จ! แอดมินจะตรวจสอบโดยเร็ว", color = AppGreen, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // ---- ปุ่มส่ง ----
            Button(
                onClick = {
                    if (title.isBlank() || message.isBlank()) {
                        android.widget.Toast.makeText(context, "กรุณากรอกหัวข้อและรายละเอียด", android.widget.Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isSending = true
                    sendSuccess = null
                    val request = UserReportRequest(
                        user_id = userId,
                        title = title,
                        message = message,
                        image_base64 = imageBase64,
                        latitude = if (userLat != 0.0) userLat else null,
                        longitude = if (userLon != 0.0) userLon else null
                    )
                    viewModel.submitUserReport(context, request) { ok ->
                        isSending = false
                        sendSuccess = ok
                        if (ok) {
                            title = ""
                            message = ""
                            selectedBitmap = null
                            imageBase64 = null
                        }
                    }
                },
                enabled = !isSending && title.isNotBlank() && message.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppRed)
            ) {
                if (isSending) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ส่งรายงานให้แอดมิน", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ===== helper functions =====

private fun decodeBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        val stream: InputStream? = context.contentResolver.openInputStream(uri)
        val options = BitmapFactory.Options().apply { inSampleSize = 2 }
        BitmapFactory.decodeStream(stream, null, options)
    } catch (e: Exception) { null }
}

fun bitmapToBase64(bitmap: Bitmap): String {
    val baos = ByteArrayOutputStream()
    // ลดคุณภาพเหลือ 60% เพื่อประหยัด bandwidth
    bitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos)
    return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
}
