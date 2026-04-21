package com.jdw.skillstestapp.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.media.ExifInterface
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.jdw.skillstestapp.data.ChatMessageDao
import com.jdw.skillstestapp.data.UserImgDao
import com.jdw.skillstestapp.data.model.ChatMessage
import com.jdw.skillstestapp.data.model.UserImg
import com.jdw.skillstestapp.data.model.weather.Weather
import com.jdw.skillstestapp.data.model.weather.WeatherData
import com.jdw.skillstestapp.data.network.WeatherApi
import java.io.File
import java.io.IOException
import javax.inject.Inject

class MyAppRepository @Inject constructor(
    private val contentResolver: ContentResolver,
    private val userImgDao: UserImgDao,
    private val chatMessageDao: ChatMessageDao,
    private val weatherApi: WeatherApi,
) {
    private val TAG = "MyAppRepository"

    //
    // GoogleMap screen methods
    //
    fun getImageListFromStorage(): List<Uri> {
        val imageList = mutableListOf<Uri>()
        val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME)
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        val query = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )

        query?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val contentUri =
                    ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)


                imageList.add(contentUri)
            }
        }
        query?.close()

        Log.d(TAG, "getImageListFromStorage: ${imageList.size}")

        return imageList
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
            projection,
            null,
            null,
            null
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

                if (!displayName.contains(".jpg")) {
                    Log.d(TAG, "not a JPEG image format : $displayName")
                    continue
                }

                val dateTaken = it.getLong(dateTakenColumn)
                val orientation = it.getInt(orientationColumn)
                val size = it.getLong(sizeColumn)

                val exifData = readExifData(imagePath)
                if (exifData == null) {
                    Log.d(TAG, "image doesn't have GPS data : $displayName")
                    continue
                }

                val userImg = UserImg(
                    imageID = imageID,
                    imageDataPath = imagePath,
                    imageDisplayName = displayName,
                    imageLat = exifData.first,
                    imageLong = exifData.second,
                    imageDateTaken = dateTaken,
                    imageOri = orientation,
                    imageSize = size,
                    imageAddress = "" // Optional: Address lookup with geocoding
                )

                // Insert new image into DB
                if (userImgDao.getImage(imageID.toString()) == null) {
                    userImgDao.insertImage(userImg)
                }
            }
        }
    }

    suspend fun getUserImgFromUri(imageUri: Uri) {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.ORIENTATION,
            MediaStore.Images.Media.SIZE
        )
        val cursor = contentResolver.query(imageUri, projection, null, null, null)

        cursor?.use {
            if (it.moveToFirst()) {
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val dataColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                val displayNameColumn =
                    it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dateTakenColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                val orientationColumn =
                    it.getColumnIndexOrThrow(MediaStore.Images.Media.ORIENTATION)
                val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)

                val imageID = it.getInt(idColumn)
                val imagePath = it.getString(dataColumn)
                val displayName = it.getString(displayNameColumn)
                val dateTaken = it.getLong(dateTakenColumn)
                val orientation = it.getInt(orientationColumn)
                val size = it.getLong(sizeColumn)

                val exifData = readExifData(imagePath)
                if (exifData == null) {
                    Log.d(TAG, "image doesn't have GPS data : $displayName")
                    return
                }

                val userImg = UserImg(
                    imageID = imageID,
                    imageDataPath = imagePath,
                    imageDisplayName = displayName,
                    imageLat = exifData.first,
                    imageLong = exifData.second,
                    imageDateTaken = dateTaken,
                    imageOri = orientation,
                    imageSize = size,
                    imageAddress = "" // Optional: Address lookup with geocoding
                )

                // Insert new image into DB
                if (userImgDao.getImage(imageID.toString()) == null) {
                    userImgDao.insertImage(userImg)
                    Log.d(TAG, "Photo send to db: ${userImg.imageDisplayName}")
                }
            }
        }
    }

    fun imageIsExist(filePath: String): Boolean {
        val file = File(filePath)
        return file.exists()
    }

    private fun readExifData(imagePath: String): Pair<Double, Double>? {
        Log.i(TAG, "try to read GPS data from image : $imagePath")
        try {
            val exif = ExifInterface(imagePath)
            val latLong = FloatArray(2)
            return if (exif.getLatLong(latLong)) {
                Pair(latLong[0].toDouble(), latLong[1].toDouble())
            } else null
        } catch (e: IOException) {
            Log.i(TAG, "no GPS values in image : $imagePath")
            return null
        }
    }

    // db call methods
    // from userImgDao
    suspend fun getAllImages(): List<UserImg> {
        return userImgDao.getAllImages()
    }

    suspend fun deleteUserImage(userImg: UserImg) {
        userImgDao.deleteImage(userImg)
    }

    //
    // Gemini AI ChatMessage screen methods
    //
    suspend fun getAllMessage(): List<ChatMessage> {
        return chatMessageDao.getAllMessage()
    }

    suspend fun insertMessage(chatMessage: ChatMessage) {
        chatMessageDao.insertMessage(chatMessage)
    }

    suspend fun deleteMessage(chatMessage: ChatMessage) {
        chatMessageDao.deleteMessage(chatMessage)
    }

    //
    // Open Weather API screen methods
    //
    suspend fun getWeather(lat: Double, lon: Double): WeatherData {
        return weatherApi.getWeather(lat.toString(), lon.toString())
    }
}