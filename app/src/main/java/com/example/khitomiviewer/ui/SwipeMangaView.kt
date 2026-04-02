package com.example.khitomiviewer.ui

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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
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
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.network.HttpException
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import com.example.khitomiviewer.R
import com.example.khitomiviewer.util.hitomiHeaders
import com.example.khitomiviewer.viewmodel.AppViewModel
import com.example.khitomiviewer.viewmodel.ViewMangaViewModel
import com.example.khitomiviewer.viewmodel.VolumeKeyEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun SwipeMangaView(
  gId: Long,
  hashToImageUrl: (String) -> String,
  imageHashes: SnapshotStateList<String>,
  isUISelectDialogOpen: MutableState<Boolean>
) {
  // 전역 viewModel들
  val activity = LocalActivity.current as ComponentActivity
  val viewMangaViewModel: ViewMangaViewModel = viewModel(activity)
  val appViewModel: AppViewModel = viewModel(activity)

  val isAutoPlayDialogOpen = remember { mutableStateOf(false) }
  var isUiVisible by remember { mutableStateOf(false) }

  val coroutineScope = rememberCoroutineScope()

  val context = LocalContext.current

  // 사용자의 설정을 저장하는 변수 (예: true면 일본식 RTL)
  val isRtl by viewMangaViewModel.isRtlMode.collectAsState(initial = false)

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
  ) {
    val snackbarHostState = remember { SnackbarHostState() }
    val pagerState = rememberPagerState(pageCount = { imageHashes.size })
    // 마지막 본 페이지 업데이트 로직
    LaunchedEffect(pagerState.currentPage) {
      delay(100)  //너무 자주 업데이트 하는 것을 방지한다.
      if (pagerState.currentPage > 0)
        viewMangaViewModel.updateLastPage(gId, pagerState.currentPage + 1)
    }
    fun movePage(direction: String) {
      coroutineScope.launch {
        if (direction == "next" && pagerState.currentPage < imageHashes.size - 1)
          pagerState.scrollToPage(pagerState.currentPage + 1)
        else if (direction == "prev" && pagerState.currentPage > 0)
          pagerState.scrollToPage(pagerState.currentPage - 1)
      }
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
          pagerState.scrollToPage(viewMangaViewModel.lastPage.intValue - 1)
        }
      }
    }
    LaunchedEffect(Unit) {
      // 볼륨 키 이벤트 구독
      appViewModel.volumeKeyEvent.collect { event ->
        if (appViewModel.isVolumeKeyPagingEnabled.value) {
          when (event) {
            VolumeKeyEvent.UP -> movePage("prev")
            VolumeKeyEvent.DOWN -> movePage("next")
          }
        }
      }
    }
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
    // rtl인지 ltr인지에 따라서 레이아웃 방향이 바뀐다.
    CompositionLocalProvider(LocalLayoutDirection provides if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
      HorizontalPager(
        state = pagerState,
        beyondViewportPageCount = 12,
        key = { idx -> imageHashes[idx] },
        modifier = Modifier.fillMaxSize()
      )
      { page ->
        val hash = imageHashes[page]
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
        // 확대/축소 및 이동 상태 변수
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        var imageSize by remember { mutableStateOf(IntSize.Zero) }
        Box(
          modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .pointerInput(Unit) {
              // 터치 감지
              detectTapGestures { offset ->
                val ratio = offset.x / size.width
                when {
                  ratio < 0.3f -> movePage(if (isRtl) "next" else "prev")
                  ratio > 0.7f -> movePage(if (isRtl) "prev" else "next")
                  else -> isUiVisible = !isUiVisible // UI 보이기/숨기기
                }
              }
            }
            .pointerInput(Unit) {
              val touchSlop = viewConfiguration.touchSlop
              // 확대/축소 및 이동 처리
              awaitEachGesture {  // 화면에 손가락을 대고(Down) 뗄 때까지(Up)의 한세트를 하나의 제스처로 본다.
                // 현재 화면 안에서 이미지가 차지하는 실제 크기 계산 (Fit 기준)
                val containerSize = size //Box의 크기 (화면 전체)
                val contentAspectRatio = imageSize.width.toFloat() / imageSize.height.toFloat()
                val containerAspectRatio =
                  containerSize.width.toFloat() / containerSize.height.toFloat()
                val displayWidth: Float
                val displayHeight: Float
                if (contentAspectRatio > containerAspectRatio) {  //위아래로 여백이 있는경우
                  displayWidth = containerSize.width.toFloat()
                  displayHeight = containerSize.width.toFloat() / contentAspectRatio
                } else { //좌우로 여백이 있는경우
                  displayHeight = containerSize.height.toFloat()
                  displayWidth = containerSize.height.toFloat() * contentAspectRatio
                }

                var zoom = 1f
                var pan = Offset.Zero
                var pastTouchSlop = false
                // 첫 번째 터치 대기
                awaitFirstDown(requireUnconsumed = false)
                val oldOffset = offset
                var firstCheck = false
                do {
                  val event = awaitPointerEvent()
                  val canceled = event.changes.any { it.isConsumed }
                  if (!canceled) {
                    // 확대/축소/이동 계산
                    val zoomChange = event.calculateZoom()
                    val panChange = event.calculatePan()
                    if (!pastTouchSlop) {
                      zoom *= zoomChange
                      pan += panChange
                      // 사용자가 의도적으로 움직였는지(Touch Slop)확인. 일정거리 이내로 움직이면 터치로 간주하고, 일정거리 이상 움직여야 드래그로 간주한다.
                      val centroidSize = event.calculateCentroidSize(useCurrent = false)
                      val zoomMotion = abs(1f - zoom) * centroidSize
                      val panMotion = pan.getDistance()
                      if (zoomMotion > touchSlop || panMotion > touchSlop) {
                        pastTouchSlop = true
                      }
                    }
                    if (pastTouchSlop) {
                      if (scale > 1f || zoomChange != 1f) {
                        scale = (scale * zoomChange).coerceIn(1f, 3f)
                        // 확대 상태일 때만 이벤트를 소비하여 부모 스크롤을 막음
                        if (scale > 1f) {
                          // 확대된 이미지의 실제 크기
                          val scaledWidth = displayWidth * scale
                          val scaledHeight = displayHeight * scale
                          // 확대한 크기가 화면보다 클 때만 여유분이 생긴다.
                          val xLimit =
                            if (scaledWidth > containerSize.width) (scaledWidth - containerSize.width) / 2 else 0f
                          val yLimit =
                            if (scaledHeight > containerSize.height) (scaledHeight - containerSize.height) / 2 else 0f
                          // 한손가락으로, 맨끝에서 더 움직이는거면 break한다.
                          if (!firstCheck) {
                            if (oldOffset.y == yLimit && oldOffset.x == xLimit && panChange.y > 0 && panChange.x > 0
                              || oldOffset.y == yLimit && oldOffset.x == -xLimit && panChange.y > 0 && panChange.x < 0
                              || oldOffset.y == -yLimit && oldOffset.x == xLimit && panChange.y < 0 && panChange.x > 0
                              || oldOffset.y == -yLimit && oldOffset.x == -xLimit && panChange.y < 0 && panChange.x < 0
                              || oldOffset.x == xLimit && panChange.x > 0 && abs(panChange.x / panChange.y) > 1
                              || oldOffset.x == -xLimit && panChange.x < 0 && abs(panChange.x / panChange.y) > 1
                            )
                              if (event.changes.size == 1)
                                break
                            firstCheck = true
                          }
                          offset = Offset(
                            (offset.x + panChange.x).coerceIn(-xLimit, xLimit),
                            (offset.y + panChange.y).coerceIn(-yLimit, yLimit)
                          )
                          // 이벤트를 소비하여 부모에게 전달하지 않음.
                          event.changes.forEach { it.consume() }
                        } else
                          offset = Offset.Zero
                      }
                      // scale이 1f이고 확대를 시작하는 상황이 아니라면 이벤트를 소비하지 않음.
                    }
                  }
                } while (!canceled && event.changes.any { it.pressed })
              }
            }
        ) {
          AsyncImage(
            model = ImageRequest.Builder(context)
              .data(hashToImageUrl(hash) + "?retry=$retryCount")
              .httpHeaders(hitomiHeaders)
              .memoryCacheKey(hash).diskCacheKey(hash)
              .build(),
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
            onSuccess = { state ->
              val size = state.painter.intrinsicSize
              imageSize = IntSize(size.width.toInt(), size.height.toInt())
              showImage = true
            },
            modifier = Modifier
              .fillMaxSize()
              .graphicsLayer(
                scaleX = scale, scaleY = scale,
                translationX = offset.x,
                translationY = offset.y
              )
          )
        }
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
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f))
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
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f))
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
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, null)
              else
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, null)
            }
            Button(
              shape = RoundedCornerShape(4.dp),
              onClick = { isUISelectDialogOpen.value = true }
            ) {
              Text("UI선택")
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
    // 스낵바가 그려질 호스트를 하단에 배치
    SnackbarHost(
      hostState = snackbarHostState,
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(bottom = 80.dp) // 하단 UI와 겹치지 않게 조절
    )
    AutoPlayDialog(isAutoPlayDialogOpen)
  }
}