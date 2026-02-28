package com.example.landslideproject_cola

import retrofit2.Response
import retrofit2.http.*

interface EarthquakeAPI {

    // ---------- AUTH ----------
    @POST("api/register")
    suspend fun register(@Body data: RegisterRequest): Response<RegisterResponse>

    @POST("api/login")
    suspend fun login(@Body data: Map<String, String>): Response<LoginResponse>

    // ---------- USER ----------
    @GET("api/user/{id}")
    suspend fun getUserProfile(@Path("id") id: String): Response<UserProfile>

    // ---------- PREDICTIONS ----------
    @GET("api/predictions")
    suspend fun getPredictions(): Response<List<PredictionResponseItem>> // Replaces old LandslidePrediction with the new Response format

    @POST("trigger-prediction") // For Admin
    suspend fun triggerPrediction(): Response<Map<String, Any>>

    // ---------- PINS ----------
    @POST("api/pins")
    suspend fun createPin(@Body data: PinRequest): Response<Map<String, String>>

    // ---------- EVENTS ----------
    @GET("api/events")
    suspend fun getEvents(): Response<List<LandslideEvent>>

    // ---------- NOTIFICATIONS ----------
    @GET("api/notifications/{user_id}")
    suspend fun getNotifications(@Path("user_id") userId: String): Response<List<NotificationItem>>

    @PUT("api/notifications/{notification_id}/read")
    suspend fun markNotificationRead(@Path("notification_id") notifId: String): Response<RegisterResponse>

    // ---------- EMERGENCY ----------
    @GET("api/emergency")
    suspend fun getEmergencyServices(): Response<List<EmergencyService>>

    // ---------- ADMIN ----------
    @GET("api/users")
    suspend fun getAllUsers(): Response<List<UserProfile>>

    @GET("api/admin/alerts/pending")
    suspend fun getPendingAlerts(): Response<List<PendingAlert>>

    @GET("api/admin/alerts/{log_id}")
    suspend fun getAlertDetails(@Path("log_id") logId: String): Response<AdminAlertDetail>

    @PUT("api/admin/alerts/{log_id}/verify")
    suspend fun verifyAlert(@Path("log_id") logId: String, @Body data: VerifyAlertRequest): Response<GenericResponse>

    @PUT("api/emergency/{service_id}")
    suspend fun updateEmergencyService(@Path("service_id") serviceId: String, @Body data: UpdateEmergencyRequest): Response<GenericResponse>

    @POST("api/emergency")
    suspend fun addEmergencyService(@Body data: UpdateEmergencyRequest): Response<GenericResponse>

    @DELETE("api/emergency/{service_id}")
    suspend fun deleteEmergencyService(@Path("service_id") serviceId: String): Response<GenericResponse>

    @POST("api/emergency/{service_id}/image")
    suspend fun uploadEmergencyImage(@Path("service_id") serviceId: String, @Body data: UploadImageRequest): Response<UploadImageResponse>

    @GET("api/admin/alerts/history")
    suspend fun getAlertHistory(): Response<List<PendingAlert>>

    @POST("trigger-gee")
    suspend fun triggerGEE(): Response<GenericResponse>

    @POST("trigger-rain")
    suspend fun triggerRain(): Response<GenericResponse>

    // ---------- PIN DASHBOARD ----------
    @GET("api/pins/user/{user_id}")
    suspend fun getUserPins(@Path("user_id") userId: String): Response<List<UserPinResponse>>

    @GET("api/pins/{pin_id}/dashboard")
    suspend fun getPinDashboard(@Path("pin_id") pinId: String): Response<UserPinDashboard>

    @DELETE("api/pins/{user_id}")
    suspend fun clearUserPins(@Path("user_id") userId: String): Response<GenericResponse>
}
