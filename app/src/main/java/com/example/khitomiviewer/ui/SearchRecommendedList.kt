package com.example.khitomiviewer.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.khitomiviewer.viewmodel.AppViewModel

@Composable
fun SearchRecommendedList(onClick: (prefix: String) -> Unit) {
  val activity = LocalActivity.current as ComponentActivity
  val appViewModel: AppViewModel = viewModel(activity)
  val tagKorean by appViewModel.tagKorean.collectAsState(true)

  if (tagKorean) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .combinedClickable(onClick = { onClick("패러디:") })
    ) {
      Text("패러디:")
    }
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .combinedClickable(onClick = { onClick("남성:") })
    ) {
      Text("남성:")
    }
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .combinedClickable(onClick = { onClick("여성:") })
    ) {
      Text("여성:")
    }
  }
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