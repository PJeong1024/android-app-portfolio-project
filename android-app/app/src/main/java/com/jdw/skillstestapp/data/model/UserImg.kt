package com.jdw.skillstestapp.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem

@Entity(tableName = "user_images")
data class UserImg  (
    @PrimaryKey @ColumnInfo(name = "image_id") var imageID: Int = 0,
    @ColumnInfo(name = "image_data_path") var imageDataPath: String = "",
    @ColumnInfo(name = "image_display_name") var imageDisplayName: String = "",
    @ColumnInfo(name = "image_lat") var imageLat: Double? = 0.0,
    @ColumnInfo(name = "image_long") var imageLong: Double? = 0.0,
    @ColumnInfo(name = "image_date_taken") var imageDateTaken: Long = 0L,
    @ColumnInfo(name = "image_orientation") var imageOri: Int = 0,
    @ColumnInfo(name = "image_size") var imageSize: Long = 0L,
    @ColumnInfo(name = "image_address") var imageAddress: String = ""
): ClusterItem {
    override fun getPosition(): LatLng {
        return LatLng(imageLat ?: 0.0, imageLong ?: 0.0)
    }

    override fun getTitle(): String? {
        return imageDisplayName
    }

    override fun getSnippet(): String? {
        return imageAddress
    }

    override fun getZIndex(): Float? {
        return 0.0f
    }
}