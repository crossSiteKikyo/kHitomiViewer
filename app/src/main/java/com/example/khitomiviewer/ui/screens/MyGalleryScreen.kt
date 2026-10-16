package com.example.khitomiviewer.ui.screens

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.khitomiviewer.Screen
import com.example.khitomiviewer.ui.GalleryListBrief
import com.example.khitomiviewer.ui.GalleryListExtended
import com.example.khitomiviewer.ui.GalleryListGrid
import com.example.khitomiviewer.ui.GalleryListUiSelect
import com.example.khitomiviewer.ui.Pagination
import com.example.khitomiviewer.viewmodel.AppViewModel
import com.example.khitomiviewer.viewmodel.GalleryViewModelKeys
import com.example.khitomiviewer.viewmodel.ProvideGalleryViewModelKey
import com.example.khitomiviewer.viewmodel.VolumeKeyEvent
import com.example.khitomiviewer.viewmodel.activityGalleryViewModel
import kotlinx.coroutines.launch

@Composable
fun MyGalleryScreen(
  navController: NavHostController,
  verticalScrollState: ScrollState,
  isTagDialogOpen: MutableState<Boolean>,
  isGalleryDialogOpen: MutableState<Boolean>,
  isGalleryDetailDialogOpen: MutableState<Boolean>,
  page: Long
) {
  ProvideGalleryViewModelKey(GalleryViewModelKeys.MY_GALLERY) {
  // 전역 viewModel들
  val activity = LocalActivity.current as ComponentActivity
  val galleryViewModel = activityGalleryViewModel(GalleryViewModelKeys.MY_GALLERY)
  val appViewModel: AppViewModel = viewModel(activity)

  val galleryListUi by appViewModel.galleryListUi.collectAsState("Extended")
  val galleryLikeStatusOrder by appViewModel.galleryLikeStatusOrder.collectAsState("statusChangedAt")
  var showMissingGidDialog by remember { mutableStateOf(false) }

  val coroutineScope = rememberCoroutineScope()

  val onPageMove: (Long) -> Unit = { targetPage ->
    navController.navigate(Screen.MyGallery.createRoute(targetPage))
    coroutineScope.launch { verticalScrollState.scrollTo(0) }
  }

  LaunchedEffect(page, galleryLikeStatusOrder) {
    galleryViewModel.getLikeGalleries(page, galleryLikeStatusOrder)
  }

  LaunchedEffect(Unit) {
    galleryViewModel.setMaxPageLikeGalleries()
    // 볼륨 키 가로채기 활성화
    appViewModel.isPaginationActive.value = true
    // 볼륨 키 이벤트 구독
    appViewModel.volumeKeyEvent.collect { event ->
      if (appViewModel.isVolumeKeyPagingEnabled.value) {
        when (event) {
          VolumeKeyEvent.UP -> if (page > 1) onPageMove(page - 1)
          VolumeKeyEvent.DOWN -> if (page < galleryViewModel.maxPage) onPageMove(page + 1)
        }
      }
    }
  }

  Column(
    modifier = Modifier
        .fillMaxSize()
        .verticalScroll(verticalScrollState)
  ) {
    Row(
      modifier = Modifier
          .fillMaxWidth()
          .height(56.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
      Button(
        onClick = {
          appViewModel.setGalleryLikeStatusOrder("statusChangedAt")
        },
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(12.dp),
        enabled = galleryLikeStatusOrder != "statusChangedAt",
        contentPadding = PaddingValues(2.dp)
      ) {
        Text("상태 변경 날짜순")
      }
      Button(
        onClick = {
          appViewModel.setGalleryLikeStatusOrder("gId")
        },
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(12.dp),
        enabled = galleryLikeStatusOrder != "gId",
        contentPadding = PaddingValues(2.dp)
      ) {
        Text("gId순")
      }
    }
    Button(
      onClick = { showMissingGidDialog = true },
      modifier = Modifier
        .fillMaxWidth()
        .height(48.dp),
      shape = RoundedCornerShape(12.dp),
      contentPadding = PaddingValues(2.dp)
    ) {
      Text("목록에 없는 gid 추가")
    }
    GalleryListUiSelect()
    Pagination(false, page, galleryViewModel.maxPage, onPageMove)
    if (galleryListUi == "Extended")
      GalleryListExtended(navController, isTagDialogOpen, isGalleryDialogOpen)
    else if (galleryListUi == "Brief")
      GalleryListBrief(navController, isGalleryDialogOpen, isGalleryDetailDialogOpen)
    else
      GalleryListGrid(isGalleryDialogOpen, isGalleryDetailDialogOpen)
    Pagination(true, page, galleryViewModel.maxPage, onPageMove)
  }
  if (showMissingGidDialog) {
    MissingGalleryImportDialog(
      onImported = {
        galleryViewModel.getLikeGalleries(page, galleryLikeStatusOrder)
        galleryViewModel.setMaxPageLikeGalleries()
      },
      onDismiss = { showMissingGidDialog = false }
    )
  }
  }
}

@Composable
private fun MissingGalleryImportDialog(
  onImported: () -> Unit,
  onDismiss: () -> Unit
) {
  val galleryViewModel = activityGalleryViewModel(GalleryViewModelKeys.MY_GALLERY)
  var gidText by remember { mutableStateOf("") }
  var message by remember { mutableStateOf("") }
  var loading by remember { mutableStateOf(false) }
  val coroutineScope = rememberCoroutineScope()

  AlertDialog(
    onDismissRequest = { if (!loading) onDismiss() },
    confirmButton = {
      TextButton(
        enabled = !loading,
        onClick = {
          if (loading) return@TextButton
          val gId = gidText.toLongOrNull()
          if (gId == null || gId <= 0L) {
            message = "숫자 gid를 입력하세요."
            return@TextButton
          }
          loading = true
          message = "가져오는 중"
          coroutineScope.launch {
            val result = galleryViewModel.importMissingGalleryAsLiked(gId)
            message = result.message
            loading = false
            if (result.saved) onImported()
          }
        }
      ) {
        Text(if (loading) "가져오는 중" else "확인")
      }
    },
    dismissButton = {
      TextButton(enabled = !loading, onClick = onDismiss) {
        Text("닫기")
      }
    },
    title = { Text("목록에 없는 gid") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("이 앱이 수집한 한국어 작품은 이미 목록에 있습니다. 거기에 없는 작품의 gid만 입력하세요. 저장하면 좋아요가 됩니다.")
        OutlinedTextField(
          value = gidText,
          onValueChange = { value -> gidText = value.filter { it.isDigit() } },
          label = { Text("gid") },
          singleLine = true,
          enabled = !loading,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.fillMaxWidth()
        )
        if (message.isNotEmpty()) {
          Text(message)
        }
      }
    }
  )
}