package com.example.khitomiviewer.ui.screens

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.network.NetworkHeaders
import com.example.khitomiviewer.ui.ScrollMangaView
import com.example.khitomiviewer.ui.SwipeMangaView
import com.example.khitomiviewer.ui.SwipeVerticalMangaView
import com.example.khitomiviewer.ui.UISelectDialog
import com.example.khitomiviewer.viewmodel.AppViewModel
import com.example.khitomiviewer.viewmodel.HitomiViewModel
import com.example.khitomiviewer.viewmodel.ViewMangaViewModel

@Composable
fun ViewMangaScreen(
  navController: NavController,
  gId: Long
) {
  // 전역 viewModel들
  val activity = LocalActivity.current as ComponentActivity
  val hitomiViewModel: HitomiViewModel = viewModel(activity)
  val viewMangaViewModel: ViewMangaViewModel = viewModel(activity)
  val appViewModel: AppViewModel = viewModel(activity)

  val isAvifFormat by appViewModel.isAvifFormat.collectAsState(true)

  val viewMethod by viewMangaViewModel.viewMethod.collectAsState("swipe")
  val isUISelectDialogOpen = remember { mutableStateOf(false) }

  val hashToImageUrl: (String) -> String = { hash ->
    val o1 = hitomiViewModel.o1.value
    val o2 = hitomiViewModel.o2.value
    val s = "${hash[hash.length - 1]}${hash[hash.length - 3]}${hash[hash.length - 2]}".toInt(16)
      .toString(10)
    val subdomainNum = if (o1 == null || o2 == null) 1 else
      (if (s in hitomiViewModel.mList) o2.toInt() else o1.toInt()) + 1
    // 구형폰에서는 avif 디코딩을 지원하지 않는거 같다. webp로 하자.어쩔 수 없다.
    val webpUrl =
      "https://w${subdomainNum}.gold-usergeneratedcontent.net/${hitomiViewModel.b.value}${s}/${hash}.webp"
    val avifUrl =
      "https://a${subdomainNum}.gold-usergeneratedcontent.net/${hitomiViewModel.b.value}${s}/${hash}.avif"
    if (isAvifFormat) avifUrl else webpUrl
  }

  val imageHashes = viewMangaViewModel.imageHashes
  var imagesLoading by viewMangaViewModel.imagesLoading

  LaunchedEffect(gId) {
    viewMangaViewModel.setGalleryImages(gId)
  }

  LaunchedEffect(Unit) {
    // 볼륨 키 가로채기 활성화
    appViewModel.isPaginationActive.value = true
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

  if (viewMangaViewModel.notExist) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text("없는 갤러리입니다")
    }
  } else if (imagesLoading) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      CircularProgressIndicator()
      Text("네트워크 요청중")
    }
  } else if (imageHashes.isEmpty()) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text("이미지 결과가 0개입니다")
    }
  } else {
    if (viewMethod == "scroll")
      ScrollMangaView(gId, hashToImageUrl, imageHashes, isUISelectDialogOpen)
    else if (viewMethod == "swipe")
      SwipeMangaView(gId, hashToImageUrl, imageHashes, isUISelectDialogOpen)
    else
      SwipeVerticalMangaView(
        gId,
        hashToImageUrl,
        imageHashes,
        isUISelectDialogOpen
      )
    UISelectDialog(isUISelectDialogOpen, gId)
  }
}