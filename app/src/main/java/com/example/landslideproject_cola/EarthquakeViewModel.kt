package com.example.landslideproject_cola

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import kotlinx.coroutines.launch

class EarthquakeViewModel : ViewModel() {

    var loginResult by mutableStateOf<LoginResponse?>(null)
        private set

    var userProfile by mutableStateOf<UserProfile?>(null)
        private set

    var predictions by mutableStateOf<List<LandslidePrediction>>(emptyList())
        private set

    var events by mutableStateOf<List<LandslideEvent>>(emptyList())
        private set

    var notifications by mutableStateOf<List<NotificationItem>>(emptyList())
        private set

    var emergencyServices by mutableStateOf<List<EmergencyService>>(emptyList())
        private set

    var allUsers by mutableStateOf<List<UserProfile>>(emptyList())
        private set

    var errorMessage by mutableStateOf("")
        private set

    var isLoading by mutableStateOf(false)
        private set

    fun resetLoginResult() {
        loginResult = null
    }

    // ================= LOGIN =================
    fun login(email: String, password: String) {
        viewModelScope.launch {
            isLoading = true
            try {
                val loginData = mapOf("email" to email, "password" to password)
                val response = EarthquakeClient.earthquakeAPI.login(loginData)
                if (response.isSuccessful) {
                    loginResult = response.body()
                    errorMessage = ""
                } else {
                    errorMessage = "เข้าสู่ระบบล้มเหลว: อีเมลหรือรหัสผ่านไม่ถูกต้อง"
                }
            } catch (e: Exception) {
                errorMessage = "เกิดข้อผิดพลาด: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    // ================= REGISTER =================
    fun register(context: Context, data: RegisterRequest, onSuccess: () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            try {
                val response = EarthquakeClient.earthquakeAPI.register(data)
                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    val errorRaw = response.errorBody()?.string()
                    val finalMessage = if (!errorRaw.isNullOrEmpty()) {
                        try {
                            Gson().fromJson(errorRaw, RegisterResponse::class.java).message
                        } catch (e: Exception) { errorRaw }
                    } else response.message()
                    errorMessage = finalMessage
                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                errorMessage = "Network Error: ${e.message}"
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    // ================= GET PROFILE =================
    fun getProfile(userId: String) {
        viewModelScope.launch {
            try {
                val response = EarthquakeClient.earthquakeAPI.getUserProfile(userId)
                if (response.isSuccessful) {
                    userProfile = response.body()
                } else {
                    errorMessage = "ไม่พบข้อมูลผู้ใช้"
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
            }
        }
    }

    // ================= GET PREDICTIONS =================
    fun getPredictions() {
        viewModelScope.launch {
            isLoading = true
            try {
                val response = EarthquakeClient.earthquakeAPI.getPredictions()
                if (response.isSuccessful) {
                    predictions = response.body() ?: emptyList()
                } else {
                    errorMessage = "ไม่สามารถโหลดข้อมูลการทำนายได้"
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    // ================= GET EVENTS =================
    fun getEvents() {
        viewModelScope.launch {
            isLoading = true
            try {
                val response = EarthquakeClient.earthquakeAPI.getEvents()
                if (response.isSuccessful) {
                    events = response.body() ?: emptyList()
                } else {
                    errorMessage = "ไม่สามารถโหลดเหตุการณ์ได้"
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    // ================= GET NOTIFICATIONS =================
    fun getNotifications(userId: String) {
        viewModelScope.launch {
            try {
                val response = EarthquakeClient.earthquakeAPI.getNotifications(userId)
                if (response.isSuccessful) {
                    notifications = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
            }
        }
    }

    // ================= MARK NOTIFICATION READ =================
    fun markRead(notifId: String, userId: String) {
        viewModelScope.launch {
            try {
                EarthquakeClient.earthquakeAPI.markNotificationRead(notifId)
                getNotifications(userId)
            } catch (_: Exception) {}
        }
    }

    // ================= GET EMERGENCY SERVICES =================
    fun getEmergencyServices() {
        viewModelScope.launch {
            isLoading = true
            try {
                val response = EarthquakeClient.earthquakeAPI.getEmergencyServices()
                if (response.isSuccessful) {
                    emergencyServices = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    // ================= GET ALL USERS (admin) =================
    fun getAllUsers() {
        viewModelScope.launch {
            try {
                val response = EarthquakeClient.earthquakeAPI.getAllUsers()
                if (response.isSuccessful) {
                    allUsers = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
            }
        }
    }
}
