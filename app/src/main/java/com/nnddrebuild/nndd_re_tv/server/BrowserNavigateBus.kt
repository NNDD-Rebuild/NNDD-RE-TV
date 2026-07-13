package com.nnddrebuild.nndd_re_tv.server

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * TV側の操作をブラウザの画面遷移として伝える単方向バス(TV→ブラウザ)。
 * ブラウザは定期ポーリングで最新の遷移先を取得する。取得時に消費されるワンショット通知。
 */
object BrowserNavigateBus {
    private val _target = MutableStateFlow<String?>(null)
    val target: StateFlow<String?> = _target

    fun navigateTo(path: String) {
        _target.value = path
    }

    fun consume(): String? {
        val current = _target.value
        _target.value = null
        return current
    }
}
