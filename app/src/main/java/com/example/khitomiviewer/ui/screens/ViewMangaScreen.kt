package com.example.khitomiviewer.ui.screens

import android.annotation.SuppressLint
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.network.HttpException
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import com.example.khitomiviewer.R
import com.example.khitomiviewer.ui.AutoPlayDialog
import com.example.khitomiviewer.viewmodel.AppViewModel
import com.example.khitomiviewer.viewmodel.HitomiViewModel
import com.example.khitomiviewer.viewmodel.ViewMangaViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun ViewMangaScreen(
    navController: NavController,
    gIdStr: String?
) {
    // 전역 viewModel들
    val activity = LocalActivity.current as ComponentActivity
    val hitomiViewModel: HitomiViewModel = viewModel(activity)
    val viewMangaViewModel: ViewMangaViewModel = viewModel(activity)
    val appViewModel: AppViewModel = viewModel(activity)

    val isAutoPlayDialogOpen = remember { mutableStateOf(false) }
    var isUiVisible by remember { mutableStateOf(false) }

    // 이걸 해놔야 out of bound 에러가 안난다.
//    imageListViewModel.imagesLoading.value = true

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val hitomiHeaders = NetworkHeaders.Builder()
        .set("Referer", "https://hitomi.la/")
        .build()
    val hashToImageUrl: (String) -> String = { hash ->
        val o1 = hitomiViewModel.o1.value
        val o2 = hitomiViewModel.o2.value
        val s = "${hash[hash.length - 1]}${hash[hash.length - 3]}${hash[hash.length - 2]}".toInt(16)
            .toString(10)
        val subdomainNum = if (o1 == null || o2 == null) 1 else
            (if (s in hitomiViewModel.mList) o2.toInt() else o1.toInt()) + 1
        // 구형폰에서는 avif 디코딩을 지원하지 않는거 같다. webp로 하자.어쩔 수 없다.
        "https://w${subdomainNum}.gold-usergeneratedcontent.net/${hitomiViewModel.b.value}${s}/${hash}.webp"
    }

    val imageHashes = viewMangaViewModel.imageHashes

    LaunchedEffect(gIdStr) {
        val gId = gIdStr?.toLong()
        if (gId != null) {
            viewMangaViewModel.setGalleryImages(gId)
        }
    }

    // 볼륨 키 가로채기 비활성화
    LaunchedEffect(Unit) {
        appViewModel.isPaginationActive.value = false
    }

    // 시스템 바 숨겨서 몰입을 높이기 위한 변수들
    val view = LocalView.current
    val window = activity.window
    val insetsController = remember(view) {
        window?.let { WindowCompat.getInsetsController(it, view) }
    }
    insetsController?.apply {
        // 시스템 바 숨기기
        hide(WindowInsetsCompat.Type.systemBars())
        // 시스템 바가 숨겨진 상태에서 화면 끝을 쓸어넘기면 잠깐 나타났다가 다시 사라지게 설정
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    DisposableEffect(Unit) {
        onDispose {
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
            viewMangaViewModel.isAutoPlaying.value = false
        }
    }

    // 사용자의 설정을 저장하는 변수 (예: true면 일본식 RTL)
    val isRtl by viewMangaViewModel.isRtlMode.collectAsState(initial = false)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
    ) {
        if (viewMangaViewModel.notExist) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("없는 갤러리입니다")
            }
        } else if (viewMangaViewModel.imagesLoading.value || imageHashes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val pagerState = rememberPagerState(pageCount = { imageHashes.size })
            // 자동넘기기 타이머 로직
            LaunchedEffect(viewMangaViewModel.isAutoPlaying.value, pagerState.currentPage) {
                if (viewMangaViewModel.isAutoPlaying.value) {
                    delay(viewMangaViewModel.tempPeriod.value.toInt() * 1000L)
                    val isLastPage = pagerState.currentPage == imageHashes.size - 1
                    if (isLastPage) {
                        if (viewMangaViewModel.tempIsLoop.value)
                            pagerState.scrollToPage(0)
                        else
                            viewMangaViewModel.isAutoPlaying.value = false
                    } else
                        pagerState.scrollToPage(pagerState.currentPage + 1)
                }
            }
            fun movePage(direction: String) {
                coroutineScope.launch {
                    if (direction == "next" && pagerState.currentPage < imageHashes.size - 1)
                        pagerState.scrollToPage(pagerState.currentPage + 1)
                    else if (direction == "prev" && pagerState.currentPage > 0)
                        pagerState.scrollToPage(pagerState.currentPage - 1)
                }
            }
            CompositionLocalProvider(LocalLayoutDirection provides if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                HorizontalPager(
                    state = pagerState,
                    beyondViewportPageCount = 12,
                    modifier = Modifier.fillMaxSize()
                )
                { page ->

                    var retryCount by remember { mutableIntStateOf(0) }
                    var shouldRetry by remember { mutableStateOf(false) }
                    var showImage by remember { mutableStateOf(false) }

                    if (!showImage) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
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
                            .data(hashToImageUrl(imageHashes[page]) + "?retry=$retryCount")
                            .httpHeaders(hitomiHeaders)
                            .diskCacheKey(imageHashes[page]).build(),
                        contentDescription = "img",
                        contentScale = ContentScale.Fit,
                        placeholder = null,
                        error = if (showImage) painterResource(R.drawable.errorimg) else null,
                        onError = { e ->
                            if ((e.result.throwable as? HttpException)?.response?.code == 503) {
                                shouldRetry = true
                            } else
                                showImage = true
                            Log.i("이미지 로드 에러", e.result.throwable.toString())
                        },
                        onSuccess = { showImage = true },
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    val ratio = offset.x / constraints.maxWidth
                                    when {
                                        ratio < 0.3f -> movePage(if (isRtl) "next" else "prev")
                                        ratio > 0.7f -> movePage(if (isRtl) "prev" else "next")
                                        else -> isUiVisible = !isUiVisible // UI 보이기/숨기기
                                    }
                                }
                            }
                    )
                }
                // 상단 ui
                AnimatedVisibility(
                    visible = isUiVisible,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.2f))
                            .statusBarsPadding() // 상태바 영역 침범 방지
                    ) {
                        Text(viewMangaViewModel.title.value)
                        HorizontalDivider()
                    }
                }
                // 하단 ui
                AnimatedVisibility(
                    visible = isUiVisible,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.2f))
                            .padding(bottom = 5.dp)
                    ) {
                        HorizontalDivider()
                        Slider(
                            value = pagerState.currentPage + pagerState.currentPageOffsetFraction,
                            onValueChange = { v ->
                                coroutineScope.launch {
                                    pagerState.scrollToPage(v.toInt())
                                }
                            },
                            valueRange = 0f..(imageHashes.size.toFloat() - 1),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF9933FF),
                                activeTrackColor = Color(0xFF9933FF),
                                inactiveTrackColor = Color(0xFF6600CC),
                            ),
                            thumb = {
                                SliderDefaults.Thumb(
                                    interactionSource = remember { MutableInteractionSource() },
                                    thumbSize = DpSize(20.dp, 20.dp)
                                )
                            },
                            track = { sliderState ->
                                SliderDefaults.Track(
                                    sliderState = sliderState,
                                    modifier = Modifier.height(5.dp)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp)
                        )
                        Text("${pagerState.currentPage + 1}/${imageHashes.size}")
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                shape = RoundedCornerShape(4.dp),
                                onClick = { viewMangaViewModel.toggleRtlMode(isRtl) }
                            ) {
                                if (isRtl)
                                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, null)
                                else
                                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, null)
                            }
                            if (viewMangaViewModel.isAutoPlaying.value)
                                Button(
                                    shape = RoundedCornerShape(4.dp),
                                    onClick = { viewMangaViewModel.isAutoPlaying.value = false }
                                ) { Icon(Icons.Outlined.Stop, null) }
                            else
                                Button(
                                    shape = RoundedCornerShape(4.dp),
                                    onClick = { isAutoPlayDialogOpen.value = true }
                                ) { Icon(Icons.Outlined.PlayArrow, null) }
                        }
                    }
                }
            }
            AutoPlayDialog(isAutoPlayDialogOpen)
        }
    }
}