package com.jdw.skillstestapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.jdw.skillstestapp.repository.MyAppRepository
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

//@AndroidEntryPoint
class PhotoBroadcastReceiver : BroadcastReceiver() {
//    @Inject
//    lateinit var repository: MyAppRepository

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("PhotoBroadcastReceiver", "onReceive : ${intent.action}")
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
//        if (intent.action == Intent.ACTION_MEDIA_SCANNER_FINISHED ||
//            intent.action == Intent.ACTION_CAMERA_BUTTON ||
//            intent.action == "android.intent.action.MEDIA_SCANNER_SCAN_FILE") {
//            val uri = intent.data
//            if (uri != null) {
//                Log.d("PhotoBroadcastReceiver", "Photo taken: $uri")
//                // Do something with the photo URI
//                CoroutineScope(Dispatchers.IO).launch {
//                    repository.getUserImgFromUri(uri)
//                }
//
//            }
//        }
    }
}