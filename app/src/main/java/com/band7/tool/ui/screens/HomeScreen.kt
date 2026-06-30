package com.band7.tool.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.band7.tool.ble.Band7BLEService
import com.band7.tool.model.BandDevice
import com.band7.tool.model.ConnectionState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    bleService: Band7BLEService,
    connectionState: ConnectionState,
    scannedDevices: List<BandDevice>,
    deviceInfo: Map<String, String>,
    onNavigate: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var showAuthDialog by remember { mutableStateOf(false) }
    var authKey by remember { mutableStateOf("") }
    var manualAddress by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("\u5c0f\u7c73\u624b\u73af7 \u5de5\u5177\u7bb1", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { ConnectionStatusCard(connectionState, deviceInfo) }
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("\u8fde\u63a5\u7ba1\u7406", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        OutlinedTextField(value = manualAddress, onValueChange = { manualAddress = it },
                            label = { Text("\u84dd\u7259\u5730\u5740") },
                            placeholder = { Text("A4:C1:38:XX:XX:XX") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { bleService.startScan() }, modifier = Modifier.weight(1f)) { Text("\u626b\u63cf\u8bbe\u5907") }
                            if (manualAddress.isNotBlank()) {
                                Button(onClick = { bleService.connectToAddress(manualAddress) }, modifier = Modifier.weight(1f)) { Text("\u8fde\u63a5\u5730\u5740") }
                            }
                        }
                        if (connectionState is ConnectionState.Authenticated || connectionState is ConnectionState.Connected) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { showAuthDialog = true }) { Text("\u8bbe\u7f6e\u5bc6\u94a5") }
                                Button(onClick = { scope.launch { bleService.authenticate() } },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) { Text("\u8ba4\u8bc1") }
                                OutlinedButton(onClick = { bleService.disconnect() },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("\u65ad\u5f00") }
                            }
                        }
                    }
                }
            }
            item { Text("\u529f\u80fd", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            item { FeatureButton("\u8868\u76d8\u58c1\u7eb8","\u4e0a\u4f20\u56fe\u7247\u4f5c\u4e3a\u624b\u73af\u58c1\u7eb8","watchface", onNavigate) }
            item { FeatureButton("\u7535\u5b50\u4e66","\u4e0a\u4f20 TXT \u7535\u5b50\u4e66","ebook", onNavigate) }
            item { FeatureButton("\u6587\u4ef6\u7ba1\u7406","\u6d4f\u89c8\u548c\u5220\u9664\u624b\u73af\u6587\u4ef6","filemanager", onNavigate) }
            item { FeatureButton("\u8ba1\u7b97\u5668","\u6807\u51c6\u8ba1\u7b97\u5668","calculator", onNavigate) }
            item { FeatureButton("\u7f16\u7801\u6a21\u5f0f","\u7f16\u5199\u7b80\u6613\u811a\u672c","script", onNavigate) }
            if (scannedDevices.isNotEmpty()) {
                item { Text("\u53d1\u73b0\u7684\u8bbe\u5907 (" + scannedDevices.size + ")", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                items(scannedDevices) { device -> DeviceItem(device) { bleService.connectToDevice(device) } }
            }
        }
    }

    if (showAuthDialog) {
        AlertDialog(
            onDismissRequest = { showAuthDialog = false },
            title = { Text("\u8bbe\u7f6e\u8ba4\u8bc1\u5bc6\u94a5") },
            text = {
                OutlinedTextField(value = authKey, onValueChange = { authKey = it },
                    label = { Text("32\u4f4d\u5341\u516d\u8fdb\u5236\u5bc6\u94a5") },
                    modifier = Modifier.fillMaxWidth())
            },
            confirmButton = { TextButton(onClick = { bleService.setAuthKey(authKey); showAuthDialog = false }) { Text("\u786e\u5b9a") } },
            dismissButton = { TextButton(onClick = { showAuthDialog = false }) { Text("\u53d6\u6d88") } }
        )
    }
}

@Composable
fun ConnectionStatusCard(connectionState: ConnectionState, deviceInfo: Map<String, String>) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = when (connectionState) {
            is ConnectionState.Authenticated -> Color(0xFFE8F5E9)
            is ConnectionState.Connected -> Color(0xFFE3F2FD)
            is ConnectionState.Error -> Color(0xFFFFEBEE)
            else -> MaterialTheme.colorScheme.surface
        })) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("status: ", fontWeight = FontWeight.Bold)
                Text(when (connectionState) {
                    is ConnectionState.Authenticated -> "\u5df2\u8ba4\u8bc1"
                    is ConnectionState.Connected -> "\u5df2\u8fde\u63a5"
                    is ConnectionState.Scanning -> "\u626b\u63cf\u4e2d..."
                    is ConnectionState.Connecting -> "\u8fde\u63a5\u4e2d..."
                    is ConnectionState.Error -> "\u9519\u8bef"
                    else -> "\u672a\u8fde\u63a5"
                }, fontWeight = FontWeight.Bold)
            }
            if (deviceInfo.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                deviceInfo.forEach { (key, value) -> Text("$key: $value", fontSize = 13.sp, color = Color.Gray) }
            }
            if (connectionState is ConnectionState.Error) {
                Spacer(Modifier.height(4.dp))
                Text(connectionState.message, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun FeatureButton(title: String, desc: String, route: String, onNavigate: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onNavigate(route) },
        shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(desc, fontSize = 13.sp, color = Color.Gray)
            }
            Text(">", color = Color.Gray, fontSize = 20.sp)
        }
    }
}

@Composable
fun DeviceItem(device: BandDevice, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, shape = RoundedCornerShape(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(device.name, fontWeight = FontWeight.Medium)
                Text(device.address, fontSize = 12.sp, color = Color.Gray)
            }
            Text("" + device.rssi + " dBm", fontSize = 12.sp, color = Color.Gray)
        }
    }
}
