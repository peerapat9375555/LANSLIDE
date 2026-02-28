package com.example.landslideproject_cola

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon as OsmPolygon

@Composable
fun PredictionsScreen(
    navController: NavHostController,
    viewModel: EarthquakeViewModel
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val sharedPref = remember { SharedPreferencesManager(context) }
    
    LaunchedEffect(Unit) {
        // Init OSMdroid config
        Configuration.getInstance().load(context, android.preference.PreferenceManager.getDefaultSharedPreferences(context))
        viewModel.getPredictions()
        val userId = sharedPref.getSavedUserId()
        if (userId.isNotEmpty()) {
            viewModel.getUserPins(userId)
        }
    }

    // Default to Nan Province roughly
    val initialPos = GeoPoint(18.783, 100.783)

    // Dialog state for Pinning
    var showPinDialog by remember { mutableStateOf(false) }
    var selectedLatLng by remember { mutableStateOf<GeoPoint?>(null) }
    var pinLabel by remember { mutableStateOf("") }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(navController = navController, onClose = { scope.launch { drawerState.close() } })
        }
    ) {
        Scaffold(
            topBar = {
                GreenTopBar(title = "Map Risk Zone") { scope.launch { drawerState.open() } }
            },
            bottomBar = { AppBottomNav(navController) },
            containerColor = Color.White
        ) { padding ->
            Box(
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                // Background map using OSMdroid
                val predictions = viewModel.predictions

                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            controller.setZoom(9.0)
                            controller.setCenter(initialPos)
                            setMultiTouchControls(true)
                            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

                            // Setup Long Press Receiver
                            val mReceive: MapEventsReceiver = object : MapEventsReceiver {
                                override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                                    return false
                                }

                                override fun longPressHelper(p: GeoPoint): Boolean {
                                    if(viewModel.loginResult?.user_id != null) {
                                       selectedLatLng = p
                                       showPinDialog = true
                                    } else {
                                       Toast.makeText(context, "กรุณาล็อกอินเพื่อปักหมุดพื้นที่ส่วนตัว", Toast.LENGTH_SHORT).show()
                                    }
                                    return true
                                }
                            }
                            overlays.add(MapEventsOverlay(mReceive))
                        }
                    },
                    update = { view ->
                        // Clear existing overlays but keep the EventReceiver
                        val eventsOverlay = view.overlays.find { it is MapEventsOverlay }
                        view.overlays.clear()
                        if (eventsOverlay != null) view.overlays.add(eventsOverlay)

                        // Draw Risk Polygons
                        predictions.forEach { item ->
                            val polyPoints = item.polygon.map { GeoPoint(it[0], it[1]) }
                            
                            val fillColorHex = try {
                                android.graphics.Color.parseColor(item.color)
                            } catch (e: Exception) { android.graphics.Color.GRAY }

                            val transparentFill = fillColorHex and 0x55FFFFFF // ~33% opacity

                            val polygon = OsmPolygon().apply {
                                points = polyPoints
                                fillColor = transparentFill
                                strokeColor = fillColorHex
                                strokeWidth = 2f
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

                // Top Loading Indicator
                if (viewModel.isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
                }

                // Clear Pins Button overlay
                if (viewModel.userPins.isNotEmpty()) {
                    FilledTonalButton(
                        onClick = { viewModel.clearUserPins(context, sharedPref.getSavedUserId()) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 16.dp, end = 16.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color.White, contentColor = AppRed)
                    ) {
                        Text("ล้างหมุดพื้นที่", fontWeight = FontWeight.Bold)
                    }
                }

                // Legend Overlay
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = 16.dp, start = 16.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("ระดับความเสี่ยง", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        LegendItem(color = Color.Red, text = "เสี่ยงสูงมาก (High)")
                        LegendItem(color = Color.Yellow, text = "เสี่ยงปานกลาง (Medium)")
                        LegendItem(color = Color.Green, text = "เสี่ยงต่ำ (Low)")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("💡 กดค้างบนแผนที่เพื่อปักหมุดส่วนตัว", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }

            // Pin Confirmation Dialog
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
                            viewModel.addPin(context, request)
                            showPinDialog = false
                            pinLabel = ""
                            // Refresh pins after a short delay or state update
                            viewModel.getUserPins(userId)
                        }) {
                            Text("บันทึก")
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
