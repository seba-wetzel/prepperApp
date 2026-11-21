package ar.nuestramerica.prepper.avisos

import ar.nuestramerica.prepper.R
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import ar.nuestramerica.prepper.DateKind
import ar.nuestramerica.prepper.MainActivity
import ar.nuestramerica.prepper.MaintenanceTask
import ar.nuestramerica.prepper.PrepperState
import ar.nuestramerica.prepper.Product
import ar.nuestramerica.prepper.ReviewWorker
import ar.nuestramerica.prepper.StockLot
import ar.nuestramerica.prepper.asQuantity
import java.util.concurrent.TimeUnit

internal data class ReviewAlert(
    val key: String, val title: String, val detail: String, val date: java.time.LocalDate,
    val lot: StockLot? = null, val product: Product? = null, val task: MaintenanceTask? = null,
)

internal fun StockLot.reviewKey() = "lot/$id/$productId/$code"

internal fun PrepperState.reviewAlerts(): List<ReviewAlert> {
    if (!settings.configured) return emptyList()
    val format = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
    return buildList {
        for ((product, lot) in expiringLots) {
            val date = requireNotNull(lot.date)
            val label = when {
                lot.dateKind != DateKind.EXPIRATION -> lot.dateKind.label
                date < today -> "Venció el"
                date == today -> "Vence hoy"
                date == today.plusDays(1) -> "Vence mañana"
                else -> "Vence el"
            }
            add(ReviewAlert(lot.reviewKey(), "${product.name} · ${lot.code}",
                "$label ${date.format(format)} · ${lot.quantityMilli.asQuantity()} ${product.unit}",
                date, lot = lot, product = product))
        }
        for (task in pendingTasks) {
            val due = taskDue(task)
            add(ReviewAlert("task/${task.id}/$due", task.title,
                "Pendiente desde el ${due.format(format)}", due, task = task))
        }
    }.sortedBy { it.date }
}

internal object ReviewReminders {
    const val OPEN_TASKS = "ar.nuestramerica.prepper.OPEN_TASKS"
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

    const val SUMMARY = "summary"
    private const val GROUP = "reserve_alerts"

    fun clearReview(context: Context) = publish(context, emptyList())

    fun publish(context: Context, alerts: List<ReviewAlert>) {
        val manager = manager(context)
        registerChannel(context)
        val keys = alerts.mapTo(mutableSetOf()) { it.key }
        for (old in manager.activeNotifications) {
            if (old.id == REVIEW_NOTIFICATION && old.tag !in keys && !(old.tag == SUMMARY && alerts.size > 1)) {
                manager.cancel(old.tag, old.id)
            }
        }
        if (alerts.isEmpty() || !hasPermission(context) || !appEnabled(context) || !channelEnabled(context)) return
        fun post(key: String, title: String, detail: String, summary: Boolean = false) {
            val intent = Intent(context, MainActivity::class.java).setAction(OPEN_TASKS)
                .setData(android.net.Uri.Builder().scheme("prepper").authority("review").appendPath(key).build())
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val builder = NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title).setContentText(detail)
                .setStyle(NotificationCompat.BigTextStyle().bigText(detail))
                .setOnlyAlertOnce(true).setAutoCancel(true)
                .setContentIntent(PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE))
            if (alerts.size > 1) builder.setGroup(GROUP).setGroupSummary(summary)
                .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            manager.notify(key, REVIEW_NOTIFICATION, builder.build())
        }
        alerts.forEach { post(it.key, it.title, it.detail) }
        if (alerts.size > 1) post(SUMMARY, "${alerts.size} avisos de revisión",
            alerts.take(3).joinToString(" · ") { it.title }, true)
    }

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
