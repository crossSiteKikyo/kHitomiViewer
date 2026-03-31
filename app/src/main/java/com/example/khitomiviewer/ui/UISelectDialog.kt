package com.example.khitomiviewer.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.khitomiviewer.viewmodel.AppViewModel
import com.example.khitomiviewer.viewmodel.ViewMangaViewModel

@Composable
fun UISelectDialog(isUISelectDialogOpen: MutableState<Boolean>, gId: Long) {
    // 전역 viewModel들
    val activity = LocalActivity.current as ComponentActivity
    val viewMangaViewModel: ViewMangaViewModel = viewModel(activity)

    val viewMethod by viewMangaViewModel.viewMethod.collectAsState("swipe")

    if (isUISelectDialogOpen.value) {
        AlertDialog(
            onDismissRequest = { },
            confirmButton = {
                TextButton(onClick = { isUISelectDialogOpen.value = false }) {
                    Text("닫기")
                }
            },
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("만화보기 UI 선택")
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            viewMangaViewModel.reloadLastPage(gId)
                            viewMangaViewModel.setViewMethod("scroll")
                            isUISelectDialogOpen.value = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = viewMethod != "scroll",
                        contentPadding = PaddingValues(2.dp)
                    ) {
                        Text("스크롤")
                    }
                    Button(
                        onClick = {
                            viewMangaViewModel.reloadLastPage(gId)
                            viewMangaViewModel.setViewMethod("swipe")
                            isUISelectDialogOpen.value = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = viewMethod != "swipe",
                        contentPadding = PaddingValues(2.dp)
                    ) {
                        Text("좌/우 스와이프")
                    }
                    Button(
                        onClick = {
                            viewMangaViewModel.reloadLastPage(gId)
                            viewMangaViewModel.setViewMethod("swipeVertical")
                            isUISelectDialogOpen.value = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = viewMethod != "swipeVertical",
                        contentPadding = PaddingValues(2.dp)
                    ) {
                        Text("상하 스와이프")
                    }
                }
            }
        )
    }
}