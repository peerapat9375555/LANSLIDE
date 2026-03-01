package com.example.landslideproject_cola

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetLocationScreen(
    navController: NavHostController,
    viewModel: EarthquakeViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sharedPref = remember { SharedPreferencesManager(context) }
    val userId = sharedPref.getSavedUserId()

    // Center of Nan Province (default)
    var centerLat by remember { mutableDoubleStateOf(18.7833) }
    var centerLon by remember { mutableDoubleStateOf(100.7833) }
    var locationName by remember { mutableStateOf("กำลังโหลดตำแหน่ง...") }
    var districtName by remember { mutableStateOf("") }   // อำเภอ → DISTRICT
    var tambonName   by remember { mutableStateOf("") }   // ตำบล  → TAMBON
    var isSaving by remember { mutableStateOf(false) }
    var isGeocoding by remember { mutableStateOf(false) }
    var saveSuccess by remember { mutableStateOf<Boolean?>(null) }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    // Reverse geocode + debounce 1500ms
    LaunchedEffect(centerLat, centerLon) {
        isGeocoding = true
        locationName = "กำลังค้นหาตำแหน่ง..."
        kotlinx.coroutines.delay(1500)

        // 1) ชื่อสถานที่ จาก Photon
        locationName = reverseGeocode(centerLat, centerLon)

        // 2) ตำบล/อำเภอ จาก Server (CSV lookup — แม่นยำกว่า)
        val (tambon, district) = fetchTambonDistrict(centerLat, centerLon)
        tambonName   = tambon
        districtName = district

        isGeocoding = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ปักหมุดที่อยู่", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "ย้อนกลับ", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppRed)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {

            // ======== OSM MAP ========
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    Configuration.getInstance().userAgentValue = ctx.packageName
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(14.0)
                        controller.setCenter(GeoPoint(centerLat, centerLon))

                        // Update coordinates on map scroll
                        addMapListener(object : MapListener {
                            override fun onScroll(event: ScrollEvent?): Boolean {
                                val center = mapCenter
                                centerLat = center.latitude
                                centerLon = center.longitude
                                return false
                            }
                            override fun onZoom(event: ZoomEvent?): Boolean = false
                        })
                        mapViewRef = this
                    }
                },
                update = { _ -> /* no-op, coordinates tracked via listener */ }
            )

            // ======== CENTER PIN (fixed) ========
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = AppRed,
                        modifier = Modifier.size(48.dp).offset(y = 4.dp)
                    )
                    // Pin shadow dot
                    Box(
                        modifier = Modifier
                            .size(8.dp, 3.dp)
                            .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                    )
                }
            }

            // ======== RIGHT SIDE BUTTONS (Zoom + MyLocation) ========
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp, bottom = 200.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Zoom In
                SmallFloatingActionButton(
                    onClick = { mapViewRef?.controller?.zoomIn() },
                    shape = RoundedCornerShape(8.dp),
                    containerColor = AppRed,
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(4.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "ซูมเข้า", modifier = Modifier.size(20.dp))
                }
                // Zoom Out
                SmallFloatingActionButton(
                    onClick = { mapViewRef?.controller?.zoomOut() },
                    shape = RoundedCornerShape(8.dp),
                    containerColor = AppRed,
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(4.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "ซูมออก", modifier = Modifier.size(20.dp))
                }
                // My Location
                SmallFloatingActionButton(
                    onClick = {
                        centerLat = 18.7833
                        centerLon = 100.7833
                        mapViewRef?.controller?.animateTo(GeoPoint(18.7833, 100.7833))
                    },
                    shape = RoundedCornerShape(8.dp),
                    containerColor = Color.White,
                    contentColor = AppRed,
                    elevation = FloatingActionButtonDefaults.elevation(4.dp)
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "ตำแหน่งปัจจุบัน", modifier = Modifier.size(20.dp))
                }
            }

            // ======== BOTTOM INFO PANEL ========
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Title
                    Text(
                        "หน้าปักหมุดที่อยู่",
                        fontSize = 13.sp,
                        color = AppTextGrey,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Location name
                    Text(
                        text = locationName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppTextDark,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // อำเภอ
                    if (districtName.isNotBlank()) {
                        Text(
                            text = "อำเภอ $districtName",
                            fontSize = 13.sp,
                            color = AppTextGrey,
                            textAlign = TextAlign.Center
                        )
                    }
                    // ตำบล
                    if (tambonName.isNotBlank()) {
                        Text(
                            text = "ตำบล $tambonName",
                            fontSize = 13.sp,
                            color = AppTextGrey,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Coordinates
                    Text(
                        text = "${"%.4f".format(centerLat)}, ${"%.4f".format(centerLon)}",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )

                    // Success message
                    if (saveSuccess == true) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("บันทึกตำแหน่งสำเร็จ!", color = AppGreen, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    } else if (saveSuccess == false) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("บันทึกไม่สำเร็จ กรุณาลองใหม่", color = AppRed, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // เลือก button
                    Button(
                        onClick = {
                            isSaving = true
                            saveSuccess = null
                            scope.launch {
                                val req = SaveLocationRequest(
                                    latitude = centerLat,
                                    longitude = centerLon,
                                    location_name = locationName,
                                    district = districtName,   // อำเภอ → DISTRICT
                                    tambon   = tambonName      // ตำบล  → TAMBON
                                )
                                // บันทึกพิกัดลง SharedPreferences เพื่อให้ notification service ใช้ได้
                                sharedPref.saveUserLatLon(centerLat, centerLon)
                                viewModel.saveUserLocation(context, userId, req) { ok ->
                                    isSaving = false
                                    saveSuccess = ok
                                    if (ok) {
                                        scope.launch {
                                            kotlinx.coroutines.delay(1200)
                                            navController.popBackStack()
                                        }
                                    }
                                }
                            }
                        },
                        enabled = !isSaving && !isGeocoding,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppRed)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("เลือก", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// ===== Nominatim OkHttp client (singleton เพื่อไม่สร้างใหม่ทุกครั้ง) =====
// ===== HTTP client สำหรับ Geocoding (singleton) =====
private val geocodeClient by lazy {
    okhttp3.OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .build()
}

// ===== Reverse geocoding ด้วย Photon — ใช้แค่หาชื่อสถานที่ (location_name) =====
suspend fun reverseGeocode(lat: Double, lon: Double): String = withContext(Dispatchers.IO) {
    return@withContext try {
        val url = "https://photon.komoot.io/reverse?lon=$lon&lat=$lat&limit=1&lang=default"
        val request = okhttp3.Request.Builder()
            .url(url)
            .header("User-Agent", "LandslideNanApp/1.0")
            .build()
        val response = geocodeClient.newCall(request).execute()
        val body = response.body?.string() ?: ""
        android.util.Log.d("GEOCODE", "Photon HTTP ${response.code}: $body")
        if (!response.isSuccessful || body.isBlank()) return@withContext "ไม่ทราบชื่อสถานที่"

        val props = org.json.JSONObject(body)
            .optJSONArray("features")?.optJSONObject(0)?.optJSONObject("properties")
        val name   = props?.optString("name")?.takeIf   { it.isNotBlank() && it != "null" }
        val street = props?.optString("street")?.takeIf { it.isNotBlank() && it != "null" }
        val city   = props?.optString("city")?.takeIf   { it.isNotBlank() && it != "null" }
        val state  = props?.optString("state")?.takeIf  { it.isNotBlank() && it != "null" }
        name ?: street ?: city ?: state ?: "ไม่ทราบชื่อสถานที่"
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        android.util.Log.e("GEOCODE", "Photon error: ${e.message}")
        "ไม่สามารถระบุชื่อสถานที่ได้"
    }
}

// ===== Fetch TAMBON + DISTRICT จาก Server (nan_province_data.csv nearest lookup) =====
// return Pair(ตำบล, อำเภอ)
suspend fun fetchTambonDistrict(lat: Double, lon: Double): Pair<String, String> = withContext(Dispatchers.IO) {
    return@withContext try {
        val url = "${EarthquakeClient.BASE_URL}api/geocode-location?lat=$lat&lon=$lon"
        val request = okhttp3.Request.Builder()
            .url(url)
            .header("User-Agent", "LandslideNanApp/1.0")
            .build()
        val response = geocodeClient.newCall(request).execute()
        val body = response.body?.string() ?: ""
        android.util.Log.d("GEOCODE", "Server geocode: $body")
        if (!response.isSuccessful || body.isBlank()) return@withContext Pair("", "")

        val json     = org.json.JSONObject(body)
        val tambon   = json.optString("tambon", "")
        val district = json.optString("district", "")
        android.util.Log.d("GEOCODE", "tambon=$tambon  district=$district")
        Pair(tambon, district)
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        android.util.Log.e("GEOCODE", "Server geocode error: ${e.message}")
        Pair("", "")
    }
}
