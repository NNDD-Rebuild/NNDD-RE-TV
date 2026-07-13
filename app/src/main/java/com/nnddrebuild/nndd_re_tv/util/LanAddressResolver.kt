package com.nnddrebuild.nndd_re_tv.util

import android.content.Context
import android.net.ConnectivityManager
import java.net.Inet4Address

/** 同一Wi-Fi内のブラウザに案内するためのTV自身のLAN IPv4アドレスを取得する。 */
object LanAddressResolver {

    fun getLanIpAddress(context: Context): String? {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return null
        val network = cm.activeNetwork ?: return null
        val linkProperties = cm.getLinkProperties(network) ?: return null
        return linkProperties.linkAddresses
            .map { it.address }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { !it.isLoopbackAddress }
            ?.hostAddress
    }
}
