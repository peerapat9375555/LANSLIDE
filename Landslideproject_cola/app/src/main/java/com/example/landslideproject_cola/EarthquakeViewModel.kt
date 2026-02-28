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

    var predictions by mutableStateOf<List<PredictionResponseItem>>(emptyList())
        private set

    var events by mutableStateOf<List<LandslideEvent>>(emptyList())
        private set

    var notifications by mutableStateOf<List<NotificationItem>>(emptyList())
        private set

    var emergencyServices by mutableStateOf<List<EmergencyService>>(emptyList())
        private set

    var allUsers by mutableStateOf<List<UserProfile>>(emptyList())
        private set

    var pendingAlerts by mutableStateOf<List<PendingAlert>>(emptyList())
        private set

    var adminAlertDetail by mutableStateOf<AdminAlertDetail?>(null)
        private set

    var alertHistory by mutableStateOf<List<PendingAlert>>(emptyList())
        private set

    var userPins by mutableStateOf<List<UserPinResponse>>(emptyList())
        private set

    var pinDashboard by mutableStateOf<UserPinDashboard?>(null)
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

    // ================= ADD PIN =================
    fun addPin(context: Context, request: PinRequest) {
        viewModelScope.launch {
            try {
                val response = EarthquakeClient.earthquakeAPI.createPin(request)
                if (response.isSuccessful) {
                    Toast.makeText(context, "เพิ่มจุดปักหมุดสำเร็จ", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "ไม่สามารถเพิ่มจุดได้", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "เกิดข้อผิดพลาด: ${e.message}", Toast.LENGTH_SHORT).show()
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

    // ================= ADMIN: GET PENDING ALERTS =================
    fun getPendingAlerts() {
        viewModelScope.launch {
            isLoading = true
            try {
                val response = EarthquakeClient.earthquakeAPI.getPendingAlerts()
                if (response.isSuccessful) {
                    pendingAlerts = response.body() ?: emptyList()
                } else {
                    errorMessage = "ดึงข้อมูลการแจ้งเตือนไม่สำเร็จ"
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    // ================= ADMIN: GET ALERT DETAILS =================
    fun getAlertDetails(logId: String) {
        viewModelScope.launch {
            isLoading = true
            adminAlertDetail = null
            try {
                val response = EarthquakeClient.earthquakeAPI.getAlertDetails(logId)
                if (response.isSuccessful) {
                    adminAlertDetail = response.body()
                } else {
                    errorMessage = "ดึงข้อมูลรายละเอียดไม่สำเร็จ"
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    // ================= ADMIN: VERIFY ALERT =================
    fun verifyAlert(context: Context, logId: String, action: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            try {
                val request = VerifyAlertRequest(action)
                val response = EarthquakeClient.earthquakeAPI.verifyAlert(logId, request)
                if (response.isSuccessful) {
                    Toast.makeText(context, "ดำเนินการสำเร็จ", Toast.LENGTH_SHORT).show()
                    onSuccess()
                } else {
                    Toast.makeText(context, "ดำเนินการไม่สำเร็จ", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    // ================= ADMIN: TRIGGER PREDICTION =================
    fun triggerPrediction(context: Context) {
        viewModelScope.launch {
            isLoading = true
            try {
                val response = EarthquakeClient.earthquakeAPI.triggerPrediction()
                if (response.isSuccessful) {
                    Toast.makeText(context, "ดึงข้อมูลและวิเคราะห์เสร็จสิ้น", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "เกิดข้อผิดพลาดในการวิเคราะห์", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isLoading = false
            }
        }
    }

    // ================= ADMIN: EDIT EMERGENCY =================
    fun updateEmergencyService(context: Context, serviceId: String, name: String, phone: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            try {
                val request = UpdateEmergencyRequest(name, phone)
                val response = EarthquakeClient.earthquakeAPI.updateEmergencyService(serviceId, request)
                if (response.isSuccessful) {
                    Toast.makeText(context, "อัปเดตเบอร์สำเร็จ", Toast.LENGTH_SHORT).show()
                    onSuccess()
                } else {
                    Toast.makeText(context, "อัปเดตเบอร์ไม่สำเร็จ", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    // ================= ADMIN: ADD EMERGENCY SERVICE =================
    fun addEmergencyService(context: Context, name: String, phone: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            try {
                val request = UpdateEmergencyRequest(name, phone)
                val response = EarthquakeClient.earthquakeAPI.addEmergencyService(request)
                if (response.isSuccessful) {
                    Toast.makeText(context, "เพิ่มเบอร์สำเร็จ", Toast.LENGTH_SHORT).show()
                    onSuccess()
                } else {
                    Toast.makeText(context, "เพิ่มเบอร์ไม่สำเร็จ", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    // ================= ADMIN: DELETE EMERGENCY SERVICE =================
    fun deleteEmergencyService(context: Context, serviceId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            try {
                val response = EarthquakeClient.earthquakeAPI.deleteEmergencyService(serviceId)
                if (response.isSuccessful) {
                    Toast.makeText(context, "ลบรายการสำเร็จ", Toast.LENGTH_SHORT).show()
                    onSuccess()
                } else {
                    Toast.makeText(context, "ลบรายการไม่สำเร็จ", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    // ================= ADMIN: UPLOAD EMERGENCY IMAGE =================
    fun uploadEmergencyImage(context: Context, serviceId: String, imageBase64: String, filename: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            isLoading = true
            try {
                val request = UploadImageRequest(imageBase64, filename)
                val response = EarthquakeClient.earthquakeAPI.uploadEmergencyImage(serviceId, request)
                if (response.isSuccessful && response.body()?.img_url != null) {
                    val imgUrl = response.body()!!.img_url!!
                    Toast.makeText(context, "อัปโหลดรูปสำเร็จ", Toast.LENGTH_SHORT).show()
                    onSuccess(imgUrl)
                } else {
                    Toast.makeText(context, "อัปโหลดรูปไม่สำเร็จ", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    // ================= USER PINS: GET PINS =================
    fun getUserPins(userId: String) {
        viewModelScope.launch {
            try {
                val response = EarthquakeClient.earthquakeAPI.getUserPins(userId)
                if (response.isSuccessful) {
                    userPins = response.body() ?: emptyList()
                }
            } catch (e: Exception) {}
        }
    }

    // ================= USER PINS: CLEAR PINS =================
    fun clearUserPins(context: Context, userId: String) {
        viewModelScope.launch {
            try {
                val response = EarthquakeClient.earthquakeAPI.clearUserPins(userId)
                if (response.isSuccessful) {
                    Toast.makeText(context, "เคลียร์หมุดสำเร็จ", Toast.LENGTH_SHORT).show()
                    userPins = emptyList() // clear local state
                    getPredictions() // refresh map to clear pins
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ================= USER PINS: GET PIN DASHBOARD =================
    fun getPinDashboard(pinId: String) {
        viewModelScope.launch {
            isLoading = true
            pinDashboard = null
            try {
                val response = EarthquakeClient.earthquakeAPI.getPinDashboard(pinId)
                if (response.isSuccessful) {
                    pinDashboard = response.body()
                } else {
                    errorMessage = "ดึงข้อมูลหมุดไม่สำเร็จ"
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    // ================= ADMIN: GET ALERT HISTORY =================
    fun getAlertHistory() {
        viewModelScope.launch {
            try {
                val response = EarthquakeClient.earthquakeAPI.getAlertHistory()
                if (response.isSuccessful) {
                    alertHistory = response.body() ?: emptyList()
                }
            } catch (e: Exception) {}
        }
    }

    // ================= ADMIN: TRIGGER GEE =================
    fun triggerGEE(context: Context) {
        viewModelScope.launch {
            isLoading = true
            try {
                val response = EarthquakeClient.earthquakeAPI.triggerGEE()
                if (response.isSuccessful) {
                    Toast.makeText(context, "โหลดข้อมูล GEE สำเร็จ", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "เกิดข้อผิดพลาดในการดึง GEE", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isLoading = false
            }
        }
    }

    // ================= ADMIN: TRIGGER RAIN + PREDICT =================
    fun triggerRain(context: Context) {
        viewModelScope.launch {
            isLoading = true
            try {
                val response = EarthquakeClient.earthquakeAPI.triggerRain()
                if (response.isSuccessful) {
                    Toast.makeText(context, "ดึงน้ำฝน + วิเคราะห์สำเร็จ", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "เกิดข้อผิดพลาด", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isLoading = false
            }
        }
    }

    // ================= SAVE USER LOCATION =================
    fun saveUserLocation(context: Context, userId: String, req: SaveLocationRequest, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val response = EarthquakeClient.earthquakeAPI.saveUserLocation(userId, req)
                if (response.isSuccessful) {
                    Toast.makeText(context, "บันทึกตำแหน่งสำเร็จ", Toast.LENGTH_SHORT).show()
                    onResult(true)
                } else {
                    Toast.makeText(context, "บันทึกไม่สำเร็จ", Toast.LENGTH_SHORT).show()
                    onResult(false)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                onResult(false)
            }
        }
    }
}
