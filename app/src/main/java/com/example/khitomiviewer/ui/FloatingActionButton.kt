package com.example.khitomiviewer.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch

@Composable
fun FloatingActionButton(verticalScrollState: ScrollState) {
    val coroutineScope = rememberCoroutineScope()

    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        FloatingActionButton(
            onClick = {
                coroutineScope.launch { verticalScrollState.scrollTo(0) }
            },
            modifier = Modifier.size(50.dp)
        ) {
            Icon(Icons.Filled.ArrowUpward, "top")
        }

        FloatingActionButton(
            onClick = {
                coroutineScope.launch { verticalScrollState.scrollTo(verticalScrollState.maxValue) }
            },
            modifier = Modifier.size(50.dp)
        ) {
            Icon(Icons.Filled.ArrowDownward, "bottom")
        }
    }
}