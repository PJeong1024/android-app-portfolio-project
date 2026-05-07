package com.jdw.skillstestapp.screens.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.jdw.skillstestapp.data.model.ChatMessage
import com.jdw.skillstestapp.repository.ChatRepository
import com.jdw.skillstestapp.utils.getTimeStamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GeminiChatRoomViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val generativeModel: GenerativeModel,
) : ViewModel() {
    private val _chatMessage: MutableStateFlow<List<ChatMessage>> = MutableStateFlow(emptyList())
    val chatMessage: StateFlow<List<ChatMessage>> = _chatMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val messages = chatRepository.getAllMessage()
            if (messages.isEmpty()) {
                chatRepository.insertMessage(
                    ChatMessage(
                        sender = "Gemini",
                        message = "Hi. Ask any question you have.",
                        timestamp = getTimeStamp()
                    )
                )
                _chatMessage.value = chatRepository.getAllMessage()
            } else {
                _chatMessage.value = messages
            }
        }
    }

    fun getAllMessage() {
        viewModelScope.launch(Dispatchers.IO) {
            _chatMessage.value = chatRepository.getAllMessage()
        }
    }

    fun sendAndReceiveMessage(chatMessage: ChatMessage) {
        viewModelScope.launch(Dispatchers.IO) {
            chatRepository.insertMessage(chatMessage)
            _chatMessage.value = chatRepository.getAllMessage()
            _isLoading.value = true
            chatRepository.insertMessage(
                ChatMessage(
                    sender = "Gemini",
                    message = generativeModel.generateContent(chatMessage.message).text
                        ?: "I missed your question. Please ask same question again",
                    timestamp = getTimeStamp()
                )
            )
            _chatMessage.value = chatRepository.getAllMessage()
            _isLoading.value = false
        }
    }

    fun deleteMessage(chatMessage: ChatMessage) {
        viewModelScope.launch(Dispatchers.IO) {
            chatRepository.deleteMessage(chatMessage)
        }
    }
}
