package com.example.landslideproject_cola

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    navController: NavHostController,
    viewModel: EarthquakeViewModel
) {
    val context = LocalContext.current
    val sharedPref = SharedPreferencesManager(context)
    val savedId   = sharedPref.getSavedUserId()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (savedId.isNotEmpty()) viewModel.getProfile(savedId)
    }

    val profile = viewModel.userProfile

    var firstName by rememberSaveable { mutableStateOf("") }
    var lastName  by rememberSaveable { mutableStateOf("") }
    var email     by rememberSaveable { mutableStateOf("") }
    var phone     by rememberSaveable { mutableStateOf("") }
    var password  by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(profile) {
        profile?.let {
            val parts = (it.name ?: "").split(" ")
            firstName = parts.getOrNull(0) ?: ""
            lastName  = parts.getOrNull(1) ?: ""
            email     = it.email ?: ""
            phone     = it.phone ?: ""
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(navController = navController, onClose = { scope.launch { drawerState.close() } })
        }
    ) {
        Scaffold(
            topBar = {
                GreenTopBar(title = "แก้ไขข้อมูลผู้ใช้") { scope.launch { drawerState.open() } }
            },
            bottomBar = { AppBottomNav(navController) },
            containerColor = AppWhite
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // ชื่อ + นามสกุล row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("ชื่อ", fontSize = 12.sp, color = AppTextGrey)
                        OutlinedTextField(
                            value = firstName,
                            onValueChange = { firstName = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AppRed, focusedLabelColor = AppRed, cursorColor = AppRed
                            )
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("นามสกุล", fontSize = 12.sp, color = AppTextGrey)
                        OutlinedTextField(
                            value = lastName,
                            onValueChange = { lastName = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AppRed, focusedLabelColor = AppRed, cursorColor = AppRed
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("อีเมล", fontSize = 12.sp, color = AppTextGrey)
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppRed, focusedLabelColor = AppRed, cursorColor = AppRed
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text("เบอร์โทรศัพท์", fontSize = 12.sp, color = AppTextGrey)
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppRed, focusedLabelColor = AppRed, cursorColor = AppRed
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text("รหัสผ่าน", fontSize = 12.sp, color = AppTextGrey)
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(onClick = { passwordVisible = !passwordVisible }) {
                            Text(if (passwordVisible) "ซ่อน" else "แสดง", color = AppTextGrey, fontSize = 12.sp)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppRed, focusedLabelColor = AppRed, cursorColor = AppRed
                    )
                )

                Spacer(modifier = Modifier.height(28.dp))

                // แก้ไข Button
                Button(
                    onClick = { /* TODO: call update API */ },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppRed)
                ) {
                    Text("แก้ไข", color = AppWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ปักหมุดที่อยู่
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { navController.navigate(Screen.SetLocation.route) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppGreen)
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = AppGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ปักหมุดที่อยู่ของฉัน", fontSize = 15.sp, color = AppGreen)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Logout
                OutlinedButton(
                    onClick = {
                        sharedPref.logout()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppRed)
                ) {
                    Text("ออกจากระบบ", fontSize = 16.sp)
                }
            }
        }
    }
}
