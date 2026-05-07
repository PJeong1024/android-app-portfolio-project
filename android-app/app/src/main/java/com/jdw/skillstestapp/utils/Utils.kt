package com.jdw.skillstestapp.utils

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.text.SimpleDateFormat
import java.util.Date

fun getTimeStamp(): Long = System.currentTimeMillis()

@SuppressLint("SimpleDateFormat")
fun convertTimeStampToDate(timeStamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    return sdf.format(Date(timeStamp))
}

@SuppressLint("SimpleDateFormat")
fun formatDateTime(timestamp: Int): String {
    val sdf = SimpleDateFormat("hh:mm:aa")
    val date = Date(timestamp.toLong() * 1000)
    return sdf.format(date)
}

fun getConnectionType(context: Context): NetworkConnectionType {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val network = connectivityManager.activeNetwork ?: return NetworkConnectionType.NoConnection
    val activeNetwork = connectivityManager.getNetworkCapabilities(network)
        ?: return NetworkConnectionType.NoConnection

    return when {
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkConnectionType.WiFi
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkConnectionType.Cellular
        else -> NetworkConnectionType.NoConnection
    }
}
