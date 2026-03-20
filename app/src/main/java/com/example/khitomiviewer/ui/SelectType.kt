package com.example.khitomiviewer.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.khitomiviewer.viewmodel.GalleryViewModel

@Composable
fun SelectType() {
    val activity = LocalActivity.current as ComponentActivity
    val galleryViewModel: GalleryViewModel = viewModel(activity)

    val typeOnOff = galleryViewModel::typeOnOff
    // 타입 선택 관련 변수들
    val disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    val typeColor: Map<Int, Long> = remember {
        mapOf<Int, Long>(
            1 to 0xFFCC9999,  // c99 "doujinshi"
            2 to 0xFFCC99CC,  // c9c "manga"
            3 to 0xFF99CCCC,  // 9cc "artistcg"
            4 to 0xFF9999CC,  // 99c "gamecg"
            5 to 0xFF999999,  // 999 "imageset"
            // CC보다 크면 안되고 66보다 작으면 안된다.
        )
    }

    var flagDoujinshi by remember { mutableStateOf(1 in galleryViewModel.showTypeIdList.value) }
    var flagManga by remember { mutableStateOf(2 in galleryViewModel.showTypeIdList.value) }
    var flagArtistcg by remember { mutableStateOf(3 in galleryViewModel.showTypeIdList.value) }
    var flagGamecg by remember { mutableStateOf(4 in galleryViewModel.showTypeIdList.value) }
    var flagImageset by remember { mutableStateOf(5 in galleryViewModel.showTypeIdList.value) }

    // 보일 타입 선택
    FlowRow(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Button(
            onClick = {
                flagDoujinshi = !flagDoujinshi
                typeOnOff(1, flagDoujinshi)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (flagDoujinshi) Color(0xFFCC9999 + 0x333333) else disabledContainerColor,
                contentColor = if (flagDoujinshi) Color(0xFFCC9999 - 0x666666) else disabledContentColor
            ),
            modifier = Modifier.fillMaxWidth(0.33f)
        ) { Text("doujinshi") }
        Button(
            onClick = {
                flagManga = !flagManga
                typeOnOff(2, flagManga)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (flagManga) Color(0xFFCC99CC + 0x333333) else disabledContainerColor,
                contentColor = if (flagManga) Color(0xFFCC99CC - 0x666666) else disabledContentColor
            ),
            modifier = Modifier.fillMaxWidth(0.33f)
        ) { Text("manga") }
        Button(
            onClick = {
                flagArtistcg = !flagArtistcg
                typeOnOff(3, flagArtistcg)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (flagArtistcg) Color(0xFF99CCCC + 0x333333) else disabledContainerColor,
                contentColor = if (flagArtistcg) Color(0xFF99CCCC - 0x666666) else disabledContentColor
            ),
            modifier = Modifier.fillMaxWidth(0.33f)
        ) { Text("artistcg") }
        Button(
            onClick = {
                flagGamecg = !flagGamecg
                typeOnOff(4, flagGamecg)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (flagGamecg) Color(0xFF9999CC + 0x333333) else disabledContainerColor,
                contentColor = if (flagGamecg) Color(0xFF9999CC - 0x666666) else disabledContentColor
            ),
            modifier = Modifier.fillMaxWidth(0.5f)
        ) { Text("gamecg") }
        Button(
            onClick = {
                flagImageset = !flagImageset
                typeOnOff(5, flagImageset)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (flagImageset) Color(0xFF999999 + 0x333333) else disabledContainerColor,
                contentColor = if (flagImageset) Color(0xFF999999 - 0x666666) else disabledContentColor
            ),
            modifier = Modifier.fillMaxWidth(0.5f)
        ) { Text("imageset") }
    }
}