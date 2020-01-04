package io.github.takusan23.nicohome

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import io.github.takusan23.nicohome.Fragment.IDFragment
import io.github.takusan23.nicohome.Fragment.LoginFragment
import io.github.takusan23.nicohome.Fragment.MylistFragment
import io.github.takusan23.nicohome.Fragment.PreferenceFragment
import io.github.takusan23.nicohome.GoogleCast.GoogleCast
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    lateinit var castContext: CastContext

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        castContext = CastContext.getSharedInstance(this)

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
                R.id.menu_setting->{
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

}
