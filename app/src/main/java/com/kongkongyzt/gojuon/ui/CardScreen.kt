package com.kongkongyzt.gojuon.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kongkongyzt.gojuon.R
import com.kongkongyzt.gojuon.data.GOJUON
import com.kongkongyzt.gojuon.data.Kana
import kotlinx.coroutines.launch

/**
 * 卡片总数。固定 46(清音平假名)。
 * Pager 用 [VIRTUAL_PAGE_COUNT] 模拟无限滚动以支持首尾循环。
 */
private const val KANA_COUNT = 46
private const val VIRTUAL_PAGE_COUNT = 10_000  // 大数模拟无限;中点附近开始
private val INITIAL_VIRTUAL_PAGE = (VIRTUAL_PAGE_COUNT / 2).let { it - it % KANA_COUNT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardScreen() {
    val pagerState = rememberPagerState(
        initialPage = INITIAL_VIRTUAL_PAGE,
        pageCount = { VIRTUAL_PAGE_COUNT }
    )
    val scope = rememberCoroutineScope()

    val currentRealIndex = ((pagerState.currentPage % KANA_COUNT) + KANA_COUNT) % KANA_COUNT
    val displayList: List<Kana> = GOJUON  // Phase 4 will swap to shuffled order when toggle is on

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "日语五十音") },
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
                val realIndex = ((virtualPage % KANA_COUNT) + KANA_COUNT) % KANA_COUNT
                CardContent(kana = displayList[realIndex])
            }

            BottomNav(
                currentIndex = currentRealIndex,
                onPrev = {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                },
                onNext = {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                },
            )
        }
    }
}

@Composable
private fun CardContent(kana: Kana) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = kana.char,
                fontSize = 220.sp,
                fontWeight = FontWeight.Normal,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = kana.romaji,
                fontSize = 36.sp,
            )
            Spacer(Modifier.height(48.dp))
            OutlinedButton(onClick = { /* Phase 7 stroke trigger */ }) {
                Text(stringResource(R.string.btn_stroke))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = kana.row,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun BottomNav(currentIndex: Int, onPrev: () -> Unit, onNext: () -> Unit) {
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
            Button(onClick = onPrev) { Text("← " + stringResource(R.string.btn_prev)) }
            Button(onClick = onNext) { Text(stringResource(R.string.btn_next) + " →") }
        }
        Spacer(Modifier.height(8.dp))
        Text(text = "${currentIndex + 1} / $KANA_COUNT", fontSize = 14.sp)
    }
}
