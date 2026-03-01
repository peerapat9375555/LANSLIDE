package com.example.landslideproject_cola

import com.google.gson.annotations.SerializedName

// ---- AUTH ----
data class LoginResponse(
    @SerializedName("error") val error: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("token") val token: String?,
    @SerializedName("user_id") val user_id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("role") val role: String?
)

data class RegisterRequest(
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("password") val password: String,
    @SerializedName("role") val role: String = "user"
)

data class RegisterResponse(
    @SerializedName("error") val error: Boolean,
    @SerializedName("message") val message: String
)

// ---- USER ----
data class UserProfile(
    @SerializedName("error") val error: Boolean?,
    @SerializedName("message") val message: String?,
    @SerializedName("user_id") val user_id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("phone") val phone: String?,
    @SerializedName("role") val role: String?
)

// ---- PREDICTION ----
data class LandslidePrediction(
    @SerializedName("prediction_id") val prediction_id: String,
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?,
    @SerializedName("district") val district: String?,
    @SerializedName("risk_score") val risk_score: Float,
    @SerializedName("risk_level") val risk_level: String?,
    @SerializedName("confidence") val confidence: Float?,
    @SerializedName("model_version") val model_version: String?,
    @SerializedName("analyzed_at") val analyzed_at: String?
)

// ---- EVENT ----
data class LandslideEvent(
    @SerializedName("event_id") val event_id: String,
    @SerializedName("district") val district: String?,
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?,
    @SerializedName("occurred_at") val occurred_at: String?,
    @SerializedName("level_name") val level_name: String?,
    @SerializedName("verified") val verified: Int?,
    @SerializedName("created_at") val created_at: String?
)

// ---- NOTIFICATION ----
data class NotificationItem(
    @SerializedName("notification_id") val notification_id: String,
    @SerializedName("user_id") val user_id: String,
    @SerializedName("log_id") val log_id: String?, // เพิ่ม log_id สำหรับลิ้งค์ไปยังกราฟ
    @SerializedName("title") val title: String,
    @SerializedName("message") val message: String,
    @SerializedName("sent_at") val sent_at: String?,
    @SerializedName("is_read") val is_read: Int?
)

// ---- EMERGENCY ----
data class EmergencyService(
    @SerializedName("service_id") val service_id: String,
    @SerializedName("service_name") val service_name: String,
    @SerializedName("phone_number") val phone_number: String,
    @SerializedName("district") val district: String?,
    @SerializedName("img_url") val img_url: String?
)

// ---- NEW MODELS ----
data class PredictionResponseItem(
    @SerializedName("id") val id: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("risk_level") val risk_level: String,
    @SerializedName("color") val color: String,
    @SerializedName("polygon") val polygon: List<List<Double>>
)

data class PinRequest(
    @SerializedName("user_id") val user_id: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("label") val label: String?
)

// ---- ADMIN & PIN DASHBOARD MODELS ----
data class VerifyAlertRequest(
    @SerializedName("action") val action: String
)

data class UpdateEmergencyRequest(
    @SerializedName("service_name") val service_name: String,
    @SerializedName("phone_number") val phone_number: String,
    @SerializedName("img_url") val img_url: String? = null
)

data class UploadImageRequest(
    @SerializedName("image_base64") val image_base64: String,
    @SerializedName("filename") val filename: String
)

data class UploadImageResponse(
    @SerializedName("status") val status: String,
    @SerializedName("img_url") val img_url: String?
)

data class PendingAlert(
    @SerializedName("log_id") val log_id: String,
    @SerializedName("node_id") val node_id: Int,
    @SerializedName("risk_level") val risk_level: String,
    @SerializedName("probability") val probability: Float,
    @SerializedName("timestamp") val timestamp: String?,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("tambon") val tambon: String?,
    @SerializedName("district") val district: String?
)

data class AdminAlertDetail(
    @SerializedName("log_id") val log_id: String,
    @SerializedName("node_id") val node_id: Int,
    @SerializedName("risk_level") val risk_level: String,
    @SerializedName("probability") val probability: Float,
    @SerializedName("status") val status: String,
    @SerializedName("timestamp") val timestamp: String?,
    @SerializedName("features_json") val features_json: Map<String, Float>?,
    @SerializedName("rain_values_json") val rain_values_json: List<Float>?,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("tambon") val tambon: String?,
    @SerializedName("district") val district: String?
)

data class UserPinDashboard(
    @SerializedName("label") val label: String?,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("rain_trend") val rain_trend: List<Float>?
)

data class GenericResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String?,
    @SerializedName("deleted") val deleted: Int?
)

data class UserPinResponse(
    @SerializedName("pin_id") val pin_id: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("label") val label: String?
)

// ---- USER LOCATION ----
data class SaveLocationRequest(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("location_name") val location_name: String?,
    @SerializedName("district") val district: String?,   // อำเภอ
    @SerializedName("tambon") val tambon: String?         // ตำบล
)

data class UserLocationData(
    @SerializedName("location_id") val location_id: String?,
    @SerializedName("user_id") val user_id: String?,
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?,
    @SerializedName("location_name") val location_name: String?,
    @SerializedName("district") val district: String?,   // อำเภอ
    @SerializedName("tambon") val tambon: String?,        // ตำบล
    @SerializedName("updated_at") val updated_at: String?
)

data class UserLocationResponse(
    @SerializedName("status") val status: String,
    @SerializedName("data") val data: UserLocationData?
)

// ---- USER REPORT (ส่งข้อมูลให้แอดมิน) ----
data class UserReportRequest(
    @SerializedName("user_id") val user_id: String,
    @SerializedName("title") val title: String,
    @SerializedName("message") val message: String,
    @SerializedName("image_base64") val image_base64: String?,
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?
)

data class UserReportItem(
    @SerializedName("report_id") val report_id: String,
    @SerializedName("user_id") val user_id: String,
    @SerializedName("user_name") val user_name: String?,
    @SerializedName("title") val title: String,
    @SerializedName("message") val message: String,
    @SerializedName("img_url") val img_url: String?,
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?,
    @SerializedName("tambon") val tambon: String?,
    @SerializedName("district") val district: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("completed_at") val completed_at: String?,
    @SerializedName("created_at") val created_at: String?
)
