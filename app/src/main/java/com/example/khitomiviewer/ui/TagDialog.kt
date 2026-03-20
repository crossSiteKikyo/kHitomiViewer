package com.example.khitomiviewer.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.khitomiviewer.viewmodel.DialogViewModel
import com.example.khitomiviewer.viewmodel.GalleryViewModel
import com.example.khitomiviewer.viewmodel.TagViewModel

@Composable
fun TagDialog(isTagDialogOpen: MutableState<Boolean>) {
    // 전역 viewModel
    val activity = LocalActivity.current as ComponentActivity
    val dialogViewModel: DialogViewModel = viewModel(activity)
    val galleryViewModel: GalleryViewModel = viewModel(activity)
    val tagViewModel: TagViewModel = viewModel(activity)

    // 다이얼로그
    if (isTagDialogOpen.value) {
        val likeStatusIntToStr = listOf("싫어요 \uD83C\uDD96", "기본", "좋아요 ♥")
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {
                TextButton(onClick = { isTagDialogOpen.value = false }) {
                    Text("닫기")
                }
            },
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("태그 상태 변경")
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("${dialogViewModel.selectedTag?.name}")

                    for (x in 0..2) {
                        if (x == dialogViewModel.selectedTag?.likeStatus)
                            Button(
                                onClick = {},
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                            ) { Text(likeStatusIntToStr[x]) }
                        else
                            Button(onClick = {
                                // db에 상태 변경
                                dialogViewModel.changeTagLike(x)
                                // 다이얼로그 닫기
                                isTagDialogOpen.value = false
                                // 갤러리 재로딩
                                galleryViewModel.galleryReLoading()
                                // 태그 재로딩
                                tagViewModel.tagReLoading()
                            }) { Text(likeStatusIntToStr[x]) }
                    }
                }
            })
    }
}