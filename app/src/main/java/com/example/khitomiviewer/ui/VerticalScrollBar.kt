package com.example.khitomiviewer.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun VerticalScrollBar(innerPadding: PaddingValues, verticalScrollState: ScrollState) {
    var visible by remember { mutableStateOf(true) }
    // 값이 변할 때마다 스크롤 타이머 재시작
    LaunchedEffect(verticalScrollState.value) {
        visible = true
        delay(1000L) // 1초 후 자동 숨김
        visible = false
    }
    Box(
        modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
    ) {
        // 커스텀 스크롤 바
        if (verticalScrollState.maxValue > 0 && visible) {
            val scrollFraction =
                verticalScrollState.value.toFloat() / verticalScrollState.maxValue.toFloat()
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .align(Alignment.CenterEnd)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.1f)
                        .graphicsLayer { translationY = size.height * 9f * scrollFraction }
                        .background(
                            color = Color.Gray,
                        )
                )
            }
        }
    }
}