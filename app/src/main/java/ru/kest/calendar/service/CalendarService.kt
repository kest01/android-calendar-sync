package ru.kest.calendar.service

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CalendarContract
import android.provider.CalendarContract.Calendars
import android.util.Log
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.core.app.ActivityCompat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset


val TAG = CalendarService::class.simpleName

data class CalendarAccount(val id: Long,
                           val displayName: String,
                           val accountName: String,
                           val ownerName: String)


data class CalendarEvent(val organizer: String,
                         val title: String,
                         val location: String,
                         val description: String,
                         val timeZone: String,
                         val start: Long,
                         val end: Long)

class CalendarService(private val context: Context) {

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
        val cur = context.contentResolver.query( Calendars.CONTENT_URI, EVENT_PROJECTION, null, null, null)

        if (cur != null) {
            while (cur.moveToNext()) {
                val account = CalendarAccount(
                    cur.getLong(0),
                    cur.getString(1),
                    cur.getString(2),
                    cur.getString(3)
                )
                Log.d(TAG,"Account: $account; synced: ${cur.getInt(4)}")
                result.add(account)
            }
            cur.close()
        }
        return result
    }

    @SuppressLint("MissingPermission")
    fun getEventsForAccount(account: CalendarAccount, daysFromToday: Int): List<CalendarEvent> {
        if (!checkPermissions()) {
            return emptyList()
        }
        val result = mutableListOf<CalendarEvent>()
        val EVENT_PROJECTION = arrayOf(
            CalendarContract.Instances.ORGANIZER,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.EVENT_TIMEZONE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END
        )
        val builder: Uri.Builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, getTimeAtStartOfDayMillis())
        ContentUris.appendId(builder, getTimeAtStartOfDayMillis(daysFromToday.toLong()))

        val cur = context.contentResolver.query(
            builder.build(),
            EVENT_PROJECTION,
            "(${CalendarContract.Instances.CALENDAR_ID} = ?)",
            arrayOf(account.id.toString()),
            CalendarContract.Instances.BEGIN
        )

        if (cur != null) {
            while (cur.moveToNext()) {
                val event = CalendarEvent(
                    cur.getString(0) ?: "",
                    cur.getString(1) ?: "",
                    cur.getString(2) ?: "",
                    cur.getString(3) ?: "",
                    cur.getString(4) ?: "",
                    cur.getLong(5) ?: 0,
                    cur.getLong(6) ?: 0
                )
                Log.d(TAG,"Event: $event")
                result.add(event)
            }
            cur.close()
        }
        return result
    }

    @SuppressLint("MissingPermission")
    fun addEvent(event: CalendarEvent, targetAccount: CalendarAccount) {
        if (!checkPermissions()) {
            return
        }
        val eventFields = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, event.start)
            put(CalendarContract.Events.DTEND, event.end)
            put(CalendarContract.Events.ORGANIZER, event.organizer)
            put(CalendarContract.Events.TITLE, "[GC] " + event.title)
            put(CalendarContract.Events.DESCRIPTION, "Organizer: ${event.organizer}\n" + event.description)
            put(CalendarContract.Events.EVENT_LOCATION, event.location)
            put(CalendarContract.Events.CALENDAR_ID, targetAccount.id)
            put(CalendarContract.Events.EVENT_TIMEZONE, event.timeZone)
        }
        val uri: Uri? = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, eventFields)
        Log.d(TAG, "Created event $event; result: $uri")
        if (uri != null) {
            val eventId: Long? = uri.lastPathSegment?.toLong()
            if (eventId != null) {
                val reminderFields = ContentValues().apply {
                    put(CalendarContract.Reminders.MINUTES, 2)
                    put(CalendarContract.Reminders.EVENT_ID, eventId)
                    put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                }
                context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderFields)
            }
        }
    }

    fun isCalendarContainsEvent(events: List<CalendarEvent>, event: CalendarEvent): Boolean =
        events.any { it.start == event.start && it.title.contains(event.title)}

    private fun getTimeAtStartOfDayMillis(addDays: Long = 0) =
        LocalDate.now()
            .plusDays(addDays)
            .atStartOfDay()
            .toEpochSecond(ZoneOffset.systemDefault().rules.getOffset(Instant.now())) * 1000

    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(context,Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(context,Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            // TODO Добавить запрос разрешений
            Log.e(TAG, "No permissions to calendar")
            Toast.makeText(context, "No permissions to calendar", LENGTH_SHORT).show()
            return false
        }
        return true
    }
}