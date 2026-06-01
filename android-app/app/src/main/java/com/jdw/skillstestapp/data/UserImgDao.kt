package com.jdw.skillstestapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jdw.skillstestapp.data.model.UserImg
import kotlinx.coroutines.flow.Flow

@Dao
interface UserImgDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(userImg: UserImg)

    @Delete
    suspend fun deleteImage(userImg: UserImg)

    @Query("SELECT * FROM user_images ORDER BY image_date_taken DESC")
    fun getAllImages(): Flow<List<UserImg>>

    @Query("SELECT image_id FROM user_images")
    suspend fun getAllImageIds(): List<Int>
}
