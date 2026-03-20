package com.example.khitomiviewer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.khitomiviewer.ui.screens.DatabaseExportImportSettingScreen
import com.example.khitomiviewer.ui.screens.DislikeGalleryScreen
import com.example.khitomiviewer.ui.screens.DislikeTagScreen
import com.example.khitomiviewer.ui.screens.HelpScreen
import com.example.khitomiviewer.ui.screens.ListScreen
import com.example.khitomiviewer.ui.screens.SettingScreen
import com.example.khitomiviewer.ui.screens.MenuScreen
import com.example.khitomiviewer.ui.screens.MyGalleryScreen
import com.example.khitomiviewer.ui.screens.MyTagScreen
import com.example.khitomiviewer.ui.screens.RankScreen
import com.example.khitomiviewer.ui.screens.SubscriptionScreen
import com.example.khitomiviewer.ui.screens.ViewMangaScreen
import com.example.khitomiviewer.ui.BottomBar
import com.example.khitomiviewer.ui.FloatingActionButton
import com.example.khitomiviewer.ui.GalleryDialog
import com.example.khitomiviewer.ui.TagDialog
import com.example.khitomiviewer.ui.UIEventHandler
import com.example.khitomiviewer.ui.VerticalScrollBar
import com.example.khitomiviewer.ui.theme.KHitomiViewerTheme
import com.example.khitomiviewer.viewmodel.AppViewModel
import com.example.khitomiviewer.viewmodel.HitomiViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            enableEdgeToEdge()
            // viewModel의 activity 주인을 정해주면 전역으로 사용할 수 있다.
            val activity = LocalActivity.current as ComponentActivity
            // 크롤링을 위해 hitomi뷰모델 생성
            val hitomiViewModel: HitomiViewModel = viewModel(activity)
            val appViewModel: AppViewModel = viewModel(activity)
            appViewModel.checkLatestVersionAtAppStart()

            val isDark by appViewModel.isDarkMode.collectAsState(initial = false)
            KHitomiViewerTheme(
                darkTheme = isDark
            ) {
                MainApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isViewMangaScreen = currentRoute?.startsWith("ViewMangaScreen") == true
    val shouldShowUtils = !(isViewMangaScreen)
    // 스크롤 하는데 사용되는 변수
    val verticalScrollState: ScrollState = rememberScrollState()
    // 전역 스낵바를 위한 상태
    val snackbarHostState = remember { SnackbarHostState() }
    // 다이얼로그 보일지 말지
    val isTagDialogOpen = remember { mutableStateOf(false) }
    val isGalleryDialogOpen = remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (shouldShowUtils) FloatingActionButton(verticalScrollState)
        },
        bottomBar = { if (shouldShowUtils) BottomBar(navController, verticalScrollState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            NavHost(
                navController = navController, startDestination = "ListScreen",
                enterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(700)
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(700)
                    )
                },
                popEnterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(700)
                    )
                },
                popExitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(700)
                    )
                },
            ) {
                composable(
                    Screen.List.route,
                    arguments = listOf(
                        navArgument("page") {
                            type = NavType.LongType
                            defaultValue = 1L
                        },
                        navArgument("tagIdList") {
                            type = NavType.LongArrayType
                            nullable = true
                        },
                        navArgument("titleKeyword") {
                            type = NavType.StringType
                            defaultValue = ""
                        },
                        navArgument("gId") {
                            type = NavType.LongType
                            defaultValue = 0L
                        },
                    )
                ) { bse ->
                    ListScreen(
                        navController,
                        verticalScrollState,
                        isTagDialogOpen,
                        isGalleryDialogOpen,
                        bse.arguments?.getLong("page") ?: 1L,
                        bse.arguments?.getLongArray("tagIdList"),
                        bse.arguments?.getString("titleKeyword"),
                        bse.arguments?.getLong("gId")
                    )
                }
                composable(
                    Screen.MyGallery.route,
                    arguments = listOf(
                        navArgument("page") {
                            type = NavType.LongType
                            defaultValue = 1L
                        })
                ) { bse ->
                    MyGalleryScreen(
                        navController,
                        verticalScrollState,
                        isTagDialogOpen,
                        isGalleryDialogOpen,
                        bse.arguments?.getLong("page") ?: 1L
                    )
                }
                composable(
                    Screen.DislikeGallery.route,
                    arguments = listOf(
                        navArgument("page") {
                            type = NavType.LongType
                            defaultValue = 1L
                        })
                ) { bse ->
                    DislikeGalleryScreen(
                        navController,
                        verticalScrollState,
                        isTagDialogOpen,
                        isGalleryDialogOpen,
                        bse.arguments?.getLong("page") ?: 1L
                    )
                }
                composable(
                    Screen.MyTag.route,
                    arguments = listOf(
                        navArgument("page") {
                            type = NavType.LongType
                            defaultValue = 1L
                        })
                ) { bse ->
                    MyTagScreen(
                        navController,
                        verticalScrollState,
                        isTagDialogOpen,
                        bse.arguments?.getLong("page") ?: 1L
                    )
                }
                composable(
                    Screen.DislikeTag.route,
                    arguments = listOf(
                        navArgument("page") {
                            type = NavType.LongType
                            defaultValue = 1L
                        })
                ) { bse ->
                    DislikeTagScreen(
                        navController,
                        verticalScrollState,
                        isTagDialogOpen,
                        bse.arguments?.getLong("page") ?: 1L
                    )
                }
                composable(
                    Screen.Subscription.route,
                    arguments = listOf(
                        navArgument("page") {
                            type = NavType.LongType
                            defaultValue = 1L
                        })
                ) { bse ->
                    SubscriptionScreen(
                        navController,
                        verticalScrollState,
                        isTagDialogOpen,
                        isGalleryDialogOpen,
                        bse.arguments?.getLong("page") ?: 1L
                    )
                }
                composable(
                    Screen.Rank.route,
                    arguments = listOf(
                        navArgument("page") {
                            type = NavType.LongType
                            defaultValue = 1L
                        },
                        navArgument("period") {
                            type = NavType.StringType
                            defaultValue = "week"
                        }
                    )
                ) { bse ->
                    RankScreen(
                        navController,
                        verticalScrollState,
                        isTagDialogOpen,
                        isGalleryDialogOpen,
                        bse.arguments?.getLong("page") ?: 1L,
                        bse.arguments?.getString("period") ?: "week",
                    )
                }
                composable(Screen.ViewManga.route) { bse ->
                    ViewMangaScreen(
                        navController,
                        bse.arguments?.getString("gidStr")
                    )
                }
                composable(Screen.Menu.route) {
                    MenuScreen(
                        navController,
                        verticalScrollState
                    )
                }
                composable(Screen.Setting.route) {
                    SettingScreen(
                        verticalScrollState
                    )
                }
                composable(Screen.Help.route) {
                    HelpScreen(
                        verticalScrollState
                    )
                }
                composable(Screen.DatabaseExportImport.route) {
                    DatabaseExportImportSettingScreen()
                }
            }
        }
        VerticalScrollBar(innerPadding, verticalScrollState)
        TagDialog(isTagDialogOpen)
        GalleryDialog(isGalleryDialogOpen)
        UIEventHandler(snackbarHostState)
    }
}