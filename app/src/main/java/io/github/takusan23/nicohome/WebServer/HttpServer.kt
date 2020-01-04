package io.github.takusan23.nicohome.WebServer

import android.content.Context
import android.net.wifi.WifiManager
import fi.iki.elonen.NanoHTTPD
import java.io.File

class HttpServer(private val context: Context, val port: Int = 8080) :
    NanoHTTPD(port) {

    var videoResponse: Response? = null

    override fun serve(session: NanoHTTPD.IHTTPSession): Response? {
        return videoResponse
    }

    fun serveVideo(path:String) {
        //ScopedStorageなので自由に使え
        val file = File(path)
        videoResponse = NanoHTTPD.newChunkedResponse(Response.Status.OK, "video/mp4", file.inputStream())
    }

    /** IPアドレス取得 */
    fun getIPAddress(): String {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        val ip_addr_i = wifiInfo.getIpAddress()
        val ip_addr =
            (ip_addr_i shr 0 and 0xFF).toString() + "." + (ip_addr_i shr 8 and 0xFF) + "." + (ip_addr_i shr 16 and 0xFF) + "." + (ip_addr_i shr 24 and 0xFF)
        return "$ip_addr:${port}"

    }
}
