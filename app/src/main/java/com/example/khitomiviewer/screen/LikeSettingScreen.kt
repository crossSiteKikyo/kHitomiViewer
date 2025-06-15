package com.example.khitomiviewer.screen

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
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
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import com.example.khitomiviewer.R
import com.example.khitomiviewer.room.entity.Tag
import com.example.khitomiviewer.viewmodel.KHitomiViewerViewModel
import com.example.khitomiviewer.viewmodel.LikeSettingViewModel
import kotlinx.serialization.json.Json

@Composable
fun LikeSettingScreen(navController: NavHostController, mainViewModel: KHitomiViewerViewModel, tagOrGallery:String?) {
    val title = if(tagOrGallery == "tag") "태그 좋아요/싫어요 관리" else "갤러리 좋아요/싫어요 관리"
    val viewModel: LikeSettingViewModel = viewModel()
    val context = LocalContext.current
    val showDialog = remember { mutableStateOf(false) }

    LaunchedEffect(tagOrGallery) {
        if (tagOrGallery != null) {
            viewModel.setTagOrGalleryList(tagOrGallery)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(5.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(title, fontWeight = FontWeight.Bold, style = TextStyle(shadow = Shadow(Color.Cyan, blurRadius = 5f), fontSize = 21.sp))
                HorizontalDivider(thickness = 2.dp)
                // 다이얼로그
                if(showDialog.value) {
                    val likeStatusIntToStr = listOf("싫어요 \uD83C\uDD96", "기본", "좋아요 ♥")
                    AlertDialog(
                        onDismissRequest = {},
                        confirmButton = {TextButton(onClick = { showDialog.value = false }) {
                            Text("닫기")
                        }},
                        title = {
                            val what = if(viewModel.whatStr.value == "tag") "태그" else "갤러리"
                            Text("${what} - ${viewModel.selectedTagNameOrGalleryId.value}")
                        },
                        text = {
                            Column (
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ){
                                for (x in 0..2) {
                                    if(x == viewModel.likeStatus.intValue)
                                        Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text(likeStatusIntToStr[x])}
                                    else
                                        Button(onClick = {
                                            // db에 상태 변경
                                            viewModel.changeLike(x)
                                            // 다이얼로그 닫기
                                            showDialog.value = false
                                        }) { Text(likeStatusIntToStr[x])}
                                }
                            }
                        })
                }
                if(tagOrGallery == "tag") {
                    val artists = viewModel.tags.filter { tag -> tag.name.startsWith("artist:") }
                    val groups = viewModel.tags.filter { tag -> tag.name.startsWith("group:") }
                    val parodies = viewModel.tags.filter { tag -> tag.name.startsWith("parody:") }
                    val characters = viewModel.tags.filter { tag -> tag.name.startsWith("character:") }
                    val males = viewModel.tags.filter { tag -> tag.name.startsWith("male:") }
                    val females = viewModel.tags.filter { tag -> tag.name.startsWith("female:") }
                    val others = viewModel.tags.filter { tag -> (!tag.name.startsWith("artist:") && !tag.name.startsWith("group:") && !tag.name.startsWith("parody:")
                            && !tag.name.startsWith("character:") && !tag.name.startsWith("male:") && !tag.name.startsWith("female:"))}

                    val titleColorLong = 0xFFCC9999
                    val typeBgColor = Color(titleColorLong + 0x333333)
                    val textColor = Color(titleColorLong - 0x666666)

                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if(artists.isNotEmpty()) {
                            for (i in artists.indices) {
                                MainTagText2(artists[i], textColor, typeBgColor, viewModel, showDialog, navController)
                            }
                        }
                        if(groups.isNotEmpty()) {
                            for (i in groups.indices) {
                                MainTagText2(groups[i], textColor, typeBgColor, viewModel, showDialog, navController)
                            }
                        }
                        if(parodies.isNotEmpty()) {
                            for (i in parodies.indices) {
                                MainTagText2(parodies[i], textColor, typeBgColor, viewModel, showDialog, navController)
                            }
                        }
                        if(characters.isNotEmpty()) {
                            for (i in characters.indices) {
                                MainTagText2(characters[i], textColor, typeBgColor, viewModel, showDialog, navController)
                            }
                        }
                        if(males.isNotEmpty()) {
                            for (tag in males) {
                                TagText2(tag, viewModel, showDialog, navController)
                            }
                        }
                        if(females.isNotEmpty()) {
                            for (tag in females) {
                                TagText2(tag, viewModel, showDialog, navController)
                            }
                        }
                        if(others.isNotEmpty()) {
                            for (tag in others) {
                                TagText2(tag, viewModel, showDialog, navController)
                            }
                        }
                        if(viewModel.tags.isEmpty())
                            Text("싫어요, 좋아요 태그가 없습니다", style = TextStyle(fontSize = 50.sp))
                    }
                }
                else {
                    val hitomiHeaders = NetworkHeaders.Builder().set("Referer", "https://hitomi.la/").build()
                    val typeColor: Map<Int, Long> = remember { mapOf<Int, Long>(
                        1 to 0xFFCC9999,  // c99 "doujinshi"
                        2 to 0xFFCC99CC,  // c9c "manga"
                        3 to 0xFF99CCCC,  // 9cc "artistcg"
                        4 to 0xFF9999CC,  // 99c "gamecg"
                        5 to 0xFF999999,  // 999 "imageset"
                        // CC보다 크면 안되고 66보다 작으면 안된다.
                    ) }
                    val realHitomiThumbnailUrl: (String) -> String = { url ->
                        val hash = """[0-9a-z]{40,}""".toRegex().find(url)?.value
                        if (hash == null) {
                            url
                        }
                        else {
                            val s = "${hash[hash.length-1]}${hash[hash.length-3]}${hash[hash.length-2]}".toInt(16).toString(10)
                            val ch = if(s in mainViewModel.mList) mainViewModel.mNextThumbChar.value else mainViewModel.mDefaultThumbChar.value
                            url.replace("tn.hitomi.la", "${ch}tn.gold-usergeneratedcontent.net")
                        }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (g in viewModel.galleries) {
                            val cardBorderColor:Color = when(g.likeStatus) {
                                2 -> Color(0xFFCC00CC)
                                else -> Color.Gray
                            }
                            val widthFloat1:Float = when(g.likeStatus) {
                                2 -> 0.5f
                                else -> 0.2f
                            }
                            val widthFloat2:Float = when(g.likeStatus) {
                                2 -> 1f
                                else -> 0.25f
                            }
                            Card(
                                shape = RoundedCornerShape(5.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 7.dp),
                                border = BorderStroke(2.dp, cardBorderColor)
                            ) {
                                val titleColorLong = typeColor[g.typeId.toInt()] ?: 0xFFEE44EE
                                val titleColor = Color(titleColorLong)
                                // 제목
                                Text(
                                    g.title.replace("""\[[\s\S]+?\]""".toRegex(), "").replace("""\([\s\S]+?\)""".toRegex(), "").trim(),
                                    maxLines = 5,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(titleColor)
                                        .padding(2.dp),
                                    color = Color.White,
                                    style = TextStyle(shadow = Shadow(Color(0xFF000000), blurRadius = 7f), fontSize = 21.sp)
                                )
                                // 이미지 미리보기
                                Row (
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {
                                                navController.navigate("MangaViewScreen/${g.gId}")
                                            },
                                            onLongClick = {
                                                viewModel.setDialog("gallery", g.gId.toString())
                                                showDialog.value = true
                                            }
                                        )
                                ){
                                    AsyncImage(
//                                    model = "https://tn.gold-usergeneratedcontent.net/webpbigtn/6/67/9a7943d3898b3f275ef4d5f6c9f012ee806e043f3c52fdc292caffbb9b988676.webp",
                                        model = ImageRequest.Builder(context).data(realHitomiThumbnailUrl(g.thumb1))
                                            .httpHeaders(hitomiHeaders).build(),
                                        contentDescription = "thumbnail",
                                        placeholder = painterResource(R.drawable.loading),
                                        error = painterResource(R.drawable.errorimg),
                                        onError = {e -> Log.i("섬네일 에러", e.toString())},
                                        contentScale = ContentScale.Crop,
                                        alignment = Alignment.TopStart,
                                        modifier = Modifier
                                            .fillMaxWidth(widthFloat1)
                                            .border(1.dp, Color.Black),
                                    )
                                    AsyncImage(
                                        model = ImageRequest.Builder(context).data(realHitomiThumbnailUrl(g.thumb2))
                                            .httpHeaders(hitomiHeaders).build(),
                                        contentDescription = "thumbnail",
                                        placeholder = painterResource(R.drawable.loading),
                                        error = painterResource(R.drawable.errorimg),
                                        onError = {e -> Log.i("섬네일 에러", e.toString())},
                                        contentScale = ContentScale.Crop,
                                        alignment = Alignment.TopStart,
                                        modifier = Modifier
                                            .fillMaxWidth(widthFloat2)
                                            .border(1.dp, Color.Black),
                                    )
                                }
                                // 갤러리id   포스트 시간   페이지 수
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // 갤러리 id
                                    Text("${g.gId}",
                                        color = Color.Gray,
                                        modifier = Modifier.padding(horizontal = 5.dp)
                                    )
                                    // 포스트 시간
                                    Text(
                                        g.date,
                                        color = titleColor,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    // 페이지 수
                                    Text("${g.filecount}p",
                                        color = Color.Gray,
                                        modifier = Modifier.padding(horizontal = 5.dp)
                                    )
                                }
                            }
                        }
                        if(viewModel.galleries.isEmpty())
                            Text("싫어요, 좋아요 갤러리가 없습니다", style = TextStyle(fontSize = 50.sp))
                    }
                }
            }
        }
    }
}

