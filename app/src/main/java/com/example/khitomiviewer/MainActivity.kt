package com.example.khitomiviewer

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.khitomiviewer.screen.DatabaseExportImportSettingScreen
import com.example.khitomiviewer.screen.GalleryLikeSettingScreen
import com.example.khitomiviewer.screen.ListScreen
import com.example.khitomiviewer.screen.MainSettingScreen
import com.example.khitomiviewer.screen.MangaViewScreen
import com.example.khitomiviewer.screen.TagLikeSettingScreen
import com.example.khitomiviewer.ui.theme.KHitomiViewerTheme
import com.example.khitomiviewer.viewmodel.KHitomiViewerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            enableEdgeToEdge()
            val prefs = getSharedPreferences("MySettings", MODE_PRIVATE)
            var isDark = remember { mutableStateOf(prefs.getBoolean("isDark", false)) }
            KHitomiViewerTheme(
                darkTheme = isDark.value
            ) {
                val mainViewModel: KHitomiViewerViewModel = viewModel()
                MyNav(mainViewModel, isDark)
            }
        }
    }
}

@Composable
fun MyNav(mainViewModel: KHitomiViewerViewModel, isDark: MutableState<Boolean>) {
    val navController = rememberNavController()
    NavHost(
        navController = navController, startDestination = "ListScreen/1//",
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(700))
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(700))
        },
        popEnterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(700))
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(700))
        },
    ) {
        composable("ListScreen/{page}/{tagIdListJson}/{titleKeyword}") { bse ->
            ListScreen(navController, mainViewModel, bse.arguments?.getString("page"), bse.arguments?.getString("tagIdListJson"), bse.arguments?.getString("titleKeyword"))
        }
        composable("MangaViewScreen/{gidStr}") { bse ->
            MangaViewScreen(navController, mainViewModel, bse.arguments?.getString("gidStr"))
        }
        composable("MainSettingScreen") { MainSettingScreen(navController, mainViewModel, isDark) }
        composable("TagLikeSettingScreen") { TagLikeSettingScreen(navController, mainViewModel) }
        composable("GalleryLikeSettingScreen") { GalleryLikeSettingScreen(navController, mainViewModel) }
        composable("DatabaseExportImportSettingScreen") { DatabaseExportImportSettingScreen(mainViewModel) }
    }
}