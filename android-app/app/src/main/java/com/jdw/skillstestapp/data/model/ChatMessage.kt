package com.jdw.skillstestapp.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gemini_chat_message")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Int = 0,
    @ColumnInfo(name = "sender")
    var sender: String = "User",
    @ColumnInfo(name = "message")
    var message: String = "",
    @ColumnInfo(name = "timestamp")
    var timestamp: Long = 0L
)
