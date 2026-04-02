package com.example.khitomiviewer.ui.screens

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.khitomiviewer.viewmodel.AppViewModel
import com.example.khitomiviewer.viewmodel.HitomiViewModel

@Composable
fun HelpScreen(verticalScrollState: ScrollState) {
  // 전역 viewModel들
  val activity = LocalActivity.current as ComponentActivity
  val appViewModel: AppViewModel = viewModel(activity)

  // 볼륨 키 가로채기 비활성화
  LaunchedEffect(Unit) {
    appViewModel.isPaginationActive.value = false
  }

  Column(
    modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(verticalScrollState)
        .padding(5.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    HelpCard("캐시와 데이터베이스", "앱 용량이 너무 커질시 캐시(임시파일)를 삭제하세요. 데이터베이스(데이터)는 절대 삭제하면 안됩니다.")
    HelpCard(
      "webp포맷과 avif포맷",
      "hitomi는 모든 이미지를 webp포맷과 avif포맷으로 지원합니다. avif포맷은 webp포맷보다 압축률이 높아 30%정도 데이터를 절약 할 수 있습니다. " +
              "구형 기기이거나 안드로이드 버전이 낮아 avif이미지가 깨질경우 설정에서 'avif포맷으로 요청'을 꺼주시고, 캐시(임시파일)을 삭제하세요."
    )
  }
}

@Composable
fun HelpCard(title: String, content: String) {
  Column(
    modifier = Modifier
        .fillMaxWidth()
        .border(2.dp, Color.Gray, RoundedCornerShape(5.dp))
        .padding(5.dp)
  ) {
    Text(
      title,
      modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 10.dp),
      fontSize = 25.sp,
      fontWeight = FontWeight.Bold,
      textAlign = TextAlign.Center
    )
    Text(
      content,
      color = Color.DarkGray,
      modifier = Modifier.fillMaxWidth()
    )
  }
}