package com.jdw.skillstestapp

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.jdw.skillstestapp.screens.MainScreen
import com.jdw.skillstestapp.ui.theme.SkillsTestAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
        setContent {
            SkillsTestAppTheme {
                SkillsTestAppScreen()
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SkillsTestAppScreen() {
    val context = LocalContext.current

    val accessPermissions =
        rememberMultiplePermissionsState(
            permissions = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                    Log.d("MainActivity", "TIRAMISU")
                    listOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_MEDIA_LOCATION,
                        Manifest.permission.READ_MEDIA_IMAGES
                    )
                }

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                    Log.d("MainActivity", "Q")
                    listOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_MEDIA_LOCATION,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                }

                else -> {
                    Log.d("MainActivity", "Under versions")
                    listOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                }
            }
        )

    LaunchedEffect(accessPermissions.allPermissionsGranted) {
        if (!accessPermissions.allPermissionsGranted) {
            accessPermissions.launchMultiplePermissionRequest()
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (!accessPermissions.allPermissionsGranted) {
            Toast.makeText(
                context,
                "Need to get all permissions it required.",
                Toast.LENGTH_SHORT
            ).show()

        } else {
            MainScreen()
        }
    }
}
