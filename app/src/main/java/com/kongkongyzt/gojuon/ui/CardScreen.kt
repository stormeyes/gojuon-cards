package com.kongkongyzt.gojuon.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kongkongyzt.gojuon.R
import com.kongkongyzt.gojuon.audio.rememberKanaAudio
import com.kongkongyzt.gojuon.data.GOJUON
import com.kongkongyzt.gojuon.data.Kana
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private const val KANA_COUNT = 46
private const val VIRTUAL_PAGE_COUNT = 10_000
private val INITIAL_VIRTUAL_PAGE = (VIRTUAL_PAGE_COUNT / 2).let { it - it % KANA_COUNT }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CardScreen() {
    // ── 持久化状态 ──
    var showRomaji by rememberSaveable { mutableStateOf(true) }
    var randomOrder by rememberSaveable { mutableStateOf(false) }
    // 当前真实 kana 索引(在 GOJUON 中的 index, 0..45),独立于 pager 的 virtual page
    var currentRealIndex by rememberSaveable { mutableStateOf(0) }
    // 随机洗牌种子;非 random 模式时不参与计算
    var shuffleSeed by rememberSaveable { mutableStateOf(0L) }

    val audio = rememberKanaAudio()

    // 每个 kana 一个 playToken。点笔顺按钮 → ++,触发 StrokeAnimator 重置并播放。
    // 不持久化:杀进程后所有 token 归零(等价于"切到那张卡片是静态显示"),这是期望行为。
    val strokeTokens = remember { mutableStateMapOf<Int, Int>() }

    val orderedIndices: List<Int> = remember(randomOrder, shuffleSeed) {
        if (randomOrder) {
            (0 until KANA_COUNT).shuffled(java.util.Random(shuffleSeed))
        } else {
            (0 until KANA_COUNT).toList()
        }
    }

    // 当前 kana 在 orderedIndices 中的位置,用于初始化 pager
    val currentDisplayPos: Int = remember(orderedIndices, currentRealIndex) {
        orderedIndices.indexOf(currentRealIndex).coerceAtLeast(0)
    }

    val pagerState = rememberPagerState(
        initialPage = INITIAL_VIRTUAL_PAGE + currentDisplayPos,
        pageCount = { VIRTUAL_PAGE_COUNT }
    )
    val scope = rememberCoroutineScope()

    // ── 把 pager 当前页同步回 currentRealIndex(用户滑动时持续更新) ──
    LaunchedEffect(pagerState, orderedIndices) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { virtualPage ->
                val displayPos = ((virtualPage % KANA_COUNT) + KANA_COUNT) % KANA_COUNT
                currentRealIndex = orderedIndices[displayPos]
            }
    }

    // ── 切换 random 开关时:刷新洗牌种子(只在 randomOrder=true 时) ──
    LaunchedEffect(randomOrder) {
        if (randomOrder) {
            shuffleSeed = System.currentTimeMillis()
        }
    }

    // ── orderedIndices 变化(随机/顺序切换或重洗)时,把 pager 跳到当前 kana 的新位置 ──
    LaunchedEffect(orderedIndices) {
        val newPos = orderedIndices.indexOf(currentRealIndex)
        if (newPos >= 0) {
            val targetVirtual = INITIAL_VIRTUAL_PAGE + newPos
            // 用 scrollToPage(瞬时),不要 animateScrollToPage(会动画穿过中间所有页)
            pagerState.scrollToPage(targetVirtual)
        }
    }

    // 切换卡片时,把所有非当前卡片的 token 清零(下次访问时回到静态)
    LaunchedEffect(currentRealIndex) {
        val keep = currentRealIndex
        val toRemove = strokeTokens.keys.filter { it != keep }
        toRemove.forEach { strokeTokens.remove(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val ctx = androidx.compose.ui.platform.LocalContext.current
                    val attrText = androidx.compose.ui.res.stringResource(R.string.attribution)
                    Text(
                        text = "日语五十音",
                        modifier = Modifier.combinedClickable(
                            onClick = {},
                            onLongClick = {
                                android.widget.Toast.makeText(
                                    ctx, attrText, android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                        )
                    )
                },
                actions = {
                    SwitchLabel(
                        text = androidx.compose.ui.res.stringResource(R.string.toggle_romaji),
                        checked = showRomaji,
                        onCheckedChange = { showRomaji = it }
                    )
                    SwitchLabel(
                        text = androidx.compose.ui.res.stringResource(R.string.toggle_random),
                        checked = randomOrder,
                        onCheckedChange = { randomOrder = it }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.dp)
                    .weight(1f)
            ) { virtualPage ->
                val displayPos = ((virtualPage % KANA_COUNT) + KANA_COUNT) % KANA_COUNT
                val kanaIndex = orderedIndices[displayPos]
                val token = strokeTokens[kanaIndex] ?: 0
                CardContent(
                    kana = GOJUON[kanaIndex],
                    showRomaji = showRomaji,
                    strokePlayToken = token,
                    onTapKana = { audio.play(GOJUON[kanaIndex].audioRes) },
                    onTapStroke = { strokeTokens[kanaIndex] = (strokeTokens[kanaIndex] ?: 0) + 1 },
                )
            }

            BottomNav(
                positionLabel = "${currentDisplayPos + 1} / $KANA_COUNT",
                onPrev = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                onNext = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
            )
        }
    }
}

@Composable
private fun SwitchLabel(text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Text(text = text, fontSize = 12.sp)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun CardContent(
    kana: Kana,
    showRomaji: Boolean,
    strokePlayToken: Int,
    onTapKana: () -> Unit,
    onTapStroke: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 大假名 / 笔顺动画区域(同一位置,固定 size 防止动画切换抖动布局)
            Box(
                modifier = Modifier.size(260.dp),
                contentAlignment = Alignment.Center
            ) {
                if (strokePlayToken > 0) {
                    StrokeAnimator(
                        drawableRes = kana.drawableRes,
                        playToken = strokePlayToken,
                        modifier = Modifier.size(220.dp)
                    )
                } else {
                    Text(
                        text = kana.char,
                        fontSize = 220.sp,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.clickable { onTapKana() }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            if (showRomaji) {
                Text(text = kana.romaji, fontSize = 36.sp)
            } else {
                Spacer(Modifier.height(36.dp))
            }
            Spacer(Modifier.height(48.dp))
            OutlinedButton(onClick = onTapStroke) {
                Text(androidx.compose.ui.res.stringResource(R.string.btn_stroke))
            }
            Spacer(Modifier.height(8.dp))
            Text(text = kana.row, fontSize = 14.sp)
        }
    }
}

@Composable
private fun BottomNav(positionLabel: String, onPrev: () -> Unit, onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = onPrev) {
                Text("← " + androidx.compose.ui.res.stringResource(R.string.btn_prev))
            }
            Button(onClick = onNext) {
                Text(androidx.compose.ui.res.stringResource(R.string.btn_next) + " →")
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(text = positionLabel, fontSize = 14.sp)
    }
}
