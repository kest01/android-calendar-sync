package ru.kest.calendar

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager

class SettingsActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    private val SYNC_KEY = "sync"

    private val TAG = this::class.simpleName

    private var initialSyncState = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        Log.i(TAG, "onCreate()")
        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        Log.i(TAG, "Initial preferences: ${logPrefs(prefs)}")
        initialSyncState = prefs.getBoolean(SYNC_KEY, false)
        prefs.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        Log.i(TAG, "onSharedPreferenceChanged(): key: $key; ${logPrefs(sharedPreferences)}")
        val newSyncState = sharedPreferences?.getBoolean(SYNC_KEY, false) ?: false
        if (!initialSyncState && newSyncState) {
            Log.w(TAG, "NEED TO START SYNC")
        }
        initialSyncState = newSyncState
    }

    class SettingsFragment : PreferenceFragmentCompat() {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        }
    }

    private fun logPrefs(prefs: SharedPreferences?) = prefs?.all?.toString() ?: "Empty prefs"
}