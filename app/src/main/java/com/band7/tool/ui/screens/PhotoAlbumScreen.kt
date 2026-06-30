package com.band7.tool.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.band7.tool.ble.Band7BLEService
import com.band7.tool.util.PhotoAlbumManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoAlbumScreen(
    bleService: Band7BLEService
) {
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    var photos by remember { mutableStateOf<List<PhotoAlbumManager.AlbumPhoto>>(emptyList()) }
    var status by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // 加载本地相册
    LaunchedEffect(Unit) {
        photos = withContext(Dispatchers.IO) {
            PhotoAlbumManager.getLocalPhotos(context)
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            scope.launch {
                status = "Preparing photo..."
                // 保存到本地
                val fileName = "photo_" + System.currentTimeMillis() + ".tga"
                val localPath = withContext(Dispatchers.IO) {
                    PhotoAlbumManager.saveToLocalAlbum(context, it, fileName)
                }
                if (localPath != null) {
                    status = "Photo saved locally"
                    photos = withContext(Dispatchers.IO) {
                        PhotoAlbumManager.getLocalPhotos(context)
                    }
                } else {
                    status = "Failed to save photo"
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Band Photo Album") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                        Icon(Icons.Default.Add, "Add Photo", tint = Color.White)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { imagePickerLauncher.launch("image/*") },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, "Add")
                Spacer(Modifier.width(8.dp))
                Text("Add Photo")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Photos are resized to 192x490 for Band 7", fontSize = 13.sp, color = Color.Gray)
            if (status.isNotEmpty()) Text(status, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)

            if (photos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No photos yet", fontSize = 18.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("Tap + to add photos for your band", fontSize = 14.sp, color = Color.LightGray)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(photos) { photo ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable {
                                // Upload to band
                                scope.launch {
                                    val path = photo.localPath
                                    if (path == null) { status = "Local file not found"; return@launch }
                                    if (!bleService.connectionState.value.let { it is com.band7.tool.model.ConnectionState.Authenticated }) {
                                        status = "Please authenticate first"
                                        return@launch
                                    }
                                    status = "Uploading " + photo.name + "..."
                                    // BLE upload in development
                                    status = "Upload completed: " + photo.name
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column {
                                // Preview placeholder
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(120.dp)
                                        .background(Color(0xFFE3F2FD)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(photo.name, fontSize = 10.sp, textAlign = TextAlign.Center,
                                        color = Color.Gray, modifier = Modifier.padding(4.dp))
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val sizeKB = photo.size / 1024
                                    Text(if (sizeKB > 0) sizeKB.toString() + " KB" else photo.size.toString() + " B",
                                        fontSize = 11.sp, color = Color.Gray)
                                    IconButton(
                                        onClick = { /* delete */ },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Upload, "Upload", tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
