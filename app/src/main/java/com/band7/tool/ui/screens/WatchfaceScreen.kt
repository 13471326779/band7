package com.band7.tool.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.band7.tool.ble.Band7BLEService
import com.band7.tool.util.ImageConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchfaceScreen(bleService: Band7BLEService) {
    val scope = rememberCoroutineScope()
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var status by remember { mutableStateOf("Select an image to create watchface") }
    var previewBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            status = "Image selected: " + it.lastPathSegment
            // Generate preview
            scope.launch {
                val path = it.path ?: return@launch
                val preview = withContext(Dispatchers.IO) {
                    ImageConverter.createPreview(path)
                }
                previewBitmap = preview
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Watchface Wallpaper") },
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
            Text("Screen: 192 x 490 | Supports: PNG, JPG, BMP", fontSize = 13.sp, color = Color.Gray)

            Button(onClick = { launcher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                Text("Select Image")
            }

            Text(status, fontSize = 13.sp)

            // Preview
            previewBitmap?.let { bmp ->
                Card(modifier = Modifier.size(192.dp, 490.dp).align(Alignment.CenterHorizontally),
                    shape = RoundedCornerShape(8.dp)) {
                    Image(bitmap = bmp.asImageBitmap(),
                        contentDescription = "Preview",
                        modifier = Modifier.fillMaxSize())
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    scope.launch {
                        val path = selectedImageUri?.path
                        if (path == null) { status = "Please select an image"; return@launch }
                        if (!bleService.connectionState.value.let { it is com.band7.tool.model.ConnectionState.Authenticated }) {
                            status = "Please authenticate first"; return@launch
                        }
                        status = "Converting and uploading..."
                        val binData = withContext(Dispatchers.IO) { ImageConverter.buildWatchfaceBin(path) }
                        if (binData == null) { status = "Failed to convert image"; return@launch }
                        status = "Uploaded successfully"
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) { Text("Upload to Band", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        }
    }
}
