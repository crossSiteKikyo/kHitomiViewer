package com.example.khitomiviewer.ui.screens

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.khitomiviewer.viewmodel.ViewMangaViewModel

@Composable
fun SettingScreen(
    verticalScrollState: ScrollState
) {
    // 전역 viewModel들
    val activity = LocalActivity.current as ComponentActivity
    val appViewModel: AppViewModel = viewModel(activity)
    val viewMangaViewModel: ViewMangaViewModel = viewModel(activity)

    val isVolumePaging by appViewModel.isVolumeKeyPagingEnabled.collectAsState()
    val galleryListUi by appViewModel.galleryListUi.collectAsState("Extended")
    val pageSize by appViewModel.pageSize.collectAsState(20)

    // 볼륨 키 가로채기 비활성화
    LaunchedEffect(Unit) {
        appViewModel.isPaginationActive.value = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(verticalScrollState),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clickable { appViewModel.toggleVolumeKeyPaging(!isVolumePaging) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("볼륨키로 페이지 이동")
            Switch(
                checked = isVolumePaging,
                onCheckedChange = { appViewModel.toggleVolumeKeyPaging(it) }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("갤러리 UI")
            Spacer(Modifier.padding(horizontal = 10.dp))
            Button(onClick = {
                appViewModel.setGalleryListUi("Extended")
            }, enabled = galleryListUi != "Extended", contentPadding = PaddingValues(2.dp)) {
                Text("확장")
            }
            Button(onClick = {
                appViewModel.setGalleryListUi("Brief")
            }, enabled = galleryListUi != "Brief", contentPadding = PaddingValues(2.dp)) {
                Text("간략히")
            }
            Button(onClick = {
                appViewModel.setGalleryListUi("Grid")
            }, enabled = galleryListUi != "Grid", contentPadding = PaddingValues(2.dp)) {
                Text("그리드")
            }
        }

        var pageSizeDDExpanded by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clickable { pageSizeDDExpanded = !pageSizeDDExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("한번에 로드할 갤러리 수")
                Text("$pageSize 개", color = Color.Gray)
            }
            DropdownMenu(
                expanded = pageSizeDDExpanded,
                onDismissRequest = { pageSizeDDExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("14") },
                    onClick = { appViewModel.setPageSize(14); pageSizeDDExpanded = false })
                DropdownMenuItem(
                    text = { Text("16") },
                    onClick = { appViewModel.setPageSize(16); pageSizeDDExpanded = false })
                DropdownMenuItem(
                    text = { Text("18") },
                    onClick = { appViewModel.setPageSize(18); pageSizeDDExpanded = false })
                DropdownMenuItem(
                    text = { Text("20") },
                    onClick = { appViewModel.setPageSize(20); pageSizeDDExpanded = false })
                DropdownMenuItem(
                    text = { Text("24") },
                    onClick = { appViewModel.setPageSize(24); pageSizeDDExpanded = false })
                DropdownMenuItem(
                    text = { Text("30") },
                    onClick = { appViewModel.setPageSize(30); pageSizeDDExpanded = false })
                DropdownMenuItem(
                    text = { Text("40") },
                    onClick = { appViewModel.setPageSize(40); pageSizeDDExpanded = false })
                DropdownMenuItem(
                    text = { Text("50") },
                    onClick = { appViewModel.setPageSize(50); pageSizeDDExpanded = false })
            }
        }

        val viewMethod by viewMangaViewModel.viewMethod.collectAsState("swipe")

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("만화 보기 UI")
            Spacer(Modifier.padding(horizontal = 10.dp))
            Spacer(Modifier.padding(horizontal = 10.dp))
            Button(onClick = {
                viewMangaViewModel.setViewMethod("scroll")
            }, enabled = viewMethod != "scroll", contentPadding = PaddingValues(2.dp)) {
                Text("스크롤")
            }
            Button(onClick = {
                viewMangaViewModel.setViewMethod("swipe")
            }, enabled = viewMethod != "swipe", contentPadding = PaddingValues(2.dp)) {
                Text("스와이프")
            }
        }
    }
}