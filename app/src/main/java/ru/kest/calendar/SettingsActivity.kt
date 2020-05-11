package ru.kest.calendar

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import ru.kest.calendar.service.CalendarService
import java.util.*

const val SYNC_KEY = "sync"

class SettingsActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

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
            syncCalendars()
        }
        initialSyncState = newSyncState
    }

    private fun syncCalendars() {
        val calendarService = CalendarService(applicationContext, contentResolver)
        val accounts = calendarService.getCalendarAccounts()
        val sourceAccount = accounts
            .find { it.ownerName.toLowerCase(Locale.ENGLISH) == "kkharitonov@luxoft.com" }
        val targetAccount = accounts
            .find { it.ownerName.toLowerCase(Locale.ENGLISH) == "konstantin.kharitonov@gmail.com" }
        if (sourceAccount == null || targetAccount == null) {
            Toast.makeText(applicationContext, "Source or target account not found", LENGTH_SHORT)
                .show()
        } else {
            val sourceEvents = calendarService.getEventsForAccount(sourceAccount, 3)
            val targetEvents = calendarService.getEventsForAccount(targetAccount, 3)
            sourceEvents.forEach {
                if (!calendarService.isCalendarContainsEvent(targetEvents, it)) {
                    calendarService.addEvent(it, targetAccount)
                }
            }
        }
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        }
    }

    private fun logPrefs(prefs: SharedPreferences?) = prefs?.all?.toString() ?: "Empty prefs"
}