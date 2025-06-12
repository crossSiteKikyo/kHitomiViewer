package com.example.khitomiviewer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.khitomiviewer.screen.DatabaseExportImportSettingScreen
import com.example.khitomiviewer.screen.FirstScreen
import com.example.khitomiviewer.screen.LikeSettingScreen
import com.example.khitomiviewer.screen.ListScreen
import com.example.khitomiviewer.screen.MainSettingScreen
import com.example.khitomiviewer.screen.MangaViewScreen
import com.example.khitomiviewer.ui.theme.KHitomiViewerTheme
import com.example.khitomiviewer.viewmodel.KHitomiViewerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefs = getSharedPreferences("MySettings", MODE_PRIVATE)
        val hasLaunched = prefs.getBoolean("hasLaunched", false)
        setContent {
            KHitomiViewerTheme {
                if(hasLaunched) {
                    val mainViewModel: KHitomiViewerViewModel = viewModel()
                    MyNav(mainViewModel)
                }
                else {
                    FirstScreen()
                }
            }
        }
    }
}

@Composable
fun MyNav(mainViewModel: KHitomiViewerViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "ListScreen/1//") {
        composable("ListScreen/{page}/{tagId}/{titleKeyword}") { bse ->
            ListScreen(navController, mainViewModel, bse.arguments?.getString("page"), bse.arguments?.getString("tagId"), bse.arguments?.getString("titleKeyword"))
        }
        composable("MangaViewScreen/{gidStr}") { bse ->
            MangaViewScreen(navController, mainViewModel, bse.arguments?.getString("gidStr"))
        }
        composable("MainSettingScreen") { MainSettingScreen(navController, mainViewModel) }
        composable("LikeSettingScreen/{tagOrGallery}") { bse ->
            LikeSettingScreen(navController, mainViewModel, bse.arguments?.getString("tagOrGallery"))
        }
        composable("DatabaseExportImportSettingScreen") { DatabaseExportImportSettingScreen(mainViewModel) }
    }
}