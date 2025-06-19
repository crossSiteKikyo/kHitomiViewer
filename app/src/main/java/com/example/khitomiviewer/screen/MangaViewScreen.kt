package com.example.khitomiviewer.screen

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun MangaViewScreen(navController: NavController, mainViewModel: KHitomiViewerViewModel, gIdStr: String?) {
    val context = LocalContext.current
    var pagerState = rememberPagerState { mainViewModel.imageUrls.size }
    val coroutineScope = rememberCoroutineScope()
    val hitomiHeaders = NetworkHeaders.Builder()
        .set("Referer", "https://hitomi.la/")
        .build()
    var visible by remember { mutableStateOf(true) }
    val interactionSource = remember { MutableInteractionSource() }
    var isDragging = interactionSource.collectIsDraggedAsState()

    // 값이 변할 때마다 타이머 재시작
    LaunchedEffect(pagerState.currentPageOffsetFraction, pagerState.currentPage, isDragging.value) {
        if(!isDragging.value) {
            visible = true
            delay(1000L) // 1초 후 자동 숨김
            visible = false
        }
        else {
            visible = true
        }
    }

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
            // 최대사이즈 size - 1, thumb모양바꾸기, 값 1초동안 안 변하면 안보이기
            if (mainViewModel.imageUrls.isNotEmpty()) {
                Slider(
                    value = pagerState.currentPage + pagerState.currentPageOffsetFraction,
                    onValueChange = { v ->
                        coroutineScope.launch {
                            pagerState.scrollToPage(v.toInt())
                        }
                    },
                    valueRange = 0f..(mainViewModel.imageUrls.size.toFloat()-1),
                    interactionSource = interactionSource,
                    colors = SliderDefaults.colors(
                        activeTrackColor  = Color.Transparent,
                        inactiveTrackColor = Color.Transparent,
                    ),
                    thumb = {
                        if(visible) {
                            Row(
                                modifier = Modifier.background(Color(0xFF9933FF), shape = RoundedCornerShape(10.dp))
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = null,
                                    modifier = Modifier.width(25.dp)
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    modifier = Modifier.width(25.dp)
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun preview() {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box() {
            Slider(
                value = 3f,
                valueRange = 0f..4f,
                onValueChange = {},
                colors = SliderDefaults.colors(
                    activeTrackColor  = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                ),
                thumb = {
                    Row(
                        modifier = Modifier.background(Color(0xFF9933FF), shape = RoundedCornerShape(10.dp))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = null,
                            modifier = Modifier.width(25.dp)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.width(25.dp)
                        )
                    }
                },
                modifier = Modifier
                    .graphicsLayer{
                        rotationZ = 90f
                        transformOrigin = TransformOrigin(0f, 0f)
                    }
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(
                            Constraints(
                                minWidth = constraints.minHeight,
                                maxWidth = constraints.maxHeight,
                                minHeight = constraints.minWidth,
                                maxHeight = constraints.maxWidth
                            )
                        )
                        layout(placeable.height, placeable.width) {
                            placeable.place(0, -placeable.height)
                        }
                    }
                    .fillMaxWidth()
//                    .align(Alignment.BottomEnd)
            )
        }
    }
}