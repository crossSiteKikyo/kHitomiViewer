package com.example.khitomiviewer.ui.tag

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Icon
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
    navController: NavHostController,
    isGalleryDetailDialogOpen: MutableState<Boolean>? = null
) {
    fun tagClick() {
        navController.navigate(Screen.List.createRoute(1L, longArrayOf(tag.tagId)))
        if (isGalleryDetailDialogOpen != null)
            isGalleryDetailDialogOpen.value = false
    }

    var name = tag.name
    var bgColor: Color = Color.Gray
    if (name.startsWith("male:")) {
        name = name.replace("male:", "")
        bgColor = Color(0xFF0080FF)
    } else if (name.startsWith("female:")) {
        name = name.replace("female:", "")
        bgColor = Color(0xFFFF66B2)
    }
    Row(
        modifier = Modifier
            .background(bgColor)
            .combinedClickable(
                onClick = { tagClick() },
                onLongClick = {
                    dialogViewModel.setTag(tag)
                    isTagDialogOpen.value = true
                }
            )
    ) {
        Text(
            name,
            color = Color.White,
            modifier = Modifier
                .padding(horizontal = 2.dp)
        )
        if (tag.likeStatus == 2)
            Icon(Icons.Outlined.ThumbUp, null, tint = Color.White)
        else if (tag.likeStatus == 0)
            Icon(Icons.Outlined.Block, null, tint = Color.White)
    }
}