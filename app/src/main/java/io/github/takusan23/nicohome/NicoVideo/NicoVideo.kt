package io.github.takusan23.nicohome.NicoVideo

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.gms.cast.MediaStatus
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.material.snackbar.Snackbar
import io.github.takusan23.nicohome.GoogleCast.GoogleCast
import io.github.takusan23.nicohome.MainActivity
import io.github.takusan23.nicohome.R
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.IOException
import java.net.HttpCookie
import java.util.*
import kotlin.concurrent.timerTask

class NicoVideo(var appCompatActivity: AppCompatActivity, var googleCast: GoogleCast) {

    lateinit var mainActivity: MainActivity

    //自動で次の動画へ
    var isAutoNextPlay = false
    //リピート
    var isRepeat = false
    //マイリスの動画配列
    var mylistVideoList = arrayListOf<String>()

    var pref_setting = PreferenceManager.getDefaultSharedPreferences(appCompatActivity)
    var user_session = pref_setting.getString("user_session", "") ?: ""
    var heartBeatTimer = Timer()

    //再生する。
    fun play(id: String) {
        heartBeatTimer.cancel()
        heartBeatTimer = Timer()
        if (googleCast.castContext.sessionManager.currentCastSession != null) {
            GlobalScope.launch {
                //動画情報取得
                val response = getNicoVideoHTML(id).await()
                response?.apply {
                    if (isSuccessful) {
                        val html = Jsoup.parse(body?.string())
                        val json =
                            html.getElementById("js-initial-watch-data").attr("data-api-data")
                        //JSONぱーす
                        val jsonObject = JSONObject(json)
                        val title = jsonObject.getJSONObject("video").getString("title")
                        val id = jsonObject.getJSONObject("video").getString("id")
                        val thumbnailURL =
                            jsonObject.getJSONObject("video").getString("thumbnailURL")
                        googleCast.mediaTitle = title
                        googleCast.mediaThumbnailURL = thumbnailURL
                        googleCast.mediaSubTitle = id

                        //リピート/次の曲へ移動する？
                        appCompatActivity.runOnUiThread {
                            var count = 0
                            val castSession =
                                googleCast.castContext.sessionManager.currentCastSession
                            castSession.remoteMediaClient.registerCallback(object :
                                RemoteMediaClient.Callback() {
                                override fun onStatusUpdated() {
                                    super.onStatusUpdated()
                                    if (castSession.remoteMediaClient.playerState == MediaStatus.IDLE_REASON_FINISHED) {
                                        println(castSession.remoteMediaClient.playerState == MediaStatus.PLAYER_STATE_IDLE)
                                        //値確認。MainActivity以外だと使えない。
                                        getValue()
                                        if (isRepeat) {
                                            //リピート？
                                            println(googleCast.mediaUri)
                                            if (googleCast.mediaUri.contains("http://192.168")) {
                                                //smileサーバーの動画
                                                googleCast.cachePlay(id, castSession)
                                            } else {
                                                //DMCさーばー
                                                googleCast.play(castSession)
                                            }
                                        } else if (isAutoNextPlay) {
                                            //次の曲
                                            val nextId = nextVideoId(id)
                                            play(nextId)
                                        }
                                    }
                                }
                            })
                        }


                        if (!jsonObject.getJSONObject("video").isNull("dmcInfo")) {
                            println("新サーバー")
                            //dmcInfoが存在する。
                            //dmcInfoがある場合はJSONを解析してAPIを叩けば動画リンク取得可能。
                            //なお1.5GB再エンコード問題が始まった。
                            //あと止めても動く動画が消えることに。。。
                            val contentUri = getContentURI(jsonObject).await()
                            //一度だけ送るハートビート？
                            setOneHeartBeat(jsonObject)
                            googleCast.mediaUri = contentUri
                            //再生
                            appCompatActivity.runOnUiThread {
                                googleCast.play(googleCast.castContext.sessionManager.currentCastSession)
                            }
                        } else {
                            println("smileサーバー")
                            /*
                            * smileサーバー
                            * smileサーバーの動画を再生するにはリクエストヘッダーに動画サイトアクセス時のレスポンスヘッダーのSet-Cookieのなかの
                            * nicohistoryをくっつけて投げないと 403 が帰ってきます。くそくそくそ
                            * dmcInfoが存在しない。
                            *
                            * しかしsmileサーバーの動画が再生できないとかこのアプリを作った意味が消失するので
                            * 一時的にローカルに動画をDLしてその後Webサーバーを展開して再生できるようにする。
                            *
                            * いやsmileサーバーの動画多くない？変換しろよ運営
                            *
                            * */
                            println("smileサーバー")
                            val url = jsonObject.getJSONObject("video").getJSONObject("smileInfo")
                                .getString("url")
                            // smileサーバーの動画を再生するときはヘッダーに nicohistory を指定する必要がある模様
                            val cookie = response.headers.get("Set-Cookie")
                            val cookieList = HttpCookie.parse(cookie)
                            //nicohistoryが見つかるまで回す
                            var nicohistory = ""
                            cookieList.forEach {
                                if (it.name.contains("nicohistory")) {
                                    nicohistory = it.value
                                }
                            }
                            googleCast.nicoHistory = nicohistory
                            googleCast.mediaUri = url
                            appCompatActivity.runOnUiThread {
                                googleCast.cachePlay(
                                    id,
                                    googleCast.castContext.sessionManager.currentCastSession
                                )
                            }
                        }
                    } else {
                        showToast("${appCompatActivity.getString(R.string.error)}\n${response.code}")
                    }
                }
            }
        } else {
            Snackbar.make(
                appCompatActivity.findViewById(android.R.id.content),
                appCompatActivity.getString(R.string.not_connect_cast_devices),
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    //リピート・次の曲自動再生するか
    fun getValue() {
        if (::mainActivity.isInitialized) {
            isRepeat = mainActivity.isRepeat
            isAutoNextPlay = mainActivity.isAutoNextPlay
        }
    }

    fun nextVideoId(oldVideoId: String): String {
        //配列の要素数が0以上で
        if (mylistVideoList.size != 0) {
            val indexOf = mylistVideoList.indexOf(oldVideoId) + 1
            if (mylistVideoList.size < indexOf) {
                return ""
            }
            return mylistVideoList.get(indexOf)
        }
        return ""
    }

    //ニコ動へアクセスしてHTMLを取得する。
    fun getNicoVideoHTML(id: String): Deferred<Response?> = GlobalScope.async {
        if (user_session.isEmpty() && id.isEmpty()) {
            return@async null
        }
        val url = "https://www.nicovideo.jp/watch/$id"
        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "NicoHome;@takusan_23")
            .addHeader("Cookie", "user_session=${user_session}")
            .get()
            .build()
        val okHttpClient = OkHttpClient()
        val response = okHttpClient.newCall(request).execute()
        return@async response
    }

    //動画のリンクを貰う。dmcInfoが存在する場合。同期処理です。
    fun getContentURI(jsonObject: JSONObject): Deferred<String> = GlobalScope.async {
        val dmcInfo = jsonObject.getJSONObject("video").getJSONObject("dmcInfo")
        val sessionAPI = dmcInfo.getJSONObject("session_api")
        //JSONつくる
        val sessionPOSTJSON = JSONObject().apply {
            put("session", JSONObject().apply {
                put("recipe_id", sessionAPI.getString("recipe_id"))
                put("content_id", sessionAPI.getString("content_id"))
                put("content_type", "movie")
                put("content_src_id_sets", JSONArray().apply {
                    this.put(JSONObject().apply {
                        this.put("content_src_ids", JSONArray().apply {
                            this.put(JSONObject().apply {
                                this.put("src_id_to_mux", JSONObject().apply {
                                    this.put("video_src_ids", sessionAPI.getJSONArray("videos"))
                                    this.put("audio_src_ids", sessionAPI.getJSONArray("audios"))
                                })
                            })
                        })
                    })
                })
                put("timing_constraint", "unlimited")
                put("keep_method", JSONObject().apply {
                    put("heartbeat", JSONObject().apply {
                        put("lifetime", 120000)
                    })
                })
                put("protocol", JSONObject().apply {
                    put("name", "http")
                    put("parameters", JSONObject().apply {
                        put("http_parameters", JSONObject().apply {
                            put("parameters", JSONObject().apply {
                                put("http_output_download_parameters", JSONObject().apply {
                                    put("use_well_known_port", "yes")
                                    put("use_ssl", "yes")
                                    put("transfer_preset", "standard2")
                                })
                            })
                        })
                    })
                })
                put("content_uri", "")
                put("session_operation_auth", JSONObject().apply {
                    put("session_operation_auth_by_signature", JSONObject().apply {
                        put("token", sessionAPI.getString("token"))
                        put("signature", sessionAPI.getString("signature"))
                    })
                })
                put("content_auth", JSONObject().apply {
                    put("auth_type", "ht2")
                    put("content_key_timeout", sessionAPI.getInt("content_key_timeout"))
                    put("service_id", "nicovideo")
                    put("service_user_id", sessionAPI.getString("service_user_id"))
                })
                put("client_info", JSONObject().apply {
                    put("player_id", sessionAPI.getString("player_id"))
                })
                put("priority", sessionAPI.getDouble("priority"))
            })
        }
        //POSTする
        val requestBody =
            sessionPOSTJSON.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("https://api.dmc.nico/api/sessions?_format=json")
            .post(requestBody)
            .addHeader("User-Agent", "NicoHome;@takusan_23")
            .addHeader("Content-Type", "application/json")
            .build()
        val okHttpClient = OkHttpClient()
        val response = okHttpClient.newCall(request).execute()
        if (response.isSuccessful) {
            val responseString = response.body?.string()
            val jsonObject = JSONObject(responseString)
            val data = jsonObject.getJSONObject("data")
            val id = data.getJSONObject("session").getString("id")
            //サーバーから切られないようにハートビートを送信する
            val url = "https://api.dmc.nico/api/sessions/${id}?_format=json&_method=PUT"
            heatBeat(url, data.toString())
            //動画のリンク
            val content_uri = data.getJSONObject("session").getString("content_uri")
            return@async content_uri
        } else {
            showToast("${appCompatActivity.getString(R.string.error)}\n${response.code}")
        }
        return@async ""
    }

    //一度だけ別のidでハードビートを行っているのでこちらでも実装する？ 同期処理です
    fun setOneHeartBeat(jsonObject: JSONObject) {
        val dmcInfo = jsonObject.getJSONObject("video").getJSONObject("dmcInfo")
        //無いときは送信しない
        if (!dmcInfo.isNull("storyboard_session_api")) {
            val storyboardSessionAPI = dmcInfo.getJSONObject("storyboard_session_api")
            val sessionPOSTJSON = JSONObject().apply {
                put("session", JSONObject().apply {
                    put("recipe_id", storyboardSessionAPI.getString("recipe_id"))
                    put("content_id", storyboardSessionAPI.getString("content_id"))
                    put("content_type", "video")
                    put("content_src_id_sets", JSONArray().apply {
                        this.put(JSONObject().apply {
                            this.put("content_src_ids", storyboardSessionAPI.getJSONArray("videos"))
                        })
                    })
                    put("timing_constraint", "unlimited")
                    put("keep_method", JSONObject().apply {
                        this.put("heartbeat", JSONObject().apply {
                            this.put("lifetime", 300000)
                        })
                    })
                    put("protocol", JSONObject().apply {
                        this.put("name", "http")
                        this.put("parameters", JSONObject().apply {
                            this.put("http_parameters", JSONObject().apply {
                                this.put("parameters", JSONObject().apply {
                                    this.put("storyboard_download_parameters", JSONObject().apply {
                                        this.put("use_well_known_port", "yes")
                                        this.put("use_ssl", "yes")
                                    })
                                })
                            })
                        })
                    })
                    put("content_uri", "")
                    put("session_operation_auth", JSONObject().apply {
                        this.put("session_operation_auth_by_signature", JSONObject().apply {
                            this.put("token", storyboardSessionAPI.getString("token"))
                            this.put("signature", storyboardSessionAPI.getString("signature"))
                        })
                    })
                    put("content_auth", JSONObject().apply {
                        this.put("auth_type", "ht2")
                        this.put(
                            "content_key_timeout",
                            storyboardSessionAPI.getInt("content_key_timeout")
                        )
                        this.put("service_id", "nicovideo")
                        this.put(
                            "service_user_id",
                            storyboardSessionAPI.getString("service_user_id")
                        )
                    })
                    put("client_info", JSONObject().apply {
                        this.put("player_id", storyboardSessionAPI.getString("player_id"))
                    })
                    put("priority", storyboardSessionAPI.getDouble("priority"))
                })
            }
            //POSTする。
            val requestBody =
                sessionPOSTJSON.toString().toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder()
                .url("https://api.dmc.nico/api/sessions?_format=json")
                .post(requestBody)
                .addHeader("User-Agent", "NicoHome;@takusan_23")
                .addHeader("Content-Type", "application/json")
                .build()
            val okHttpClient = OkHttpClient()
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {

                //成功したら　レスポンスJSONから idを取得する
                val jsonObject = JSONObject(response.body?.string())
                val data = jsonObject.getJSONObject("data")
                val id = data.getJSONObject("session").getString("id")
                val requestBody =
                    data.toString().toRequestBody("application/json".toMediaTypeOrNull())
                val request = Request.Builder()
                    .url("https://api.dmc.nico/api/sessions/${id}?_format=json&_method=PUT")
                    .post(requestBody)
                    .addHeader("User-Agent", "NicoHome;@takusan_23")
                    .addHeader("Content-Type", "application/json")
                    .build()
                val okHttpClient = OkHttpClient()
                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    println("ハートビート　一度だけ　送信成功 ${response.code}")
                } else {
                    showToast("${appCompatActivity.getString(R.string.error)}\n${response.code}")
                }

            } else {
                showToast("${appCompatActivity.getString(R.string.error)}\n${response.code}")
            }

        }
    }

    //ハートビート？40秒ごとに送信しないといけない模様。
    //ハートビートPOSTで送るJSONはsession_apiでAPI叩いたあとのJSONのdataの中身。レスポンスJSON全部投げるわけではない。
    fun heatBeat(url: String, json: String) {
        heartBeatTimer.schedule(timerTask {
            val request = Request.Builder()
                .url(url)
                .post(json.toRequestBody("application/json".toMediaTypeOrNull()))
                .addHeader("User-Agent", "NicoHome;@takusan_23")
                .build()
            val okHttpClient = OkHttpClient()
            okHttpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {

                }

                override fun onResponse(call: Call, response: Response) {
                    println("ハートビート ${response.code}")
                }
            })
        }, 40 * 1000, 40 * 1000)
    }

    fun showToast(message: String) {
        appCompatActivity.runOnUiThread {
            Toast.makeText(appCompatActivity, message, Toast.LENGTH_SHORT).show()
        }
    }


}