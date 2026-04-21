package com.jdw.skillstestapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jdw.skillstestapp.data.model.UserImg

@Dao
interface UserImgDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(userImg: UserImg)

    @Delete
    suspend fun deleteImage(userImg: UserImg)

    @Query("SELECT * FROM user_images")
    suspend fun getAllImages(): List<UserImg>

    @Query("SELECT * FROM user_images WHERE image_id = :imageID LIMIT 1")
    suspend fun getImage(imageID: String): UserImg?

    @Query("DELETE FROM user_images WHERE image_id = :imageID")
    suspend fun deleteByImageID(imageID: Int)
}