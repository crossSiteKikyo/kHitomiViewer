package com.example.khitomiviewer.ui.screens

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.khitomiviewer.Screen
import com.example.khitomiviewer.ui.GalleryListBrief
import com.example.khitomiviewer.ui.GalleryListExtended
import com.example.khitomiviewer.ui.GalleryListGrid
import com.example.khitomiviewer.ui.GalleryListUiSelect
import com.example.khitomiviewer.ui.Pagination
import com.example.khitomiviewer.viewmodel.AppViewModel
import com.example.khitomiviewer.viewmodel.GalleryViewModel
import com.example.khitomiviewer.viewmodel.VolumeKeyEvent
import kotlinx.coroutines.launch

@Composable
fun RankScreen(
    navController: NavHostController,
    verticalScrollState: ScrollState,
    isTagDialogOpen: MutableState<Boolean>,
    isGalleryDialogOpen: MutableState<Boolean>,
    isGalleryDetailDialogOpen: MutableState<Boolean>,
    page: Long,
    period: String
) {
    // 전역 viewModel들
    val activity = LocalActivity.current as ComponentActivity
    val galleryViewModel: GalleryViewModel = viewModel(activity)
    val appViewModel: AppViewModel = viewModel(activity)

    val galleryListUi by appViewModel.galleryListUi.collectAsState("Extended")

    val coroutineScope = rememberCoroutineScope()

    val onPageMove: (Long) -> Unit = { targetPage ->
        navController.navigate(Screen.Rank.createRoute(targetPage, period))
        coroutineScope.launch { verticalScrollState.scrollTo(0) }
    }

    LaunchedEffect(page, period) {
        // hitomi에서 popular를 가져온다.
        galleryViewModel.getPopularFromHitomi(page, period)
    }

    LaunchedEffect(Unit) {
        // 볼륨 키 가로채기 활성화
        appViewModel.isPaginationActive.value = true
        // 볼륨 키 이벤트 구독
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            PeriodButton(navController, "today", period, "일간")
            PeriodButton(navController, "week", period, "주간")
            PeriodButton(navController, "month", period, "월간")
            PeriodButton(navController, "year", period, "년간")
        }
        
        GalleryListUiSelect()
        Pagination(false, page, galleryViewModel.maxPage, onPageMove)
        if (galleryListUi == "Extended")
            GalleryListExtended(navController, isTagDialogOpen, isGalleryDialogOpen)
        else if (galleryListUi == "Brief")
            GalleryListBrief(navController, isGalleryDialogOpen, isGalleryDetailDialogOpen)
        else
            GalleryListGrid(isGalleryDialogOpen, isGalleryDetailDialogOpen)
        Pagination(true, page, galleryViewModel.maxPage, onPageMove)
    }
}

@Composable
fun PeriodButton(
    navController: NavHostController,
    period: String,
    currentPeriod: String,
    text: String
) {
    Button(
        enabled = period != currentPeriod,
        onClick = {
            navController.navigate(
                Screen.Rank.createRoute(
                    1L,
                    period
                )
            )
        }) { Text(text) }
}