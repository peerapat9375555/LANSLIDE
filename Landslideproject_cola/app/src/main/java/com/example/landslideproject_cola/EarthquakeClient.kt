package com.example.landslideproject_cola

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object EarthquakeClient {

    // 10.0.2.2 = localhost ของเครื่องคอม สำหรับ Android Emulator
    // หากใช้มือถือจริง ให้เปลี่ยนเป็น "http://192.168.1.213:8000/"
    private const val BASE_URL = "http://10.0.2.2:8000/"

    val earthquakeAPI: EarthquakeAPI by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(EarthquakeAPI::class.java)
    }
}
