package io.github.takusan23.nicohome.NicoVideo

import android.app.Activity
import android.content.Context
import android.widget.Toast
import io.github.takusan23.nicohome.R
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class NicoVideoCache(var context: Context) {
    fun getVideo(
        id: String,
        mediaUri: String,
        userSession: String?,
        nicoHistory: String
    ): Deferred<String> =
        GlobalScope.async {
            //動画ダウンロード
            var urlConnection: HttpURLConnection? = null
            try {
                val url = URL(mediaUri)

                urlConnection = url.openConnection() as HttpURLConnection
                urlConnection.readTimeout = 10000
                urlConnection.connectTimeout = 20000
                urlConnection.requestMethod = "GET"
                urlConnection.instanceFollowRedirects = false

                urlConnection.setRequestProperty("Accept-Language", "jp")
                urlConnection.setRequestProperty("User-Agent", "NicoHome;@takusan_23")
                urlConnection.setRequestProperty(
                    "Cookie",
                    "user_session=$userSession;nicohistory=$nicoHistory"
                )

                // 接続
                urlConnection.connect()

                println(urlConnection.responseCode)

                if (urlConnection.responseCode == HttpURLConnection.HTTP_OK) {
                    //成功時
                    //保存する
                    val inputStream = urlConnection.inputStream
                    //キャッシュ領域 消されてもおかしくない。
                    val cacheDir = context.externalCacheDir?.path
                    val path = "$cacheDir/$id.mp4"
                    val outputStream = File(path).outputStream()
                    val buffer = ByteArray(1024)
                    try {
                        while (true) {
                            //いれていく
                            val data = inputStream.read(buffer)
                            if (data == -1) {
                                //Snackbarにだす
                                (context as Activity).runOnUiThread {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.video_cache_ok),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    println("キャッシュ領域に保存しました。 $id")
                                }
                                return@async path
                                break
                            }
                            outputStream.write(buffer, 0, data)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    inputStream.close()
                    outputStream.close()
                } else {
                    (context as Activity).runOnUiThread {
                        //問題発生
                        Toast.makeText(
                            context,
                            context.getString(R.string.error),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                (context as Activity).runOnUiThread {
                    //問題発生
                    Toast.makeText(
                        context,
                        context.getString(R.string.error),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } finally {
                urlConnection?.disconnect()
            }
            return@async ""
        }
}