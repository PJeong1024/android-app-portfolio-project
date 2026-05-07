package com.jdw.skillstestapp.repository

import android.content.ContentResolver
import android.media.ExifInterface
import android.provider.MediaStore
import android.util.Log
import com.jdw.skillstestapp.data.UserImgDao
import com.jdw.skillstestapp.data.model.UserImg
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleMapsRepository @Inject constructor(
    private val contentResolver: ContentResolver,
    private val userImgDao: UserImgDao,
) {
    companion object {
        private const val TAG = "GoogleMapsRepository"
    }

    suspend fun fetchImages() {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.ORIENTATION,
            MediaStore.Images.Media.SIZE
        )

        val cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection, null, null, null
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val displayNameColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateTakenColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val orientationColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.ORIENTATION)
            val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)

            while (it.moveToNext()) {
                val imageID = it.getInt(idColumn)
                val imagePath = it.getString(dataColumn)
                val displayName = it.getString(displayNameColumn)

                val lowerName = displayName.lowercase()
                if (!lowerName.endsWith(".jpg") && !lowerName.endsWith(".jpeg")) {
                    Log.d(TAG, "not a JPEG image format : $displayName")
                    continue
                }

                val exifData = readExifData(imagePath)
                if (exifData == null) {
                    Log.d(TAG, "image doesn't have GPS data : $displayName")
                    continue
                }

                if (userImgDao.getImage(imageID) == null) {
                    userImgDao.insertImage(
                        UserImg(
                            imageID = imageID,
                            imageDataPath = imagePath,
                            imageDisplayName = displayName,
                            imageLat = exifData.first,
                            imageLong = exifData.second,
                            imageDateTaken = it.getLong(dateTakenColumn),
                            imageOri = it.getInt(orientationColumn),
                            imageSize = it.getLong(sizeColumn),
                            imageAddress = ""
                        )
                    )
                }
            }
        }
    }

    suspend fun syncAndGetImages(): List<UserImg> {
        val images = userImgDao.getAllImages()
        val (existing, deleted) = images.partition { File(it.imageDataPath).exists() }
        deleted.forEach { userImgDao.deleteImage(it) }
        return existing.sortedByDescending { it.imageDateTaken }
    }

    private fun readExifData(imagePath: String): Pair<Double, Double>? {
        return try {
            val exif = ExifInterface(imagePath)
            val latLong = FloatArray(2)
            if (exif.getLatLong(latLong)) Pair(latLong[0].toDouble(), latLong[1].toDouble())
            else null
        } catch (e: IOException) {
            Log.i(TAG, "no GPS values in image : $imagePath")
            null
        }
    }
}
