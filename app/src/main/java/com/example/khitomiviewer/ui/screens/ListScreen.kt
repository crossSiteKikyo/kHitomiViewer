package com.example.khitomiviewer.ui.screens

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.khitomiviewer.Screen
import com.example.khitomiviewer.ui.GalleryListBrief
import com.example.khitomiviewer.ui.GalleryListExtended
import com.example.khitomiviewer.ui.GalleryListGrid
import com.example.khitomiviewer.ui.GalleryListUiSelect
import com.example.khitomiviewer.ui.Pagination
import com.example.khitomiviewer.ui.Search
import com.example.khitomiviewer.ui.SearchResultBar
import com.example.khitomiviewer.ui.SearchSortBar
import com.example.khitomiviewer.viewmodel.AppViewModel
import com.example.khitomiviewer.viewmodel.SearchViewModel
import com.example.khitomiviewer.viewmodel.GalleryViewModelKeys
import com.example.khitomiviewer.viewmodel.ProvideGalleryViewModelKey
import com.example.khitomiviewer.viewmodel.VolumeKeyEvent
import com.example.khitomiviewer.viewmodel.activityGalleryViewModel
import kotlinx.coroutines.launch

@Composable
fun ListScreen(
  navController: NavHostController,
  verticalScrollState: ScrollState,
  isTagDialogOpen: MutableState<Boolean>,
  isGalleryDialogOpen: MutableState<Boolean>,
  isGalleryDetailDialogOpen: MutableState<Boolean>,
  page: Long, tagIdList: LongArray?, titleKeyword: String?, gId: Long?
) {
  ProvideGalleryViewModelKey(GalleryViewModelKeys.LIST) {
  // 전역 viewModel들
  val activity = LocalActivity.current as ComponentActivity
  val galleryViewModel = activityGalleryViewModel(GalleryViewModelKeys.LIST)
  val searchViewModel: SearchViewModel = viewModel(activity)
  val appViewModel: AppViewModel = viewModel(activity)

  val coroutineScope = rememberCoroutineScope()

  // 검색 시트 보일지 말지
  val isSearchSheetVisible = remember { mutableStateOf(false) }

  val galleryListUi by appViewModel.galleryListUi.collectAsState("Extended")

  LaunchedEffect(page, tagIdList?.joinToString(","), titleKeyword, gId) {
    // gId가 있다면 gId로 검색
    if (gId != null && gId != 0L)
      galleryViewModel.findByGalleryIds(listOf(gId))
    else
      galleryViewModel.setGalleryList(page, tagIdList, titleKeyword)
  }

  // 최대 페이지는 검색 조건이 달라졌을 때만 다시 조회한다.
  LaunchedEffect(tagIdList?.joinToString(","), titleKeyword, gId) {
    if (gId != null && gId != 0L)
      galleryViewModel.maxPage = 1
    else {
      galleryViewModel.setMaxPage(tagIdList, titleKeyword)
      searchViewModel.setCurrentSearchTags(tagIdList)
    }
  }

  val onPageMove: (Long) -> Unit = { targetPage ->
    navController.navigate(Screen.List.createRoute(targetPage, tagIdList, titleKeyword))
    coroutineScope.launch { verticalScrollState.scrollTo(0) }
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

  if (galleryViewModel.loading.value) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      CircularProgressIndicator()
    }
  } else {
    Column(
      modifier = Modifier
          .fillMaxSize()
          .verticalScroll(verticalScrollState)
    ) {
      SearchResultBar(
        navController,
        isTagDialogOpen,
        isSearchSheetVisible,
        titleKeyword,
        gId
      )
      if (gId == null || gId == 0L) {
        SearchSortBar(
          isPopular = false,
          onNewest = {},
          onPopular = {
            navController.navigate(
              Screen.Rank.createRoute(1L, "week", tagIdList, titleKeyword)
            )
          }
        )
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
    Search(navController, isSearchSheetVisible, titleKeyword)
  }
  }
}