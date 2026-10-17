package com.example.khitomiviewer.ui

import android.annotation.SuppressLint
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.network.httpHeaders
import com.example.khitomiviewer.R
import com.example.khitomiviewer.util.hitomiHeaders
import com.example.khitomiviewer.util.previewThumbnailUrl
import com.example.khitomiviewer.viewmodel.AppViewModel
import com.example.khitomiviewer.viewmodel.DialogViewModel
import com.example.khitomiviewer.viewmodel.GalleryPreviewFile
import com.example.khitomiviewer.viewmodel.HitomiViewModel
import com.example.khitomiviewer.viewmodel.activityGalleryViewModelOrNull

private const val PreviewPageSize = 9

@Composable
fun GalleryDialog(isGalleryDialogOpen: MutableState<Boolean>) {
  // 전역 viewModel
  val activity = LocalActivity.current as ComponentActivity
  val dialogViewModel: DialogViewModel = viewModel(activity)
  val galleryViewModel = activityGalleryViewModelOrNull()
  val hitomiViewModel: HitomiViewModel = viewModel(activity)
  val appViewModel: AppViewModel = viewModel(activity)
  val isAvifFormat by appViewModel.isAvifFormat.collectAsState(false)
  val mList = hitomiViewModel.mList.toList()

  // 다이얼로그
  if (isGalleryDialogOpen.value) {
    AlertDialog(
      onDismissRequest = {},
      confirmButton = {
        TextButton(onClick = { isGalleryDialogOpen.value = false }) {
          Text("닫기")
        }
      },
      title = null,
      text = {
        Column(
          modifier = Modifier
              .fillMaxWidth()
              .verticalScroll(rememberScrollState()),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
          Text("갤러리 상태 변경")
          Text("${dialogViewModel.selectedGallery?.gId}")
          Text("${dialogViewModel.selectedGallery?.title}")
          Button(
            enabled = dialogViewModel.selectedGallery?.likeStatus != 0,
            onClick = {
              dialogViewModel.changeGalleryLike(0)    // db에 상태 변경
              isGalleryDialogOpen.value = false   // 다이얼로그 닫기
              galleryViewModel?.galleryReLoading() // 갤러리 재로딩
              dialogViewModel.galleryDetailReloading() //갤러리 디테일 재로딩
            }
          ) {
            Row {
              Text("싫어요")
              Icon(Icons.Outlined.Block, null)
            }
          }
          Button(
            enabled = dialogViewModel.selectedGallery?.likeStatus != 1,
            onClick = {
              dialogViewModel.changeGalleryLike(1)    // db에 상태 변경
              isGalleryDialogOpen.value = false   // 다이얼로그 닫기
              galleryViewModel?.galleryReLoading() // 갤러리 재로딩
              dialogViewModel.galleryDetailReloading() //갤러리 디테일 재로딩
            }
          ) {
            Row {
              Text("상태 없음")
            }
          }
          Button(
            enabled = dialogViewModel.selectedGallery?.likeStatus != 2,
            onClick = {
              dialogViewModel.changeGalleryLike(2)    // db에 상태 변경
              isGalleryDialogOpen.value = false   // 다이얼로그 닫기
              galleryViewModel?.galleryReLoading() // 갤러리 재로딩
              dialogViewModel.galleryDetailReloading() //갤러리 디테일 재로딩
            }
          ) {
            Row {
              Text("좋아요")
              Icon(Icons.Outlined.ThumbUp, null)
            }
          }
        }
      }
    )
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
            val cellWidth = (maxWidth - 4.dp) / 3
            val gridHeight = cellWidth * (3f / 2f) * 3 + 4.dp
            HorizontalPager(
              state = pagerState,
              modifier = Modifier.height(gridHeight)
            ) { page ->
              Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                for (row in 0 until 3) {
                  Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    for (column in 0 until 3) {
                      val index = page * PreviewPageSize + row * 3 + column
                      if (index < files.size) {
                        PreviewThumbnail(
                          file = files[index],
                          useAvif = useAvif,
                          mList = mList,
                          thumbChar2 = thumbChar2,
                          thumbChar1 = thumbChar1,
                          modifier = Modifier
                              .weight(1f)
                              .aspectRatio(2f / 3f)
                        )
                      } else {
                        Spacer(
                          modifier = Modifier
                              .weight(1f)
                              .aspectRatio(2f / 3f)
                        )
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
    placeholder = painterResource(R.drawable.loading),
    error = painterResource(R.drawable.errorimg),
    contentScale = ContentScale.Crop,
    alignment = Alignment.TopStart,
    modifier = modifier
  )
}