package com.jdw.skillstestapp.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.Review
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.clustering.Clustering
import com.google.maps.android.compose.rememberCameraPositionState
import com.jdw.skillstestapp.components.LineWithSpacer
import com.jdw.skillstestapp.map.PlaceClusterItem
import com.jdw.skillstestapp.screens.viewmodel.FoodSearchViewModel

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun FoodSearchScreen(
    navController: NavController,
    viewModel: FoodSearchViewModel,
    paddingValues: PaddingValues
) {
    val locationPermission = rememberPermissionState(android.Manifest.permission.ACCESS_FINE_LOCATION)

    if (!locationPermission.status.isGranted) {
        LaunchedEffect(Unit) { locationPermission.launchPermissionRequest() }
        FoodPermissionRequestView { locationPermission.launchPermissionRequest() }
    } else {
        LaunchedEffect(Unit) { viewModel.searchNearby() }
        FoodSearchMapContent(viewModel = viewModel, paddingValues = paddingValues)
    }
}

@OptIn(MapsComposeExperimentalApi::class)
@Composable
private fun FoodSearchMapContent(
    viewModel: FoodSearchViewModel,
    paddingValues: PaddingValues
) {
    val places by viewModel.places.collectAsState()
    val myLocation by viewModel.myLocation.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val placePhotoThumbnails by viewModel.placePhotoThumbnails.collectAsState()
    val selectedPlacePhotos by viewModel.selectedPlacePhotos.collectAsState()

    val clusterItems = remember(places) {
        places.filter { it.latLng != null }.map { PlaceClusterItem(it) }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(37.5665, 126.9780), 13f)
    }

    LaunchedEffect(myLocation) {
        myLocation?.let {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(it, 14f))
        }
    }

    var bottomBarState by remember { mutableStateOf(FoodBottomBarState.EmptyState) }
    var selectedCluster by remember { mutableStateOf(listOf<Place>()) }
    var selectedPlace by remember { mutableStateOf<Place?>(null) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.searchNearby() }) {
                Icon(Icons.Filled.Refresh, contentDescription = "refresh")
            }
        },
        bottomBar = {
            when (bottomBarState) {
                FoodBottomBarState.EmptyState -> Unit
                FoodBottomBarState.PlaceListState -> {
                    FoodBottomBarPlaceList(
                        places = selectedCluster,
                        placePhotoThumbnails = placePhotoThumbnails
                    ) { place ->
                        selectedPlace = place
                        viewModel.loadPhotosForPlace(place)
                        bottomBarState = FoodBottomBarState.PlaceDetailState
                    }
                }
                FoodBottomBarState.PlaceDetailState -> {
                    selectedPlace?.let { place ->
                        FoodBottomBarPlaceDetail(
                            place = place,
                            photos = selectedPlacePhotos,
                            hasPhotoMeta = place.photoMetadatas?.isNotEmpty() == true,
                            onBack = {
                                bottomBarState = if (selectedCluster.size > 1)
                                    FoodBottomBarState.PlaceListState
                                else
                                    FoodBottomBarState.EmptyState
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = true),
                onMapClick = { bottomBarState = FoodBottomBarState.EmptyState }
            ) {
                Clustering(
                    items = clusterItems,
                    onClusterClick = { cluster ->
                        selectedCluster = cluster.items.map { it.source }
                        bottomBarState = FoodBottomBarState.PlaceListState
                        false
                    },
                    onClusterItemClick = { item ->
                        selectedPlace = item.source
                        selectedCluster = listOf(item.source)
                        viewModel.loadPhotosForPlace(item.source)
                        bottomBarState = FoodBottomBarState.PlaceDetailState
                        false
                    },
                    clusterContent = null,
                    clusterItemContent = { item ->
                        PlaceMarkerContent(bitmap = item.source.id?.let { placePhotoThumbnails[it] })
                    }
                )
            }

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            error?.let { errorMsg ->
                Card(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = errorMsg,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

// 개별 마커: 이미지 로딩 완료 시 이미지 표시, 미완료 시 Restaurant 아이콘
@Composable
private fun PlaceMarkerContent(bitmap: Bitmap?) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Restaurant,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// 가게 상세: 기본 정보 + 영업시간 + 가격대 + 사진 그리드 + 리뷰
@Composable
private fun FoodBottomBarPlaceDetail(
    place: Place,
    photos: List<Bitmap>,
    hasPhotoMeta: Boolean,
    onBack: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 520.dp),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // 헤더: 뒤로가기 + 가게명 + 영업 상태 chip
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "back")
                }
                Text(
                    text = place.name ?: "이름 없음",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                BusinessStatusChip(place = place)
            }

            // 평점 + 가격대
            Row(verticalAlignment = Alignment.CenterVertically) {
                place.rating?.let { rating ->
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = String.format("%.1f", rating),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                place.priceLevel?.toPriceSymbol()?.let { symbol ->
                    Text(
                        text = "  ·  $symbol",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 주소
            place.address?.let { address ->
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 영업시간
            val weekdayText = place.openingHours?.weekdayText
            if (!weekdayText.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "영업시간",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                weekdayText.forEach { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, top = 1.dp)
                    )
                }
            }

            // 사진 그리드
            if (photos.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                PhotoGrid(photos = photos)
            } else if (hasPhotoMeta) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }

            // 리뷰
            val reviews = place.reviews
            if (!reviews.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "리뷰",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                reviews.forEach { review ->
                    ReviewItem(review = review)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun BusinessStatusChip(place: Place) {
    val chipInfo: Pair<String, Color>? = when {
        place.businessStatus == Place.BusinessStatus.CLOSED_PERMANENTLY ->
            "폐업" to MaterialTheme.colorScheme.error
        place.businessStatus == Place.BusinessStatus.CLOSED_TEMPORARILY ->
            "임시 휴업" to Color(0xFFE65100)
        @Suppress("DEPRECATION") place.isOpen() == false ->
            "영업 종료" to MaterialTheme.colorScheme.outline
        else -> null
    }
    chipInfo?.let { (text, color) ->
        Box(
            modifier = Modifier
                .background(color.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = text,
                color = color,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ReviewItem(review: Review) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val starCount = review.rating?.toInt() ?: 0
            repeat(starCount) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    modifier = Modifier.size(11.dp),
                    tint = Color(0xFFFFB300)
                )
            }
            repeat(5 - starCount) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    modifier = Modifier.size(11.dp),
                    tint = MaterialTheme.colorScheme.outlineVariant
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = review.authorAttribution.name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
            review.relativePublishTimeDescription?.let { time ->
                Text(
                    text = " · $time",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        review.text?.let { text ->
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun Int.toPriceSymbol(): String = when (this) {
    1 -> "₩"
    2 -> "₩₩"
    3 -> "₩₩₩"
    4 -> "₩₩₩₩"
    else -> ""
}

// 3열 이미지 그리드 (LazyLayout 미사용 — verticalScroll과 중첩 불가)
@Composable
private fun PhotoGrid(photos: List<Bitmap>) {
    val columns = 3
    val rows = (photos.size + columns - 1) / columns
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        for (row in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                for (col in 0 until columns) {
                    val index = row * columns + col
                    if (index < photos.size) {
                        Image(
                            bitmap = photos[index].asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// 클러스터 리스트: 썸네일 + 가게 정보
@Composable
private fun FoodBottomBarPlaceList(
    places: List<Place>,
    placePhotoThumbnails: Map<String, Bitmap>,
    onPlaceClick: (Place) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 300.dp),
        shadowElevation = 8.dp
    ) {
        LazyColumn(contentPadding = PaddingValues(16.dp)) {
            items(places, key = { it.id ?: it.hashCode().toString() }) { place ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPlaceClick(place) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 썸네일
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        val bitmap = place.id?.let { placePhotoThumbnails[it] }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                Icons.Filled.Restaurant,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    // 텍스트
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = place.name ?: "이름 없음",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        place.address?.let { address ->
                            Text(
                                text = address,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                    place.rating?.let { rating ->
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = String.format("%.1f", rating),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                LineWithSpacer(lineHeight = 1.dp)
            }
        }
    }
}

@Composable
private fun FoodPermissionRequestView(onRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.LocationOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "주변 음식점 검색을 위해 위치 권한이 필요합니다",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onRequest) {
            Text("위치 권한 허용")
        }
    }
}

private enum class FoodBottomBarState {
    EmptyState,
    PlaceListState,
    PlaceDetailState,
}
