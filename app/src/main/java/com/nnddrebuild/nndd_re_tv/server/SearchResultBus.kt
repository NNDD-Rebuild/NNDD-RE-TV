package com.nnddrebuild.nndd_re_tv.server

import com.nnddrebuild.nndd_re_tv.nico.search.SearchResultItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * ブラウザ側で実行された検索の結果をTV UIへ伝えるためのバス。
 * TVはテキスト入力を伴わないため、検索語入力はブラウザに寄せ、結果だけをTVへ反映する。
 */
object SearchResultBus {
    private val _results = MutableStateFlow<List<SearchResultItem>>(emptyList())
    val results: StateFlow<List<SearchResultItem>> = _results

    fun update(items: List<SearchResultItem>) {
        _results.value = items
    }
}
