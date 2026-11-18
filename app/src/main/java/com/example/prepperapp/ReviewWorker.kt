package com.example.prepperapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteException
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class ReviewWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result = try {
        val state = PrepperRepository.get(applicationContext).load()
        if (!state.settings.configured) {
            ReviewReminders.clearReview(applicationContext)
        } else {
            val parts = buildList {
                if (state.pendingTasks.isNotEmpty()) add("${state.pendingTasks.size} tareas pendientes")
                if (state.expiringLots.isNotEmpty()) add("${state.expiringLots.size} lotes vencidos o próximos a vencer")
            }
            if (parts.isNotEmpty()) {
                ReviewReminders.send(applicationContext, parts.joinToString(" · "))
            } else {
                ReviewReminders.clearReview(applicationContext)
            }
        }
        Result.success()
    } catch (_: SQLiteException) {
        Result.retry()
    }
}

internal object ReviewReminders {
    const val OPEN_TASKS = "com.example.prepperapp.OPEN_TASKS"
    const val CHANNEL = "reserve_reviews"
    private const val REVIEW_NOTIFICATION = 1
    private const val TEST_NOTIFICATION = 2
    private const val PERIODIC_WORK = "daily-reserve-review"
    private const val IMMEDIATE_WORK = "check-reserve-review"

    fun schedule(context: Context) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<ReviewWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(1, TimeUnit.DAYS)
                .build()
        )
    }

    fun checkNow(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_WORK,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<ReviewWorker>().build()
        )
    }

    fun stop(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK)
        WorkManager.getInstance(context).cancelUniqueWork(IMMEDIATE_WORK)
        clearReview(context)
    }

    fun registerChannel(context: Context) {
        manager(context).createNotificationChannel(
            NotificationChannel(CHANNEL, "Revisiones y vencimientos", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    fun hasPermission(context: Context) = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun appEnabled(context: Context) = manager(context).areNotificationsEnabled()

    fun channelEnabled(context: Context) =
        manager(context).getNotificationChannel(CHANNEL)?.importance?.let {
            it != NotificationManager.IMPORTANCE_NONE
        } ?: false

    fun clearReview(context: Context) = manager(context).cancel(REVIEW_NOTIFICATION)

    fun send(context: Context, message: String, test: Boolean = false): Boolean {
        registerChannel(context)
        if (!hasPermission(context) || !appEnabled(context) || !channelEnabled(context)) return false
        val openTasks = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java)
                .setAction(OPEN_TASKS)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(if (test) "Aviso de prueba" else "Mi kit de emergencia")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(openTasks)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()
        return try {
            manager(context).notify(if (test) TEST_NOTIFICATION else REVIEW_NOTIFICATION, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    private fun manager(context: Context) = context.getSystemService(NotificationManager::class.java)
}
