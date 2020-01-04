package io.github.takusan23.nicohome.Fragment

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import io.github.takusan23.nicohome.Adapter.MylistAdapter
import io.github.takusan23.nicohome.GoogleCast.GoogleCast
import io.github.takusan23.nicohome.MainActivity
import io.github.takusan23.nicohome.NicoVideo.NicoVideo
import io.github.takusan23.nicohome.R
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_mylist.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.regex.Pattern

class MylistFragment : Fragment() {

    lateinit var googleCast: GoogleCast
    lateinit var nicoVideo: NicoVideo

    lateinit var mylistAdapter: MylistAdapter
    val mylistVideoList = arrayListOf<ArrayList<String>>()

    lateinit var pref_setting: SharedPreferences
    var user_session = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_mylist, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        googleCast = GoogleCast(context!!)
        googleCast.snackbarView = fragment_mylist_recyclerview
        googleCast.snackbarPosView = (activity as MainActivity).main_activity_bottom_nav

        //ニコニコ動画再生をまとめたやつ。
        nicoVideo = NicoVideo(
            activity as AppCompatActivity,
            googleCast
        )
        //RecyclerView初期化
        initRecyclerView()
        mylistAdapter.nicoVideo = nicoVideo

        pref_setting = PreferenceManager.getDefaultSharedPreferences(context)
        user_session = pref_setting.getString("user_session", "") ?: ""

        GlobalScope.launch {
            //マイリスト取得APIに必要なNicoAPI.Tokenを取得
            val token = getNicoAPIToken().await()
            if (token.isNotEmpty()) {
                //マイリスト一覧取得
                getMylistGroup(token)
                //Tab押したとき
                fragment_mylist_tablayout.addOnTabSelectedListener(object :
                    TabLayout.OnTabSelectedListener {
                    override fun onTabReselected(tab: TabLayout.Tab?) {

                    }

                    override fun onTabUnselected(tab: TabLayout.Tab?) {

                    }

                    override fun onTabSelected(tab: TabLayout.Tab?) {
                        val id = tab?.tag as String
                        getMylist(token, id)
                        //引っ張ったとき
                        fragment_mylist_swipe_refresh.setOnRefreshListener {
                            getMylist(token, id)
                        }
                    }
                })
            }
        }

    }

    private fun getMylist(token: String, id: String) {
        fragment_mylist_swipe_refresh.isRefreshing = true
        mylistVideoList.clear()
        val url = "https://www.nicovideo.jp/api/mylist/list"
        val formBody = FormBody.Builder().add("token", token).add("group_id", id).build()
        val request = Request.Builder()
            .url(url)
            .post(formBody)
            .addHeader("User-Agent", "NicoHome:@takusan_23")
            .addHeader("Cookie", "user_session=$user_session")
            .build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                showToast(getString(R.string.error))
            }

            override fun onResponse(call: Call, response: Response) {
                val responseString = response.body?.string()
                if (response.isSuccessful) {
                    val jsonObject = JSONObject(responseString)
                    val mylistItem = jsonObject.getJSONArray("mylistitem")
                    for (i in 0 until mylistItem.length()) {
                        val mylist = mylistItem.getJSONObject(i)
                        val itemData = mylist.getJSONObject("item_data")
                        val title = itemData.getString("title")
                        val id = itemData.getString("video_id")
                        val thumbnail = itemData.getString("thumbnail_url")
                        //RecyclerView追加
                        val item = arrayListOf<String>().apply {
                            add("")
                            add(title)
                            add(id)
                            add(thumbnail)
                        }
                        mylistVideoList.add(item)
                    }
                    activity?.runOnUiThread {
                        //RecyclerView更新
                        fragment_mylist_swipe_refresh.isRefreshing = false
                        mylistAdapter.notifyDataSetChanged()
                    }
                } else {
                    showToast("${getString(R.string.error)}\n${response.code}")
                }
            }
        })
    }

    private fun getMylistGroup(nicoAPIToken: String) {
        val url = "https://www.nicovideo.jp/api/mylistgroup/list"
        val formBody = FormBody.Builder().add("token", nicoAPIToken).build()
        val request = Request.Builder()
            .url(url)
            .post(formBody)
            .addHeader("User-Agent", "NicoHome:@takusan_23")
            .addHeader("Cookie", "user_session=$user_session")
            .build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                showToast(getString(R.string.error))
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseString = response.body?.string()
                    val jsonObject = JSONObject(responseString)
                    val mylistGroup = jsonObject.getJSONArray("mylistgroup")
                    for (i in 0 until mylistGroup.length()) {
                        val mylist = mylistGroup.getJSONObject(i)
                        val id = mylist.getString("id")
                        val name = mylist.getString("name")
                        //タブ作成
                        val tabItem = fragment_mylist_tablayout.newTab()
                        tabItem.apply {
                            text = name
                            tag = id
                        }
                        activity?.runOnUiThread {
                            fragment_mylist_tablayout.addTab(tabItem)
                        }
                    }
                } else {
                    showToast("${getString(R.string.error)}\n${response.code}")
                }
            }
        })
    }

    fun initRecyclerView() {
        fragment_mylist_recyclerview.setHasFixedSize(true)
        val mLayoutManager = LinearLayoutManager(context)
        fragment_mylist_recyclerview.layoutManager = mLayoutManager as RecyclerView.LayoutManager?
        mylistAdapter = MylistAdapter(mylistVideoList)
        fragment_mylist_recyclerview.adapter = mylistAdapter
        val recyclerViewLayoutManager = fragment_mylist_recyclerview.layoutManager!!
    }

    //マイリスト取得に必要なトークンを取ってくる
    fun getNicoAPIToken(): Deferred<String> = GlobalScope.async {
        val url = "https://www.nicovideo.jp/my/mylist"
        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "NicoHome:@takusan_23")
            .addHeader("Cookie", "user_session=$user_session")
            .build()
        val okHttpClient = OkHttpClient()
        val response = okHttpClient.newCall(request).execute()
        if (response.isSuccessful) {
            //正規表現で
            val regex = "NicoAPI.token = \"(.+?)\";"
            val pattern = Pattern.compile(regex)
            val matcher = pattern.matcher(response.body?.string())
            if (matcher.find()) {
                val token = matcher.group(1)
                return@async token
            }
        } else {
            showToast("${getString(R.string.error)}\n${response.code}")
        }
        return@async ""
    }

    fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
        }
    }
}