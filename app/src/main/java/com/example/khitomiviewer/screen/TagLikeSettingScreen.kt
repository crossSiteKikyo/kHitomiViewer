package com.example.khitomiviewer.screen

import android.content.Context
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import com.example.khitomiviewer.R
import com.example.khitomiviewer.room.TagFullDto
import com.example.khitomiviewer.viewmodel.KHitomiViewerViewModel
import com.example.khitomiviewer.viewmodel.LikeSettingViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagLikeSettingScreen(navController: NavHostController, mainViewModel: KHitomiViewerViewModel) {
    val viewModel: LikeSettingViewModel = viewModel()
    val context = LocalContext.current
    val showDialog = remember { mutableStateOf(false) }
    val tabs = listOf("태그 좋아요", "태그 싫어요")
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val hitomiHeaders = NetworkHeaders.Builder().set("Referer", "https://hitomi.la/").build()
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

    LaunchedEffect("tag") {
        viewModel.setTagOrGalleryList("tag")
    }

    val coroutineScope = rememberCoroutineScope()
    val scrollState: ScrollState = rememberScrollState()
    val interactionSource = remember { MutableInteractionSource() }
    var visible by remember { mutableStateOf(true) }
    var isDragging = interactionSource.collectIsDraggedAsState()

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
            Row (
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                FloatingActionButton(
                    onClick = {coroutineScope.launch {scrollState.scrollTo(0)}},
                    modifier = Modifier.size(50.dp)
                ) {
                    Icon(Icons.Filled.KeyboardArrowUp, "top")
                }

                FloatingActionButton(
                    onClick = {coroutineScope.launch {scrollState.scrollTo(scrollState.maxValue)}},
                    modifier = Modifier.size(50.dp)
                ) {
                    Icon(Icons.Filled.KeyboardArrowDown, "bottom")
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TabRow(
                    selectedTabIndex = selectedTabIndex
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = index == selectedTabIndex,
                            onClick = {
                                selectedTabIndex = index
                                if(index == 0) // 좋아요 태그
                                    viewModel.loadTag(2)
                                else // 싫어요 태그
                                    viewModel.loadTag(0)
                            },
                            text = { Text(title) }
                        )
                    }
                }
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
                val artists = viewModel.tags.filter { tag -> tag.name.startsWith("artist:") }
                val groups = viewModel.tags.filter { tag -> tag.name.startsWith("group:") }
                val parodies = viewModel.tags.filter { tag -> tag.name.startsWith("parody:") }
                val characters = viewModel.tags.filter { tag -> tag.name.startsWith("character:") }
                val males = viewModel.tags.filter { tag -> tag.name.startsWith("male:") }
                val females = viewModel.tags.filter { tag -> tag.name.startsWith("female:") }
                val others = viewModel.tags.filter { tag -> (!tag.name.startsWith("artist:") && !tag.name.startsWith("group:") && !tag.name.startsWith("parody:")
                        && !tag.name.startsWith("character:") && !tag.name.startsWith("male:") && !tag.name.startsWith("female:"))}

                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if(artists.isNotEmpty()) {
                        for (i in artists.indices) {
                            MainTagCard(artists[i], viewModel, showDialog, navController, hitomiHeaders, realHitomiThumbnailUrl, context)
                        }
                    }
                    if(groups.isNotEmpty()) {
                        for (i in groups.indices) {
                            MainTagCard(groups[i], viewModel, showDialog, navController, hitomiHeaders, realHitomiThumbnailUrl, context)
                        }
                    }
                    if(parodies.isNotEmpty()) {
                        for (i in parodies.indices) {
                            MainTagCard(parodies[i], viewModel, showDialog, navController, hitomiHeaders, realHitomiThumbnailUrl, context)
                        }
                    }
                    if(characters.isNotEmpty()) {
                        for (i in characters.indices) {
                            MainTagCard(characters[i], viewModel, showDialog, navController, hitomiHeaders, realHitomiThumbnailUrl, context)
                        }
                    }
                    if(males.isNotEmpty()) {
                        for (tag in males) {
                            TagTextCard(tag, viewModel, showDialog, navController, hitomiHeaders, realHitomiThumbnailUrl, context)
                        }
                    }
                    if(females.isNotEmpty()) {
                        for (tag in females) {
                            TagTextCard(tag, viewModel, showDialog, navController, hitomiHeaders, realHitomiThumbnailUrl, context)
                        }
                    }
                    if(others.isNotEmpty()) {
                        for (tag in others) {
                            TagTextCard(tag, viewModel, showDialog, navController, hitomiHeaders, realHitomiThumbnailUrl, context)
                        }
                    }
                    if(viewModel.tags.isEmpty())
                        Text("싫어요, 좋아요 태그가 없습니다", style = TextStyle(fontSize = 50.sp))
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

@Composable
fun MainTagCard(tag: TagFullDto, viewModel: LikeSettingViewModel, showDialog: MutableState<Boolean>,
                navController: NavHostController, hitomiHeaders: NetworkHeaders, realHitomiThumbnailUrl: (String) -> String,
                context: Context
) {
    fun tagClick() {
        navController.navigate("ListScreen/1/${Json.encodeToString(listOf(tag.tagId))}/")
    }
    var name = when(tag.likeStatus) {
        0 -> tag.name + " \uD83C\uDD96"
        2 -> tag.name + " ♥"
        else -> tag.name
    }
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
    val titleColorLong = 0xFFCC9999
    val titleColor = Color(titleColorLong)
    val typeBgColor = Color(titleColorLong + 0x333333)
    val textColor = Color(titleColorLong - 0x666666)
    val cardBorderColor:Color = when(tag.likeStatus) {
        2 -> Color(0xFFCC00CC)
        else -> Color.Gray
    }
    Card(
        shape = RoundedCornerShape(5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        border = BorderStroke(2.dp, cardBorderColor)
    ) {
        Text(
            name,
            color = textColor,
            modifier = Modifier
                .combinedClickable(
                    onClick = { tagClick() },
                    onLongClick = {
                        viewModel.setDialog("tag", tag.name)
                        showDialog.value = true
                    }
                )
                .background(color = typeBgColor)
                .clip(RoundedCornerShape(5.dp))
                .padding(horizontal = 2.dp)
                .fillMaxWidth()
        )
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(color = titleColor))
        Row (
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
        ){
            for (i in tag.galleries.indices) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(realHitomiThumbnailUrl(tag.galleries[i].thumb1))
                        .httpHeaders(hitomiHeaders).build(),
                    contentDescription = "thumbnail",
                    placeholder = painterResource(R.drawable.loading),
                    error = painterResource(R.drawable.errorimg),
                    onError = {e -> Log.i("섬네일 에러", e.toString())},
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopStart,
                    modifier = Modifier
                        .fillMaxWidth(if(i == 0) 0.33f else if (i == 1) 0.5f else 1f)
                        .border(1.dp, Color.Black),
                )
            }
        }
    }

}

