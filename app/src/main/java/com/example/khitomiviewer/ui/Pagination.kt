package com.example.khitomiviewer.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.example.khitomiviewer.viewmodel.GalleryViewModel
import kotlinx.coroutines.launch

@Composable
fun Pagination(
    inputPageNum: Boolean,
    page: Long,
    maxPage: Long,
    pageMoveNavigate: (Long) -> Unit
) {
    // 전역 viewModel들
    val activity = LocalActivity.current as ComponentActivity
    val galleryViewModel: GalleryViewModel = viewModel(activity)

    val pageList = remember { mutableStateListOf<Long>() }

    LaunchedEffect(page, maxPage) {
        val _pageList = mutableListOf<Long>()
        var i = page - 2
        while (_pageList.size < 5) {
            if (i > 0) _pageList.add(i)
            if (i >= maxPage) break
            i++
        }
        pageList.clear()
        pageList.addAll(_pageList)
    }

    val pageMove: (Long) -> Unit = { page ->
        var num = page
        if (page <= 0) num = 1L
        else if (page > maxPage) num = maxPage

        pageMoveNavigate(num)
    }
    // 페이징
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Button(
            onClick = { pageMove(page - 3) },
            shape = RoundedCornerShape(8.dp), // 덜 둥글게 (기본은 20.dp 이상)
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp), // 패딩 축소
            modifier = Modifier
                .height(36.dp)
                .weight(1f), // 버튼 높이도 줄일 수 있음
            enabled = page != 1L,
        ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null) }
        pageList.forEach { p ->
            PageButton(
                pageMove, page != p, p, Modifier
                    .height(36.dp)
                    .weight(1f)
            )
        }
        Button(
            onClick = { pageMove(page + 3) },
            shape = RoundedCornerShape(8.dp), // 덜 둥글게 (기본은 20.dp 이상)
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp), // 패딩 축소
            modifier = Modifier
                .height(36.dp)
                .weight(1f), // 버튼 높이도 줄일 수 있음
            enabled = page != maxPage,
        ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
    }
    if (inputPageNum) {
        // 숫자로 페이지 이동
        var pageNum by remember { mutableStateOf("") }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                placeholder = {
                    Text(maxPage.toString())
                },
                value = pageNum,
                onValueChange = {
                    if (it.isEmpty() || it.matches("""^\d+$""".toRegex())) {
                        pageNum = it
                        if (pageNum.isNotEmpty() && (pageNum.toLong() > maxPage))
                            pageNum = maxPage.toString()
                        else if (pageNum.isNotEmpty() && (pageNum.toLong() <= 0))
                            pageNum = "1"
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                maxLines = 1,
                modifier = Modifier.width(100.dp)
            )
            Button(
                shape = RoundedCornerShape(2.dp), // 덜 둥글게 (기본은 20.dp 이상)
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp), // 패딩 축소
                modifier = Modifier.height(56.dp), // 버튼 높이도 줄일 수 있음
                onClick = {
                    if (pageNum.isNotEmpty() && (pageNum != page.toString()))
                        pageMoveNavigate(pageNum.toLong())
                }
            ) { Text("페이지 이동") }
        }
    }
}

@Composable
fun PageButton(
    pageMove: (Long) -> Unit,
    enable: Boolean,
    page: Long,
    modifier: Modifier
) {
    Button(
        onClick = { pageMove(page) },
        shape = RoundedCornerShape(8.dp), // 덜 둥글게 (기본은 20.dp 이상)
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp), // 패딩 축소
        modifier = modifier,
        enabled = enable,
    ) { Text("$page") }
}