package com.jdw.skillstestapp.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_images")
data class UserImg(
    @PrimaryKey @ColumnInfo(name = "image_id") val imageID: Int = 0,
    @ColumnInfo(name = "image_data_path") val imageDataPath: String = "",
    @ColumnInfo(name = "image_display_name") val imageDisplayName: String = "",
    @ColumnInfo(name = "image_lat") val imageLat: Double? = null,
    @ColumnInfo(name = "image_long") val imageLong: Double? = null,
    @ColumnInfo(name = "image_date_taken") val imageDateTaken: Long = 0L,
    @ColumnInfo(name = "image_orientation") val imageOri: Int = 0,
    @ColumnInfo(name = "image_size") val imageSize: Long = 0L,
    @ColumnInfo(name = "image_address") val imageAddress: String = ""
)
