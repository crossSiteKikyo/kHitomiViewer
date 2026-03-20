package com.example.khitomiviewer.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.khitomiviewer.viewmodel.AppViewModel
import com.example.khitomiviewer.viewmodel.UIEvent
import androidx.core.net.toUri

@Composable
fun UIEventHandler(snackbarHostState: SnackbarHostState) {
    // 전역 viewModel
    val activity = LocalActivity.current as ComponentActivity
    val appViewModel: AppViewModel = viewModel(activity)

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        appViewModel.uiEvent.collect { event ->
            when (event) {
                is UIEvent.UpdateAvailable -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "새 버전 ${event.version}이 있습니다",
                        actionLabel = "인터넷 창 열기",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            event.url.toUri()
                        )
                        context.startActivity(intent)
                    }
                }

                is UIEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }

                is UIEvent.ShowSnackBar -> {
                    val result = snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = event.actionLabel,
                        duration = SnackbarDuration.Long
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        event.onAction?.invoke()
                    }
                }
            }
        }
    }
}