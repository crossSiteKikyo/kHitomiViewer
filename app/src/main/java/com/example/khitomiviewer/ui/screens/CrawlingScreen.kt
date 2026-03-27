package com.example.khitomiviewer.ui.screens

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.khitomiviewer.viewmodel.AppViewModel
import com.example.khitomiviewer.viewmodel.HitomiViewModel

@Composable
fun CrawlingScreen(
    verticalScrollState: ScrollState
) {
    // 전역 viewModel들
    val activity = LocalActivity.current as ComponentActivity
    val hitomiViewModel: HitomiViewModel = viewModel(activity)
    val appViewModel: AppViewModel = viewModel(activity)

    // 볼륨 키 가로채기 비활성화
    LaunchedEffect(Unit) {
        appViewModel.isPaginationActive.value = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 5.dp)
            .verticalScroll(verticalScrollState),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "크롤링",
            modifier = Modifier.padding(vertical = 10.dp),
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
        )
        Text("마지막 크롤링 에러")
        Text(hitomiViewModel.crawlErrorStr.value, color = Color.Gray)
        Text("크롤링 상황")
        Text(hitomiViewModel.crawlStatusStr.value, color = Color.Gray)
        Text("남은 크롤링 개수")
        Text("${hitomiViewModel.filteredIds.size}", color = Color.Gray)
    }
}