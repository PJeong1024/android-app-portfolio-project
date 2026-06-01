package com.jdw.skillstestapp.map

import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import com.google.maps.android.clustering.ClusterItem

class PlaceClusterItem(val source: Place) : ClusterItem {
    override fun getPosition(): LatLng = source.latLng ?: LatLng(0.0, 0.0)
    override fun getTitle(): String? = source.name
    override fun getSnippet(): String? = source.address
    override fun getZIndex(): Float = 0f
}
