package com.example.khitomiviewer.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.khitomiviewer.viewmodel.AppViewModel
import com.example.khitomiviewer.viewmodel.ViewMangaViewModel

@Composable
fun GalleryListUiSelect() {
    // 전역 viewModel들
    val activity = LocalActivity.current as ComponentActivity
    val appViewModel: AppViewModel = viewModel(activity)

    val galleryListUi by appViewModel.galleryListUi.collectAsState("Extended")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Button(
            onClick = {
                appViewModel.setGalleryListUi("Extended")
            },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            enabled = galleryListUi != "Extended",
            contentPadding = PaddingValues(2.dp)
        ) {
            Text("확장")
        }
        Button(
            onClick = {
                appViewModel.setGalleryListUi("Brief")
            },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            enabled = galleryListUi != "Brief",
            contentPadding = PaddingValues(2.dp)
        ) {
            Text("간략히")
        }
        Button(
            onClick = {
                appViewModel.setGalleryListUi("Grid")
            },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            enabled = galleryListUi != "Grid",
            contentPadding = PaddingValues(2.dp)
        ) {
            Text("그리드2")
        }
        Button(
            onClick = {
                appViewModel.setGalleryListUi("Grid3")
            },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            enabled = galleryListUi != "Grid3",
            contentPadding = PaddingValues(2.dp)
        ) {
            Text("그리드3")
        }
        Button(
            onClick = {
                appViewModel.setGalleryListUi("Grid4")
            },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            enabled = galleryListUi != "Grid4",
            contentPadding = PaddingValues(2.dp)
        ) {
            Text("그리드4")
        }
        Button(
            onClick = {
                appViewModel.setGalleryListUi("Grid5")
            },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            enabled = galleryListUi != "Grid5",
            contentPadding = PaddingValues(2.dp)
        ) {
            Text("그리드5")
        }
    }
}