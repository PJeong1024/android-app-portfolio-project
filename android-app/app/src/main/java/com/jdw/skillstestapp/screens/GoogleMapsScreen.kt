package com.jdw.skillstestapp.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.clustering.Clustering
import com.google.maps.android.compose.rememberCameraPositionState
import com.jdw.skillstestapp.components.LineWithSpacer
import com.jdw.skillstestapp.data.model.UserImg
import com.jdw.skillstestapp.map.UserImgClusterItem
import com.jdw.skillstestapp.screens.viewmodel.GoogleMapScreenViewModel


@Composable
fun GoogleMapsScreen(
    navController: NavController,
    viewModel: GoogleMapScreenViewModel,
    paddingValues: PaddingValues
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        DisplayGoogleMap(viewModel)
    }
}

@OptIn(MapsComposeExperimentalApi::class)
@Composable
fun DisplayGoogleMap(viewModel: GoogleMapScreenViewModel) {
    val incheonAirport = LatLng(37.461400, 126.452702)
    val imgList = viewModel.userImages.collectAsState().value
    val clusterItems = remember(imgList) { imgList.map { UserImgClusterItem(it) } }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(incheonAirport, 10f)
    }

    var selectedCluster by remember { mutableStateOf(listOf<UserImg>()) }
    var selectedClusterItem by remember { mutableStateOf(UserImg()) }
    var bottomBarState by remember { mutableStateOf(BottomBarState.EmptyState) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            when (bottomBarState) {
                BottomBarState.EmptyState -> Unit

                BottomBarState.ImageListState -> {
                    BottomBarImageListView(selectedCluster) { clickedItem ->
                        selectedClusterItem = clickedItem
                        bottomBarState = BottomBarState.ImageItemState
                    }
                }

                BottomBarState.ImageItemState -> {
                    BottomBarImageContent(
                        clusterItem = selectedClusterItem,
                        onBack = {
                            bottomBarState = if (selectedCluster.size > 1) BottomBarState.ImageListState
                                             else BottomBarState.EmptyState
                        }
                    )
                }
            }
        }) { innerPadding ->
        Surface(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                onMapLoaded = {
                    val latestImage = imgList.firstOrNull() ?: UserImg(
                        imageLat = 37.461400,
                        imageLong = 126.452702
                    )
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(
                        LatLng(
                            latestImage.imageLat ?: 37.461400,
                            latestImage.imageLong ?: 126.452702
                        ), 10f
                    )
                },
                onMapClick = {
                    Log.d("GoogleMapsScreen", "map clicked! $it")
                    bottomBarState = BottomBarState.EmptyState
                }
            ) {
                Clustering(
                    items = clusterItems,
                    onClusterClick = { cluster ->
                        val images = cluster.items
                            .map { it.source }
                            .sortedByDescending { it.imageDateTaken }
                        selectedCluster = images
                        bottomBarState = BottomBarState.ImageListState
                        viewModel.sendMarkerData(images)
                        false
                    },
                    onClusterItemClick = { clusterItem ->
                        val image = clusterItem.source
                        selectedClusterItem = image
                        bottomBarState = BottomBarState.ImageItemState
                        viewModel.sendMarkerData(listOf(image))
                        false
                    },
                    clusterContent = null,
                    clusterItemContent = null,
                )
            }
        }
    }
}

@Composable
fun BottomBarImageContent(clusterItem: UserImg, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(10.dp)
            .fillMaxWidth()
            .heightIn(max = 250.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "back"
                )
            }
            Text(text = clusterItem.imageDisplayName)
        }
        Spacer(modifier = Modifier.padding(5.dp))
        Image(
            painter = rememberAsyncImagePainter(model = clusterItem.imageDataPath),
            contentDescription = clusterItem.imageDisplayName
        )
    }
}

@Composable
fun BottomBarImageListView(selectedCluster: List<UserImg>, listClicked: (UserImg) -> Unit) {
    Surface(
        modifier = Modifier
            .padding(10.dp)
            .fillMaxWidth()
            .heightIn(max = 250.dp)
    ) {
        LazyColumn {
            items(selectedCluster) { userImg ->
                Row(
                    modifier = Modifier.clickable { listClicked(userImg) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        modifier = Modifier
                            .width(200.dp)
                            .height(200.dp),
                        painter = rememberAsyncImagePainter(model = userImg.imageDataPath),
                        contentDescription = userImg.imageDisplayName
                    )
                    Spacer(modifier = Modifier.padding(5.dp))
                    Text(text = userImg.imageDisplayName)
                }
                LineWithSpacer(2.dp)
            }
        }
    }
}

private enum class BottomBarState {
    EmptyState,
    ImageListState,
    ImageItemState,
}
