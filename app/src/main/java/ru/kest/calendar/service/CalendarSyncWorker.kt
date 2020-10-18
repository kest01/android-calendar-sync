package ru.kest.calendar.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.*


class CalendarSyncWorker(appContext: Context, workerParams: WorkerParameters): Worker(
    appContext,
    workerParams
) {

    private val TAG = this::class.simpleName

    override fun doWork(): Result {
        val calendarService = CalendarService(applicationContext)
        val accounts = calendarService.getCalendarAccounts()
        val sourceAccount = accounts
            .find { it.ownerName.toLowerCase(Locale.ENGLISH) == "kkharitonov@luxoft.com" }
        val targetAccount = accounts
            .find { it.ownerName.toLowerCase(Locale.ENGLISH) == "konstantin.kharitonov@gmail.com" }
        if (sourceAccount == null || targetAccount == null) {
            backgroundToast(applicationContext, "Source or target account not found")
        } else {
            val sourceEvents = calendarService.getEventsForAccount(sourceAccount, 3)
            val targetEvents = calendarService.getEventsForAccount(targetAccount, 3)
            var count = 0
            sourceEvents.forEach {
                if (!calendarService.isCalendarContainsEvent(targetEvents, it)) {
                    calendarService.addEvent(it, targetAccount)
                    count++
                }
            }
            Log.i(TAG, "Sync completed. $count event synced.")
            backgroundToast(applicationContext, "Sync completed. $count event synced.")
        }

        // Indicate whether the work finished successfully with the Result
        return Result.success()
    }

    private fun backgroundToast(context: Context, msg: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }
}