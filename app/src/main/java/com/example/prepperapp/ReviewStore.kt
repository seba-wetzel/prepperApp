package com.example.prepperapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

internal class ReviewStore private constructor(context: Context) :
    SQLiteOpenHelper(context, "prepper.db", null, 1) {

    private val state = MutableStateFlow<ReviewSchedule?>(null)
    val schedule = state.asStateFlow()

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE review (id INTEGER PRIMARY KEY, first_due TEXT NOT NULL, last_check INTEGER)")
        db.execSQL("CREATE TABLE completion (id INTEGER PRIMARY KEY AUTOINCREMENT, local_date TEXT NOT NULL, timestamp INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE sent_notice (due TEXT PRIMARY KEY NOT NULL)")
        db.execSQL("INSERT INTO review (id, first_due) VALUES (1, ?)", arrayOf(LocalDate.now().toString()))
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        error("Falta una migración de la base local de la versión $oldVersion a $newVersion")
    }

    @Synchronized
    fun refresh() {
        state.value = read(writableDatabase)
    }

    @Synchronized
    fun complete(clearNotice: () -> Unit): Boolean {
        val db = writableDatabase
        val now = Instant.now()
        val date = now.atZone(ZoneId.systemDefault()).toLocalDate()
        if (read(db).history.firstOrNull()?.date == date) return false
        db.insertOrThrow("completion", null, ContentValues().apply {
            put("local_date", date.toString())
            put("timestamp", now.toEpochMilli())
        })
        clearNotice()
        refresh()
        return true
    }

    @Synchronized
    fun undo(id: Long, clearNotice: () -> Unit): Boolean {
        val db = writableDatabase
        if (read(db).history.firstOrNull()?.id != id) return false
        db.delete("completion", "id = ?", arrayOf(id.toString()))
        clearNotice()
        refresh()
        return true
    }

    // Serialize the check with user edits and with the foreground/periodic workers.
    @Synchronized
    fun check(notify: (ReviewSchedule) -> Boolean) {
        val db = writableDatabase
        val current = read(db)
        val due = current.due.toString()
        val sent = db.rawQuery("SELECT 1 FROM sent_notice WHERE due = ?", arrayOf(due))
            .use { it.moveToFirst() }
        if (current.shouldNotify(sent) && notify(current)) {
            db.insertOrThrow("sent_notice", null, ContentValues().apply { put("due", due) })
        }
        db.update("review", ContentValues().apply {
            put("last_check", Instant.now().toEpochMilli())
        }, "id = 1", null)
        refresh()
    }

    private fun read(db: SQLiteDatabase): ReviewSchedule {
        val (firstDue, lastCheck) = db.rawQuery("SELECT first_due, last_check FROM review WHERE id = 1", null)
            .use {
                check(it.moveToFirst())
                LocalDate.parse(it.getString(0)) to
                    if (it.isNull(1)) null else Instant.ofEpochMilli(it.getLong(1))
            }
        val history = db.rawQuery(
            "SELECT id, local_date, timestamp FROM completion ORDER BY id DESC", null
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(Completion(cursor.getLong(0), LocalDate.parse(cursor.getString(1)),
                        Instant.ofEpochMilli(cursor.getLong(2))))
                }
            }
        }
        return ReviewSchedule(firstDue, history, lastCheck, LocalDate.now())
    }

    companion object {
        private var instance: ReviewStore? = null

        @Synchronized
        fun get(context: Context): ReviewStore =
            instance ?: ReviewStore(context.applicationContext).also { instance = it }
    }
}
