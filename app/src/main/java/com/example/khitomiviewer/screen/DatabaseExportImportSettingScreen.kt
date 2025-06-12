package com.example.khitomiviewer.screen

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.khitomiviewer.viewmodel.KHitomiViewerViewModel

@Composable
fun DatabaseExportImportSettingScreen(mainViewModel: KHitomiViewerViewModel) {
    val context = LocalContext.current
    val rawexportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if(uri != null) {
            mainViewModel.backupDatabaseToFolderRaw(context, uri)
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if(uri != null) {
            mainViewModel.backupDatabaseToFolder(context, uri)
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if(uri != null) {
            mainViewModel.importFromJsonFile(context, uri)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(5.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("데이터베이스 내보내기/불러오기 설정", fontWeight = FontWeight.Bold, style = TextStyle(shadow = Shadow(Color.Cyan, blurRadius = 5f), fontSize = 21.sp))
                HorizontalDivider(thickness = 2.dp)
                if(mainViewModel.dbExportImportStr.value == "백업중" || mainViewModel.dbExportImportStr.value == "불러오기중") {
                    Text(
                        text = mainViewModel.dbExportImportStr.value,
                        color = Color.Black,
                        fontSize = 50.sp
                    )
                    CircularProgressIndicator()
                    Text(
                        text = "시간이 오래 걸립니다",
                        color = Color.Black,
                        fontSize = 30.sp
                    )
                    Text(
                        text = "이 화면을 벗어나지 마세요",
                        color = Color.Black,
                        fontSize = 30.sp
                    )
                    if(mainViewModel.dbExportImportStr.value == "불러오기중") {
                        Text(
                            text = "불러오기가 끝나면 앱이 종료됩니다.",
                            color = Color.Black,
                            fontSize = 20.sp
                        )
                    }
                }
                else {
                    Button(
                        shape = RoundedCornerShape(7.dp),
                        onClick = {
                            if(mainViewModel.crawling.value) {
                                mainViewModel.dbExportImportStr.value = "크롤링 중에는 할 수 없습니다"
                                return@Button
                            }
                            rawexportLauncher.launch(null)
                        }
                    ) {
                        Icon(Icons.Filled.Settings, "setting")
                        Text("데이터베이스 raw 내보내기: 폴더 선택 후 저장")
                    }

                    Button(
                        shape = RoundedCornerShape(7.dp),
                        onClick = {
                            if(mainViewModel.crawling.value) {
                                mainViewModel.dbExportImportStr.value = "크롤링 중에는 할 수 없습니다"
                                return@Button
                            }
                            exportLauncher.launch(null)
                        }
                    ) {
                        Icon(Icons.Filled.Settings, "setting")
                        Text("데이터베이스 내보내기: 폴더 선택 후 저장")
                    }

                    Button(
                        shape = RoundedCornerShape(7.dp),
                        onClick = {
                            if(mainViewModel.crawling.value) {
                                mainViewModel.dbExportImportStr.value = "크롤링 중에는 할 수 없습니다"
                                return@Button
                            }
                            importLauncher.launch(arrayOf("application/json"))
                        }
                    ) {
                        Icon(Icons.Filled.Settings, "setting")
                        Text("데이터베이스 불러오기: 파일 선택 후 db에 저장")
                    }
                    Text(
                        text = mainViewModel.dbExportImportStr.value,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}