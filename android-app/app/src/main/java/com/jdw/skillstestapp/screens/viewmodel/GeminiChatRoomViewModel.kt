package com.jdw.skillstestapp.screens.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.jdw.skillstestapp.data.model.ChatMessage
import com.jdw.skillstestapp.repository.MyAppRepository
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
    private val appRepository: MyAppRepository,
    private val generativeModel: GenerativeModel,
) : ViewModel() {
    private val _chatMessage: MutableStateFlow<List<ChatMessage>> = MutableStateFlow(emptyList())
    val chatMessage: StateFlow<List<ChatMessage>> = _chatMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        getAllMessage()

        if (_chatMessage.value.isEmpty()) {
            insertMessage(
                ChatMessage(
                    sender = "Gemini",
                    message = "Hi. Ask any question you have.",
                    timestamp = getTimeStamp()
                )
            )
        }
    }

    // for chatMessage

    fun getAllMessage() {
        viewModelScope.launch(Dispatchers.IO) {
            _chatMessage.value = appRepository.getAllMessage()
        }
    }

    fun sendAndReceiveMessage(chatMessage: ChatMessage) {
        viewModelScope.launch(Dispatchers.IO) {
            appRepository.insertMessage(chatMessage)
            _chatMessage.value = appRepository.getAllMessage()
            _isLoading.value = true
            appRepository.insertMessage(
                ChatMessage(
                    sender = "Gemini",
                    message = generativeModel.generateContent(chatMessage.message).text
                        ?: "I missed your question. Please ask same question again",
                    timestamp = getTimeStamp()
                )
            )
            _chatMessage.value = appRepository.getAllMessage()
            _isLoading.value = false
        }
    }

    fun insertMessage(chatMessage: ChatMessage) {
        viewModelScope.launch(Dispatchers.IO) {
            appRepository.insertMessage(chatMessage)
        }
    }

    fun deleteMessage(chatMessage: ChatMessage) {
        viewModelScope.launch(Dispatchers.IO) {
            appRepository.deleteMessage(chatMessage)
        }
    }
}