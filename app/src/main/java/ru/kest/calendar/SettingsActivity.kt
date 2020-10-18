package ru.kest.calendar

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import ru.kest.calendar.service.CalendarSyncWorker
import java.util.concurrent.TimeUnit

const val SYNC_KEY = "sync"

class SettingsActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    private val TAG = this::class.simpleName
    private val WORKER_TAG = "CalendarSyncWorker"

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
        syncCalendars(initialSyncState)
        prefs.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        Log.i(TAG, "onSharedPreferenceChanged(): key: $key; ${logPrefs(sharedPreferences)}")
        val newSyncState = sharedPreferences?.getBoolean(SYNC_KEY, false) ?: false
        if (initialSyncState != newSyncState) {
            syncCalendars(newSyncState)
        }
        initialSyncState = newSyncState
    }

    private fun syncCalendars(enable: Boolean) {
        WorkManager
            .getInstance(applicationContext)
            .cancelAllWorkByTag(WORKER_TAG)

        if (enable) {

            val immediateSyncWorkerRequest: WorkRequest = OneTimeWorkRequestBuilder<CalendarSyncWorker>()
                .addTag(WORKER_TAG)
                .build()
            WorkManager
                .getInstance(applicationContext)
                .enqueue(immediateSyncWorkerRequest)

            val periodicSyncWorkRequest: WorkRequest = PeriodicWorkRequestBuilder<CalendarSyncWorker>(
                30, TimeUnit.MINUTES,
                15, TimeUnit.MINUTES
            )
                .addTag(WORKER_TAG)
                .build()

            WorkManager
                .getInstance(applicationContext)
                .enqueue(periodicSyncWorkRequest)
            Log.i(TAG,"$WORKER_TAG is started")
        } else {
            Log.i(TAG,"$WORKER_TAG is canceled")
        }
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        }
    }

    private fun logPrefs(prefs: SharedPreferences?) = prefs?.all?.toString() ?: "Empty prefs"
}