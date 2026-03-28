package com.example.khitomiviewer.ui

import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import com.example.khitomiviewer.R
import com.example.khitomiviewer.Screen
import com.example.khitomiviewer.ui.tag.MainTag
import com.example.khitomiviewer.ui.tag.SubTag
import com.example.khitomiviewer.viewmodel.DialogViewModel
import com.example.khitomiviewer.viewmodel.GalleryViewModel
import com.example.khitomiviewer.viewmodel.HitomiViewModel

@Composable
fun GalleryDetailDialog(
    navController: NavHostController,
    isTagDialogOpen: MutableState<Boolean>,
    isGalleryDialogOpen: MutableState<Boolean>,
    isGalleryDetailDialogOpen: MutableState<Boolean>
) {
    // 전역 viewModel
    val activity = LocalActivity.current as ComponentActivity
    val hitomiViewModel: HitomiViewModel = viewModel(activity)
    val galleryViewModel: GalleryViewModel = viewModel(activity)
    val dialogViewModel: DialogViewModel = viewModel(activity)

    val galleryDetail by dialogViewModel.selectedGalleryDetail

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
    // th.hitomi.la는 가짜 url이다. 진짜 url로 바꾼다.
    val realHitomiThumbnailUrl: (String) -> String = { url ->
        val hash = """[0-9a-z]{40,}""".toRegex().find(url)?.value
        if (hash == null) {
            url
        } else {
            val s =
                "${hash[hash.length - 1]}${hash[hash.length - 3]}${hash[hash.length - 2]}".toInt(16)
                    .toString(10)
            val ch =
                if (s in hitomiViewModel.mList) hitomiViewModel.thumbChar2.value else hitomiViewModel.thumbChar1.value
            url.replace("tn.hitomi.la", "${ch}tn.gold-usergeneratedcontent.net")
        }
    }
    val context = LocalContext.current
    val hitomiHeaders = NetworkHeaders.Builder().set("Referer", "https://hitomi.la/").build()

    // 다이얼로그
    if (isGalleryDetailDialogOpen.value) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {
                TextButton(onClick = { isGalleryDetailDialogOpen.value = false }) {
                    Text("닫기")
                }
            },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier.fillMaxWidth(),
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("갤러리 정보")
                }
            },
            text = {
                galleryDetail?.let { g ->
                    val cardBorderColor: Color = when (g.likeStatus) {
                        0 -> Color.Gray
                        2 -> Color(0xFFCC00CC)
                        else -> Color.Black
                    }
                    Card(
                        shape = RoundedCornerShape(5.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 7.dp),
                        border = BorderStroke(2.dp, cardBorderColor)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            val titleColorLong = typeColor[g.typeName] ?: 0xFFEE44EE
                            val titleColor = Color(titleColorLong)
                            val typeBgColor = Color(titleColorLong + 0x333333)
                            val textColor = Color(titleColorLong - 0x666666)
                            // 제목
                            SelectionContainer {
                                Text(
                                    g.title,
                                    maxLines = 6,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(titleColor)
                                        .padding(2.dp),
                                    color = Color.White,
                                    style = TextStyle(
                                        shadow = Shadow(
                                            Color(0xFF000000),
                                            blurRadius = 7f
                                        ), fontSize = 21.sp
                                    )
                                )
                            }
                            // 타입
                            Text(
                                "종류: ${g.typeName}",
                                color = textColor,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(typeBgColor)
                                    .padding(horizontal = 3.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(color = titleColor)
                            )
                            // 작가
                            val artists =
                                g.tags.filter { tag -> tag.name.startsWith("artist:") }
                            if (artists.isNotEmpty()) {
                                FlowRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(color = typeBgColor)
                                        .padding(horizontal = 3.dp)
                                ) {
                                    Text("작가: ", color = textColor)
                                    for (i in artists.indices) {
                                        if (i != 0)
                                            Text(", ", color = textColor)
                                        MainTag(
                                            artists[i],
                                            textColor,
                                            dialogViewModel,
                                            isTagDialogOpen,
                                            navController
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(color = titleColor)
                                )
                            }
                            // 그룹
                            val groups = g.tags.filter { tag -> tag.name.startsWith("group:") }
                            if (groups.isNotEmpty()) {
                                FlowRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(color = typeBgColor)
                                        .padding(horizontal = 3.dp)
                                ) {
                                    Text("그룹: ", color = textColor)
                                    for (i in groups.indices) {
                                        if (i != 0)
                                            Text(", ", color = textColor)
                                        MainTag(
                                            groups[i],
                                            textColor,
                                            dialogViewModel,
                                            isTagDialogOpen,
                                            navController
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(color = titleColor)
                                )
                            }
                            // 시리즈
                            val parodies =
                                g.tags.filter { tag -> tag.name.startsWith("parody:") }
                            if (parodies.isNotEmpty()) {
                                FlowRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(color = typeBgColor)
                                        .padding(horizontal = 3.dp)
                                ) {
                                    Text("시리즈: ", color = textColor)
                                    for (i in parodies.indices) {
                                        if (i != 0)
                                            Text(", ", color = textColor)
                                        MainTag(
                                            parodies[i],
                                            textColor,
                                            dialogViewModel,
                                            isTagDialogOpen,
                                            navController
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(color = titleColor)
                                )
                            }
                            // 캐릭터
                            val characters =
                                g.tags.filter { tag -> tag.name.startsWith("character:") }
                            if (characters.isNotEmpty()) {
                                FlowRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(color = typeBgColor)
                                        .padding(horizontal = 3.dp)
                                ) {
                                    Text("캐릭터: ", color = textColor)
                                    for (i in characters.indices) {
                                        if (i != 0)
                                            Text(", ", color = textColor)
                                        MainTag(
                                            characters[i],
                                            textColor,
                                            dialogViewModel,
                                            isTagDialogOpen,
                                            navController
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(color = titleColor)
                                )
                            }
                            // 이미지 미리보기
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            // ggjs정보가 없는데 보면 에러남.
                                            if (hitomiViewModel.mList.isNotEmpty()) {
                                                isGalleryDetailDialogOpen.value = false
                                                navController.navigate(
                                                    Screen.ViewManga.createRoute(
                                                        g.gId
                                                    )
                                                )
                                            } else
                                                Toast.makeText(
                                                    context,
                                                    "서버 정보를 받아오는중입니다. 잠시 기다려주세요",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                        },
                                        onLongClick = {
                                            dialogViewModel.setGallery(g.gId)
                                            isGalleryDialogOpen.value = true
                                        }
                                    )
                            ) {
                                AsyncImage(
//                                    model = "https://tn.gold-usergeneratedcontent.net/webpbigtn/6/67/9a7943d3898b3f275ef4d5f6c9f012ee806e043f3c52fdc292caffbb9b988676.webp",
                                    model = ImageRequest.Builder(context)
                                        .data(realHitomiThumbnailUrl(g.thumb1))
                                        .diskCacheKey(g.thumb1).httpHeaders(hitomiHeaders)
                                        .build(),
                                    contentDescription = "thumbnail",
                                    placeholder = painterResource(R.drawable.loading),
                                    error = painterResource(R.drawable.errorimg),
                                    onError = { e -> Log.i("섬네일 에러", e.toString()) },
                                    contentScale = ContentScale.Crop,
                                    alignment = Alignment.TopStart,
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(1.dp, Color.Black),
                                )
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(realHitomiThumbnailUrl(g.thumb2))
                                        .diskCacheKey(g.thumb2).httpHeaders(hitomiHeaders)
                                        .build(),
                                    contentDescription = "thumbnail",
                                    placeholder = painterResource(R.drawable.loading),
                                    error = painterResource(R.drawable.errorimg),
                                    onError = { e -> Log.i("섬네일 에러", e.toString()) },
                                    contentScale = ContentScale.Crop,
                                    alignment = Alignment.TopStart,
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(1.dp, Color.Black),
                                )
                            }
                            // 나머지 태그들
                            val males = g.tags.filter { tag -> tag.name.startsWith("male:") }
                            val females =
                                g.tags.filter { tag -> tag.name.startsWith("female:") }
                            val others = g.tags.filter { tag ->
                                (!tag.name.startsWith("artist:") && !tag.name.startsWith("group:") && !tag.name.startsWith(
                                    "parody:"
                                )
                                        && !tag.name.startsWith("character:") && !tag.name.startsWith(
                                    "male:"
                                ) && !tag.name.startsWith("female:"))
                            }
                            FlowRow(
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.padding(3.dp)
                            ) {
                                for (tag in males) {
                                    SubTag(tag, dialogViewModel, isTagDialogOpen, navController)
                                }
                                for (tag in females) {
                                    SubTag(tag, dialogViewModel, isTagDialogOpen, navController)
                                }
                                for (tag in others) {
                                    SubTag(tag, dialogViewModel, isTagDialogOpen, navController)
                                }
                            }
                            // 갤러리id   포스트 시간   페이지 수
                            SelectionContainer {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // 갤러리 id
                                    Text(
                                        "${g.gId}",
                                        color = Color.Gray,
                                        modifier = Modifier.padding(horizontal = 5.dp)
                                    )
                                    // 포스트 시간
                                    Text(
                                        formatSavedDate(g.date),
                                        color = titleColor,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    // 페이지 수
                                    Text(
                                        "${g.filecount}p",
                                        color = Color.Gray,
                                        modifier = Modifier.padding(horizontal = 5.dp)
                                    )
                                }
                            }
                            // 기록이 있다면. 기록삭제, 마지막기록시간, 보던페이지
                            if (g.lastReadAt > 0) {
                                HorizontalDivider(Modifier.fillMaxWidth())
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "기록삭제",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        modifier = Modifier
                                            .padding(horizontal = 5.dp)
                                            .background(Color(0x77FF0000), RoundedCornerShape(3.dp))
                                            .clickable(onClick = {
                                                galleryViewModel.resetGalleryRecord(
                                                    g.gId
                                                )
                                                dialogViewModel.galleryDetailReloading()
                                            })
                                    )
                                    Text("기록:")
                                    Text(formatLongDate(g.lastReadAt))
                                    Text(
                                        "${g.lastReadPage}p",
                                        modifier = Modifier.padding(horizontal = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                } ?: Text("로딩중")
            }
        )
    }
}