package io.github.takusan23.nicohome.Activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_this_app.*
import android.content.Intent
import androidx.core.net.toUri
import io.github.takusan23.nicohome.R


class ThisAppActivity : AppCompatActivity() {

    val github = "https://github.com/takusan23/NicoHome"
    val twitter = "https://twitter.com/takusan__23"
    val mastodon = "https://best-friends.chat/@takusan_23"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_this_app)

        activity_this_app_github.setOnClickListener {
            launchBrowser(github)
        }
        activity_this_app_twitter.setOnClickListener {
            launchBrowser(twitter)
        }
        activity_this_app_mastodon.setOnClickListener {
            launchBrowser(mastodon)
        }

    }

    fun launchBrowser(url: String) {
        val i = Intent(Intent.ACTION_VIEW, url.toUri())
        startActivity(i)
    }

}
