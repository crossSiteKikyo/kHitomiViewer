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
import com.example.khitomiviewer.ui.GalleryList
import com.example.khitomiviewer.ui.Pagination
import com.example.khitomiviewer.ui.Search
import com.example.khitomiviewer.ui.tag.SearchedTag
import com.example.khitomiviewer.viewmodel.DialogViewModel
import com.example.khitomiviewer.viewmodel.GalleryViewModel
import com.example.khitomiviewer.viewmodel.SearchViewModel
import kotlinx.coroutines.launch

@Composable
fun ListScreen(
    navController: NavHostController,
    verticalScrollState: ScrollState,
    isTagDialogOpen: MutableState<Boolean>,
    isGalleryDialogOpen: MutableState<Boolean>,
    page: Long, tagIdList: LongArray?, titleKeyword: String?, gId: Long?
) {
    // 전역 viewModel들
    val activity = LocalActivity.current as ComponentActivity
    val galleryViewModel: GalleryViewModel = viewModel(activity)
    val searchViewModel: SearchViewModel = viewModel(activity)
    val dialogViewModel: DialogViewModel = viewModel(activity)

    val coroutineScope = rememberCoroutineScope()

    // 검색 시트 보일지 말지
    val isSearchSheetVisible = remember { mutableStateOf(false) }

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
                        .border(width = 1.dp, color = Color.Gray, shape = RoundedCornerShape(2.dp))
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
            Pagination(false, page, galleryViewModel.maxPage) { page ->
                navController.navigate(Screen.List.createRoute(page, tagIdList, titleKeyword))
                coroutineScope.launch { verticalScrollState.scrollTo(0) }
            }
            GalleryList(navController, isTagDialogOpen, isGalleryDialogOpen)
            Pagination(true, page, galleryViewModel.maxPage) { page ->
                navController.navigate(Screen.List.createRoute(page, tagIdList, titleKeyword))
                coroutineScope.launch { verticalScrollState.scrollTo(0) }
            }
        }
        Search(navController, isSearchSheetVisible, titleKeyword)
    }
}