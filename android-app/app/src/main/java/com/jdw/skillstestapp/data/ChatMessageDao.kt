package com.jdw.skillstestapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jdw.skillstestapp.data.model.ChatMessage
import com.jdw.skillstestapp.data.model.UserImg

@Dao
interface ChatMessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(chatMessage: ChatMessage)

    @Delete
    suspend fun deleteMessage(chatMessage: ChatMessage)

    @Query("SELECT * FROM gemini_chat_message")
    suspend fun getAllMessage(): List<ChatMessage>

    @Query("SELECT * FROM gemini_chat_message WHERE id = :messageID LIMIT 1")
    suspend fun getMessage(messageID: Int): ChatMessage?

    @Query("DELETE FROM gemini_chat_message WHERE id = :messageID")
    suspend fun deleteByID(messageID: Int)
}