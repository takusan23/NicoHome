package io.github.takusan23.nicohome.GoogleCast

import android.content.Context
import androidx.core.net.toUri
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.common.images.WebImage

class GoogleCast(val context: Context) {

    val castContext = CastContext.getSharedInstance(context)

    var mediaUri = ""
    var mediaTitle = ""
    var mediaSubTitle = ""
    var mediaThumbnailURL = ""

    var sessionManagerListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarted(p0: CastSession?, p1: String?) {
            play(p0)
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
            setContentType("videos/mp4")
            setMetadata(mediaMetadata)
        }
        val mediaLoadRequestData = MediaLoadRequestData.Builder().apply {
            setMediaInfo(mediaInfo.build())
        }
        val remoteMediaClient = castSession?.remoteMediaClient
        remoteMediaClient?.load(mediaLoadRequestData.build())
    }

}