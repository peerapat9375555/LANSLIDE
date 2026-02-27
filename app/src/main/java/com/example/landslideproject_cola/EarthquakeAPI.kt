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
    suspend fun getPredictions(): Response<List<LandslidePrediction>>

    @GET("api/predictions/{id}")
    suspend fun getPredictionById(@Path("id") id: String): Response<LandslidePrediction>

    @POST("api/predictions")
    suspend fun createPrediction(@Body data: Map<String, String>): Response<RegisterResponse>

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
}