@Composable
fun MainTagText2(tag: Tag, textColor:Color, bgColor:Color, viewModel: LikeSettingViewModel, showDialog: MutableState<Boolean>, navController: NavHostController) {
    fun tagClick() {
        navController.navigate("ListScreen/1/${Json.encodeToString(listOf(tag.tagId))}/")
    }
    var name = when(tag.likeStatus) {
        0 -> tag.name + " \uD83C\uDD96"
        2 -> tag.name + " ♥"
        else -> tag.name
    }
    var color: Color = textColor
    if(name.startsWith("parody:")) {
        name = name.replace("parody:", "시리즈:")
    }
    else if(name.startsWith("character:")) {
        name = name.replace("character:", "캐릭터:")
    }
    else if(name.startsWith("group:")) {
        name = name.replace("group:", "그룹:")
    }
    else if(name.startsWith("artist:")) {
        name = name.replace("artist:", "작가:")
    }
    Text(
        name,
        color = color,
        modifier = Modifier
            .combinedClickable(
                onClick = { tagClick() },
                onLongClick = {
                    viewModel.setDialog("tag", tag.name)
                    showDialog.value = true
                }
            )
            .background(color = bgColor)
            .clip(RoundedCornerShape(5.dp))
            .padding(horizontal = 2.dp)
    )
}

@Composable
fun TagText2(tag: Tag, mainViewModel: LikeSettingViewModel, showDialog: MutableState<Boolean>, navController: NavHostController) {
    fun tagClick() {
        navController.navigate("ListScreen/1/${Json.encodeToString(listOf(tag.tagId))}/")
    }
    var name = when(tag.likeStatus) {
        0 -> tag.name + " \uD83C\uDD96"
        2 -> tag.name + " ♥"
        else -> tag.name
    }
    var bgColor: Color = Color.Gray
    if(name.startsWith("male:")) {
        name = name.replace("male:", "")
        bgColor = Color(0xFF0080FF)
    }
    else if(name.startsWith("female:")) {
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
                    mainViewModel.setDialog("tag", tag.name)
                    showDialog.value = true
                }
            )
            .clip(RoundedCornerShape(5.dp))
            .background(bgColor)
            .padding(horizontal = 2.dp)
    )
}