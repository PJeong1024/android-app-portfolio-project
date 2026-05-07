package com.jdw.skillstestapp.map

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem
import com.jdw.skillstestapp.data.model.UserImg

class UserImgClusterItem(val source: UserImg) : ClusterItem {
    override fun getPosition(): LatLng =
        LatLng(source.imageLat ?: 0.0, source.imageLong ?: 0.0)

    override fun getTitle(): String = source.imageDisplayName

    override fun getSnippet(): String = source.imageAddress

    override fun getZIndex(): Float = 0f
}
