package ru.kest.calendar

import android.Manifest
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract.Calendars
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat


class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.i(TAG, "Hello world TAG!")
        logCalendars()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    private val EVENT_PROJECTION = arrayOf(
        Calendars._ID,  // 0
        Calendars.ACCOUNT_NAME,  // 1
        Calendars.CALENDAR_DISPLAY_NAME,  // 2
        Calendars.OWNER_ACCOUNT // 3
    )

    // The indices for the projection array above.
    private val PROJECTION_ID_INDEX = 0
    private val PROJECTION_ACCOUNT_NAME_INDEX = 1
    private val PROJECTION_DISPLAY_NAME_INDEX = 2
    private val PROJECTION_OWNER_ACCOUNT_INDEX = 3

    fun logCalendars() {
        // Run query
        var cur: Cursor? = null
        val cr = contentResolver
        val uri: Uri = Calendars.CONTENT_URI

        // Submit the query and get a Cursor object back.
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CALENDAR
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "No permissions to calendar")
            return
        }
        cur = cr.query(uri, EVENT_PROJECTION, null, null, null)

        while (cur!!.moveToNext()) {
            // Get the field values
            val calID = cur.getLong(PROJECTION_ID_INDEX)
            val displayName = cur.getString(PROJECTION_DISPLAY_NAME_INDEX)
            val accountName = cur.getString(PROJECTION_ACCOUNT_NAME_INDEX)
            val ownerName = cur.getString(PROJECTION_OWNER_ACCOUNT_INDEX)

            Log.i(TAG, "calID: $calID; displayName: $displayName; accountName: $accountName; ownerName: $ownerName")

//            if (accountName == "KKharitonov@luxoft.com")
        }
    }
}
