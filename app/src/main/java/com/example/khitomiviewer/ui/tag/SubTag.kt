package com.example.khitomiviewer.ui.tag

import androidx.compose.foundation.background
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
import kotlinx.serialization.json.Json

@Composable
fun SubTag(
    tag: Tag,
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
    var bgColor: Color = Color.Gray
    if (name.startsWith("male:")) {
        name = name.replace("male:", "")
        bgColor = Color(0xFF0080FF)
    } else if (name.startsWith("female:")) {
        name = name.replace("female:", "")
        bgColor = Color(0xFFFF66B2)
    }

    Text(
        name,
        color = Color.White,
        modifier = Modifier
            .combinedClickable(
                onClick = { tagClick() },
                onLongClick = {
                    dialogViewModel.setTag(tag)
                    isTagDialogOpen.value = true
                }
            )
            .clip(RoundedCornerShape(5.dp))
            .background(bgColor)
            .padding(horizontal = 2.dp)
    )
}