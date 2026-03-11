package com.example.landslideproject_cola

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polygon
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight

@Composable
fun AdminMapScreen(navController: NavHostController, viewModel: EarthquakeViewModel, targetLat: Float? = null, targetLon: Float? = null) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    LaunchedEffect(Unit) {
        viewModel.getPredictions()
    }

    val predictions = viewModel.predictions

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
        drawerContent = { AdminDrawer(navController = navController, onClose = { scope.launch { drawerState.close() } }) }
    ) {
        Scaffold(
            topBar = { AdminTopBar(title = "แผนที่ความเสี่ยง", onMenuClick = { scope.launch { drawerState.open() } }) },
            bottomBar = { AdminBottomNavigationBar(navController, currentRoute) },
            containerColor = AppGrey
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                AndroidView(
                    factory = { ctx ->
                        Configuration.getInstance().userAgentValue = ctx.packageName
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            if (targetLat != null && targetLon != null) {
                                controller.setZoom(16.0)
                                controller.setCenter(GeoPoint(targetLat.toDouble(), targetLon.toDouble()))
                            } else {
                                controller.setZoom(10.0)
                                controller.setCenter(GeoPoint(19.0, 100.8))
                            }
                        }
                    },
                    update = { mapView ->
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
                        if (mapView.tileProvider.tileSource != newTileSource) {
                            mapView.setTileSource(newTileSource)
                        }

                        mapView.overlays.clear()
                        predictions.filter { item ->
                            val isHigh = item.risk_level == "High"
                            val isMedium = item.risk_level == "Medium"
                            val isLow = item.risk_level == "Low"
                            
                            (isHigh && showHighRisk) || (isMedium && showMediumRisk) || (isLow && showLowRisk) || (!isHigh && !isMedium && !isLow)
                        }.forEach { item ->
                            val poly = Polygon().apply {
                                title = "${item.risk_level} risk"
                                item.polygon.forEach { coord ->
                                    if (coord.size >= 2) addPoint(GeoPoint(coord[0], coord[1]))
                                }
                                fillPaint.color = AndroidColor.parseColor(item.color)
                                fillPaint.alpha = 80
                                outlinePaint.color = AndroidColor.parseColor(item.color)
                                outlinePaint.strokeWidth = 2f
                            }
                            mapView.overlays.add(poly)
                        }
                        mapView.invalidate()
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // ===== Filters Overlay =====
                Card(
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text("แสดงความเสี่ยง", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
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
                            Text("ต่ำ (Low)", fontSize = 12.sp, color = Color(0xFF4CAF50))
                        }
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
            }
        }
    }
}
