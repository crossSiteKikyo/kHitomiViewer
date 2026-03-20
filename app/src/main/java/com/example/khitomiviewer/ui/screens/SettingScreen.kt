package com.example.khitomiviewer.ui.screens

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.khitomiviewer.viewmodel.HitomiViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(
    verticalScrollState: ScrollState
) {
    // 전역 viewModel들
    val activity = LocalActivity.current as ComponentActivity
    val hitomiViewModel: HitomiViewModel = viewModel(activity)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 5.dp)
            .verticalScroll(verticalScrollState),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "크롤링 세팅",
            fontWeight = FontWeight.Bold,
            style = TextStyle(
                shadow = Shadow(Color.Cyan, blurRadius = 5f),
                fontSize = 21.sp
            )
        )
        Text("마지막 크롤링 에러")
        Text(hitomiViewModel.crawlErrorStr.value, color = Color.Gray)
        Text("크롤링 상황")
        Text(hitomiViewModel.crawlStatusStr.value, color = Color.Gray)
        Text("남은 크롤링 개수")
        Text("${hitomiViewModel.filteredIds.size}", color = Color.Gray)
        HorizontalDivider(thickness = 2.dp)

        // 캐시 설정
        Text(
            "캐시 설정",
            fontWeight = FontWeight.Bold,
            style = TextStyle(
                shadow = Shadow(Color.Cyan, blurRadius = 5f),
                fontSize = 21.sp
            )
        )
        Text("한번이라도 본 이미지는 캐시에 저장됩니다", color = Color.Gray)
        Text("앱 용량이 너무 커질시 캐시(임시파일)를 설정에서 삭제하세요", color = Color.Gray)
        Text("데이터베이스 (데이터)는 절대 삭제하시면 안됩니다.", color = Color.Gray)
    }
}