package com.example.khitomiviewer.ui.tag

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.khitomiviewer.Screen
import com.example.khitomiviewer.room.entity.Tag
import com.example.khitomiviewer.viewmodel.DialogViewModel

@Composable
fun MainTag(
    tag: Tag,
    textColor: Color,
    dialogViewModel: DialogViewModel,
    isTagDialogOpen: MutableState<Boolean>,
    navController: NavHostController
) {
    fun tagClick() {
        navController.navigate(Screen.List.createRoute(1L, longArrayOf(tag.tagId)))
    }

    var name = when (tag.likeStatus) {
        2 -> tag.name + "♥"
        else -> tag.name
    }
    var color: Color = textColor
    if (name.startsWith("parody:")) {
        name = name.replace("parody:", "")
    } else if (name.startsWith("character:")) {
        name = name.replace("character:", "")
    } else if (name.startsWith("group:")) {
        name = name.replace("group:", "")
    } else if (name.startsWith("artist:")) {
        name = name.replace("artist:", "")
    }

    Text(
        name,
        color = color,
        modifier = Modifier
            .combinedClickable(
                onClick = { tagClick() },
                onLongClick = {
                    dialogViewModel.setTag(tag)
                    isTagDialogOpen.value = true
                }
            )
            .clip(RoundedCornerShape(5.dp))
            .padding(horizontal = 2.dp)
    )
}