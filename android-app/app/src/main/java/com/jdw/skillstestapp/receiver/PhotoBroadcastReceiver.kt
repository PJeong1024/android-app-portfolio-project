package com.jdw.skillstestapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class PhotoBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("PhotoBroadcastReceiver", "onReceive : ${intent.action}")
    }
}
