package com.nnddrebuild.nndd_re_tv.ui.screen.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Card
import androidx.tv.material3.Text
import com.nnddrebuild.nndd_re_tv.server.BrowserNavigateBus
import com.nnddrebuild.nndd_re_tv.server.SearchResultBus

/**
 * 検索語の入力はブラウザ側で行い、TVはブラウザから通知された最新の検索結果を受動表示するのみ。
 * まだ何も検索されていない状態でこの画面を開いた場合は、ブラウザ側を検索画面へ誘導する。
 */
@Composable
fun SearchScreen(onVideoSelected: (String) -> Unit) {
    val items by SearchResultBus.results.collectAsState()

    LaunchedEffect(Unit) {
        if (SearchResultBus.results.value.isEmpty()) {
            BrowserNavigateBus.navigateTo("/search.html")
        }
    }

    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("ブラウザで検索するとここに結果が表示されます")
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(items) { item ->
                Card(onClick = { onVideoSelected(item.videoId) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(text = item.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}
