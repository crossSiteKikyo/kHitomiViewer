package com.example.khitomiviewer.ui

import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import com.example.khitomiviewer.R
import com.example.khitomiviewer.Screen
import com.example.khitomiviewer.ui.tag.MainTag
import com.example.khitomiviewer.ui.tag.SubTag
import com.example.khitomiviewer.util.decodeThumbnail
import com.example.khitomiviewer.util.hitomiHeaders
import com.example.khitomiviewer.viewmodel.AppViewModel
import com.example.khitomiviewer.viewmodel.DialogViewModel
import com.example.khitomiviewer.viewmodel.GalleryViewModel
import com.example.khitomiviewer.viewmodel.HitomiViewModel
import com.example.khitomiviewer.viewmodel.ViewMangaViewModel
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.abs

@Composable
fun GalleryListExtended(
  navController: NavHostController,
  isTagDialogOpen: MutableState<Boolean>,
  isGalleryDialogOpen: MutableState<Boolean>
) {
  // 전역 viewModel들
  val activity = LocalActivity.current as ComponentActivity
  val hitomiViewModel: HitomiViewModel = viewModel(activity)
  val galleryViewModel: GalleryViewModel = viewModel(activity)
  val dialogViewModel: DialogViewModel = viewModel(activity)
  val viewMangaViewModel: ViewMangaViewModel = viewModel(activity)
  val appViewModel: AppViewModel = viewModel(activity)

  val isAvifFormat by appViewModel.isAvifFormat.collectAsState(true)

  val galleries by remember { derivedStateOf { galleryViewModel.galleries } }

  val typeColor: Map<String, Long> = remember {
    mapOf<String, Long>(
      "doujinshi" to 0xFFCC9999,  // c99
      "manga" to 0xFFCC99CC,      // c9c
      "artistcg" to 0xFF99CCCC,  // 9cc
      "gamecg" to 0xFF9999CC,    // 99c
      "imageset" to 0xFF999999,  // 999
      // CC보다 크면 안되고 66보다 작으면 안된다.
    )
  }
  val context = LocalContext.current

  if (galleries.isEmpty())
    Text("결과가 없습니다", style = TextStyle(fontSize = 50.sp))
  else {
    // 갤러리마다 카드 만들기
    for (g in galleries) {
      val cardBorderColor: Color = when (g.likeStatus) {
        0 -> Color.Gray
        2 -> Color(0xFFCC00CC)
        else -> Color.Black
      }
      Card(
        shape = RoundedCornerShape(5.dp),
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 7.dp),
        border = BorderStroke(2.dp, cardBorderColor)
      ) {
        Column(
          modifier = Modifier.fillMaxWidth()
        ) {
          val titleColorLong = typeColor[g.typeName] ?: 0xFFEE44EE
          val titleColor = Color(titleColorLong)
          val typeBgColor = Color(titleColorLong + 0x333333)
          val textColor = Color(titleColorLong - 0x666666)
          // 제목
          SelectionContainer {
            Text(
              g.title,
              maxLines = 6,
              modifier = Modifier
                .fillMaxWidth()
                .background(titleColor)
                .padding(2.dp),
              color = Color.White,
              style = TextStyle(
                shadow = Shadow(
                  Color(0xFF000000),
                  blurRadius = 7f
                ), fontSize = 21.sp
              )
            )
          }
          // 타입
          Text(
            "종류: ${g.typeName}",
            color = textColor,
            modifier = Modifier
              .fillMaxWidth()
              .background(typeBgColor)
              .padding(horizontal = 3.dp)
          )
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(1.dp)
              .background(color = titleColor)
          )
          // 작가
          val artists =
            g.tags.filter { tag -> tag.name.startsWith("artist:") }
          if (artists.isNotEmpty()) {
            FlowRow(
              modifier = Modifier
                .fillMaxWidth()
                .background(color = typeBgColor)
                .padding(horizontal = 3.dp)
            ) {
              Text("작가: ", color = textColor)
              for (i in artists.indices) {
                if (i != 0)
                  Text(", ", color = textColor)
                MainTag(
                  artists[i],
                  textColor,
                  dialogViewModel,
                  isTagDialogOpen,
                  navController
                )
              }
            }
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(color = titleColor)
            )
          }
          // 그룹
          val groups = g.tags.filter { tag -> tag.name.startsWith("group:") }
          if (groups.isNotEmpty()) {
            FlowRow(
              modifier = Modifier
                .fillMaxWidth()
                .background(color = typeBgColor)
                .padding(horizontal = 3.dp)
            ) {
              Text("그룹: ", color = textColor)
              for (i in groups.indices) {
                if (i != 0)
                  Text(", ", color = textColor)
                MainTag(
                  groups[i],
                  textColor,
                  dialogViewModel,
                  isTagDialogOpen,
                  navController
                )
              }
            }
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(color = titleColor)
            )
          }
          // 시리즈
          val parodies =
            g.tags.filter { tag -> tag.name.startsWith("parody:") }
          if (parodies.isNotEmpty()) {
            FlowRow(
              modifier = Modifier
                .fillMaxWidth()
                .background(color = typeBgColor)
                .padding(horizontal = 3.dp)
            ) {
              Text("시리즈: ", color = textColor)
              for (i in parodies.indices) {
                if (i != 0)
                  Text(", ", color = textColor)
                MainTag(
                  parodies[i],
                  textColor,
                  dialogViewModel,
                  isTagDialogOpen,
                  navController
                )
              }
            }
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(color = titleColor)
            )
          }
          // 캐릭터
          val characters =
            g.tags.filter { tag -> tag.name.startsWith("character:") }
          if (characters.isNotEmpty()) {
            FlowRow(
              modifier = Modifier
                .fillMaxWidth()
                .background(color = typeBgColor)
                .padding(horizontal = 3.dp)
            ) {
              Text("캐릭터: ", color = textColor)
              for (i in characters.indices) {
                if (i != 0)
                  Text(", ", color = textColor)
                MainTag(
                  characters[i],
                  textColor,
                  dialogViewModel,
                  isTagDialogOpen,
                  navController
                )
              }
            }
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(color = titleColor)
            )
          }
          // 확대/축소 및 이동 상태 변수
          var scale by remember { mutableFloatStateOf(1f) }
          var offset by remember { mutableStateOf(Offset.Zero) }
          // 이미지 미리보기
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .fillMaxWidth()
              .clipToBounds()
              .combinedClickable(
                onClick = {
                  // ggjs정보가 없는데 보면 에러남.
                  if (hitomiViewModel.mList.isNotEmpty()) {
                    viewMangaViewModel.imagesLoading.value = true
                    navController.navigate(Screen.ViewManga.createRoute(g.gId))
                  } else
                    Toast.makeText(
                      context,
                      "서버 정보를 받아오는중입니다. 잠시 기다려주세요",
                      Toast.LENGTH_SHORT
                    ).show()
                },
                onLongClick = {
                  dialogViewModel.setGallery(g.gId)
                  isGalleryDialogOpen.value = true
                }
              )
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
          ) {

            AsyncImage(
              model = ImageRequest.Builder(context)
                .data(
                  decodeThumbnail(
                    g.thumb1,
                    hitomiViewModel.mList,
                    hitomiViewModel.thumbChar2.value,
                    hitomiViewModel.thumbChar1.value,
                    isAvifFormat
                  )
                )
                .diskCacheKey(g.thumb1).httpHeaders(hitomiHeaders)
                .build(),
              contentDescription = "thumbnail",
              placeholder = painterResource(R.drawable.loading),
              error = painterResource(R.drawable.errorimg),
              onError = { e -> Log.i("섬네일 에러", e.toString()) },
              contentScale = ContentScale.Crop,
              alignment = Alignment.TopStart,
              modifier = Modifier
                .fillMaxWidth(0.5f)
                .border(1.dp, Color.Black)


            )
            AsyncImage(
              model = ImageRequest.Builder(context)
                .data(
                  decodeThumbnail(
                    g.thumb2,
                    hitomiViewModel.mList,
                    hitomiViewModel.thumbChar2.value,
                    hitomiViewModel.thumbChar1.value,
                    isAvifFormat
                  )
                )
                .diskCacheKey(g.thumb2).httpHeaders(hitomiHeaders)
                .build(),
              contentDescription = "thumbnail",
              placeholder = painterResource(R.drawable.loading),
              error = painterResource(R.drawable.errorimg),
              onError = { e -> Log.i("섬네일 에러", e.toString()) },
              contentScale = ContentScale.Crop,
              alignment = Alignment.TopStart,
              modifier = Modifier
                .fillMaxWidth(1f)
                .border(1.dp, Color.Black),
            )
          }
          // 나머지 태그들
          val males = g.tags.filter { tag -> tag.name.startsWith("male:") }
          val females =
            g.tags.filter { tag -> tag.name.startsWith("female:") }
          val others = g.tags.filter { tag ->
            (!tag.name.startsWith("artist:") && !tag.name.startsWith("group:") && !tag.name.startsWith(
              "parody:"
            )
                    && !tag.name.startsWith("character:") && !tag.name.startsWith(
              "male:"
            ) && !tag.name.startsWith("female:"))
          }
          FlowRow(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(3.dp)
          ) {
            for (tag in males) {
              SubTag(tag, dialogViewModel, isTagDialogOpen, navController)
            }
            for (tag in females) {
              SubTag(tag, dialogViewModel, isTagDialogOpen, navController)
            }
            for (tag in others) {
              SubTag(tag, dialogViewModel, isTagDialogOpen, navController)
            }
          }
          // 갤러리id   포스트 시간   페이지 수
          SelectionContainer {
            Row(
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()
            ) {
              // 갤러리 id
              Text(
                "${g.gId}",
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 5.dp)
              )
              // 포스트 시간
              Text(
                formatSavedDate(g.date),
                color = titleColor,
                fontWeight = FontWeight.Bold,
              )
              // 페이지 수
              Text(
                "${g.filecount}p",
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 5.dp)
              )
            }
          }
          // 기록이 있다면. 기록삭제, 마지막기록시간, 보던페이지
          if (g.lastReadAt > 0) {
            HorizontalDivider(Modifier.fillMaxWidth())
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                "기록삭제",
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier
                  .padding(horizontal = 5.dp)
                  .background(Color(0x77FF0000), RoundedCornerShape(3.dp))
                  .clickable(onClick = {
                    galleryViewModel.resetGalleryRecord(g.gId)
                    dialogViewModel.galleryDetailReloading()
                  })
              )
              Text("기록:")
              Text(formatLongDate(g.lastReadAt))
              Text(
                "${g.lastReadPage}p",
                modifier = Modifier.padding(horizontal = 5.dp)
              )
            }
          }
        }
      }
    }
  }
}

