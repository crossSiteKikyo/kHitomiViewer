package com.example.khitomiviewer.ui.screens

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.khitomiviewer.Screen
import com.example.khitomiviewer.ui.GalleryList
import com.example.khitomiviewer.ui.Pagination
import com.example.khitomiviewer.viewmodel.AppViewModel
import com.example.khitomiviewer.viewmodel.GalleryViewModel
import com.example.khitomiviewer.viewmodel.VolumeKeyEvent
import kotlinx.coroutines.launch

@Composable
fun DislikeGalleryScreen(
    navController: NavHostController,
    verticalScrollState: ScrollState,
    isTagDialogOpen: MutableState<Boolean>,
    isGalleryDialogOpen: MutableState<Boolean>,
    page: Long
) {
    // 전역 viewModel들
    val activity = LocalActivity.current as ComponentActivity
    val galleryViewModel: GalleryViewModel = viewModel(activity)
    val appViewModel: AppViewModel = viewModel(activity)

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(page) {
        galleryViewModel.getDislikeGalleries(page)
    }

    LaunchedEffect(Unit) {
        galleryViewModel.setMaxPageDislikeGalleries()
        // 볼륨 키 가로채기 활성화
        appViewModel.isPaginationActive.value = true
    }

    val onPageMove: (Long) -> Unit = { targetPage ->
        navController.navigate(Screen.DislikeGallery.createRoute(targetPage))
        coroutineScope.launch { verticalScrollState.scrollTo(0) }
    }

    // 2. 볼륨 키 이벤트 구독 (단 한 곳에서만 수행)
    LaunchedEffect(page, galleryViewModel.maxPage) {
        appViewModel.volumeKeyEvent.collect { event ->
            if (appViewModel.isVolumeKeyPagingEnabled.value) {
                when (event) {
                    VolumeKeyEvent.UP -> if (page > 1) onPageMove(page - 1)
                    VolumeKeyEvent.DOWN -> if (page < galleryViewModel.maxPage) onPageMove(page + 1)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(verticalScrollState)
    ) {
        Pagination(false, page, galleryViewModel.maxPage, onPageMove)
        GalleryList(navController, isTagDialogOpen, isGalleryDialogOpen)
        Pagination(true, page, galleryViewModel.maxPage, onPageMove)
    }
}