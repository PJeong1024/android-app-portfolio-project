package com.jdw.skillstestapp.repository

import com.jdw.skillstestapp.data.ChatMessageDao
import com.jdw.skillstestapp.data.model.ChatMessage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val chatMessageDao: ChatMessageDao
) {
    suspend fun getAllMessage(): List<ChatMessage> = chatMessageDao.getAllMessage()
    suspend fun insertMessage(chatMessage: ChatMessage) = chatMessageDao.insertMessage(chatMessage)
    suspend fun deleteMessage(chatMessage: ChatMessage) = chatMessageDao.deleteMessage(chatMessage)
}
