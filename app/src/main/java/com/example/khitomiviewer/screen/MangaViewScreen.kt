package com.example.khitomiviewer.screen

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.network.HttpException
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import com.example.khitomiviewer.R
import com.example.khitomiviewer.viewmodel.KHitomiViewerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun MangaViewScreen(navController: NavController, mainViewModel: KHitomiViewerViewModel, gIdStr: String?) {
    val context = LocalContext.current
    var pagerState = rememberPagerState { mainViewModel.imageUrls.size }
    val coroutineScope = rememberCoroutineScope()
    val hitomiHeaders = NetworkHeaders.Builder()
        .set("Referer", "https://hitomi.la/")
        .build()

    LaunchedEffect(gIdStr) {
        val gId = gIdStr?.toLong()
        if (gId != null) {
            mainViewModel.setGalleryImages(gId, context)
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 15,
                modifier = Modifier.fillMaxSize()
            ) { page ->

                var retryCount by remember { mutableIntStateOf(0) }
                var shouldRetry by remember { mutableStateOf(false) }

                AsyncImage(
                    model = ImageRequest.Builder(context).data(mainViewModel.imageUrls[page] + "?retry=$retryCount").httpHeaders(hitomiHeaders)
                        .diskCacheKey(mainViewModel.imageUrls[page]).build(),
                    contentDescription = "img",
                    contentScale = ContentScale.Fit,
                    placeholder = painterResource(R.drawable.loading),
                    error = painterResource(R.drawable.errorimg),
                    onError = { e->
                        if((e.result.throwable as? HttpException)?.response?.code == 503) {
                            shouldRetry = true
                        }
                        Log.i("이미지 로드 에러", e.result.throwable.toString())
                    },
                    modifier = Modifier
                        .width(maxWidth)
                        .height(maxHeight)
                        .clickable(onClick = {
                            coroutineScope.launch {
                                pagerState.scrollToPage(page + 1)
                            }
                        })
                )

                LaunchedEffect(shouldRetry) {
                    if (shouldRetry) {
                        delay(50) // 딜레이 추가
                        retryCount++
                        shouldRetry = false
                    }
                }
            }
            // 아래 슬라이더
            if (mainViewModel.imageUrls.isNotEmpty()) {
                Slider(
                    value = pagerState.currentPage.toFloat(),
                    onValueChange = { v ->
                        coroutineScope.launch {
                            pagerState.scrollToPage(v.toInt())
                        }
                    },
                    valueRange = 0f..(mainViewModel.imageUrls.size.toFloat()),
                    colors = SliderDefaults.colors(
                        activeTrackColor  = Color.Transparent,
                        inactiveTrackColor = Color.Transparent,
                        thumbColor = Color.Blue
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .align(Alignment.BottomCenter)
                        .padding(start = 20.dp)
                )
            }
        }
    }
}