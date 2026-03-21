package com.example.khitomiviewer.ui.tag

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.khitomiviewer.room.entity.Tag

@Composable
fun FilteredTag(tag: Tag, onClick: () -> Unit) {
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .combinedClickable(
                onClick = onClick,
            )
    ) {
        Text(
            tag.name, color = color, modifier = Modifier.padding(0.dp)
        )
        if (tag.likeStatus == 2)
            Icon(Icons.Outlined.ThumbUp, null)
        else if (tag.likeStatus == 0)
            Icon(Icons.Outlined.Block, null)
    }
}