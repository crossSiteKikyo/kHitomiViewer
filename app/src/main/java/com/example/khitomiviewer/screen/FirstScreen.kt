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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.khitomiviewer.viewmodel.FirstScreenViewModel

@Composable
fun FirstScreen() {
    val context = LocalContext.current
    val viewModel: FirstScreenViewModel = viewModel()
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if(uri != null) {
            viewModel.importFromJsonFile(context, uri)
        }
    }
    // 처음 실행시 서버에서 db를 받아와야 한다.
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
                if(viewModel.dbExportImportStr.value == "불러오기중") {
                    Text(
                        text = viewModel.dbExportImportStr.value,
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
                    Text(
                        text = "불러오기가 끝나면 앱이 종료됩니다.",
                        color = Color.Black,
                        fontSize = 20.sp
                    )
                } else {
                    Text("앱 처음 실행", fontWeight = FontWeight.Bold, style = TextStyle(shadow = Shadow(Color.Cyan, blurRadius = 5f), fontSize = 21.sp))
                    HorizontalDivider(thickness = 2.dp)
                    Text(
                        text = viewModel.dbExportImportStr.value,
                        color = Color.Gray
                    )
                    Button(
                        shape = RoundedCornerShape(7.dp),
                        onClick = {
                            importLauncher.launch(arrayOf("application/json"))
                        }
                    ) {
                        Icon(Icons.Filled.Settings, "setting")
                        Text("데이터베이스 불러오기: 파일 선택 후 db에 저장")
                    }
                    Text(text = """휴대폰을 바꿔 데이터베이스를 이전한다면 이전 폰에서 '데이터베이스 내보내기'를 이용해 json파일을 만든 후 해당 파일을 불러오면 됩니다.""", color = Color.Gray)
                    Text(text = """처음 설치하신거라면 Github release에서 'KHitomiViewerDBraw.zip'를 받아 압축해제 후 json파일을 불러오면 됩니다.""", color = Color.Gray)
                }
            }
        }
    }
}