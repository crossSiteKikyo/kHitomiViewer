package com.example.khitomiviewer.ui.screens

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.khitomiviewer.Screen
import com.example.khitomiviewer.ui.GalleryListBrief
import com.example.khitomiviewer.ui.GalleryListExtended
import com.example.khitomiviewer.ui.GalleryListGrid
import com.example.khitomiviewer.ui.Pagination
import com.example.khitomiviewer.ui.Search
import com.example.khitomiviewer.ui.tag.SearchedTag
import com.example.khitomiviewer.viewmodel.AppViewModel
import com.example.khitomiviewer.viewmodel.DialogViewModel
import com.example.khitomiviewer.viewmodel.GalleryViewModel
import com.example.khitomiviewer.viewmodel.SearchViewModel
import com.example.khitomiviewer.viewmodel.VolumeKeyEvent
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
    // 전역 viewModel들
    val activity = LocalActivity.current as ComponentActivity
    val galleryViewModel: GalleryViewModel = viewModel(activity)
    val searchViewModel: SearchViewModel = viewModel(activity)
    val dialogViewModel: DialogViewModel = viewModel(activity)
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

    // 볼륨 키 가로채기 활성화
    LaunchedEffect(Unit) {
        appViewModel.isPaginationActive.value = true
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            width = 1.dp,
                            color = Color.Gray,
                            shape = RoundedCornerShape(2.dp)
                        )
                ) {
                    // gId검색결과 보여주기
                    if (gId != null && gId != 0L) {
                        Text(
                            "id검색 - $gId",
                            fontWeight = FontWeight.Bold,
                        )
                    } else {
                        // 제목검색결과 보여주기
                        if (!titleKeyword.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                            ) {
                                Text(
                                    "제목검색 - $titleKeyword",
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                        // 태그검색결과 보여주기
                        if (searchViewModel.currentSearchTags.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                            ) {
                                Text(
                                    "태그검색 - ",
                                    fontWeight = FontWeight.Bold,
                                )
                                searchViewModel.currentSearchTags.map { tag ->
                                    SearchedTag(
                                        tag,
                                        dialogViewModel,
                                        isTagDialogOpen,
                                        navController
                                    )
                                }
                            }
                        }
                    }
                }
                Button(
                    onClick = { isSearchSheetVisible.value = true },
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .width(40.dp)
                        .fillMaxHeight()
                        .padding(0.dp)
                ) { Icon(Icons.Filled.Search, "search") }
            }
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