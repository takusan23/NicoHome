package io.github.takusan23.nicohome.GoogleCast

import android.app.Activity
import android.content.Context
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.common.images.WebImage
import io.github.takusan23.nicohome.NicoVideo.NicoVideoCache
import io.github.takusan23.nicohome.WebServer.HttpServer
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File


class GoogleCast(val context: Context) {

    val castContext = CastContext.getSharedInstance(context)
    var nicoVideoCache = NicoVideoCache(context)

    var mediaUri = ""
    var mediaTitle = ""
    var mediaSubTitle = ""
    var mediaThumbnailURL = ""

    var nicoHistory = ""

    var pref_sessiong = PreferenceManager.getDefaultSharedPreferences(context)
    var user_session = pref_sessiong.getString("user_session", "")

    var sessionManagerListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarted(p0: CastSession?, p1: String?) {

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

    fun resume() {
        castContext.sessionManager.addSessionManagerListener(
            sessionManagerListener,
            CastSession::class.java
        )
    }

    fun pause() {
        castContext.sessionManager.removeSessionManagerListener(
            sessionManagerListener,
            CastSession::class.java
        )
    }

    fun play(castSession: CastSession?) {
        val mediaMetadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE).apply {
            putString(MediaMetadata.KEY_TITLE, mediaTitle)
            putString(MediaMetadata.KEY_SUBTITLE, mediaSubTitle)
            addImage(WebImage(mediaThumbnailURL.toUri()))
        }
        val mediaInfo = MediaInfo.Builder(mediaUri).apply {
            setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            setContentType("video/mp4")
            setMetadata(mediaMetadata)
        }
        val mediaLoadRequestData = MediaLoadRequestData.Builder().apply {
            setMediaInfo(mediaInfo.build())
        }
        val remoteMediaClient = castSession?.remoteMediaClient
        remoteMediaClient?.load(mediaLoadRequestData.build())


/*
        //ExoPlayer
        val player = ExoPlayerFactory.newSimpleInstance(context)
        val defaultHttpDataSourceFactory =
            DefaultHttpDataSourceFactory("NicoHome;@takusan_23", null)
        defaultHttpDataSourceFactory.defaultRequestProperties.set(
            "Cookie",
            "user_session=$user_session;nicohistory=$nicoHistory"
        )
        val dataSourceFactory =
            DefaultDataSourceFactory(
                context,
                null,
                defaultHttpDataSourceFactory
            )
        val mediaSource =
            ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaUri.toUri())
        player.prepare(mediaSource)
        player.playWhenReady = true
*/

    }

    /*
    * smileサーバーの動画をキャッシュ領域に落とす。
    *
    * */
    fun cachePlay(id: String, session: CastSession) {
        val path = context.externalCacheDir?.path
        // println(path)
        //キャッシュ領域
        val cacheDir = File(path)
        cacheDir.apply {
            //フォルダの中身取得
            val dirChildList = cacheDir.listFiles()
            if (dirChildList != null) {
                //既に存在するかもしれない
                var isExists = false
                var path = ""
                dirChildList.forEach {
                    if (it.name == "$id.mp4") {
                        isExists = true
                        path = it.path
                    }
                }
                //Webさーばー
                if (!isExists) {
                    //キャッシュ領域にない場合は取りに行く。
                    val nicoVideoCache = NicoVideoCache(context)
                    GlobalScope.launch {
                        //ファイルの場所を返す
                        val path =nicoVideoCache.getVideo(id, mediaUri, user_session, nicoHistory).await()
                        val httpServer = HttpServer(context, path)
                        httpServer.serveVideo()
                        //Webサーバー開始
                        httpServer.start()
                        //GoogleCastで再生
                        mediaUri = "http://${httpServer.getIPAddress()}"
                        (context as Activity).runOnUiThread { play(session) }
                    }
                } else {
                    //キャッシュ領域にあるのでWebサーバーを展開
                    if (File(path).exists()) {
                        val httpServer = HttpServer(context, path)
                        //Webサーバー開始
                        httpServer.serveVideo()
                        httpServer.start()
                        //GoogleCastで再生
                        mediaUri = "http://${httpServer.getIPAddress()}"
                        (context as Activity).runOnUiThread {
                            play(session)
                        }
                    }
                }
            }
        }
    }

}