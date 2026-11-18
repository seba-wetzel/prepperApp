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
        ReviewStore.get(applicationContext).check { review ->
            ReviewReminders.send(applicationContext, "Próxima revisión: ${review.due.displayDate()}")
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

    fun schedule(context: Context) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "daily-reserve-review",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<ReviewWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(1, TimeUnit.DAYS)
                .build()
        )
    }

    fun checkNow(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            "check-reserve-review",
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            OneTimeWorkRequestBuilder<ReviewWorker>().build()
        )
    }

    fun registerChannel(context: Context) {
        manager(context).createNotificationChannel(
            NotificationChannel(CHANNEL, "Revisiones de reservas", NotificationManager.IMPORTANCE_DEFAULT)
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
        if (!appEnabled(context) || !channelEnabled(context)) return false
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
        ) return false
        val openTasks = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java)
                .setAction(OPEN_TASKS)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(if (test) "Aviso de prueba" else REVIEW_TITLE)
            .setContentText(message)
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
