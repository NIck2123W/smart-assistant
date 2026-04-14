package com.example.smartassistant.location

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// 1. 定義 Google Directions API 的回傳格式
data class DirectionsResponse(val routes: List<Route>)
data class Route(val legs: List<Leg>)
data class Leg(val steps: List<Step>, val distance: TextValue, val duration: TextValue)
data class Step(
    val distance: TextValue,
    val html_instructions: String,
    val maneuver: String? // 轉彎方向，例如 turn-left
)
data class TextValue(val text: String, val value: Int)

// 2. 建立 Retrofit 介面
interface DirectionsApiService {
    @GET("maps/api/directions/json")
    suspend fun getDirections(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("key") apiKey: String,
        @Query("language") language: String = "zh-TW" // 強制回傳繁體中文指令
    ): DirectionsResponse
}

// 3. 建立連線實體
object RetrofitClient {
    private const val BASE_URL = "https://maps.googleapis.com/"

    val apiService: DirectionsApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DirectionsApiService::class.java)
    }
}