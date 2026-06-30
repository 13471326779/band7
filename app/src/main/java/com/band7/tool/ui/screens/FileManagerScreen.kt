package com.band7.tool.ui.screens

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
import com.band7.tool.model.BandFile
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen(
    bleService: Band7BLEService,
    fileList: List<BandFile>
) {
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("") }

    val typeNames = mapOf(1 to "Watchface", 2 to "Firmware", 3 to "Font",
        4 to "Image", 5 to "Music", 6 to "File", 7 to "E-Book")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("File Manager") },
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { scope.launch { bleService.listFiles("/") } }) {
                    Text("Refresh")
                }
                Spacer(Modifier.weight(1f))
                Text(status, fontSize = 13.sp, color = Color.Gray, modifier = Modifier.align(Alignment.CenterVertically))
            }

            if (fileList.isEmpty()) {
                Text("No files found. Tap Refresh to load.", color = Color.Gray, modifier = Modifier.padding(16.dp))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(fileList) { file ->
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(file.name, fontWeight = FontWeight.Medium)
                                    Row {
                                        Text(typeNames.getOrDefault(file.type, "Type " + file.type) + " | ",
                                            fontSize = 12.sp, color = Color.Gray)
                                        val sizeStr = if (file.size < 1024) file.size.toString() + " B"
                                            else if (file.size < 1024*1024) String.format("%.1f KB", file.size / 1024.0)
                                            else String.format("%.1f MB", file.size / (1024.0*1024.0))
                                        Text(sizeStr, fontSize = 12.sp, color = Color.Gray)
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
