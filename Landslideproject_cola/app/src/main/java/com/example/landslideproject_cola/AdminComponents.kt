package com.example.landslideproject_cola

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

// ====== Admin Bottom Nav: 3 tabs only ======
@Composable
fun AdminBottomNavigationBar(navController: NavHostController, currentRoute: String?) {
    val isHome = currentRoute?.startsWith("admin_home_screen") == true
    val isAlerts = currentRoute == Screen.AdminAlerts.route
    val isMap = currentRoute?.startsWith("admin_map_screen") == true

    NavigationBar(containerColor = Color.White, contentColor = AppRed) {
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
            label = { Text("หน้าหลัก", fontSize = 11.sp, color = if (isHome) AppRed else Color.Gray) },
            selected = isHome,
            onClick = {
                if (!isHome) {
                    navController.navigate(Screen.AdminHome.createRoute()) {
                        popUpTo(Screen.AdminHome.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = AppRed, unselectedIconColor = Color.Gray, indicatorColor = Color.Transparent)
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Notifications, contentDescription = "Alerts") },
            label = { Text("แจ้งเตือน", fontSize = 11.sp, color = if (isAlerts) AppRed else Color.Gray) },
            selected = isAlerts,
            onClick = {
                if (!isAlerts) {
                    navController.navigate(Screen.AdminAlerts.route) {
                        popUpTo(Screen.AdminHome.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = AppRed, unselectedIconColor = Color.Gray, indicatorColor = Color.Transparent)
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.LocationOn, contentDescription = "Map") },
            label = { Text("แผนที่", fontSize = 11.sp, color = if (isMap) AppRed else Color.Gray) },
            selected = isMap,
            onClick = {
                if (!isMap) {
                    navController.navigate(Screen.AdminMap.createRoute()) {
                        popUpTo(Screen.AdminHome.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = AppRed, unselectedIconColor = Color.Gray, indicatorColor = Color.Transparent)
        )
    }
}

// ====== Admin Hamburger Drawer ======
@Composable
fun AdminDrawer(navController: NavHostController, onClose: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPref = SharedPreferencesManager(context)

    ModalDrawerSheet(
        drawerContainerColor = Color.White,
        modifier = Modifier.width(280.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Admin Panel",
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = AppRed
        )

        Divider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), color = Color.LightGray)

        val items = listOf(
            Triple(Icons.Default.Build, "ดึงข้อมูล GEE / น้ำฝน", Screen.AdminAnalysis),
            Triple(Icons.Default.Phone, "แก้ไขเบอร์ฉุกเฉิน", Screen.AdminEmergency),
            Triple(Icons.Default.DateRange, "ประวัติการยืนยัน", Screen.AdminNotificationHistory),
            Triple(Icons.Default.Notifications, "ประวัติการแจ้งเตือน", Screen.AdminSentNotificationHistory),
            Triple(Icons.Default.Inbox, "รายงานจากผู้ใช้", Screen.AdminReports),
            Triple(Icons.Default.History, "ประวัติการช่วยเหลือ", Screen.AdminReportHistory)
        )

        items.forEachIndexed { index, (icon, label, screen) ->
            NavigationDrawerItem(
                icon = { Icon(icon, null, tint = AppTextDark) },
                label = { Text(label, color = AppTextDark, fontSize = 15.sp) },
                selected = false,
                onClick = {
                    onClose()
                    navController.navigate(screen.route) { launchSingleTop = true }
                },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
            if (index < items.lastIndex) {
                Divider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = Color(0xFFBBBBBB),
                    thickness = 1.dp
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Divider(modifier = Modifier.padding(horizontal = 16.dp), color = Color.LightGray)
        Spacer(modifier = Modifier.height(8.dp))

        NavigationDrawerItem(
            icon = { Icon(Icons.Default.ExitToApp, null, tint = AppRed) },
            label = { Text("ออกจากระบบ", color = AppRed, fontSize = 15.sp, fontWeight = FontWeight.Bold) },
            selected = false,
            onClick = {
                onClose()
                sharedPref.logout()
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = AppRed.copy(alpha = 0.08f))
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ====== Admin TopBar ======
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTopBar(title: String, onMenuClick: () -> Unit) {
    TopAppBar(
        title = { Text(text = title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White) },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.Menu, contentDescription = "เมนู", tint = Color.White, modifier = Modifier.size(28.dp))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = AppRed)
    )
}
