package com.jdw.skillstestapp.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.ui.graphics.vector.ImageVector

object Constants {
    val items = listOf(
        BottomNaviBarScreen.GoogleMaps,
        BottomNaviBarScreen.GeminiCharRoom,
        BottomNaviBarScreen.FirebaseAuthScreen,
        BottomNaviBarScreen.WeatherApiScreen,
    )

    const val BASE_URL = "https://api.openweathermap.org/"

    val NetworkConnectionTypes = listOf(
        NetworkConnectionType.NoConnection,
        NetworkConnectionType.WiFi,
        NetworkConnectionType.Cellular
    )
}

enum class BottomNaviBarScreen(val label: String, val icon: ImageVector, val route: String) {
    GoogleMaps("Google Maps", Icons.Filled.Map, "googleMaps"),
    GeminiCharRoom("Chat with Gemini", Icons.AutoMirrored.Filled.Message, "geminiChatRoom"),
    FirebaseAuthScreen("Firebase Auth", Icons.AutoMirrored.Filled.Login, "firebaseAuthScreen"),
    WeatherApiScreen("Weather Api Screen", Icons.Filled.Menu, "weatherApiScreen"),
}

enum class NetworkConnectionType(val label: String) {
    NoConnection("No connection"),
    WiFi("WiFi"),
    Cellular("Cellular")
}

