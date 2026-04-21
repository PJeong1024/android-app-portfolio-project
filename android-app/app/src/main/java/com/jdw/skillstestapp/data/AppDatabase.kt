package com.jdw.skillstestapp.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.jdw.skillstestapp.data.model.ChatMessage
import com.jdw.skillstestapp.data.model.UserImg

@Database(entities = [UserImg::class, ChatMessage::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userImgDao(): UserImgDao
    abstract fun chatMessageDao(): ChatMessageDao
}