@Composable
fun TagTextCard(tag: TagFullDto, mainViewModel: LikeSettingViewModel, showDialog: MutableState<Boolean>,
                navController: NavHostController, hitomiHeaders: NetworkHeaders, realHitomiThumbnailUrl: (String) -> String,
                context: Context
) {
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
    val cardBorderColor:Color = when(tag.likeStatus) {
        2 -> Color(0xFFCC00CC)
        else -> Color.Gray
    }
    Card(
        shape = RoundedCornerShape(5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        border = BorderStroke(2.dp, cardBorderColor)
    ) {
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
                .fillMaxWidth()
        )
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(color = Color.Black))
        Row (
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
        ){
            for (i in tag.galleries.indices) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(realHitomiThumbnailUrl(tag.galleries[i].thumb1))
                        .httpHeaders(hitomiHeaders).build(),
                    contentDescription = "thumbnail",
                    placeholder = painterResource(R.drawable.loading),
                    error = painterResource(R.drawable.errorimg),
                    onError = {e -> Log.i("섬네일 에러", e.toString())},
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopStart,
                    modifier = Modifier
                        .fillMaxWidth(if(i == 0) 0.33f else if (i == 1) 0.5f else 1f)
                        .border(1.dp, Color.Black),
                )
            }
        }
    }
}