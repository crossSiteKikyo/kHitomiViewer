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
import com.example.khitomiviewer.ui.Pagination
import com.example.khitomiviewer.ui.TagList
import com.example.khitomiviewer.viewmodel.TagViewModel
import kotlinx.coroutines.launch

@Composable
fun DislikeTagScreen(
    navController: NavHostController,
    verticalScrollState: ScrollState,
    isTagDialogOpen: MutableState<Boolean>,
    page: Long
) {
    // 전역 viewModel들
    val activity = LocalActivity.current as ComponentActivity
    val tagViewModel: TagViewModel = viewModel(activity)

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(page) {
        tagViewModel.getDislikeTags(page)
    }

    LaunchedEffect(true) {
        tagViewModel.setMaxPageDislikeTags()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(verticalScrollState)
    ) {
        Pagination(false, page, tagViewModel.maxPage) { page ->
            navController.navigate(Screen.DislikeTag.createRoute(page))
            coroutineScope.launch { verticalScrollState.scrollTo(0) }
        }
        TagList(navController, isTagDialogOpen)
        Pagination(true, page, tagViewModel.maxPage) { page ->
            navController.navigate(Screen.DislikeTag.createRoute(page))
            coroutineScope.launch { verticalScrollState.scrollTo(0) }
        }
    }
}