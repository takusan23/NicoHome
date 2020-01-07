package io.github.takusan23.nicohome

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.media.widget.MiniControllerFragment
import io.github.takusan23.nicohome.Fragment.IDFragment
import io.github.takusan23.nicohome.Fragment.LoginFragment
import io.github.takusan23.nicohome.Fragment.MylistFragment
import io.github.takusan23.nicohome.Fragment.PreferenceFragment
import io.github.takusan23.nicohome.GoogleCast.GoogleCast
import io.github.takusan23.nicohome.NicoVideo.NicoVideo
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    lateinit var castContext: CastContext

    //GoogleCastまとめたやつ
    lateinit var googleCast: GoogleCast
    lateinit var nicoVideo: NicoVideo

    //リピート
    var isRepeat = false
    //自動で次の曲
    var isAutoNextPlay = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        castContext = CastContext.getSharedInstance(this)

        googleCast = GoogleCast(this)
        nicoVideo = NicoVideo(this, googleCast)

        val videoIDFragment = IDFragment()
        supportFragmentManager.beginTransaction()
            .replace(main_activity_fragment.id, videoIDFragment).commit()

        main_activity_bottom_nav.selectedItemId = R.id.menu_video_id
        supportActionBar?.title = getString(R.string.video_id)
        main_activity_bottom_nav.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.menu_login -> {
                    supportFragmentManager.beginTransaction()
                        .replace(main_activity_fragment.id, LoginFragment()).commit()
                    supportActionBar?.title = getString(R.string.login)
                }
                R.id.menu_video_id -> {
                    supportFragmentManager.beginTransaction()
                        .replace(main_activity_fragment.id, IDFragment()).commit()
                    supportActionBar?.title = getString(R.string.video_id)
                }
                R.id.menu_mylist -> {
                    supportFragmentManager.beginTransaction()
                        .replace(main_activity_fragment.id, MylistFragment()).commit()
                    supportActionBar?.title = getString(R.string.mylist)
                }
                R.id.menu_setting -> {
                    supportFragmentManager.beginTransaction()
                        .replace(main_activity_fragment.id, PreferenceFragment()).commit()
                    supportActionBar?.title = getString(R.string.settings)
                }
            }
            true
        }


    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.activity_main_menu, menu)
        CastButtonFactory.setUpMediaRouteButton(this, menu, R.id.media_route_menu_item)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.bar_menu_repeat -> {
                isRepeat = !isRepeat
                nicoVideo.isRepeat = isRepeat
                if (isRepeat) {
                    item.setIcon(R.drawable.ic_repeat_one_24px)
                } else {
                    item.setIcon(R.drawable.ic_repeat_24px)
                }
            }
            R.id.bar_menu_auto_next -> {
                isAutoNextPlay = !isAutoNextPlay
                nicoVideo.isAutoNextPlay = isRepeat
                if (isAutoNextPlay) {
                    item.setIcon(R.drawable.ic_queue_music_24px)
                } else {
                    item.setIcon(R.drawable.ic_clear_24px)
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        googleCast.resume()
    }

    override fun onDestroy() {
        super.onDestroy()
        googleCast.pause()
    }

}
