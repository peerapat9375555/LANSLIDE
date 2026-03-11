package com.example.landslideproject_cola

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon as OsmPolygon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PredictionsScreen(
    navController: NavHostController,
    viewModel: EarthquakeViewModel,
    targetLat: Float? = null,
    targetLon: Float? = null
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val sharedPref = remember { SharedPreferencesManager(context) }

    // MapView reference for zoom/location control
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    val initialPos = if (targetLat != null && targetLon != null) {
        GeoPoint(targetLat.toDouble(), targetLon.toDouble())
    } else {
        GeoPoint(18.783, 100.783)
    }

    val initialZoom = if (targetLat != null && targetLon != null) 16.0 else 9.0

    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", android.content.Context.MODE_PRIVATE))
        viewModel.getPredictions()
        val userId = sharedPref.getSavedUserId()
        if (userId.isNotEmpty()) {
            viewModel.getUserPins(userId)
        }
    }

    // Dialog state for Pinning
    var showPinDialog by remember { mutableStateOf(false) }
    var selectedLatLng by remember { mutableStateOf<GeoPoint?>(null) }
    var pinLabel by remember { mutableStateOf("") }
    
    // Filter State
    var showHighRisk by remember { mutableStateOf(true) }
    var showMediumRisk by remember { mutableStateOf(true) }
    var showLowRisk by remember { mutableStateOf(true) }

    // Map Style State: 0 = Default, 1 = Satellite, 2 = Terrain
    var mapStyleIndex by remember { mutableIntStateOf(0) }
    val mapStyleLabels = listOf("🗺️ ปกติ", "🛰️ ดาวเทียม", "🏔️ ภูมิประเทศ")
    var showStyleMenu by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(navController = navController, onClose = { scope.launch { drawerState.close() } })
        }
    ) {
        Scaffold(
            topBar = {
                // ===== Red Top Bar เหมือน Admin =====
                TopAppBar(
                    title = {
                        Text(
                            "แผนที่ความเสี่ยงดินถล่ม",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "เมนู",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = AppRed)
                )
            },
            bottomBar = { AppBottomNav(navController) },
            containerColor = Color.White
        ) { padding ->
            Box(
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                val predictions = viewModel.predictions

                // ===== OSM MAP =====
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            Configuration.getInstance().userAgentValue = ctx.packageName
                            controller.setZoom(initialZoom)
                            controller.setCenter(initialPos)
                            setMultiTouchControls(true)
                            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

                            val mReceive: MapEventsReceiver = object : MapEventsReceiver {
                                override fun singleTapConfirmedHelper(p: GeoPoint): Boolean = false
                                override fun longPressHelper(p: GeoPoint): Boolean {
                                    if (viewModel.loginResult?.user_id != null) {
                                        selectedLatLng = p
                                        showPinDialog = true
                                    } else {
                                        Toast.makeText(ctx, "กรุณาล็อกอินเพื่อปักหมุดพื้นที่ส่วนตัว", Toast.LENGTH_SHORT).show()
                                    }
                                    return true
                                }
                            }
                            overlays.add(MapEventsOverlay(mReceive))
                            mapViewRef = this
                        }
                    },
                    update = { view ->
                        // Apply map tile source based on style selection
                        val newTileSource = when (mapStyleIndex) {
                            1 -> object : OnlineTileSourceBase(
                                "EsriWorldImagery", 0, 18, 256, ".jpg",
                                arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/")
                            ) {
                                override fun getTileURLString(pMapTileIndex: Long): String {
                                    val zoom = org.osmdroid.util.MapTileIndex.getZoom(pMapTileIndex)
                                    val x = org.osmdroid.util.MapTileIndex.getX(pMapTileIndex)
                                    val y = org.osmdroid.util.MapTileIndex.getY(pMapTileIndex)
                                    return "${baseUrl}$zoom/$y/$x"
                                }
                            }
                            2 -> XYTileSource(
                                "OpenTopoMap", 0, 17, 256, ".png",
                                arrayOf("https://a.tile.opentopomap.org/", "https://b.tile.opentopomap.org/")
                            )
                            else -> TileSourceFactory.MAPNIK
                        }
                        if (view.tileProvider.tileSource != newTileSource) {
                            view.setTileSource(newTileSource)
                        }

                        val eventsOverlay = view.overlays.find { it is MapEventsOverlay }
                        view.overlays.clear()
                        if (eventsOverlay != null) view.overlays.add(eventsOverlay)

                        // Draw Risk Polygons
                        predictions.filter { item ->
                            val isHigh = item.risk_level == "High"
                            val isMedium = item.risk_level == "Medium"
                            val isLow = item.risk_level == "Low"
                            
                            (isHigh && showHighRisk) || (isMedium && showMediumRisk) || (isLow && showLowRisk) || (!isHigh && !isMedium && !isLow)
                        }.forEach { item ->
                            val polyPoints = item.polygon.map { GeoPoint(it[0], it[1]) }
                            val fillColorHex = try {
                                android.graphics.Color.parseColor(item.color)
                            } catch (e: Exception) { android.graphics.Color.GRAY }
                            val transparentFill = fillColorHex and 0x55FFFFFF

                            val polygon = OsmPolygon().apply {
                                points = polyPoints
                                fillPaint.color = transparentFill
                                outlinePaint.color = fillColorHex
                                outlinePaint.strokeWidth = 2f
                            }
                            view.overlays.add(polygon)
                        }

                        // Draw Selected Temporary Pin
                        selectedLatLng?.let { geo ->
                            val marker = Marker(view).apply {
                                position = geo
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                title = "จุดรับแจ้งเตือน"
                            }
                            view.overlays.add(marker)
                        }

                        // Draw Existing User Pins
                        viewModel.userPins.forEach { pin ->
                            val marker = Marker(view).apply {
                                position = GeoPoint(pin.latitude, pin.longitude)
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                title = pin.label ?: "จุดปักหมุด"
                                setOnMarkerClickListener { _, _ ->
                                    navController.navigate(Screen.PinDashboard.createRoute(pin.pin_id))
                                    true
                                }
                            }
                            view.overlays.add(marker)
                        }

                        view.invalidate()
                    }
                )

                // ===== Filters Overlay =====
                Card(
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text("แสดงความเสี่ยง", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = showHighRisk, onCheckedChange = { showHighRisk = it }, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("สูง (High)", fontSize = 12.sp, color = Color.Red)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = showMediumRisk, onCheckedChange = { showMediumRisk = it }, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ปานกลาง (Med)", fontSize = 12.sp, color = Color(0xFFFF9800))
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = showLowRisk, onCheckedChange = { showLowRisk = it }, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ต่ำ (Low)", fontSize = 12.sp, color = AppGreen)
                        }
                    }
                }

                // ===== Loading =====
                if (viewModel.isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                        color = AppRed
                    )
                }

                // ===== Clear Pins Button =====
                if (viewModel.userPins.isNotEmpty()) {
                    FilledTonalButton(
                        onClick = { viewModel.clearUserPins(context, sharedPref.getSavedUserId()) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 12.dp, end = 12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color.White,
                            contentColor = AppRed
                        )
                    ) {
                        Text("ล้างหมุดพื้นที่", fontWeight = FontWeight.Bold)
                    }
                }

                // ===== RIGHT SIDE: Zoom + MyLocation (เหมือน SetLocationScreen) =====
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp, bottom = 120.dp),
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
                    // My Location (กลับมาจุดเริ่ม)
                    SmallFloatingActionButton(
                        onClick = {
                            mapViewRef?.controller?.animateTo(initialPos)
                        },
                        shape = RoundedCornerShape(8.dp),
                        containerColor = Color.White,
                        contentColor = AppRed,
                        elevation = FloatingActionButtonDefaults.elevation(4.dp)
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = "ใกล้ฉัน", modifier = Modifier.size(20.dp))
                    }
                }

                // ===== Map Style Switcher =====
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = 16.dp)
                ) {
                    SmallFloatingActionButton(
                        onClick = { showStyleMenu = !showStyleMenu },
                        shape = RoundedCornerShape(8.dp),
                        containerColor = Color.White,
                        contentColor = AppTextDark,
                        elevation = FloatingActionButtonDefaults.elevation(4.dp)
                    ) {
                        Text(mapStyleLabels[mapStyleIndex].take(2), fontSize = 16.sp)
                    }
                    DropdownMenu(
                        expanded = showStyleMenu,
                        onDismissRequest = { showStyleMenu = false }
                    ) {
                        mapStyleLabels.forEachIndexed { index, label ->
                            DropdownMenuItem(
                                text = { Text(label, fontSize = 14.sp, fontWeight = if (index == mapStyleIndex) FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    mapStyleIndex = index
                                    showStyleMenu = false
                                }
                            )
                        }
                    }
                }

                // ===== Legend Overlay =====
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = 16.dp, start = 16.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("ระดับความเสี่ยง", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AppTextDark)
                        Spacer(modifier = Modifier.height(4.dp))
                        LegendItem(color = Color.Red,    text = "เสี่ยงสูงมาก (High)")
                        LegendItem(color = Color.Yellow, text = "เสี่ยงปานกลาง (Medium)")
                        LegendItem(color = Color.Green,  text = "เสี่ยงต่ำ (Low)")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("💡 กดค้างบนแผนที่เพื่อปักหมุดส่วนตัว", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }

            // ===== Pin Confirmation Dialog =====
            if (showPinDialog && selectedLatLng != null) {
                AlertDialog(
                    onDismissRequest = { showPinDialog = false },
                    title = { Text("ตั้งจุดเตือนภัยส่วนตัว") },
                    text = {
                        Column {
                            Text("คุณต้องการรับการแจ้งเตือนดินถล่มที่จุดนี้ใช่หรือไม่?")
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = pinLabel,
                                onValueChange = { pinLabel = it },
                                label = { Text("ชื่อสถานที่ (เช่น บ้าน, ที่ทำงาน)") }
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val userId = sharedPref.getSavedUserId()
                            val request = PinRequest(
                                user_id = userId,
                                latitude = selectedLatLng!!.latitude,
                                longitude = selectedLatLng!!.longitude,
                                label = pinLabel.ifEmpty { "Pinned Location" }
                            )
                            viewModel.addPin(context, request) {
                                viewModel.getUserPins(userId)
                            }
                            showPinDialog = false
                            pinLabel = ""
                        }) {
                            Text("บันทึก", color = AppRed, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPinDialog = false }) {
                            Text("ยกเลิก")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Box(modifier = Modifier.size(12.dp).background(color, RoundedCornerShape(2.dp)))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, fontSize = 12.sp)
    }
}
