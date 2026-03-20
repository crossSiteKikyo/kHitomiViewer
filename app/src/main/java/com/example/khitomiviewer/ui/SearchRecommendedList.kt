package com.example.khitomiviewer.ui

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun SearchRecommendedList(onClick: (prefix: String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = { onClick("artist:") })
    ) {
        Text("artist:")
        Text("작가", color = Color.Gray)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = { onClick("group:") })
    ) {
        Text("group:")
        Text("그룹", color = Color.Gray)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = { onClick("parody:") })
    ) {
        Text("parody:")
        Text("시리즈", color = Color.Gray)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = { onClick("character:") })
    ) {
        Text("character:")
        Text("캐릭터", color = Color.Gray)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = { onClick("male:") })
    ) {
        Text("male:")
        Text("남성", color = Color.Gray)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = { onClick("female:") })
    ) {
        Text("female:")
        Text("여성", color = Color.Gray)
    }
}