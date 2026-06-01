package com.jdw.skillstestapp.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.jdw.skillstestapp.data.transport.TransportState
import com.jdw.skillstestapp.screens.viewmodel.ConnectionSettingsViewModel

@Composable
fun ConnectionSettingsScreen(
    navController: NavController,
    viewModel: ConnectionSettingsViewModel,
    paddingValues: PaddingValues
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TcpConnectionCard(viewModel)
            UsbConnectionCard(viewModel)
        }
    }
}

@Composable
private fun TcpConnectionCard(viewModel: ConnectionSettingsViewModel) {
    val tcpState by viewModel.tcpState.collectAsState()
    val tcpIp by viewModel.tcpIp.collectAsState()
    val tcpPort by viewModel.tcpPort.collectAsState()
    val isConnected = tcpState is TransportState.Connected
    val isConnecting = tcpState is TransportState.Connecting

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("TCP / Wi-Fi", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            TransportStatusRow(tcpState)
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = tcpIp,
                onValueChange = viewModel::onTcpIpChanged,
                label = { Text("IP Address") },
                placeholder = { Text("예: 192.168.0.10") },
                singleLine = true,
                enabled = !isConnected && !isConnecting,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = tcpPort,
                onValueChange = viewModel::onTcpPortChanged,
                label = { Text("Port") },
                placeholder = { Text("예: 8080") },
                singleLine = true,
                enabled = !isConnected && !isConnecting,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (isConnected) {
                OutlinedButton(
                    onClick = viewModel::disconnectTcp,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                ) {
                    Text("연결 해제")
                }
            } else {
                Button(
                    onClick = viewModel::connectTcp,
                    enabled = viewModel.isTcpConnectEnabled,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isConnecting) "연결 중..." else "연결")
                }
            }
        }
    }
}

@Composable
private fun UsbConnectionCard(viewModel: ConnectionSettingsViewModel) {
    val usbState by viewModel.usbState.collectAsState()
    val isConnected = usbState is TransportState.Connected
    val isConnecting = usbState is TransportState.Connecting

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("USB", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            TransportStatusRow(usbState)
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isConnecting) "USB 디바이스 연결을 기다리는 중..."
                       else "USB 케이블 연결 시 자동으로 감지됩니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (isConnected || isConnecting) {
                OutlinedButton(
                    onClick = viewModel::disconnectUsb,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                ) {
                    Text("감지 중지")
                }
            } else {
                Button(
                    onClick = viewModel::connectUsb,
                    enabled = viewModel.isUsbConnectEnabled,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("감지 시작")
                }
            }
        }
    }
}

@Composable
private fun TransportStatusRow(state: TransportState) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(10.dp),
            shape = CircleShape,
            color = state.indicatorColor()
        ) {}
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = state.label(),
            style = MaterialTheme.typography.bodyMedium,
            color = state.indicatorColor()
        )
    }
}

private fun TransportState.indicatorColor(): Color = when (this) {
    TransportState.Connected -> Color(0xFF2E7D32)
    TransportState.Connecting -> Color(0xFFF9A825)
    TransportState.Disconnected -> Color.Gray
    is TransportState.Error -> Color(0xFFC62828)
}

private fun TransportState.label(): String = when (this) {
    TransportState.Connected -> "연결됨"
    TransportState.Connecting -> "연결 중..."
    TransportState.Disconnected -> "연결 끊김"
    is TransportState.Error -> "오류: $message"
}
