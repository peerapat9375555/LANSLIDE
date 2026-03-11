package com.example.landslideproject_cola

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.util.Log
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

    // Track which logId the admin is viewing on the Dashboard
    var currentDashboardLogId by mutableStateOf<String?>(null)

    fun setDashboardLogId(logId: String?) {
        currentDashboardLogId = logId
    }

    var alertHistory by mutableStateOf<List<PendingAlert>>(emptyList())
        private set

    var userPins by mutableStateOf<List<UserPinResponse>>(emptyList())
        private set

    var userLocation by mutableStateOf<UserLocationData?>(null)
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
                    userProfile = null
                }
            } catch (e: Exception) {
                userProfile = null
            }
        }
    }

    fun updateProfile(context: Context, userId: String, request: UpdateProfileRequest, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                val response = EarthquakeClient.earthquakeAPI.updateUserProfile(userId, request)
                if (response.isSuccessful) {
                    Toast.makeText(context, "แก้ไขข้อมูลสำเร็จ", Toast.LENGTH_SHORT).show()
                    getProfile(userId) // Refresh profile data
                    onComplete()
                } else {
                    Toast.makeText(context, "แก้ไขข้อมูลไม่สำเร็จ: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
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

    // ================= UPLOAD EMERGENCY IMAGE =================
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

    // ================= USER LOCATION: GET LOCATION =================
    fun getUserLocation(userId: String) {
        viewModelScope.launch {
            try {
                android.util.Log.d("UserLocation", "Calling GET /api/user-location for user: $userId")
                val response = EarthquakeClient.earthquakeAPI.getUserLocation(userId)
                android.util.Log.d("UserLocation", "Response code: ${response.code()}")
                if (response.isSuccessful && response.body()?.status == "success") {
                    val data = response.body()?.data
                    android.util.Log.d("UserLocation", "Parsed Data: lat=${data?.latitude}, lon=${data?.longitude}, tambon=${data?.tambon}")
                    userLocation = data
                } else {
                    val errBody = response.errorBody()?.string()
                    android.util.Log.e("UserLocation", "Error or Not Found: ${response.code()} Body: $errBody")
                    userLocation = null
                }
            } catch (e: Exception) {
                android.util.Log.e("UserLocation", "Exception: ${e.message}", e)
                userLocation = null
            }
        }
    }

    // ================= ADMIN: GET ALERT HISTORY =================
    fun getAlertHistory(startDate: String? = null, endDate: String? = null) {
        viewModelScope.launch {
            try {
                val response = EarthquakeClient.earthquakeAPI.getAlertHistory(startDate, endDate)
                if (response.isSuccessful) {
                    alertHistory = response.body() ?: emptyList()
                }
            } catch (e: Exception) {}
        }
    }

    var sentNotificationHistory by mutableStateOf<List<PendingAlert>>(emptyList())
        private set

    // ================= ADMIN: GET SENT NOTIFICATION HISTORY =================
    fun getSentNotificationHistory(startDate: String? = null, endDate: String? = null) {
        viewModelScope.launch {
            try {
                val response = EarthquakeClient.earthquakeAPI.getSentNotificationHistory(startDate, endDate)
                if (response.isSuccessful) {
                    sentNotificationHistory = response.body() ?: emptyList()
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

    // ================= USER REPORT: ส่งรายงานไปแอดมิน =================
    var userReports by mutableStateOf<List<UserReportItem>>(emptyList())
        private set

    fun submitUserReport(context: Context, request: UserReportRequest, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            isLoading = true
            try {
                val response = EarthquakeClient.earthquakeAPI.submitReport(request)
                if (response.isSuccessful) {
                    Toast.makeText(context, "ส่งรายงานสำเร็จ!", Toast.LENGTH_SHORT).show()
                    onResult(true)
                } else {
                    Toast.makeText(context, "ส่งรายงานไม่สำเร็จ", Toast.LENGTH_SHORT).show()
                    onResult(false)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                onResult(false)
            } finally {
                isLoading = false
            }
        }
    }

    // ================= ADMIN: ดูรายงานจาก user =================
    fun getAdminReports() {
        viewModelScope.launch {
            isLoading = true
            try {
                android.util.Log.d("AdminReports", "Calling GET /api/reports ...")
                val response = EarthquakeClient.earthquakeAPI.getAdminReports()
                android.util.Log.d("AdminReports", "Response code: ${response.code()}")
                android.util.Log.d("AdminReports", "Response body: ${response.body()?.size} items")
                if (response.isSuccessful) {
                    userReports = response.body() ?: emptyList()
                    android.util.Log.d("AdminReports", "Loaded ${userReports.size} reports")
                } else {
                    val errBody = response.errorBody()?.string()
                    android.util.Log.e("AdminReports", "Error ${response.code()}: $errBody")
                    errorMessage = "โหลดรายงานไม่สำเร็จ (${response.code()})"
                }
            } catch (e: Exception) {
                android.util.Log.e("AdminReports", "Exception: ${e.message}", e)
                errorMessage = "Error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    // ================= ADMIN: ช่วยเหลือเสร็จสิ้น =================
    fun completeReport(context: Context, reportId: String, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                val response = EarthquakeClient.earthquakeAPI.completeReport(reportId)
                if (response.isSuccessful) {
                    // ลบออกจากรายการ pending
                    userReports = userReports.filter { it.report_id != reportId }
                    Toast.makeText(context, "✅ บันทึกว่าช่วยเหลือเสร็จสิ้นแล้ว", Toast.LENGTH_SHORT).show()
                    onDone()
                } else {
                    Toast.makeText(context, "เกิดข้อผิดพลาด", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ================= ADMIN: ประวัติการช่วยเหลือ =================
    var completedReports by mutableStateOf<List<UserReportItem>>(emptyList())
        private set

    fun getReportHistory() {
        viewModelScope.launch {
            isLoading = true
            try {
                val response = EarthquakeClient.earthquakeAPI.getReportHistory()
                if (response.isSuccessful) {
                    completedReports = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    // --- Restore Rain Trend from Location Dashboard ---
    fun getDashboardByLocation(lat: Double, lon: Double) {
        viewModelScope.launch {
            try {
                isLoading = true
                val response = EarthquakeClient.earthquakeAPI.getDashboardByLocation(lat, lon)
                if (response.isSuccessful) {
                    pinDashboard = response.body()
                } else {
                    Log.e("EarthquakeVM", "getDashboardByLocation error: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("EarthquakeVM", "getDashboardByLocation failed: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }
}
