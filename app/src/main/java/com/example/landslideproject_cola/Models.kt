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
