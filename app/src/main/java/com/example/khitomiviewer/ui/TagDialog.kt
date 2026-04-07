package com.example.khitomiviewer.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.twotone.ThumbDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.khitomiviewer.viewmodel.AppViewModel
import com.example.khitomiviewer.viewmodel.DialogViewModel
import com.example.khitomiviewer.viewmodel.GalleryViewModel
import com.example.khitomiviewer.viewmodel.SearchViewModel
import com.example.khitomiviewer.viewmodel.TagViewModel

@Composable
fun TagDialog(isTagDialogOpen: MutableState<Boolean>) {
  // 전역 viewModel
  val activity = LocalActivity.current as ComponentActivity
  val dialogViewModel: DialogViewModel = viewModel(activity)
  val galleryViewModel: GalleryViewModel = viewModel(activity)
  val tagViewModel: TagViewModel = viewModel(activity)
  val searchViewModel: SearchViewModel = viewModel(activity)

  // 다이얼로그
  if (isTagDialogOpen.value) {
    AlertDialog(
      onDismissRequest = {},
      confirmButton = {
        TextButton(onClick = { isTagDialogOpen.value = false }) {
          Text("닫기")
        }
      },
      title = {
        Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text("태그 상태 변경")
        }
      },
      text = {
        Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text("${dialogViewModel.selectedTag?.name}")
          if (dialogViewModel.selectedTag?.koreanName != null)
            Text("${dialogViewModel.selectedTag?.koreanName}")
          Button(
            enabled = dialogViewModel.selectedTag?.likeStatus != 0,
            onClick = {
              dialogViewModel.changeTagLike(0)    // db에 상태 변경
              isTagDialogOpen.value = false   // 다이얼로그 닫기
              galleryViewModel.galleryReLoading() // 갤러리 재로딩
              tagViewModel.tagReLoading() // 태그 재로딩
              dialogViewModel.galleryDetailReloading() //갤러리 디테일 재로딩
              searchViewModel.currentSearchTagsReLoading() //태그 검색 결과 재로딩
            }
          ) {
            Row {
              Text("싫어요")
              Icon(Icons.Outlined.Block, null)
            }
          }
          Button(
            enabled = dialogViewModel.selectedTag?.likeStatus != 1,
            onClick = {
              dialogViewModel.changeTagLike(1)    // db에 상태 변경
              isTagDialogOpen.value = false   // 다이얼로그 닫기
              galleryViewModel.galleryReLoading() // 갤러리 재로딩
              tagViewModel.tagReLoading() // 태그 재로딩
              dialogViewModel.galleryDetailReloading() //갤러리 디테일 재로딩
              searchViewModel.currentSearchTagsReLoading() //태그 검색 결과 재로딩
            }
          ) {
            Row {
              Text("상태 없음")
            }
          }
          Button(
            enabled = dialogViewModel.selectedTag?.likeStatus != 2,
            onClick = {
              dialogViewModel.changeTagLike(2)    // db에 상태 변경
              isTagDialogOpen.value = false   // 다이얼로그 닫기
              galleryViewModel.galleryReLoading() // 갤러리 재로딩
              tagViewModel.tagReLoading() // 태그 재로딩
              dialogViewModel.galleryDetailReloading() //갤러리 디테일 재로딩
              searchViewModel.currentSearchTagsReLoading() //태그 검색 결과 재로딩
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