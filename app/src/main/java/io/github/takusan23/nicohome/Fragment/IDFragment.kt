package io.github.takusan23.nicohome.Fragment

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.material.snackbar.Snackbar
import io.github.takusan23.nicohome.GoogleCast.GoogleCast
import io.github.takusan23.nicohome.R
import kotlinx.android.synthetic.main.fragment_id.*
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.IOException
import java.security.cert.PKIXReason
import java.util.*
import kotlin.concurrent.timerTask

class IDFragment : Fragment() {

    lateinit var castContext: CastContext
    lateinit var sessionManagerListener: SessionManagerListener<CastSession>

    lateinit var googleCast: GoogleCast

    lateinit var pref_sessing: SharedPreferences
    var user_session = ""

    var heartBeatTimer = Timer()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_id, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pref_sessing = PreferenceManager.getDefaultSharedPreferences(context)
        user_session = pref_sessing.getString("user_session", "") ?: ""
        googleCast = GoogleCast(context!!)

        fragment_id_play_button.setOnClickListener {
            Snackbar.make(
                fragment_id_play_button,
                context?.getString(R.string.not_connect_cast_devices)!!,
                Snackbar.LENGTH_SHORT
            ).show()
        }

        googleCast.sessionManagerListener = object : SessionManagerListener<CastSession> {
            override fun onSessionStarted(p0: CastSession?, p1: String?) {
                fragment_id_play_button.setOnClickListener {
                    GlobalScope.launch {
                        //動画情報取得
                        val json = getNicoVideoHTML().await()
                        //JSONぱーす
                        val jsonObject = JSONObject(json)
                        val title = jsonObject.getJSONObject("video").getString("title")
                        val id = jsonObject.getJSONObject("video").getString("id")
                        val thumbnailURL =
                            jsonObject.getJSONObject("video").getString("thumbnailURL")
                        googleCast.mediaTitle = title
                        googleCast.mediaThumbnailURL = thumbnailURL
                        googleCast.mediaSubTitle = id
                        val contentUri = getContentURI(jsonObject).await()
                        googleCast.mediaUri = contentUri
                        //再生
                        activity?.runOnUiThread {
                            googleCast.play(p0)
                        }
                    }
                }
            }

            override fun onSessionResumeFailed(p0: CastSession?, p1: Int) {

            }

            override fun onSessionSuspended(p0: CastSession?, p1: Int) {

            }

            override fun onSessionEnded(p0: CastSession?, p1: Int) {

            }

            override fun onSessionResumed(p0: CastSession?, p1: Boolean) {

            }

            override fun onSessionStarting(p0: CastSession?) {

            }

            override fun onSessionResuming(p0: CastSession?, p1: String?) {

            }

            override fun onSessionEnding(p0: CastSession?) {

            }

            override fun onSessionStartFailed(p0: CastSession?, p1: Int) {

            }

        }
    }

    //ニコ動へアクセスしてHTMLを取得する。
    fun getNicoVideoHTML(): Deferred<String> = GlobalScope.async {
        val id = fragment_id_play_id_input.text.toString()
        if (user_session.isEmpty() && id.isEmpty()) {
            return@async ""
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
        if (response.isSuccessful) {
            val responseString = response.body?.string()
            val html = Jsoup.parse(responseString)
            val json = html.getElementById("js-initial-watch-data").attr("data-api-data")
            val jsonObject = JSONObject(json)

            if (!jsonObject.getJSONObject("video").isNull("dmcInfo")) {
                println("新サーバー")
                //dmcInfoが存在する。
                //dmcInfoがある場合はJSONを解析してAPIを叩けば動画リンク取得可能。
                return@async jsonObject.toString()
            } else {
                //dmcInfoが存在しない。
                println("smileサーバー")
                val url = jsonObject.getJSONObject("video").getJSONObject("smileInfo")
                    .getString("url")
                activity?.runOnUiThread {
                    Snackbar.make(
                        fragment_id_play_button,
                        context?.getString(R.string.smile_server_error)!!,
                        Snackbar.LENGTH_SHORT
                    ).show()
                } // googleCast.mediaUri = url
            }
        } else {
            showToast("${getString(R.string.error)}\n${response.code}")
        }
        return@async ""
    }

    //動画のリンクを貰う。dmcInfoが存在する場合。同期処理です。
    fun getContentURI(jsonObject: JSONObject): Deferred<String> = GlobalScope.async {
        val dmcInfo = jsonObject.getJSONObject("video").getJSONObject("dmcInfo")
        val sessionAPI = dmcInfo.getJSONObject("session_api")
        println(sessionAPI.toString())
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
            val session = jsonObject.getJSONObject("data").getJSONObject("session")
            val id = session.getString("id")
            //サーバーから切られないようにハートビートを送信する
            val url = "https://api.dmc.nico/api/sessions/${id}?_format=json&_method=PUT"
            heatBeat(url, jsonObject.toString())
            //動画のリンク
            val content_uri = session.getString("content_uri")
            return@async content_uri
        } else {
            showToast("${getString(R.string.error)}\n${response.code}")
        }
        return@async ""
    }

    //ハートビート？40秒ごとに送信しないといけない模様。
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
                    println("ハートビート")
                }
            })
        }, 40 * 1000, 40 * 1000)
    }

    fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPause() {
        super.onPause()
        googleCast.pause()
    }

    override fun onResume() {
        super.onResume()
        googleCast.resume()
    }

    override fun onDestroy() {
        super.onDestroy()
        heartBeatTimer.cancel()
    }

}