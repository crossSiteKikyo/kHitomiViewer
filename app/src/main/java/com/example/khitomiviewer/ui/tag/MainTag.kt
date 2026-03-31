package com.example.khitomiviewer.ui.tag

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

@Composable
fun MainTag(
    tag: Tag,
    textColor: Color,
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
    if (name.startsWith("parody:")) {
        name = name.replace("parody:", "")
    } else if (name.startsWith("character:")) {
        name = name.replace("character:", "")
    } else if (name.startsWith("group:")) {
        name = name.replace("group:", "")
    } else if (name.startsWith("artist:")) {
        name = name.replace("artist:", "")
    }

    Row(
        modifier = Modifier
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
            color = textColor,
            modifier = Modifier
                .padding(horizontal = 2.dp)
        )
        if (tag.likeStatus == 2)
            Icon(Icons.Outlined.ThumbUp, null, tint = textColor)
        else if (tag.likeStatus == 0)
            Icon(Icons.Outlined.Block, null, tint = textColor)
    }
}