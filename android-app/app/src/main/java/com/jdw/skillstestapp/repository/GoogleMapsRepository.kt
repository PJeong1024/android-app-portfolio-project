package com.jdw.skillstestapp.repository

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.provider.MediaStore
import android.util.Log
import com.jdw.skillstestapp.data.UserImgDao
import com.jdw.skillstestapp.data.model.UserImg
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.io.ByteArrayOutputStream
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

    fun getAllImages(): Flow<List<UserImg>> = userImgDao.getAllImages()

    suspend fun syncDeletedImages() {
        val images = userImgDao.getAllImages().first()
        val deleted = images.filter { !File(it.imageDataPath).exists() }
        deleted.forEach { userImgDao.deleteImage(it) }
    }

    suspend fun fetchImages() {
        val existingIds = userImgDao.getAllImageIds().toSet()

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
                if (imageID in existingIds) continue

                val displayName = it.getString(displayNameColumn)
                val lowerName = displayName.lowercase()
                if (!lowerName.endsWith(".jpg") && !lowerName.endsWith(".jpeg")) {
                    Log.d(TAG, "not a JPEG image format : $displayName")
                    continue
                }

                val imagePath = it.getString(dataColumn)
                val exifData = readExifData(imagePath)
                if (exifData == null) {
                    Log.d(TAG, "image doesn't have GPS data : $displayName")
                    continue
                }

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

    /**
     * Loads image at [imagePath], scales down so the longest side ≤ 512px, returns JPEG bytes.
     * Called on IO dispatcher by the ViewModel.
     */
    fun loadThumbnailBytes(imagePath: String): ByteArray? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(imagePath, bounds)
        val maxDim = maxOf(bounds.outWidth, bounds.outHeight)
        var sampleSize = 1
        while (maxDim / sampleSize > 512) sampleSize *= 2

        val bitmap = BitmapFactory.decodeFile(imagePath, BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }) ?: return null

        ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 75, out)
            bitmap.recycle()
            out.toByteArray()
        }
    }.getOrElse {
        Log.e(TAG, "loadThumbnailBytes failed ($imagePath): ${it.message}")
        null
    }

    /**
     * Loads full-size image at [imagePath] and returns JPEG bytes.
     * Called on IO dispatcher by the ViewModel.
     */
    fun loadRawImageBytes(imagePath: String): ByteArray? = runCatching {
        val bitmap = BitmapFactory.decodeFile(imagePath) ?: return null
        ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            bitmap.recycle()
            out.toByteArray()
        }
    }.getOrElse {
        Log.e(TAG, "loadRawImageBytes failed ($imagePath): ${it.message}")
        null
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
