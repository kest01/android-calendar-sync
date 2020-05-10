package ru.kest.calendar.service

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.provider.CalendarContract.Calendars
import android.util.Log
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.core.app.ActivityCompat
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*


val TAG = SyncService::class.simpleName

data class CalendarAccount(val id: Long,
                           val displayName: String,
                           val accountName: String,
                           val ownerName: String)

class SyncService(private val context: Context,
                  private val contentResolver: ContentResolver) {

    @SuppressLint("MissingPermission")
    fun getCalendarAccounts(): List<CalendarAccount> {
        if (!checkPermissions()) {
            return emptyList()
        }
        val result = mutableListOf<CalendarAccount>()
        val EVENT_PROJECTION = arrayOf(
            Calendars._ID,  // 0
            Calendars.ACCOUNT_NAME,  // 1
            Calendars.CALENDAR_DISPLAY_NAME,  // 2
            Calendars.OWNER_ACCOUNT,  // 3
            Calendars.SYNC_EVENTS // 3
        )
        val cur = contentResolver.query( Calendars.CONTENT_URI, EVENT_PROJECTION, null, null, null)

        if (cur != null) {
            while (cur.moveToNext()) {
                val account = CalendarAccount(
                    cur.getLong(0),
                    cur.getString(1),
                    cur.getString(2),
                    cur.getString(3)
                )
                Log.i(TAG,"Account: $account; synced: ${cur.getInt(4)}")
                result.add(account)
            }
            cur.close()
        }
        Toast.makeText(context, "Sync compete", LENGTH_SHORT).show()
        return result
    }

    data class CalendarEvent(val organizer: String,
                             val title: String,
                             val location: String,
                             val description: String,
                             val start: Long,
                             val end: Long)

    @SuppressLint("MissingPermission")
    fun getEventsForAccount(account: CalendarAccount): List<CalendarEvent> {
        if (!checkPermissions()) {
            return emptyList()
        }
        val result = mutableListOf<CalendarEvent>()
// TODO Не ищет рекурсивные события
        val EVENT_PROJECTION = arrayOf(
            CalendarContract.Events.ORGANIZER,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.RDATE
        )
        val cur = contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            EVENT_PROJECTION,
            "((${CalendarContract.Events.CALENDAR_ID} = ?) AND (${CalendarContract.Events.DTSTART} > ?))",
            arrayOf(account.id.toString(), getUnixTimeAtStartOfDay().toString()),
            null
        )

        if (cur != null) {
            while (cur.moveToNext()) {
                val event = CalendarEvent(
                    cur.getString(0) ?: "",
                    cur.getString(1) ?: "",
                    cur.getString(2) ?: "",
                    cur.getString(3) ?: "",
                    cur.getLong(4) ?: 0,
                    cur.getLong(5) ?: 0
                )
                Log.i(TAG,"Event: $event")
                result.add(event)
            }
            cur.close()
        }
        Log.i(TAG,"getEventsForAccount(): $account")
        return result
    }

    private fun getUnixTimeAtStartOfDay(addDays: Long = 0) =
        LocalDate.now()
            .plusDays(addDays)
            .atStartOfDay()
            .toEpochSecond(ZoneOffset.systemDefault().rules.getOffset(Instant.now())) * 1000

    private fun checkPermissions(): Boolean {
        // TODO Добавить запрос разрешений
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CALENDAR
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "No permissions to calendar")
            Toast.makeText(context, "No permissions to calendar", LENGTH_SHORT).show()
            return false
        }
        return true
    }
}