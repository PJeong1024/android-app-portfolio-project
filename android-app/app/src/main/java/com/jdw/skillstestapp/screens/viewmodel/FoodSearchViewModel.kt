package com.jdw.skillstestapp.screens.viewmodel

import android.annotation.SuppressLint
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import com.jdw.skillstestapp.repository.PlacesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

@HiltViewModel
class FoodSearchViewModel @Inject constructor(
    private val placesRepository: PlacesRepository,
    private val fusedLocationClient: FusedLocationProviderClient
) : ViewModel() {

    private val _places = MutableStateFlow<List<Place>>(emptyList())
    val places: StateFlow<List<Place>> = _places.asStateFlow()

    private val _myLocation = MutableStateFlow<LatLng?>(null)
    val myLocation: StateFlow<LatLng?> = _myLocation.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // placeId → thumbnail Bitmap (마커 + 클러스터 리스트용)
    private val _placePhotoThumbnails = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    val placePhotoThumbnails: StateFlow<Map<String, Bitmap>> = _placePhotoThumbnails.asStateFlow()

    // 선택된 가게의 전체 사진 (상세 그리드용)
    private val _selectedPlacePhotos = MutableStateFlow<List<Bitmap>>(emptyList())
    val selectedPlacePhotos: StateFlow<List<Bitmap>> = _selectedPlacePhotos.asStateFlow()

    @SuppressLint("MissingPermission")
    fun searchNearby() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _placePhotoThumbnails.value = emptyMap()
            _selectedPlacePhotos.value = emptyList()
            try {
                val location = getCurrentLocation()
                if (location == null) {
                    _error.value = "현재 위치를 가져올 수 없습니다. GPS를 확인해주세요."
                    return@launch
                }
                _myLocation.value = location
                placesRepository.searchNearbyRestaurants(location)
                    .onSuccess { places ->
                        _places.value = places
                        loadThumbnailsAsync(places)
                    }
                    .onFailure { _error.value = it.message ?: "검색 실패" }
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 검색 완료 후 마커/리스트용 썸네일 병렬 로딩
    private fun loadThumbnailsAsync(places: List<Place>) {
        places.forEach { place ->
            val meta = place.photoMetadatas?.firstOrNull() ?: return@forEach
            val id = place.id ?: return@forEach
            viewModelScope.launch {
                placesRepository.fetchPlacePhoto(meta, maxWidth = 200, maxHeight = 200)
                    .onSuccess { bitmap ->
                        _placePhotoThumbnails.update { current -> current + (id to bitmap) }
                    }
            }
        }
    }

    // 가게 상세 진입 시 전체 사진 병렬 로딩
    fun loadPhotosForPlace(place: Place) {
        _selectedPlacePhotos.value = emptyList()
        val metas = place.photoMetadatas ?: return
        metas.forEach { meta ->
            viewModelScope.launch {
                placesRepository.fetchPlacePhoto(meta, maxWidth = 600, maxHeight = 600)
                    .onSuccess { bitmap ->
                        _selectedPlacePhotos.update { current -> current + bitmap }
                    }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getCurrentLocation(): LatLng? = suspendCancellableCoroutine { cont ->
        fusedLocationClient.lastLocation
            .addOnSuccessListener { loc ->
                cont.resume(loc?.let { LatLng(it.latitude, it.longitude) })
            }
            .addOnFailureListener { cont.resume(null) }
    }
}
