package com.example.khitomiviewer.ui.screens

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

  val tagLikeStatusOrder by appViewModel.tagLikeStatusOrder.collectAsState("statusChangedAt")

  val coroutineScope = rememberCoroutineScope()

  val onPageMove: (Long) -> Unit = { targetPage ->
    navController.navigate(Screen.MyTag.createRoute(targetPage))
    coroutineScope.launch { verticalScrollState.scrollTo(0) }
  }

  LaunchedEffect(page, tagLikeStatusOrder) {
    tagViewModel.getLikeTags(page, tagLikeStatusOrder)
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
    Row(
      modifier = Modifier
          .fillMaxWidth()
          .height(56.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
      Button(
        onClick = {
          appViewModel.setTagLikeStatusOrder("statusChangedAt")
        },
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(12.dp),
        enabled = tagLikeStatusOrder != "statusChangedAt",
        contentPadding = PaddingValues(2.dp)
      ) {
        Text("상태 변경 날짜순")
      }
      Button(
        onClick = {
          appViewModel.setTagLikeStatusOrder("dict")
        },
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(12.dp),
        enabled = tagLikeStatusOrder != "dict",
        contentPadding = PaddingValues(2.dp)
      ) {
        Text("사전순")
      }
    }
    Pagination(false, page, tagViewModel.maxPage, onPageMove)
    TagList(navController, isTagDialogOpen)
    Pagination(true, page, tagViewModel.maxPage, onPageMove)
  }
}
