package com.example.khitomiviewer.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.khitomiviewer.repository.AppRepositories
import com.example.khitomiviewer.repository.SyncLogEntry
import com.example.khitomiviewer.repository.SyncLogKind
import com.example.khitomiviewer.viewmodel.AppViewModel
import com.example.khitomiviewer.work.CrawlScheduler

@Composable
fun CrawlingScreen(
    verticalScrollState: ScrollState
) {
    val activity = LocalActivity.current as ComponentActivity
    val appViewModel: AppViewModel = viewModel(activity)
    val context = LocalContext.current
    val hitomiSync = remember { AppRepositories.get(context).hitomiSync }
    val crawlStatus by hitomiSync.crawlStatus.collectAsState()
    val remainingCount by hitomiSync.remainingCount.collectAsState()
    val pendingTotal by hitomiSync.pendingTotal.collectAsState()
    val logs by hitomiSync.logs.collectAsState()
    val backgroundCrawlEnabled by appViewModel.backgroundCrawlEnabled.collectAsState(true)
    val backgroundCrawlWifiOnly by appViewModel.backgroundCrawlWifiOnly.collectAsState(true)
    val backgroundCrawlIntervalHours by appViewModel.backgroundCrawlIntervalHours.collectAsState(8)
    var intervalMenuExpanded by remember { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        appViewModel.isPaginationActive.value = false
    }

    LaunchedEffect(backgroundCrawlEnabled) {
        if (!backgroundCrawlEnabled || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@LaunchedEffect
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val errorCount = logs.count { it.kind == SyncLogKind.Error }
    val doneCount = logs.count { it.kind == SyncLogKind.Done }
    val progress = if (pendingTotal > 0) {
        ((pendingTotal - remainingCount).coerceAtLeast(0).toFloat() / pendingTotal)
            .coerceIn(0f, 1f)
    } else {
        0f
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
            .verticalScroll(verticalScrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "크롤링",
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 10.dp),
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                CrawlSettingRow(
                    title = "백그라운드 크롤링",
                    checked = backgroundCrawlEnabled,
                    enabled = true,
                    onCheckedChange = { appViewModel.setBackgroundCrawlEnabled(it) }
                )
                CrawlSettingRow(
                    title = "와이파이에서만",
                    checked = backgroundCrawlWifiOnly,
                    enabled = backgroundCrawlEnabled,
                    onCheckedChange = { appViewModel.setBackgroundCrawlWifiOnly(it) }
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clickable(enabled = backgroundCrawlEnabled) {
                                intervalMenuExpanded = true
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "크롤링 주기",
                            color = if (backgroundCrawlEnabled) {
                                Color.Unspecified
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            }
                        )
                        Text(
                            "${backgroundCrawlIntervalHours}시간",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = intervalMenuExpanded && backgroundCrawlEnabled,
                        onDismissRequest = { intervalMenuExpanded = false }
                    ) {
                        CrawlScheduler.allowedHours.forEach { hours ->
                            DropdownMenuItem(
                                text = { Text("${hours}시간") },
                                onClick = {
                                    appViewModel.setBackgroundCrawlIntervalHours(hours)
                                    intervalMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("현재 상황", style = MaterialTheme.typography.labelLarge)
                Text(
                    crawlStatus,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (pendingTotal > 0 && remainingCount > 0) {
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "진행 ${pendingTotal - remainingCount} / $pendingTotal  ·  남은 ${remainingCount}개",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text(
                        "대기 중인 신규 크롤 ${remainingCount}개",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("완료 $doneCount", color = doneColor())
                    Text("오류 $errorCount", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        Text("기록", style = MaterialTheme.typography.titleMedium)
        if (logs.isEmpty()) {
            Text(
                "아직 기록이 없습니다",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            logs.forEachIndexed { index, entry ->
                SyncLogRow(entry)
                if (index != logs.lastIndex) {
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun CrawlSettingRow(
    title: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(enabled = enabled) { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            title,
            color = if (enabled) {
                Color.Unspecified
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            }
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
private fun SyncLogRow(entry: SyncLogEntry) {
    val kindColor = when (entry.kind) {
        SyncLogKind.Error -> MaterialTheme.colorScheme.error
        SyncLogKind.Done -> doneColor()
        SyncLogKind.Skipped -> MaterialTheme.colorScheme.onSurfaceVariant
        SyncLogKind.Info -> MaterialTheme.colorScheme.primary
    }
    val kindLabel = when (entry.kind) {
        SyncLogKind.Error -> "오류"
        SyncLogKind.Done -> "완료"
        SyncLogKind.Skipped -> "건너뜀"
        SyncLogKind.Info -> "진행"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                kindLabel,
                color = kindColor,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                entry.timeLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium
            )
        }
        Text(entry.message, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun doneColor(): Color {
    return Color(0xFF2E7D32)
}
