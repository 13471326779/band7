package com.band7.tool.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.band7.tool.ble.Band7BLEService
import com.band7.tool.util.EBookConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EBookScreen(bleService: Band7BLEService) {
    val scope = rememberCoroutineScope()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var status by remember { mutableStateOf("Select a TXT file to upload") }
    var previewText by remember { mutableStateOf("") }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedUri = it
            status = "File selected"
            scope.launch {
                val input: InputStream? = try {
                    withContext(Dispatchers.IO) {
                        com.band7.tool.util.EBookConverter::class.java
                    }
                    null
                } catch (e: Exception) { null }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("E-Book Upload") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Upload TXT e-books to your Xiaomi Smart Band 7", fontSize = 13.sp, color = Color.Gray)
            Button(onClick = { launcher.launch("text/plain") }, modifier = Modifier.fillMaxWidth()) {
                Text("Select TXT File")
            }
            Text(status, fontSize = 13.sp)
            if (previewText.isNotBlank()) {
                Card(modifier = Modifier.fillMaxWidth().height(200.dp), shape = RoundedCornerShape(8.dp)) {
                    Text(previewText, modifier = Modifier.padding(12.dp), fontSize = 12.sp)
                }
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    scope.launch {
                        status = "Uploading... (BLE transfer feature in development)"
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) { Text("Upload to Band", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        }
    }
}
