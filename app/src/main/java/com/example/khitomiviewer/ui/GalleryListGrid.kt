package com.example.khitomiviewer.ui

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import com.example.khitomiviewer.R
import com.example.khitomiviewer.util.decodeThumbnail
import com.example.khitomiviewer.util.hitomiHeaders
import com.example.khitomiviewer.viewmodel.AppViewModel
import com.example.khitomiviewer.viewmodel.DialogViewModel
import com.example.khitomiviewer.viewmodel.GalleryViewModel
import com.example.khitomiviewer.viewmodel.HitomiViewModel

@Composable
fun GalleryListGrid(
  isGalleryDialogOpen: MutableState<Boolean>,
  isGalleryDetailDialogOpen: MutableState<Boolean>
) {
  // 전역 viewModel들
  val activity = LocalActivity.current as ComponentActivity
  val hitomiViewModel: HitomiViewModel = viewModel(activity)
  val galleryViewModel: GalleryViewModel = viewModel(activity)
  val dialogViewModel: DialogViewModel = viewModel(activity)
  val appViewModel: AppViewModel = viewModel(activity)

  val isAvifFormat by appViewModel.isAvifFormat.collectAsState(true)
  val galleryListUi by appViewModel.galleryListUi.collectAsState("Extended")
  val chunkSize = when (galleryListUi) {
    "Grid5" -> 5
    "Grid4" -> 4
    "Grid3" -> 3
    else -> 2
  }

  val galleries by remember { derivedStateOf { galleryViewModel.galleries } }

  val typeColor: Map<String, Long> = remember {
    mapOf<String, Long>(
      "doujinshi" to 0xFFCC9999,  // c99
      "manga" to 0xFFCC99CC,      // c9c
      "artistcg" to 0xFF99CCCC,  // 9cc
      "gamecg" to 0xFF9999CC,    // 99c
      "imageset" to 0xFF999999,  // 999
      // CC보다 크면 안되고 66보다 작으면 안된다.
    )
  }
  val context = LocalContext.current

  if (galleries.isEmpty())
    Text("결과가 없습니다", style = TextStyle(fontSize = 50.sp))
  else {
    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      galleries.chunked(chunkSize).forEach { rowItems ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(1.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          for (g in rowItems) {
            val cardBorderColor: Color = when (g.likeStatus) {
              0 -> Color.Gray
              2 -> Color(0xFFCC00CC)
              else -> Color.Black
            }
            Box(
              modifier = Modifier
                .weight(1f)
                .border(1.dp, cardBorderColor),
              contentAlignment = Alignment.Center
            ) {
              AsyncImage(
                model = ImageRequest.Builder(context)
                  .data(
                    decodeThumbnail(
                      g.thumb1,
                      hitomiViewModel.mList,
                      hitomiViewModel.thumbChar2.value,
                      hitomiViewModel.thumbChar1.value,
                      isAvifFormat
                    )
                  )
                  .diskCacheKey(g.thumb1).httpHeaders(hitomiHeaders)
                  .build(),
                contentDescription = "thumbnail",
                placeholder = painterResource(R.drawable.loading),
                error = painterResource(R.drawable.errorimg),
                onError = { e -> Log.i("섬네일 에러", e.toString()) },
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                  .fillMaxWidth()
                  .combinedClickable(
                    onClick = {
                      dialogViewModel.setGalleryDetail(g)
                      isGalleryDetailDialogOpen.value = true
                    },
                    onLongClick = {
                      dialogViewModel.setGallery(g.gId)
                      isGalleryDialogOpen.value = true
                    }
                  ),
              )
            }
          }
          if (rowItems.size < chunkSize) {
            for (i in 1..chunkSize - rowItems.size)
              Spacer(modifier = Modifier.weight(1f))
          }
        }
      }
    }
  }
}