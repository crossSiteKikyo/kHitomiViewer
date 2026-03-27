package com.example.khitomiviewer.ui.screens

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.ImportExport
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.khitomiviewer.R
import com.example.khitomiviewer.Screen
import com.example.khitomiviewer.viewmodel.AppViewModel

@Composable
fun MenuScreen(
    navController: NavHostController,
    verticalScrollState: ScrollState
) {
    // 전역 viewModel들
    val activity = LocalActivity.current as ComponentActivity
    val appViewModel: AppViewModel = viewModel(activity)

    // 볼륨 키 가로채기 비활성화
    LaunchedEffect(Unit) {
        appViewModel.isPaginationActive.value = false
    }

    val isDark by appViewModel.isDarkMode.collectAsState(initial = false)

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(verticalScrollState),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.mainicon),
            null,
            modifier = Modifier.width(100.dp)
        )

        Text("kHitomiViewer 버전: ${appViewModel.currentVersion}", color = Color.Gray)

        MenuRow(onClick = {
            val intent = Intent(
                Intent.ACTION_VIEW,
                "https://discord.gg/X7r2ADfAH2".toUri()
            )
            context.startActivity(intent)
        }) {
            Icon(Icons.Outlined.Link, null)
            Text("디스코드")
        }

        MenuRow(onClick = {
            navController.navigate(Screen.Help.route)
        }) {
            Icon(Icons.AutoMirrored.Outlined.HelpOutline, null)
            Text("도움말")
        }

        MenuRow(onClick = {
            appViewModel.toggleDarkMode(isDark)
        }) {
            if (isDark) {
                Icon(Icons.Outlined.DarkMode, "다크모드")
                Text("다크모드")
            } else {
                Icon(Icons.Outlined.LightMode, "라이트모드")
                Text("라이트모드")
            }
        }

        MenuRow(onClick = {
            navController.navigate(Screen.DislikeGallery.route)
        }) {
            Icon(Icons.Outlined.ThumbDown, null)
            Text("갤러리 싫어요(차단)")
        }

        MenuRow(onClick = {
            navController.navigate(Screen.DislikeTag.route)
        }) {
            Icon(Icons.Outlined.ThumbDown, null)
            Text("태그 싫어요(차단)")
        }

        MenuRow(onClick = {
            navController.navigate(Screen.Setting.route)
        }) {
            Icon(Icons.Outlined.Settings, null)
            Text("설정")
        }

        MenuRow(onClick = {
            navController.navigate(Screen.Crawling.route)
        }) {
            Icon(Icons.Outlined.SmartToy, null)
            Text("크롤링")
        }

        MenuRow(onClick = {
            navController.navigate(Screen.DatabaseExportImport.route)
        }) {
            Icon(Icons.Outlined.ImportExport, null)
            Text("내보내기/불러오기")
        }

        MenuRow(onClick = {
            appViewModel.vacuumDataBase()
        }) {
            Icon(Icons.Outlined.Refresh, null)
            Text("데이터베이스 용량 최적화")
        }

        MenuRow(onClick = {
            appViewModel.checkLatestVersion()
        }) {
            Icon(Icons.Outlined.Refresh, null)
            Text("최신버전 체크")
        }
    }
}

@Composable
fun MenuRow(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, Color.Gray)
            .clickable(onClick = onClick)
            .padding(vertical = 15.dp, horizontal = 10.dp)
    ) {
        content()
    }
}