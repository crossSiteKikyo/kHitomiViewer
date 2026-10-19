package com.example.khitomiviewer.ui

import android.annotation.SuppressLint
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.first
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.network.httpHeaders
import com.example.khitomiviewer.R
import com.example.khitomiviewer.util.animatedImageLoader
import com.example.khitomiviewer.util.hitomiHeaders
import com.example.khitomiviewer.util.previewThumbnailUrl
import com.example.khitomiviewer.viewmodel.AppViewModel
import com.example.khitomiviewer.viewmodel.DialogViewModel
import com.example.khitomiviewer.viewmodel.GalleryPreviewFile
import com.example.khitomiviewer.viewmodel.HitomiViewModel
import com.example.khitomiviewer.viewmodel.activityGalleryViewModelOrNull

private const val PreviewPageSize = 9

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryDialog(
  navController: NavHostController,
  isTagDialogOpen: MutableState<Boolean>,
  isGalleryDialogOpen: MutableState<Boolean>
) {
  // 전역 viewModel
  val activity = LocalActivity.current as ComponentActivity
  val dialogViewModel: DialogViewModel = viewModel(activity)
  val galleryViewModel = activityGalleryViewModelOrNull()
  val hitomiViewModel: HitomiViewModel = viewModel(activity)
  val appViewModel: AppViewModel = viewModel(activity)
  val isAvifFormat by appViewModel.isAvifFormat.collectAsState(false)
  val galleryPreviewEnabled by appViewModel.galleryPreviewEnabled.collectAsState(true)
  val galleryStatusChangeEnabled by appViewModel.galleryStatusChangeEnabled.collectAsState(true)
  val galleryRelatedEnabled by appViewModel.galleryRelatedEnabled.collectAsState(true)
  val mList = hitomiViewModel.mList.toList()

  // 다이얼로그
  if (isGalleryDialogOpen.value) {
    BasicAlertDialog(
      onDismissRequest = { isGalleryDialogOpen.value = false },
      properties = DialogProperties(usePlatformDefaultWidth = false),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
      ) {
        val scrollState = rememberScrollState()
        LaunchedEffect(dialogViewModel.selectedGallery?.gId) {
          snapshotFlow { dialogViewModel.previewLoading }.first { !it }
          scrollState.scrollTo(0)
        }
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .clickable { isGalleryDialogOpen.value = false },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Text("닫기", fontSize = 20.sp)
          }
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .padding(horizontal = 10.dp)
              .clickable {
                appViewModel.setGalleryPreviewEnabled(!galleryPreviewEnabled)
              },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("미리보기", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Switch(
              checked = galleryPreviewEnabled,
              onCheckedChange = { appViewModel.setGalleryPreviewEnabled(it) }
            )
          }
          if (galleryPreviewEnabled) {
            GalleryPreviewPager(
              gId = dialogViewModel.selectedGallery?.gId,
              files = dialogViewModel.previewFiles,
              loading = dialogViewModel.previewLoading,
              error = dialogViewModel.previewError,
              useAvif = isAvifFormat,
              mList = mList,
              thumbChar2 = hitomiViewModel.thumbChar2.value,
              thumbChar1 = hitomiViewModel.thumbChar1.value
            )
          }
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .padding(horizontal = 10.dp)
              .clickable {
                appViewModel.setGalleryStatusChangeEnabled(!galleryStatusChangeEnabled)
              },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("갤러리 상태 변경", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Switch(
              checked = galleryStatusChangeEnabled,
              onCheckedChange = { appViewModel.setGalleryStatusChangeEnabled(it) }
            )
          }
          if (galleryStatusChangeEnabled) {
            Text(
              "${dialogViewModel.selectedGallery?.gId}",
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
              textAlign = TextAlign.Start
            )
            Text(
              "${dialogViewModel.selectedGallery?.title}",
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
              textAlign = TextAlign.Start
            )
            Row(
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Button(
                enabled = dialogViewModel.selectedGallery?.likeStatus != 0,
                onClick = {
                  dialogViewModel.changeGalleryLike(0)
                  isGalleryDialogOpen.value = false
                  galleryViewModel?.galleryReLoading()
                  dialogViewModel.galleryDetailReloading()
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
              ) {
                Text("싫어요")
                Icon(Icons.Outlined.Block, null)
              }
              Button(
                enabled = dialogViewModel.selectedGallery?.likeStatus != 1,
                onClick = {
                  dialogViewModel.changeGalleryLike(1)
                  isGalleryDialogOpen.value = false
                  galleryViewModel?.galleryReLoading()
                  dialogViewModel.galleryDetailReloading()
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
              ) {
                Text("상태 없음")
              }
              Button(
                enabled = dialogViewModel.selectedGallery?.likeStatus != 2,
                onClick = {
                  dialogViewModel.changeGalleryLike(2)
                  isGalleryDialogOpen.value = false
                  galleryViewModel?.galleryReLoading()
                  dialogViewModel.galleryDetailReloading()
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
              ) {
                Text("좋아요")
                Icon(Icons.Outlined.ThumbUp, null)
              }
            }
          }
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .padding(horizontal = 10.dp)
              .clickable {
                appViewModel.setGalleryRelatedEnabled(!galleryRelatedEnabled)
              },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("관련된 갤러리", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Switch(
              checked = galleryRelatedEnabled,
              onCheckedChange = { appViewModel.setGalleryRelatedEnabled(it) }
            )
          }
          if (galleryRelatedEnabled) {
            when {
              dialogViewModel.previewLoading && dialogViewModel.relatedGalleries.isEmpty() -> {
                CircularProgressIndicator(modifier = Modifier.padding(bottom = 8.dp))
              }

              dialogViewModel.previewError != null && dialogViewModel.relatedGalleries.isEmpty() -> {
                Text(
                  "관련된 갤러리를 가져오지 못했습니다.",
                  modifier = Modifier.padding(bottom = 8.dp)
                )
              }

              dialogViewModel.relatedGalleries.isEmpty() -> {
                Text(
                  "관련된 갤러리가 없습니다",
                  modifier = Modifier.padding(bottom = 8.dp)
                )
              }

              else -> {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                ) {
                  for (gallery in dialogViewModel.relatedGalleries) {
                    key(gallery.gId) {
                      GalleryExtendedCard(
                        g = gallery,
                        navController = navController,
                        isTagDialogOpen = isTagDialogOpen,
                        isGalleryDialogOpen = isGalleryDialogOpen
                      )
                    }
                  }
                }
              }
            }
          }
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .clickable { isGalleryDialogOpen.value = false },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Text("닫기", fontSize = 20.sp)
          }
        }
      }
    }
  }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun GalleryPreviewPager(
  gId: Long?,
  files: List<GalleryPreviewFile>,
  loading: Boolean,
  error: String?,
  useAvif: Boolean,
  mList: List<String>,
  thumbChar2: String,
  thumbChar1: String
) {
  when {
    loading && files.isEmpty() -> {
      CircularProgressIndicator(modifier = Modifier.padding(bottom = 8.dp))
    }

    error != null && files.isEmpty() -> {
      Text(error, modifier = Modifier.padding(bottom = 8.dp))
    }

    files.isNotEmpty() -> {
      val pageCount = (files.size + PreviewPageSize - 1) / PreviewPageSize
      key(gId) {
        val pagerState = rememberPagerState { pageCount }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val rowGap = 4.dp
            val cellWidth = (maxWidth - 4.dp) / 3
            val gridHeight = (0 until pageCount).maxOf { page ->
              var total = 0.dp
              var rowCount = 0
              for (row in 0 until 3) {
                var rowHeight = 0.dp
                var hasFile = false
                for (column in 0 until 3) {
                  val index = page * PreviewPageSize + row * 3 + column
                  val file = files.getOrNull(index) ?: continue
                  hasFile = true
                  val height = if (file.width > 0 && file.height > 0) {
                    cellWidth * file.height.toFloat() / file.width
                  } else {
                    cellWidth * 3f / 2f
                  }
                  if (height > rowHeight) rowHeight = height
                }
                if (hasFile) {
                  total += rowHeight
                  rowCount++
                }
              }
              if (rowCount > 1) total + rowGap * (rowCount - 1) else total
            }
            HorizontalPager(
              state = pagerState,
              beyondViewportPageCount = 2,
              modifier = Modifier.height(gridHeight)
            ) { page ->
              Column(verticalArrangement = Arrangement.spacedBy(rowGap)) {
                for (row in 0 until 3) {
                  if ((0 until 3).all { column ->
                      files.getOrNull(page * PreviewPageSize + row * 3 + column) == null
                    }
                  ) continue
                  Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    for (column in 0 until 3) {
                      val index = page * PreviewPageSize + row * 3 + column
                      val file = files.getOrNull(index)
                      if (file != null) {
                        val ratio = if (file.width > 0 && file.height > 0) {
                          file.width.toFloat() / file.height
                        } else {
                          2f / 3f
                        }
                        PreviewThumbnail(
                          file = file,
                          useAvif = useAvif,
                          mList = mList,
                          thumbChar2 = thumbChar2,
                          thumbChar1 = thumbChar1,
                          modifier = Modifier
                            .weight(1f)
                            .aspectRatio(ratio)
                        )
                      } else {
                        Spacer(modifier = Modifier.weight(1f))
                      }
                    }
                  }
                }
              }
            }
          }
          if (pageCount > 1) {
            Text(
              "${pagerState.currentPage + 1} / $pageCount",
              modifier = Modifier.padding(vertical = 4.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
private fun PreviewThumbnail(
  file: GalleryPreviewFile,
  useAvif: Boolean,
  mList: List<String>,
  thumbChar2: String,
  thumbChar1: String,
  modifier: Modifier
) {
  val context = LocalContext.current
  val url = previewThumbnailUrl(
    hash = file.hash,
    useAvif = useAvif && file.hasAvif,
    mList = mList,
    thumbChar2 = thumbChar2,
    thumbChar1 = thumbChar1
  )
  AsyncImage(
    model = ImageRequest.Builder(context)
      .data(url)
      .diskCacheKey(url)
      .httpHeaders(hitomiHeaders)
      .build(),
    contentDescription = "미리보기",
    imageLoader = animatedImageLoader(context),
    placeholder = painterResource(R.drawable.loading),
    error = painterResource(R.drawable.errorimg),
    contentScale = ContentScale.FillWidth,
    alignment = Alignment.TopStart,
    modifier = modifier
  )
}