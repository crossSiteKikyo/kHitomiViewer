package com.example.khitomiviewer.screen

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import com.example.khitomiviewer.R
import com.example.khitomiviewer.room.entity.Tag
import com.example.khitomiviewer.viewmodel.KHitomiViewerViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(
    navController: NavHostController,
    mainViewModel: KHitomiViewerViewModel,
    page: String?, tagIdListJson: String?, titleKeyword: String?
) {
    val context = LocalContext.current
    val hitomiHeaders = NetworkHeaders.Builder().set("Referer", "https://hitomi.la/").build()
    val pageCount by rememberUpdatedState(mainViewModel.pageCount.collectAsState().value)
    val galleries by remember { derivedStateOf { mainViewModel.galleries } }
    var pageNum by remember { mutableStateOf(if(page==null) "1" else page) }
    val showDialog = remember { mutableStateOf(false) }
    val typeColor: Map<String, Long> = remember { mapOf<String, Long>(
        "doujinshi" to 0xFFCC9999,  // c99
        "manga" to 0xFFCC99CC,      // c9c
        "artistcg" to 0xFF99CCCC,  // 9cc
        "gamecg" to 0xFF9999CC,    // 99c
        "imageset" to 0xFF999999,  // 999
        // CC보다 크면 안되고 66보다 작으면 안된다.
    ) }
    // th.hitomi.la는 가짜 url이다. 진짜 url로 바꾼다.
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

    val coroutineScope = rememberCoroutineScope()
    val scrollState: ScrollState = rememberScrollState()
    val interactionSource = remember { MutableInteractionSource() }
    var visible by remember { mutableStateOf(true) }
    var isDragging = interactionSource.collectIsDraggedAsState()

    LaunchedEffect(page, tagIdListJson, titleKeyword) {
        if (page != null) {
            mainViewModel.setGalleryList(page.toLong(), tagIdListJson, titleKeyword)
        }
    }

    // 값이 변할 때마다 타이머 재시작
    LaunchedEffect(scrollState.value, isDragging.value) {
        if(!isDragging.value) {
            visible = true
            delay(1000L) // 1초 후 자동 숨김
            visible = false
        }
        else {
            visible = true
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {navController.navigate("MainSettingScreen")}
            ) {
                Icon(Icons.Filled.Settings, "setting")
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if(mainViewModel.loading.value) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState),
                ) {
                    // 다이얼로그
                    if(showDialog.value) {
                        val likeStatusIntToStr = listOf("싫어요 \uD83C\uDD96", "기본", "좋아요 ♥")
                        AlertDialog(
                            onDismissRequest = {},
                            confirmButton = {TextButton(onClick = { showDialog.value = false }) {
                                Text("닫기")
                            }},
                            title = {
                                val what = if(mainViewModel.whatStr.value == "tag") "태그" else "갤러리"
                                Text("${what} - ${mainViewModel.selectedTagNameOrGalleryId.value}")
                            },
                            text = {
                                Column (
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ){
                                    for (x in 0..2) {
                                        if(x == mainViewModel.likeStatus.intValue)
                                            Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text(likeStatusIntToStr[x])}
                                        else
                                            Button(onClick = {
                                                // db에 상태 변경
                                                mainViewModel.changeLike(x, "ListScreen")
                                                // 다이얼로그 닫기
                                                showDialog.value = false
                                            }) { Text(likeStatusIntToStr[x])}
                                    }
                                }
                            })
                    }
                    SelectionContainer {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // 제목검색일경우
                            if(!titleKeyword.isNullOrBlank()) {
                                Box(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                                    Text(
                                        "제목 - ${mainViewModel.lastTitleKeyword}",
                                        fontWeight = FontWeight.Bold,
                                        style = TextStyle(
                                            shadow = Shadow(Color.Cyan, blurRadius = 5f),
                                            fontSize = 25.sp
                                        )
                                    )
                                }
                            }
                            // 태그검색일경우
                            if(mainViewModel.lastTags.isNotEmpty()) {
                                Box(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                                    Text(
                                        "태그 - ${mainViewModel.lastTags.joinToString(", ") { tag -> tag.name }}",
                                        fontWeight = FontWeight.Bold,
                                        style = TextStyle(
                                            shadow = Shadow(Color.Cyan, blurRadius = 5f),
                                            fontSize = 25.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                    if(galleries.isEmpty())
                        Text("결과가 없습니다", style = TextStyle(fontSize = 50.sp))
                    else {
                        // 갤러리마다 카드 만들기
                        for (g in galleries) {
                            val cardBorderColor:Color = when(g.likeStatus) {
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
                                Column (
                                    modifier = Modifier.fillMaxWidth()
                                ){
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
                                            style = TextStyle(shadow = Shadow(Color(0xFF000000), blurRadius = 7f), fontSize = 21.sp)
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
                                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(color = titleColor))
                                    // 작가
                                    val artists = g.tags.filter { tag -> tag.name.startsWith("artist:") }
                                    if(artists.isNotEmpty()) {
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth().background(color = typeBgColor).padding(horizontal = 3.dp)
                                        ) {
                                            Text("작가: ", color = textColor)
                                            for (i in artists.indices) {
                                                if(i != 0)
                                                    Text(", ", color = textColor)
                                                MainTagText(artists[i], textColor, mainViewModel, showDialog, navController)
                                            }
                                        }
                                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(color = titleColor))
                                    }
                                    // 그룹
                                    val groups = g.tags.filter { tag -> tag.name.startsWith("group:") }
                                    if(groups.isNotEmpty()) {
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth().background(color = typeBgColor).padding(horizontal = 3.dp)
                                        ) {
                                            Text("그룹: ", color = textColor)
                                            for (i in groups.indices) {
                                                if(i != 0)
                                                    Text(", ", color = textColor)
                                                MainTagText(groups[i], textColor, mainViewModel, showDialog, navController)
                                            }
                                        }
                                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(color = titleColor))
                                    }
                                    // 시리즈
                                    val parodies = g.tags.filter { tag -> tag.name.startsWith("parody:") }
                                    if(parodies.isNotEmpty()) {
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth().background(color = typeBgColor).padding(horizontal = 3.dp)
                                        ) {
                                            Text("시리즈: ", color = textColor)
                                            for (i in parodies.indices) {
                                                if(i != 0)
                                                    Text(", ", color = textColor)
                                                MainTagText(parodies[i], textColor, mainViewModel, showDialog, navController)
                                            }
                                        }
                                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(color = titleColor))
                                    }
                                    // 캐릭터
                                    val characters = g.tags.filter { tag -> tag.name.startsWith("character:") }
                                    if(characters.isNotEmpty()) {
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth().background(color = typeBgColor).padding(horizontal = 3.dp)
                                        ) {
                                            Text("캐릭터: ", color = textColor)
                                            for (i in characters.indices) {
                                                if(i != 0)
                                                    Text(", ", color = textColor)
                                                MainTagText(characters[i], textColor, mainViewModel, showDialog, navController)
                                            }
                                        }
                                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(color = titleColor))
                                    }
                                    // 이미지 미리보기
                                    Row (
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = {
                                                    // ggjs정보가 없는데 보면 에러남.
                                                    if(mainViewModel.mList.isNotEmpty())
                                                        navController.navigate("MangaViewScreen/${g.gId}")
                                                    else
                                                        Toast.makeText(context, "서버 정보를 받아오는중입니다. 잠시 기다려주세요", Toast.LENGTH_SHORT).show()
                                                },
                                                onLongClick = {
                                                    mainViewModel.setDialog("gallery", g.gId.toString())
                                                    showDialog.value = true
                                                }
                                            )
                                    ){
                                        AsyncImage(
//                                    model = "https://tn.gold-usergeneratedcontent.net/webpbigtn/6/67/9a7943d3898b3f275ef4d5f6c9f012ee806e043f3c52fdc292caffbb9b988676.webp",
                                            model = ImageRequest.Builder(context).data(realHitomiThumbnailUrl(g.thumb1))
                                                .diskCacheKey(g.thumb1).httpHeaders(hitomiHeaders).build(),
                                            contentDescription = "thumbnail",
                                            placeholder = painterResource(R.drawable.loading),
                                            error = painterResource(R.drawable.errorimg),
                                            onError = {e -> Log.i("섬네일 에러", e.toString())},
                                            contentScale = ContentScale.Crop,
                                            alignment = Alignment.TopStart,
                                            modifier = Modifier
                                                .fillMaxWidth(0.5f)
                                                .border(1.dp, Color.Black),
                                        )
                                        AsyncImage(
                                            model = ImageRequest.Builder(context).data(realHitomiThumbnailUrl(g.thumb2))
                                                .diskCacheKey(g.thumb2).httpHeaders(hitomiHeaders).build(),
                                            contentDescription = "thumbnail",
                                            placeholder = painterResource(R.drawable.loading),
                                            error = painterResource(R.drawable.errorimg),
                                            onError = {e -> Log.i("섬네일 에러", e.toString())},
                                            contentScale = ContentScale.Crop,
                                            alignment = Alignment.TopStart,
                                            modifier = Modifier
                                                .fillMaxWidth(1f)
                                                .border(1.dp, Color.Black),
                                        )
                                    }
                                    // 나머지 태그들
                                    val males = g.tags.filter { tag -> tag.name.startsWith("male:") }
                                    val females = g.tags.filter { tag -> tag.name.startsWith("female:") }
                                    val others = g.tags.filter { tag -> (!tag.name.startsWith("artist:") && !tag.name.startsWith("group:") && !tag.name.startsWith("parody:")
                                            && !tag.name.startsWith("character:") && !tag.name.startsWith("male:") && !tag.name.startsWith("female:"))}
                                    FlowRow(
                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        modifier = Modifier.padding(3.dp)
                                    ) {
                                        for (tag in males) {
                                            TagText(tag, mainViewModel, showDialog, navController)
                                        }
                                        for (tag in females) {
                                            TagText(tag, mainViewModel, showDialog, navController)
                                        }
                                        for (tag in others) {
                                            TagText(tag, mainViewModel, showDialog, navController)
                                        }
                                    }
                                    // 갤러리id   포스트 시간   페이지 수
                                    SelectionContainer {
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
                            }
                        }
                    }
                    // 페이징
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        if (page != null) {
                            val p = page.toLong()
                            // 1페이지는 현재 페이지가 2이상이면 보임
                            if (p >= 2)
                                PageButton(navController, true, 1, tagIdListJson, titleKeyword)
                            // ...은 현재 페이지가 4 이상이면 보임
                            if (p >= 4)
                                Text("...")
                            // 전 페이지는 현재페이지가 3이상이면 보임
                            if (p >= 3)
                                PageButton(navController, true, p - 1, tagIdListJson, titleKeyword)
                            // 현재 페이지는 항상 보임
                            PageButton(navController, false, p, tagIdListJson, titleKeyword)
                            // 후 페이지는 마지막페이지 - 현재페이지가 2이상일경우 보임
                            if ((pageCount - p) >= 2)
                                PageButton(navController, true, p + 1, tagIdListJson, titleKeyword)
                            // ...은 마지막페이지 - 현페이지가 3 이상일경우 보임
                            if ((pageCount - p) >= 3)
                                Text("...")
                            // 마지막 페이지는 현재 페이지가 마지막이 아니면 보임
                            if (p != pageCount && pageCount >= 2)
                                PageButton(navController, true, pageCount, tagIdListJson, titleKeyword)
                        }
                    }
                    // 숫자로 페이지 이동
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = pageNum,
                            onValueChange = {
                                if (it.isEmpty() || it.matches("""^\d+$""".toRegex())) {
                                    pageNum = it
                                    if(pageNum.isNotEmpty() && (pageNum.toLong() > pageCount))
                                        pageNum = pageCount.toString()
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            maxLines = 1,
                            modifier = Modifier.width(200.dp)
                        )
                        Button(
                            shape = RoundedCornerShape(2.dp), // 덜 둥글게 (기본은 20.dp 이상)
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp), // 패딩 축소
                            modifier = Modifier.height(56.dp), // 버튼 높이도 줄일 수 있음
                            onClick = {
                                if(pageNum.isNotEmpty() && (pageNum != page))
                                    navController.navigate("ListScreen/$pageNum/${tagIdListJson}/${titleKeyword}")
                            }
                        ) { Text("페이지 이동") }
                    }
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    Slider(
                        value = scrollState.value.toFloat(),
                        valueRange = 0f..scrollState.maxValue.toFloat(),
                        onValueChange = { v ->
                            coroutineScope.launch {
                                scrollState.scrollTo(v.toInt())
                            }
                        },
                        interactionSource = interactionSource,
                        colors = SliderDefaults.colors(
                            activeTrackColor  = Color.Transparent,
                            inactiveTrackColor = Color.Transparent,
                        ),
                        thumb = {
                            Row(
                                modifier = Modifier.background(Color(0xFF9933FF), shape = RoundedCornerShape(10.dp))
                            ) {
                                if (visible) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                        contentDescription = null,
                                        modifier = Modifier.width(25.dp)
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        modifier = Modifier.width(25.dp)
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .graphicsLayer{
                                rotationZ = 90f
                                transformOrigin = TransformOrigin(0f, 0f)
                            }
                            .layout { measurable, constraints ->
                                val placeable = measurable.measure(
                                    Constraints(
                                        minWidth = constraints.minHeight,
                                        maxWidth = constraints.maxHeight,
                                        minHeight = constraints.minWidth,
                                        maxHeight = constraints.maxWidth
                                    )
                                )
                                layout(placeable.height, placeable.width) {
                                    placeable.place(0, -placeable.height)
                                }
                            }
                            .fillMaxWidth()
                    )
                }
            }

        }
    }
}

