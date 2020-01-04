package io.github.takusan23.nicohome.Fragment

import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import io.github.takusan23.nicohome.Activity.LicenceActivity
import io.github.takusan23.nicohome.R
import io.github.takusan23.nicohome.Activity.ThisAppActivity
import java.io.File

class PreferenceFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference, rootKey)
        //キャッシュ削除
        findPreference<Preference>("setting_cache_clear")?.setOnPreferenceClickListener {
            deleteCache()
            false
        }
        //ライセンス
        findPreference<Preference>("setting_licence")?.setOnPreferenceClickListener {
            val intent = Intent(context, LicenceActivity::class.java)
            startActivity(intent)
            false
        }
        //このアプリについて
        findPreference<Preference>("setting_this_app")?.setOnPreferenceClickListener {
            val intent = Intent(context, ThisAppActivity::class.java)
            startActivity(intent)
            false
        }
    }

    //キャッシュ削除
    private fun deleteCache() {
        val cache = context?.externalCacheDir?.path
        val file = File(cache)
        val files = file.listFiles()
        for (i in files.indices) {
            files[i].delete()
        }
    }
}