package com.example.khitomiviewer.ui.screens

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.khitomiviewer.Screen
import com.example.khitomiviewer.ui.Pagination
import com.example.khitomiviewer.ui.TagList
import com.example.khitomiviewer.viewmodel.AppViewModel
import com.example.khitomiviewer.viewmodel.TagViewModel
import com.example.khitomiviewer.viewmodel.VolumeKeyEvent
import kotlinx.coroutines.launch

@Composable
fun MyTagScreen(
    navController: NavHostController,
    verticalScrollState: ScrollState,
    isTagDialogOpen: MutableState<Boolean>,
    page: Long
) {
    // 전역 viewModel들
    val activity = LocalActivity.current as ComponentActivity
    val tagViewModel: TagViewModel = viewModel(activity)
    val appViewModel: AppViewModel = viewModel(activity)

    val coroutineScope = rememberCoroutineScope()

    val onPageMove: (Long) -> Unit = { targetPage ->
        navController.navigate(Screen.MyTag.createRoute(targetPage))
        coroutineScope.launch { verticalScrollState.scrollTo(0) }
    }

    LaunchedEffect(page) {
        tagViewModel.getLikeTags(page)
    }

    LaunchedEffect(Unit) {
        tagViewModel.setMaxPageLikeTags()
        // 볼륨 키 가로채기 활성화
        appViewModel.isPaginationActive.value = true
        // 볼륨 키 이벤트 구독
        appViewModel.volumeKeyEvent.collect { event ->
            if (appViewModel.isVolumeKeyPagingEnabled.value) {
                when (event) {
                    VolumeKeyEvent.UP -> if (page > 1) onPageMove(page - 1)
                    VolumeKeyEvent.DOWN -> if (page < tagViewModel.maxPage) onPageMove(page + 1)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(verticalScrollState)
    ) {
        Pagination(false, page, tagViewModel.maxPage, onPageMove)
        TagList(navController, isTagDialogOpen)
        Pagination(true, page, tagViewModel.maxPage, onPageMove)
    }
}
