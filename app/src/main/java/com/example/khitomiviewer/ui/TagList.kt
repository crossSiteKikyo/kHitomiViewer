package com.example.khitomiviewer.ui

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.collection.longListOf
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.ThumbsUpDown
import androidx.compose.material.icons.twotone.ThumbDown
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import com.example.khitomiviewer.R
import com.example.khitomiviewer.Screen
import com.example.khitomiviewer.room.entity.Tag
import com.example.khitomiviewer.util.decodeThumbnail
import com.example.khitomiviewer.util.hitomiHeaders
import com.example.khitomiviewer.viewmodel.AppViewModel
import com.example.khitomiviewer.viewmodel.DialogViewModel
import com.example.khitomiviewer.viewmodel.GalleryViewModel
import com.example.khitomiviewer.viewmodel.HitomiViewModel
import com.example.khitomiviewer.viewmodel.TagViewModel

@Composable
fun TagList(
  navController: NavHostController,
  isTagDialogOpen: MutableState<Boolean>
) {
  // 전역 viewModel들
  val activity = LocalActivity.current as ComponentActivity
  val hitomiViewModel: HitomiViewModel = viewModel(activity)
  val tagViewModel: TagViewModel = viewModel(activity)
  val dialogViewModel: DialogViewModel = viewModel(activity)
  val appViewModel: AppViewModel = viewModel(activity)

  val isAvifFormat by appViewModel.isAvifFormat.collectAsState(true)
  val context = LocalContext.current

  val tagDtoList by remember { derivedStateOf { tagViewModel.tagDtoList } }

  if (tagDtoList.isEmpty())
    Text("결과가 없습니다", style = TextStyle(fontSize = 50.sp))
  else {
    // 태그마다 카드 만들기
    for (t in tagDtoList) {
      val cardBorderColor: Color = when (t.likeStatus) {
        0 -> Color.Gray
        2 -> Color(0xFFCC00CC)
        else -> Color.Black
      }
      var textColor = Color.White
      var bgColor: Color = Color.Gray
      if (t.name.startsWith("artist:") || t.name.startsWith("group:") ||
        t.name.startsWith("parody:") || t.name.startsWith("character:")
      ) {
        bgColor = Color(0xFFCC9999 + 0x333333)
        textColor = Color(0xFFCC9999 - 0x666666)
      } else if (t.name.startsWith("male:"))
        bgColor = Color(0xFF0080FF)
      else if (t.name.startsWith("female:"))
        bgColor = Color(0xFFFF66B2)
      Card(
        shape = RoundedCornerShape(5.dp),
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 7.dp)
          .combinedClickable(
            onClick = {
              navController.navigate(
                Screen.List.createRoute(
                  1L,
                  longArrayOf(t.tagId)
                )
              )
            },
            onLongClick = {
              dialogViewModel.setTag(Tag(t.tagId, t.name, t.likeStatus))
              isTagDialogOpen.value = true
            }
          ),
        border = BorderStroke(2.dp, cardBorderColor)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .fillMaxWidth()
            .background(color = bgColor)
        ) {
          Text(
            t.name, color = textColor, modifier = Modifier
              .padding(horizontal = 2.dp)
          )
          if (t.likeStatus == 2)
            Icon(Icons.Outlined.ThumbUp, null, tint = textColor)
          else if (t.likeStatus == 0)
            Icon(Icons.Outlined.Block, null, tint = textColor)
        }
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color = Color.Black)
        )
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .fillMaxWidth()
        ) {
          for (i in t.galleries.indices) {
            AsyncImage(
              model = ImageRequest.Builder(context)
                .data(
                  decodeThumbnail(
                    t.galleries[i].thumb1,
                    hitomiViewModel.mList,
                    hitomiViewModel.thumbChar2.value,
                    hitomiViewModel.thumbChar1.value,
                    isAvifFormat
                  )
                )
                .httpHeaders(hitomiHeaders).build(),
              contentDescription = "thumbnail",
              placeholder = painterResource(R.drawable.loading),
              error = painterResource(R.drawable.errorimg),
              onError = { e -> Log.i("섬네일 에러", e.toString()) },
              contentScale = ContentScale.Crop,
              alignment = Alignment.TopStart,
              modifier = Modifier
                .fillMaxWidth(if (i == 0) 0.33f else if (i == 1) 0.5f else 1f)
                .border(1.dp, Color.Black),
            )
          }
        }
      }
    }
  }
}