@Composable
fun MainTagText(tag: Tag, textColor:Color, mainViewModel: KHitomiViewerViewModel, showDialog: MutableState<Boolean>, navController: NavHostController) {
    fun tagClick() {
        navController.navigate("ListScreen/1/${Json.encodeToString(listOf(tag.tagId))}/")
    }
    var name = when(tag.likeStatus) {
        2 -> tag.name + "♥"
        else -> tag.name
    }
    var color: Color = textColor
    if(name.startsWith("parody:")) {
        name = name.replace("parody:", "")
    }
    else if(name.startsWith("character:")) {
        name = name.replace("character:", "")
    }
    else if(name.startsWith("group:")) {
        name = name.replace("group:", "")
    }
    else if(name.startsWith("artist:")) {
        name = name.replace("artist:", "")
    }

    Text(
        name,
        color = color,
        modifier = Modifier
            .combinedClickable(
                onClick = { tagClick() },
                onLongClick = {
                    mainViewModel.setDialog("tag", tag.name)
                    showDialog.value = true
                }
            )
            .clip(RoundedCornerShape(5.dp))
            .padding(horizontal = 2.dp)
    )
}

@Composable
fun TagText(tag: Tag, mainViewModel: KHitomiViewerViewModel, showDialog: MutableState<Boolean>, navController: NavHostController) {
    fun tagClick() {
        navController.navigate("ListScreen/1/${Json.encodeToString(listOf(tag.tagId))}/")
    }
    var name = when(tag.likeStatus) {
        2 -> tag.name + "♥"
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

@Composable
fun PageButton(navController: NavController, enable: Boolean, page: Long, tagIdListJson: String?, titleKeyword: String?) {
    Button(
        onClick = { navController.navigate("ListScreen/$page/${tagIdListJson}/${titleKeyword}") },
        shape = RoundedCornerShape(8.dp), // 덜 둥글게 (기본은 20.dp 이상)
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp), // 패딩 축소
        modifier = Modifier.height(36.dp), // 버튼 높이도 줄일 수 있음
        enabled = enable,
    ) { Text("$page") }
}