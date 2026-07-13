package com.nnddrebuild.nndd_re_tv.library

/** PC版NNDD-REのライブラリ共有サーバー接続先。 */
data class LibraryConnection(val host: String, val port: Int) {
    fun baseUrl(): String = "http://$host:$port"
}
