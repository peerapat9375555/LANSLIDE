package com.example.landslideproject_cola

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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

@Composable
fun RegisterScreen(
    navController: NavHostController,
    viewModel: EarthquakeViewModel
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var firstName by rememberSaveable { mutableStateOf("") }
    var lastName  by rememberSaveable { mutableStateOf("") }
    var email     by rememberSaveable { mutableStateOf("") }
    var phone     by rememberSaveable { mutableStateOf("") }
    var password  by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    val isLoading = viewModel.isLoading

    Column(modifier = Modifier.fillMaxSize().background(AppWhite)) {

        // ====== Red Header ======
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppRed)
                .padding(vertical = 28.dp, horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🛡", fontSize = 32.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "กรอกรายละเอียดเพื่อสมัคร\nไอดีของคุณ",
                    color = AppWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }

        // ====== Sub-header ======
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("มีไอดีหรือยังไม่มีก็ไม่มี ", color = AppTextGrey, fontSize = 14.sp)
            TextButton(onClick = { navController.popBackStack() }) {
                Text("เข้าสู่ระบบ", color = AppRed, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        // ====== Form ======
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
        ) {
            // ชื่อ + นามสกุล
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("ชื่อ") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppRed, focusedLabelColor = AppRed, cursorColor = AppRed
                    )
                )
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("นามสกุล") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppRed, focusedLabelColor = AppRed, cursorColor = AppRed
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // อีเมล
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("อีเมล") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppRed, focusedLabelColor = AppRed, cursorColor = AppRed
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // เบอร์โทรศัพท์
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("เบอร์โทรศัพท์") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppRed, focusedLabelColor = AppRed, cursorColor = AppRed
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // รหัสผ่าน
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
                    focusedBorderColor = AppRed, focusedLabelColor = AppRed, cursorColor = AppRed
                )
            )

            Spacer(modifier = Modifier.height(28.dp))

            // สมัคร Button
            Button(
                onClick = {
                    val data = RegisterRequest(
                        name     = "$firstName $lastName".trim(),
                        email    = email,
                        phone    = phone,
                        password = password
                    )
                    viewModel.register(context, data) {
                        Toast.makeText(context, "สมัครสมาชิกสำเร็จ!", Toast.LENGTH_SHORT).show()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Register.route) { inclusive = true }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(8.dp),
                enabled = firstName.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty() && !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = AppRed)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = AppWhite, modifier = Modifier.size(20.dp))
                } else {
                    Text("สมัคร", color = AppWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // ====== Back Button ======
        Box(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, bottom = 16.dp)) {
            FloatingActionButton(
                onClick = { navController.popBackStack() },
                containerColor = AppRed,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "ย้อนกลับ", tint = AppWhite)
            }
        }
    }
}
