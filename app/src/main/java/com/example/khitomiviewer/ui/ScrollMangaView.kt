package com.example.khitomiviewer.ui

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.network.HttpException
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import com.example.khitomiviewer.R
import com.example.khitomiviewer.viewmodel.AppViewModel
import com.example.khitomiviewer.viewmodel.ViewMangaViewModel
import com.example.khitomiviewer.viewmodel.VolumeKeyEvent
import kotlinx.coroutines.delay

@Composable
fun ScrollMangaView(
    gId: Long,
    hitomiHeaders: NetworkHeaders,
    hashToImageUrl: (String) -> String,
    imageHashes: SnapshotStateList<String>
) {
    // 전역 viewModel들
    val activity = LocalActivity.current as ComponentActivity
    val viewMangaViewModel: ViewMangaViewModel = viewModel(activity)
    val appViewModel: AppViewModel = viewModel(activity)

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val listState = rememberLazyListState()
    val currentPage by remember { derivedStateOf { listState.firstVisibleItemIndex } }

    // 마지막 본 페이지 업데이트 로직
    LaunchedEffect(currentPage) {
        delay(100)  //너무 자주 업데이트 하는 것을 방지한다.
        if (currentPage > 0)
            viewMangaViewModel.updateLastPage(gId, currentPage)
    }

    LaunchedEffect(Unit) {
        // 마지막 읽던 페이지가 1 초과라면 snackbar로 이동하기가 뜬다.
        if (viewMangaViewModel.lastPage.intValue > 1) {
            val result =
                snackbarHostState.showSnackbar(
                    "마지막으로 읽던 페이지 ${viewMangaViewModel.lastPage.intValue}",
                    "이동", duration = SnackbarDuration.Long
                )
            if (result == SnackbarResult.ActionPerformed) {
                listState.scrollToItem(viewMangaViewModel.lastPage.intValue)
            }
        }
    }
    LaunchedEffect(Unit) {
        // 볼륨 키 이벤트 구독
        appViewModel.volumeKeyEvent.collect { event ->
            if (appViewModel.isVolumeKeyPagingEnabled.value) {
                when (event) {
                    VolumeKeyEvent.UP -> {
                        if (currentPage > 0)
                            listState.scrollToItem(currentPage - 1)
                    }

                    VolumeKeyEvent.DOWN -> {
                        if (currentPage < imageHashes.size - 1)
                            listState.scrollToItem(currentPage + 1)
                    }
                }
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            imageHashes.size,
            key = { idx -> imageHashes[idx] }
        ) { page ->
            val hash = imageHashes[page]

            var retryCount by remember { mutableIntStateOf(0) }
            var shouldRetry by remember { mutableStateOf(false) }
//            var showImage by remember { mutableStateOf(false) }

            // 이미지가 로드되었는지 여부와 비율을 기억합니다.
            var isLoaded by remember { mutableStateOf(false) }
            var aspectRatio by remember { mutableFloatStateOf(0f) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    //이미 로드된 적이 있다면 비율을 유지하여 스크롤 점프 방지
                    .then(
                        if (aspectRatio > 0f) Modifier.aspectRatio(aspectRatio)
                        else Modifier.heightIn(min = 300.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (!isLoaded) {
                    CircularProgressIndicator()
                }

                LaunchedEffect(shouldRetry) {
                    if (shouldRetry) {
                        delay(50) // 딜레이 추가
                        retryCount++
                        shouldRetry = false
                    }
                }

                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(hashToImageUrl(hash) + "?retry=$retryCount")
                        .httpHeaders(hitomiHeaders)
                        .memoryCacheKey(hash).diskCacheKey(hash)
                        .build(),
                    contentDescription = "img",
                    contentScale = ContentScale.FillWidth,
                    placeholder = null,
                    error = if (isLoaded) painterResource(R.drawable.errorimg) else null,
                    onError = { e ->
                        if ((e.result.throwable as? HttpException)?.response?.code == 503) {
                            shouldRetry = true
                        } else
                            isLoaded = true
                        Log.i("이미지 로드 에러", e.result.throwable.toString())
                    },
                    onSuccess = { result ->
                        isLoaded = true
                        // 이미지의 실제 비율을 계산하여 저장
                        val width = result.painter.intrinsicSize.width
                        val height = result.painter.intrinsicSize.height
                        if (height > 0) {
                            aspectRatio = width / height
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
    // 스낵바가 그려질 호스트를 하단에 배치
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier
            .padding(bottom = 80.dp) // 하단 UI와 겹치지 않게 조절
    )
}