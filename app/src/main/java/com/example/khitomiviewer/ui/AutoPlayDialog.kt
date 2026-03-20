package com.example.khitomiviewer.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.khitomiviewer.ui.common.CustomTextField
import com.example.khitomiviewer.viewmodel.DialogViewModel
import com.example.khitomiviewer.viewmodel.GalleryViewModel
import com.example.khitomiviewer.viewmodel.ViewMangaViewModel

@Composable
fun AutoPlayDialog(isAutoPlayDialogOpen: MutableState<Boolean>) {
    // 전역 viewModel
    val activity = LocalActivity.current as ComponentActivity
    val viewMangaViewModel: ViewMangaViewModel = viewModel(activity)

    val isAutoPlayLoop by viewMangaViewModel.isAutoPlayLoop.collectAsState(initial = false)
    val autoPlayPeriod by viewMangaViewModel.autoPlayPeriod.collectAsState(initial = 10)

    LaunchedEffect(isAutoPlayDialogOpen.value) {
        if (isAutoPlayDialogOpen.value) {
            viewMangaViewModel.tempIsLoop.value = isAutoPlayLoop
            viewMangaViewModel.tempPeriod.value = autoPlayPeriod.toString()
        }
    }

    // 다이얼로그
    if (isAutoPlayDialogOpen.value) {
        AlertDialog(
            onDismissRequest = { },
            dismissButton = {
                TextButton(onClick = { isAutoPlayDialogOpen.value = false }) {
                    Text("취소")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    isAutoPlayDialogOpen.value = false
                    if (viewMangaViewModel.tempPeriod.value.isNotEmpty())
                        viewMangaViewModel.isAutoPlaying.value = true
                }) {
                    Text("시작")
                }
            },
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("자동 넘기기")
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text("주기: ")
                        CustomTextField(
                            value = viewMangaViewModel.tempPeriod.value,
                            onValueChange = {
                                if (it.isEmpty() || it.matches("""^\d+$""".toRegex())) {
                                    viewMangaViewModel.tempPeriod.value = it
                                    if (it.isNotEmpty()) {
                                        if (it.toInt() < 0)
                                            viewMangaViewModel.tempPeriod.value = "0"
                                        else if (it.toInt() > 1000)
                                            viewMangaViewModel.tempPeriod.value = "1000"
                                        viewMangaViewModel.updateAutoPlayPeriod(it.toInt())
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            maxLines = 1,
                            modifier = Modifier.width(70.dp),
                            contentPadding = PaddingValues(horizontal = 5.dp, vertical = 7.dp)
                        )
                        Text("초")
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("반복: ")
                        Checkbox(
                            checked = viewMangaViewModel.tempIsLoop.value,
                            onCheckedChange = {
                                viewMangaViewModel.tempIsLoop.value = it
                                viewMangaViewModel.toggleIsAutoPlayLoop(it)
                            }
                        )
                    }
                }
            }
        )
    }
}