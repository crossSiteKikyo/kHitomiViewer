package com.example.khitomiviewer.ui.tag

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.khitomiviewer.room.entity.Tag
import com.example.khitomiviewer.viewmodel.AppViewModel

@Composable
fun SelectedTag(tag: Tag, onRemove: () -> Unit) {
  val activity = LocalActivity.current as ComponentActivity
  val appViewModel: AppViewModel = viewModel(activity)
  val tagKorean by appViewModel.tagKorean.collectAsState(true)
  // 메인 태그 색깔. 동인지 색으로 통일한다.
  val titleColorLong = 0xFFCC9999
  val typeBgColor = Color(titleColorLong + 0x333333)
  val textColor = Color(titleColorLong - 0x666666)

  var color: Color = Color.White
  var bgColor: Color = Color.Gray

  if (tag.name.startsWith("artist:") || tag.name.startsWith("group:")
    || tag.name.startsWith("parody:") || tag.name.startsWith("character:")
  ) {
    color = textColor
    bgColor = typeBgColor
  } else if (tag.name.startsWith("female:")) {
    bgColor = Color(0xFFFF66B2)
  } else if (tag.name.startsWith("male:")) {
    bgColor = Color(0xFF0080FF)
  }

  val name = if (tagKorean && tag.koreanName != null) tag.koreanName else tag.name

  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.background(bgColor)
  ) {
    Text(
      name, color = color, modifier = Modifier
            .padding(0.dp)
            .clip(RoundedCornerShape(5.dp))
    )
    Icon(
      Icons.Default.Close, null, Modifier.combinedClickable(
        onClick = onRemove,
      )
    )
  }
}