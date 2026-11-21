package ar.nuestramerica.prepper

import android.content.Context
import android.database.sqlite.SQLiteException
import androidx.work.Worker
import androidx.work.WorkerParameters
import ar.nuestramerica.prepper.avisos.ReviewReminders
import ar.nuestramerica.prepper.avisos.reviewAlerts

class ReviewWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result = try {
        val state = PrepperRepository.get(applicationContext).load()
        ReviewReminders.publish(applicationContext, state.reviewAlerts())
        Result.success()
    } catch (_: SQLiteException) {
        Result.retry()
    }
}
