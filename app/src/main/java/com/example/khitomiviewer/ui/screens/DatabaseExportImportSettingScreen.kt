package com.example.khitomiviewer.ui.screens

import android.R
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.khitomiviewer.viewmodel.AppViewModel
import com.example.khitomiviewer.viewmodel.DataExportImportViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun nowAsFileTimestamp(): String {
    val formatter = DateTimeFormatter.ofPattern("yy년MM월dd일HH시mm분ss")
    return LocalDateTime.now().format(formatter)
}

@Composable
fun DatabaseExportImportSettingScreen() {
    // 전역 viewModel들
    val activity = LocalActivity.current as ComponentActivity
    val dataExportImportViewModel: DataExportImportViewModel = viewModel(activity)
    val appViewModel: AppViewModel = viewModel(activity)

    // 볼륨 키 가로채기 비활성화
    LaunchedEffect(Unit) {
        appViewModel.isPaginationActive.value = false
    }

    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            dataExportImportViewModel.backupDatabaseToUri(context, uri)
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            dataExportImportViewModel.importFromJsonFile(context, uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(5.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "갤러리&태그 정보 내보내기/불러오기",
            fontWeight = FontWeight.Bold,
            style = TextStyle(fontSize = 21.sp)
        )
        HorizontalDivider(thickness = 2.dp)
        if (dataExportImportViewModel.dbExportImportProgress.value) {
            CircularProgressIndicator()
            Text(
                text = "시간이 오래 걸릴 수 있습니다",
                fontSize = 30.sp
            )
            Text(
                text = "이 화면을 벗어나지 마세요",
                fontSize = 30.sp
            )
        } else {
            Button(
                shape = RoundedCornerShape(7.dp),
                onClick = {
                    exportLauncher.launch("KHitomiViewer${nowAsFileTimestamp()}.json")
                }
            ) {
                Text("정보 내보내기: 파일로 저장")
            }

            Button(
                shape = RoundedCornerShape(7.dp),
                onClick = {
                    importLauncher.launch(arrayOf("application/json"))
                }
            ) {
                Text("정보 불러오기: 파일 선택 후 db에 저장")
            }
            Text(dataExportImportViewModel.statusString.value, color = Color.Gray)
        }

    }
}