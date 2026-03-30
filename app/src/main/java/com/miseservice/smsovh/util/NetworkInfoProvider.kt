package com.miseservice.smsovh.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.wifi.WifiManager
import android.os.Build
import android.net.LinkProperties
import java.util.Locale

object NetworkInfoProvider {
    @Volatile
    private var hostIp: String? = null

    fun getHostIp(): String {
        return hostIp ?: "127.0.0.1"
    }

    fun init(context: Context) {
        updateIp(context)
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                updateIp(context)
            }
            override fun onLost(network: Network) {
                updateIp(context)
            }
        })
    }

    private fun updateIp(context: Context) {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val ip: String? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ (API 31+)
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val network = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                val isWifi = capabilities?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) == true
                val linkProperties: LinkProperties? = if (isWifi) connectivityManager.getLinkProperties(network) else null
                linkProperties?.linkAddresses
                    ?.firstOrNull { it.address.hostAddress?.contains('.') == true }
                    ?.address?.hostAddress
            } else {
                // Avant Android 12 (API < 31)
                @Suppress("DEPRECATION")
                val ipInt = wifiManager.connectionInfo.ipAddress
                String.format(
                    Locale.getDefault(),
                    "%d.%d.%d.%d",
                    ipInt and 0xff,
                    ipInt shr 8 and 0xff,
                    ipInt shr 16 and 0xff,
                    ipInt shr 24 and 0xff
                )
            }
            hostIp = ip ?: "127.0.0.1"
        } catch (_: Exception) {
            hostIp = "127.0.0.1"
        }
    }
}