fun formatSavedDate(dateString: String): String {
  // 1. 저장된 문자열의 패턴 정의 (연-월-일 시:분:초-오프셋)
  // 참고: -06 같은 오프셋은 'X'나 'x'로 처리합니다.
  val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssX", Locale.ENGLISH)

  // 2. 문자열을 OffsetDateTime 객체로 파싱
  val parsedDate = OffsetDateTime.parse(dateString, inputFormatter)

  // 3. 사용자의 시스템 타임존으로 변경 (예: 한국이면 +09:00로 자동 변환)
  val localDate = parsedDate.atZoneSameInstant(java.time.ZoneId.systemDefault())

  // 4. JS의 toLocaleString()처럼 보기 편한 포맷으로 출력
  // FormatStyle.MEDIUM은 "2007. 11. 18. 오전 9:19:00" 같은 식입니다.
  val outputFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
    .withLocale(Locale.getDefault())

  return localDate.format(outputFormatter)
}

fun formatLongDate(lastReadAt: Long): String {
  // 1. Long(ms)을 Instant 객체로 변환 (UTC 기준점)
  val instant = Instant.ofEpochMilli(lastReadAt)
  // 2. 사용자의 시스템 타임존으로 변경
  val zonedDateTime = instant.atZone(ZoneId.systemDefault())
  // 3. 기존에 사용하시던 포맷터 설정 그대로 적용
  // FormatStyle.MEDIUM -> "2026. 3. 28. 오후 10:41:00" (한국 기준)
  val outputFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
    .withLocale(Locale.getDefault())

  return zonedDateTime.format(outputFormatter)
}