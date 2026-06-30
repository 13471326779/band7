package com.band7.tool.ui.screens

import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.band7.tool.ble.Band7BLEService
import com.band7.tool.model.BandFile
import com.band7.tool.model.ConnectionState

@Composable
fun AppNavigation(
    bleService: Band7BLEService,
    connectionState: ConnectionState,
    scannedDevices: List<com.band7.tool.model.BandDevice>,
    deviceInfo: Map<String, String>,
    fileList: List<BandFile>
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                bleService = bleService,
                connectionState = connectionState,
                scannedDevices = scannedDevices,
                deviceInfo = deviceInfo,
                onNavigate = { route -> navController.navigate(route) }
            )
        }
        composable("watchface") {
            WatchfaceScreen(bleService = bleService)
        }
        composable("photo_album") {
            PhotoAlbumScreen(bleService = bleService)
        }
        composable("ebook") {
            EBookScreen(bleService = bleService)
        }
        composable("filemanager") {
            FileManagerScreen(bleService = bleService, fileList = fileList)
        }
        composable("calculator") {
            CalculatorScreen()
        }
        composable("script") {
            ScriptScreen()
        }
    }
}
