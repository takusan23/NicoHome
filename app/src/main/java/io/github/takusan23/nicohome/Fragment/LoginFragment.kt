package io.github.takusan23.nicohome.Fragment

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import io.github.takusan23.nicohome.R
import kotlinx.android.synthetic.main.fragment_login.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import kotlin.math.log

class LoginFragment : Fragment() {

    lateinit var pref_setting: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pref_setting = PreferenceManager.getDefaultSharedPreferences(context)

        //メアドが保存されている場合？
        if (pref_setting.getString("mail", "")?.isNotEmpty() == true) {
            fragment_login_mail.setText(pref_setting.getString("mail", ""))
            fragment_login_password.setText(pref_setting.getString("password", ""))
        }

        //ログインする
        fragment_login_button.setOnClickListener {
            GlobalScope.launch {
                login()
            }
        }
    }

    fun login() {
        //メアド、パス取得
        val mail = fragment_login_mail.text.toString()
        val pass = fragment_login_password.text.toString()
        //ログインする
        // 使用するサーバーのURLに合わせる
        val urlSt = "https://secure.nicovideo.jp/secure/login?site=niconico"

        var httpConn: HttpURLConnection? = null

        val postData = "mail_tel=$mail&password=$pass"

        try {
            // URL設定
            val url = URL(urlSt)

            // HttpURLConnection
            httpConn = url.openConnection() as HttpURLConnection

            // request POST
            httpConn.requestMethod = "POST"

            // no Redirects
            httpConn.instanceFollowRedirects = false

            // データを書き込む
            httpConn.doOutput = true

            // 時間制限
            httpConn.readTimeout = 10000
            httpConn.connectTimeout = 20000

            //ユーザーエージェント
            httpConn.setRequestProperty("User-Agent", "TatimiDroid;@takusan_23")

            // 接続
            httpConn.connect()

            try {
                httpConn.outputStream.use { outStream ->
                    outStream.write(postData.toByteArray(StandardCharsets.UTF_8))
                    outStream.flush()
                }
            } catch (e: IOException) {
                // POST送信エラー
                e.printStackTrace()
            }
            // POSTデータ送信処理
            val status = httpConn.responseCode

            for (cookie in httpConn.headerFields.get("Set-Cookie")!!) {
                //user_sessionだけほしい！！！
                if (cookie.contains("user_session") && !cookie.contains("deleted") && !cookie.contains(
                        "secure"
                    )
                ) {
                    //邪魔なのを取る
                    var user_session = cookie.replace("user_session=", "")
                    //uset_settionは文字数86なので切り取る
                    user_session = user_session.substring(0, 86)
                    //保存する
                    val editor = pref_setting.edit()
                    editor.putString("user_session", user_session)
                    //めあど、ぱすわーども保存する
                    editor.putString("mail", mail)
                    editor.putString("password", pass)
                    editor.apply()
                    showToast(getString(R.string.succeeded) + "\n$status")
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            httpConn?.disconnect()
        }
    }

    fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}