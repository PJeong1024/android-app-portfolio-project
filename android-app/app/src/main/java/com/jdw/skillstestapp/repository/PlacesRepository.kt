package com.jdw.skillstestapp.repository

import android.graphics.Bitmap
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.model.PhotoMetadata
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPhotoRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.net.SearchNearbyRequest
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class PlacesRepository @Inject constructor(
    private val placesClient: PlacesClient
) {
    private val placeFields = listOf(
        Place.Field.ID,
        Place.Field.NAME,
        Place.Field.LAT_LNG,
        Place.Field.RATING,
        Place.Field.ADDRESS,
        Place.Field.TYPES,
        Place.Field.PHOTO_METADATAS,
        Place.Field.BUSINESS_STATUS,
        Place.Field.CURRENT_OPENING_HOURS,
        Place.Field.OPENING_HOURS,
        Place.Field.PRICE_LEVEL,
        Place.Field.REVIEWS,
    )

    suspend fun searchNearbyRestaurants(
        location: LatLng,
        radiusMeters: Double = 1000.0,
        maxCount: Int = 20
    ): Result<List<Place>> = suspendCancellableCoroutine { cont ->
        val bounds = CircularBounds.newInstance(location, radiusMeters)
        val request = SearchNearbyRequest.builder(bounds, placeFields)
            .setIncludedTypes(listOf("restaurant"))
            .setMaxResultCount(maxCount)
            .build()

        placesClient.searchNearby(request)
            .addOnSuccessListener { response ->
                if (cont.isActive) cont.resume(Result.success(response.places))
            }
            .addOnFailureListener { e ->
                if (cont.isActive) cont.resume(Result.failure(e))
            }
    }

    suspend fun fetchPlacePhoto(
        photoMetadata: PhotoMetadata,
        maxWidth: Int = 400,
        maxHeight: Int = 400
    ): Result<Bitmap> = suspendCancellableCoroutine { cont ->
        val request = FetchPhotoRequest.builder(photoMetadata)
            .setMaxWidth(maxWidth)
            .setMaxHeight(maxHeight)
            .build()

        placesClient.fetchPhoto(request)
            .addOnSuccessListener { response ->
                if (cont.isActive) cont.resume(Result.success(response.bitmap))
            }
            .addOnFailureListener { e ->
                if (cont.isActive) cont.resume(Result.failure(e))
            }
    }
}
