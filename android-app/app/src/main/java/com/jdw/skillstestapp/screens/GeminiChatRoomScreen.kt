package com.jdw.skillstestapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.jdw.skillstestapp.components.LineWithSpacer
import com.jdw.skillstestapp.data.model.ChatMessage
import com.jdw.skillstestapp.screens.viewmodel.GeminiChatRoomViewModel
import com.jdw.skillstestapp.utils.convertTimeStampToDate
import com.jdw.skillstestapp.utils.getTimeStamp


@Composable
fun GeminiCharRoomScreen(
    navController: NavController,
    viewModel: GeminiChatRoomViewModel,
    paddingValues: PaddingValues
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        val chatMessage = viewModel.chatMessage.collectAsState()

        ChatScreen(
            viewModel = viewModel,
            messages = chatMessage.value,
            onSendMessage = { newMessage ->
                // Handle sending the new message (e.g., update the state or database)
                viewModel.sendAndReceiveMessage(newMessage)
                // add message from gemini here
            }
        )
    }
}

@Composable
fun ChatScreen(
    viewModel: GeminiChatRoomViewModel,
    messages: List<ChatMessage>,
    onSendMessage: (ChatMessage) -> Unit
) {
    var sendMessage by remember { mutableStateOf("") }
    val messageListState = rememberLazyListState()

    val isLoading by viewModel.isLoading.collectAsState()

    // Whenever a new message is added, scroll to the bottom


    LaunchedEffect(isLoading, messages.size) {
        if (!isLoading) viewModel.getAllMessage()

        val targetIndex = if (messages.isNotEmpty()) messages.size - 1 else 0
        messageListState.animateScrollToItem(targetIndex)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            state = messageListState
        ) {
            items(messages) { message ->
                ChatMessageItem(message)
            }
        }

        LineWithSpacer(2.dp)

        Row(
            modifier = Modifier
                .height(70.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = sendMessage,
                onValueChange = { sendMessage = it },
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(3f)
                    .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                    .padding(5.dp),
                placeholder = { Text("Type a message...") },
                maxLines = 1
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    onSendMessage(
                        ChatMessage(
                            sender = "User",
                            message = sendMessage,
                            timestamp = getTimeStamp()
                        )
                    )
                    sendMessage = ""
                },
                enabled = sendMessage.isNotEmpty() || !isLoading
            ) {
                Text("Send")
            }
        }
    }
}

@Preview
@Composable
fun ChatMessageItem(
    message: ChatMessage = ChatMessage(
        message = "hi. there",
        sender = "Gemini",
        timestamp = 1000000L
    )
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = if (message.sender == "User") Alignment.End else Alignment.Start
    ) {
        // Message bubbles with alignment
        Row (modifier = Modifier,
            verticalAlignment = Alignment.CenterVertically){
            Text(
                text = message.message,
                modifier = Modifier
                    .padding(top = 8.dp, start = 8.dp)
                    .background(
                        if (message.sender == "Gemini") Color.Cyan else Color.LightGray,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp),
                color = Color.Black,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.width(2.dp))
            Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Delete message",
                modifier = Modifier
                    .size(40.dp)
                    .padding(4.dp).clickable {
                        // delete message

                    })
        }
        Text(
            text = convertTimeStampToDate(message.timestamp),
            modifier = Modifier.padding(bottom = 8.dp, start = 8.dp, end = 8.dp),
            color = Color.Black,
            fontSize = 8.sp
        )
    }
}
