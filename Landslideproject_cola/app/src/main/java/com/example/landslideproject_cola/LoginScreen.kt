package com.example.landslideproject_cola

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

// ====== Color Palette ======
val AppRed     = Color(0xFFD32F2F)
val AppRedDark = Color(0xFFB71C1C)
val AppGreen   = Color(0xFF388E3C)
val AppGreenDark = Color(0xFF2E7D32)
val AppWhite   = Color(0xFFFFFFFF)
val AppGrey    = Color(0xFFF5F5F5)
val AppTextDark = Color(0xFF212121)
val AppTextGrey = Color(0xFF757575)

@Composable
fun LoginScreen(
    navController: NavHostController,
    viewModel: EarthquakeViewModel
) {
    val context = LocalContext.current
    val sharedPref = SharedPreferencesManager(context)

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var rememberMe by rememberSaveable { mutableStateOf(false) }

    val loginResult = viewModel.loginResult
    val isLoading = viewModel.isLoading

    LaunchedEffect(loginResult) {
        loginResult?.let {
            if (!it.error) {
                sharedPref.saveLoginStatus(
                    isLoggedIn = true,
                    userId  = it.user_id ?: "",
                    name    = it.name ?: "",
                    email   = it.email ?: "",
                    role    = it.role ?: "user"
                )
                viewModel.resetLoginResult()
                if (it.role == "admin") {
                    navController.navigate(Screen.AdminHome.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                } else {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            } else {
                viewModel.resetLoginResult()
                Toast.makeText(context, it.message ?: "เข้าสู่ระบบล้มเหลว", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppWhite)
            .imePadding()
    ) {

        // ====== Red Header ======
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppRed)
                .padding(vertical = 36.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Shield icon (ใช้ text แทน)
                Text("🛡", fontSize = 36.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "ล็อคอินไอดีของคุณ",
                    color = AppWhite,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // ====== White Form Area ======
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {

            // Email Field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("อีเมล") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppRed,
                    focusedLabelColor = AppRed,
                    cursorColor = AppRed
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Password Field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("รหัสผ่าน") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    TextButton(onClick = { passwordVisible = !passwordVisible }) {
                        Text(if (passwordVisible) "ซ่อน" else "แสดง", color = AppTextGrey, fontSize = 12.sp)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppRed,
                    focusedLabelColor = AppRed,
                    cursorColor = AppRed
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Remember Me + Forgot Password
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = rememberMe,
                        onCheckedChange = { rememberMe = it },
                        colors = CheckboxDefaults.colors(checkedColor = AppRed)
                    )
                    Text("จดจำฉัน", fontSize = 13.sp, color = AppTextGrey)
                }
                TextButton(onClick = {}) {
                    Text("ลืมรหัสผ่าน?", color = AppRed, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Login Button
            Button(
                onClick = { viewModel.login(email, password) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(8.dp),
                enabled = email.isNotEmpty() && password.isNotEmpty() && !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = AppRed)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = AppWhite, modifier = Modifier.size(20.dp))
                } else {
                    Text("เข้าสู่ระบบ", color = AppWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // ====== Register Link at Bottom ======
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("มีไอดีหรือยังไม่มีก็ไม่มี ", color = AppTextGrey, fontSize = 14.sp)
            TextButton(onClick = { navController.navigate(Screen.Register.route) }) {
                Text("สมัครไอดีที่นี่", color = AppRed, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}
