package com.example.khitomiviewer.ui

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
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
@Composable
fun ScrollMangaView(
  gId: Long,
  hashToImageUrl: (String) -> String,
  imageHashes: SnapshotStateList<String>,
  isUISelectDialogOpen: MutableState<Boolean>
) {
  // 전역 viewModel들
  val activity = LocalActivity.current as ComponentActivity
  val viewMangaViewModel: ViewMangaViewModel = viewModel(activity)
  val appViewModel: AppViewModel = viewModel(activity)

  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }
  val isAutoPlayDialogOpen = remember { mutableStateOf(false) }
  var isUiVisible by remember { mutableStateOf(false) }

  val listState = rememberLazyListState()
  val currentPage by remember { derivedStateOf { listState.firstVisibleItemIndex } }
  val coroutineScope = rememberCoroutineScope()

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
  // 자동넘기기 타이머 로직
  LaunchedEffect(viewMangaViewModel.isAutoPlaying.value, currentPage) {
    if (viewMangaViewModel.isAutoPlaying.value) {
      delay(viewMangaViewModel.tempPeriod.value.toInt() * 1000L)
      val isLastPage = currentPage == imageHashes.size - 1
      if (isLastPage) {
        if (viewMangaViewModel.tempIsLoop.value)
          listState.scrollToItem(0)
        else
          viewMangaViewModel.isAutoPlaying.value = false
      } else
        listState.scrollToItem(currentPage + 1)
    }
  }
  Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
      state = listState,
      modifier = Modifier
        .fillMaxSize()
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
        ) {
          isUiVisible = !isUiVisible
        },
      verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
      items(
        imageHashes.size,
//        key = { idx -> imageHashes[idx] } // 중복되는 hash값이 있다면 오류가 난다.
      ) { page ->
        val hash = imageHashes[page]

        var retryCount by remember { mutableIntStateOf(0) }
        var shouldRetry by remember { mutableStateOf(false) }

        // 이미지가 로드되었는지 여부와 비율을 기억합니다.
        var isLoaded by remember { mutableStateOf(false) }
        var aspectRatio by remember { mutableFloatStateOf(0f) }

        // 확대/축소 및 이동 상태 변수
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        Box(
          modifier = Modifier
            .fillMaxWidth()
            //이미 로드된 적이 있다면 비율을 유지하여 스크롤 점프 방지
            .then(
              if (aspectRatio > 0f) Modifier.aspectRatio(aspectRatio)
              else Modifier.heightIn(min = 300.dp)
            )
            .clipToBounds()
            .pointerInput(Unit) {
              val touchSlop = viewConfiguration.touchSlop
              // 확대/축소 및 이동 처리
              awaitEachGesture {  // 화면에 손가락을 대고(Down) 뗄 때까지(Up)의 한세트를 하나의 제스처로 본다.
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
                          val extraWidth = (scale - 1) * size.width
                          val extraHeight = (scale - 1) * size.height
                          val xLimit = extraWidth / 2
                          val yLimit = extraHeight / 2
                          // 한손가락으로, 맨끝에서 더 움직이는거면 break한다.
                          if (!firstCheck) {
                            if (oldOffset.y == yLimit && oldOffset.x == xLimit && panChange.y > 0 && panChange.x > 0
                              || oldOffset.y == yLimit && oldOffset.x == -xLimit && panChange.y > 0 && panChange.x < 0
                              || oldOffset.y == -yLimit && oldOffset.x == xLimit && panChange.y < 0 && panChange.x > 0
                              || oldOffset.y == -yLimit && oldOffset.x == -xLimit && panChange.y < 0 && panChange.x < 0
                              || oldOffset.y == yLimit && panChange.y > 0 && abs(panChange.y / panChange.x) > 1
                              || oldOffset.y == -yLimit && panChange.y < 0 && abs(panChange.y / panChange.x) > 1
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
            .graphicsLayer(
              scaleX = scale, scaleY = scale,
              translationX = offset.x,
              translationY = offset.y
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
          value = currentPage.toFloat(),
          onValueChange = { v ->
            coroutineScope.launch {
              listState.scrollToItem(v.toInt())
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
        Text("${currentPage + 1}/${imageHashes.size}")
        Row(
          horizontalArrangement = Arrangement.SpaceEvenly,
          modifier = Modifier.fillMaxWidth()
        ) {
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