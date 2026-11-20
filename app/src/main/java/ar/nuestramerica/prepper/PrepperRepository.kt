package ar.nuestramerica.prepper

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate

internal class PrepperRepository private constructor(context: Context) :
    SQLiteOpenHelper(context, "prepper_app.db", null, 3) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE settings (
            id INTEGER PRIMARY KEY CHECK(id = 1), people INTEGER NOT NULL, weeks INTEGER NOT NULL,
            daily_kcal INTEGER NOT NULL, water_milli INTEGER NOT NULL, last_backup INTEGER,
            configured INTEGER NOT NULL, task_start TEXT NOT NULL)""")
        db.execSQL("""CREATE TABLE product (
            id TEXT PRIMARY KEY, name TEXT NOT NULL, unit TEXT NOT NULL, category TEXT NOT NULL,
            weekly_milli INTEGER NOT NULL, kcal INTEGER NOT NULL, step_milli INTEGER NOT NULL,
            note TEXT NOT NULL, custom INTEGER NOT NULL DEFAULT 0,
            visible INTEGER NOT NULL DEFAULT 1)""")
        db.execSQL("""CREATE TABLE lot (
            id INTEGER PRIMARY KEY AUTOINCREMENT, product_id TEXT NOT NULL REFERENCES product(id) ON DELETE CASCADE,
            quantity_milli INTEGER NOT NULL CHECK(quantity_milli >= 0), local_date TEXT,
            date_kind TEXT NOT NULL, location TEXT NOT NULL, usable INTEGER NOT NULL DEFAULT 1,
            code TEXT NOT NULL)""")
        db.execSQL("""CREATE TABLE task (
            id TEXT PRIMARY KEY, title TEXT NOT NULL, period TEXT NOT NULL, active INTEGER NOT NULL DEFAULT 1,
            custom INTEGER NOT NULL DEFAULT 0)""")
        db.execSQL("""CREATE TABLE task_completion (
            id INTEGER PRIMARY KEY AUTOINCREMENT, task_id TEXT NOT NULL REFERENCES task(id) ON DELETE CASCADE,
            local_date TEXT NOT NULL, instant INTEGER NOT NULL)""")
        db.execSQL(
            "INSERT INTO settings (id,people,weeks,daily_kcal,water_milli,last_backup,configured,task_start) VALUES (1,5,2,1900,4000,NULL,0,?)",
            arrayOf(LocalDate.now().toString())
        )
        seedProducts(db)
        seedTasks(db)
    }

    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE settings ADD COLUMN configured INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE settings ADD COLUMN task_start TEXT NOT NULL DEFAULT '1970-01-01'")
            db.execSQL("UPDATE settings SET task_start=?", arrayOf(LocalDate.now().toString()))
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE product ADD COLUMN visible INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE lot ADD COLUMN code TEXT NOT NULL DEFAULT ''")
            assignMissingLotCodes(db)
        }
    }

    @Synchronized
    fun load(): PrepperState {
        val db = readableDatabase
        val settings = db.rawQuery(
            "SELECT people, weeks, daily_kcal, water_milli, last_backup, configured, task_start FROM settings WHERE id=1", null
        ).use {
            check(it.moveToFirst())
            HouseholdSettings(
                people = it.getInt(0),
                weeks = it.getInt(1),
                dailyKcal = it.getInt(2),
                dailyWaterLitresMilli = it.getLong(3),
                lastBackup = if (it.isNull(4)) null else Instant.ofEpochMilli(it.getLong(4)),
                configured = it.getInt(5) == 1,
                taskScheduleStart = LocalDate.parse(it.getString(6))
            )
        }
        val products = db.rawQuery(
            "SELECT id,name,unit,category,weekly_milli,kcal,step_milli,note,custom,visible FROM product ORDER BY rowid", null
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(Product(
                    cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3),
                    cursor.getLong(4), cursor.getInt(5), cursor.getLong(6), cursor.getString(7),
                    cursor.getInt(8) == 1, cursor.getInt(9) == 1
                ))
            }
        }
        val lots = db.rawQuery(
            "SELECT id,product_id,quantity_milli,local_date,date_kind,location,usable,code FROM lot WHERE quantity_milli>0", null
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(StockLot(
                    cursor.getLong(0), cursor.getString(1), cursor.getLong(2),
                    cursor.getString(3)?.let(LocalDate::parse), DateKind.valueOf(cursor.getString(4)),
                    cursor.getString(5), cursor.getInt(6) == 1, cursor.getString(7)
                ))
            }
        }
        val completions = db.rawQuery(
            "SELECT id,task_id,local_date,instant FROM task_completion ORDER BY instant DESC", null
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(TaskCompletion(
                    cursor.getLong(0), cursor.getString(1), LocalDate.parse(cursor.getString(2)),
                    Instant.ofEpochMilli(cursor.getLong(3))
                ))
            }
        }
        val tasks = db.rawQuery("SELECT id,title,period,active FROM task ORDER BY rowid", null).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    val id = cursor.getString(0)
                    add(MaintenanceTask(
                        id, cursor.getString(1), TaskPeriod.valueOf(cursor.getString(2)),
                        cursor.getInt(3) == 1, completions.filter { it.taskId == id }
                    ))
                }
            }
        }
        return PrepperState(settings, products, lots, tasks, LocalDate.now(), loading = false)
    }

    @Synchronized
    fun updateSettings(settings: HouseholdSettings) {
        require(settings.people in 1..30 && settings.weeks in 1..52)
        require(settings.dailyKcal in 1000..5000 && settings.dailyWaterLitresMilli in 500..20_000)
        writableDatabase.update("settings", ContentValues().apply {
            put("people", settings.people)
            put("weeks", settings.weeks)
            put("daily_kcal", settings.dailyKcal)
            put("water_milli", settings.dailyWaterLitresMilli)
            put("configured", if (settings.configured) 1 else 0)
            put("task_start", settings.taskScheduleStart.toString())
        }, "id=1", null)
    }

    @Synchronized
    fun adjustProduct(product: Product, deltaMilli: Long) {
        if (deltaMilli == 0L) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            if (deltaMilli > 0) {
                val id = db.rawQuery(
                    "SELECT id FROM lot WHERE product_id=? AND local_date IS NULL AND location='' AND usable=1 LIMIT 1",
                    arrayOf(product.id)
                ).use { if (it.moveToFirst()) it.getLong(0) else null }
                if (id == null) insertLot(db, product.id, deltaMilli, null, DateKind.EXPIRATION, "", true)
                else db.execSQL("UPDATE lot SET quantity_milli=quantity_milli+? WHERE id=?", arrayOf(deltaMilli, id))
            } else {
                consume(db, product.id, -deltaMilli)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    @Synchronized
    fun addLot(productId: String, quantityMilli: Long, date: LocalDate?, kind: DateKind, location: String): String {
        require(quantityMilli > 0)
        return insertLot(writableDatabase, productId, quantityMilli, date, kind, location.trim(), true)
    }

    @Synchronized
    fun consumeLot(lotId: Long, quantityMilli: Long) {
        require(quantityMilli > 0)
        val available = readableDatabase.rawQuery(
            "SELECT quantity_milli FROM lot WHERE id=?",
            arrayOf(lotId.toString()),
        ).use { cursor ->
            require(cursor.moveToFirst()) { "La existencia ya no está disponible" }
            cursor.getLong(0)
        }
        require(quantityMilli <= available) { "La cantidad supera la existencia elegida" }
        writableDatabase.execSQL(
            "UPDATE lot SET quantity_milli=quantity_milli-? WHERE id=?",
            arrayOf(quantityMilli, lotId),
        )
    }

    private fun consume(db: SQLiteDatabase, productId: String, requested: Long) {
        var remaining = requested
        val candidates = db.rawQuery(
            """SELECT id,quantity_milli FROM lot WHERE product_id=? AND quantity_milli>0
                ORDER BY usable DESC, local_date IS NULL, local_date, id""", arrayOf(productId)
        ).use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.getLong(0) to cursor.getLong(1)) }
        }
        for ((id, available) in candidates) {
            if (remaining <= 0) break
            val used = minOf(available, remaining)
            db.execSQL("UPDATE lot SET quantity_milli=quantity_milli-? WHERE id=?", arrayOf(used, id))
            remaining -= used
        }
    }

    @Synchronized
    fun updateLot(id: Long, quantityMilli: Long, date: LocalDate?, kind: DateKind, location: String) {
        require(quantityMilli > 0)
        val values = ContentValues().apply {
            put("quantity_milli", quantityMilli)
            put("local_date", date?.toString())
            put("date_kind", kind.name)
            put("location", location.trim())
        }
        check(writableDatabase.update("lot", values, "id=? AND quantity_milli>0", arrayOf(id.toString())) == 1) {
            "La existencia ya no está disponible"
        }
    }

    @Synchronized
    fun deleteLot(id: Long) {
        writableDatabase.delete("lot", "id=?", arrayOf(id.toString()))
    }

    @Synchronized
    fun addProduct(name: String, unit: String, category: String, weeklyMilli: Long, kcal: Int, stepMilli: Long) {
        require(name.isNotBlank() && unit.isNotBlank() && weeklyMilli >= 0 && kcal >= 0 && stepMilli > 0)
        val id = "custom_${Instant.now().toEpochMilli()}"
        writableDatabase.insertOrThrow("product", null, ContentValues().apply {
            put("id", id); put("name", name.trim()); put("unit", unit.trim())
            put("category", category.trim().ifBlank { "Agregados por vos" })
            put("weekly_milli", weeklyMilli); put("kcal", kcal); put("step_milli", stepMilli)
            put("note", ""); put("custom", 1)
        })
    }

    @Synchronized
    fun deleteCustomProduct(id: String) {
        writableDatabase.delete("product", "id=? AND custom=1", arrayOf(id))
    }

    @Synchronized
    fun setProductVisible(id: String, visible: Boolean) {
        writableDatabase.update("product", ContentValues().apply {
            put("visible", if (visible) 1 else 0)
        }, "id=?", arrayOf(id))
    }

    @Synchronized
    fun completeTask(taskId: String) {
        val now = Instant.now()
        val date = LocalDate.now()
        val already = readableDatabase.rawQuery(
            "SELECT 1 FROM task_completion WHERE task_id=? AND local_date=? LIMIT 1",
            arrayOf(taskId, date.toString())
        ).use { it.moveToFirst() }
        if (already) return
        writableDatabase.insertOrThrow("task_completion", null, ContentValues().apply {
            put("task_id", taskId); put("local_date", date.toString()); put("instant", now.toEpochMilli())
        })
    }

    @Synchronized
    fun undoTask(taskId: String) {
        writableDatabase.execSQL(
            "DELETE FROM task_completion WHERE id=(SELECT id FROM task_completion WHERE task_id=? ORDER BY instant DESC LIMIT 1)",
            arrayOf(taskId)
        )
    }

    @Synchronized
    fun addTask(title: String, period: TaskPeriod) {
        require(title.isNotBlank())
        writableDatabase.insertOrThrow("task", null, ContentValues().apply {
            put("id", "custom_${Instant.now().toEpochMilli()}")
            put("title", title.trim()); put("period", period.name); put("active", 1); put("custom", 1)
        })
    }

    @Synchronized
    fun updateTask(id: String, title: String, period: TaskPeriod) {
        require(title.isNotBlank())
        writableDatabase.update("task", ContentValues().apply {
            put("title", title.trim())
            put("period", period.name)
        }, "id=?", arrayOf(id))
    }

    @Synchronized
    fun setTaskActive(id: String, active: Boolean) {
        writableDatabase.update("task", ContentValues().apply { put("active", if (active) 1 else 0) }, "id=?", arrayOf(id))
    }

    @Synchronized
    fun resetAll() {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("task_completion", null, null)
            db.delete("lot", null, null)
            db.delete("task", null, null)
            db.delete("product", null, null)
            db.update("settings", ContentValues().apply {
                put("people", 5); put("weeks", 2); put("daily_kcal", 1900)
                put("water_milli", 4000); putNull("last_backup")
                put("configured", 0); put("task_start", LocalDate.now().toString())
            }, "id=1", null)
            seedProducts(db)
            seedTasks(db)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    @Synchronized
    fun markBackup() {
        writableDatabase.update("settings", ContentValues().apply {
            put("last_backup", Instant.now().toEpochMilli())
        }, "id=1", null)
    }

    @Synchronized
    fun exportJson(): String {
        val state = load()
        return JSONObject().apply {
            put("version", 2)
            put("exportedAt", Instant.now().toString())
            put("settings", JSONObject().apply {
                put("people", state.settings.people); put("weeks", state.settings.weeks)
                put("dailyKcal", state.settings.dailyKcal)
                put("dailyWaterLitresMilli", state.settings.dailyWaterLitresMilli)
                put("configured", state.settings.configured)
                put("taskScheduleStart", state.settings.taskScheduleStart.toString())
            })
            put("products", JSONArray().apply { state.products.forEach { p ->
                put(JSONObject().apply {
                    put("id",p.id); put("name",p.name); put("unit",p.unit); put("category",p.category)
                    put("weeklyMilli",p.weeklyForFiveMilli); put("kcal",p.kcalPerUnit)
                    put("stepMilli",p.stepMilli); put("note",p.note); put("custom",p.custom)
                    put("visible",p.visible)
                })
            } })
            put("lots", JSONArray().apply { state.lots.forEach { lot ->
                put(JSONObject().apply {
                    put("productId",lot.productId); put("quantityMilli",lot.quantityMilli)
                    put("date",lot.date?.toString() ?: JSONObject.NULL); put("dateKind",lot.dateKind.name)
                    put("location",lot.location); put("usable",lot.usable); put("code",lot.code)
                })
            } })
            put("tasks", JSONArray().apply { state.tasks.forEach { task ->
                put(JSONObject().apply {
                    put("id",task.id); put("title",task.title); put("period",task.period.name); put("active",task.active)
                    put("completions",JSONArray().apply { task.completions.forEach { c ->
                        put(JSONObject().apply { put("date",c.date.toString()); put("instant",c.instant.toEpochMilli()) })
                    } })
                })
            } })
        }.toString(2)
    }

    @Synchronized
    fun importJson(text: String) {
        val root = JSONObject(text)
        require(root.getInt("version") == 2) { "Versión de copia no compatible" }
        val settingsJson = root.getJSONObject("settings")
        val productsJson = root.getJSONArray("products")
        val lotsJson = root.getJSONArray("lots")
        val tasksJson = root.getJSONArray("tasks")
        require(productsJson.length() in 1..1000 && tasksJson.length() <= 1000 && lotsJson.length() <= 10_000)
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("task_completion", null, null); db.delete("lot", null, null)
            db.delete("task", null, null); db.delete("product", null, null)
            val settings = HouseholdSettings(
                people = settingsJson.getInt("people"),
                weeks = settingsJson.getInt("weeks"),
                dailyKcal = settingsJson.getInt("dailyKcal"),
                dailyWaterLitresMilli = settingsJson.getLong("dailyWaterLitresMilli"),
                configured = settingsJson.optBoolean("configured", true),
                taskScheduleStart = settingsJson.optString("taskScheduleStart")
                    .takeIf { it.isNotBlank() }?.let(LocalDate::parse) ?: LocalDate.now()
            )
            updateSettings(settings)
            db.execSQL("UPDATE settings SET last_backup=NULL WHERE id=1")
            for (i in 0 until productsJson.length()) {
                val p = productsJson.getJSONObject(i)
                db.insertOrThrow("product", null, ContentValues().apply {
                    put("id",p.getString("id")); put("name",p.getString("name")); put("unit",p.getString("unit"))
                    put("category",p.getString("category")); put("weekly_milli",p.getLong("weeklyMilli"))
                    put("kcal",p.getInt("kcal")); put("step_milli",p.getLong("stepMilli"))
                    put("note",p.optString("note")); put("custom",if(p.optBoolean("custom")) 1 else 0)
                    put("visible",if(p.optBoolean("visible",true)) 1 else 0)
                })
            }
            for (i in 0 until lotsJson.length()) {
                val lot = lotsJson.getJSONObject(i)
                val date = if (lot.isNull("date")) null else LocalDate.parse(lot.getString("date"))
                insertLot(db,lot.getString("productId"),lot.getLong("quantityMilli"),date,
                    DateKind.valueOf(lot.getString("dateKind")),lot.optString("location"),
                    lot.optBoolean("usable",true),lot.optString("code").takeIf { it.isNotBlank() })
            }
            for (i in 0 until tasksJson.length()) {
                val task = tasksJson.getJSONObject(i)
                db.insertOrThrow("task", null, ContentValues().apply {
                    put("id",task.getString("id")); put("title",task.getString("title"))
                    put("period",TaskPeriod.valueOf(task.getString("period")).name)
                    put("active",if(task.optBoolean("active",true)) 1 else 0); put("custom",1)
                })
                val history = task.getJSONArray("completions")
                for (j in 0 until history.length()) {
                    val c = history.getJSONObject(j)
                    db.insertOrThrow("task_completion", null, ContentValues().apply {
                        put("task_id",task.getString("id")); put("local_date",LocalDate.parse(c.getString("date")).toString())
                        put("instant",c.getLong("instant"))
                    })
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun nextLotCodeFor(db: SQLiteDatabase, productId: String): String {
        val productName = db.rawQuery("SELECT name FROM product WHERE id=?", arrayOf(productId)).use {
            require(it.moveToFirst()) { "El producto ya no está disponible" }
            it.getString(0)
        }
        val prefix = lotCodePrefix(productId, productName)
        val existing = db.rawQuery("SELECT code FROM lot WHERE code LIKE ?", arrayOf("$prefix%"))
            .use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.getString(0)) } }
        return nextLotCode(prefix, existing)
    }

    private fun assignMissingLotCodes(db: SQLiteDatabase) {
        val ids = db.rawQuery("SELECT id,product_id FROM lot WHERE code='' ORDER BY id", null).use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.getLong(0) to cursor.getString(1)) }
        }
        ids.forEach { (id, productId) ->
            db.execSQL("UPDATE lot SET code=? WHERE id=?", arrayOf<Any>(nextLotCodeFor(db, productId), id))
        }
    }

    private fun insertLot(
        db: SQLiteDatabase, productId: String, quantity: Long, date: LocalDate?,
        kind: DateKind, location: String, usable: Boolean, preferredCode: String? = null,
    ): String {
        val code = preferredCode ?: nextLotCodeFor(db, productId)
        db.insertOrThrow("lot", null, ContentValues().apply {
            put("product_id", productId); put("quantity_milli", quantity)
            if (date == null) putNull("local_date") else put("local_date", date.toString())
            put("date_kind", kind.name); put("location", location); put("usable", if (usable) 1 else 0)
            put("code", code)
        })
        return code
    }

    private fun seedProducts(db: SQLiteDatabase) {
        BASE_PRODUCTS.forEach { p ->
            db.insertOrThrow("product", null, ContentValues().apply {
                put("id",p.id); put("name",p.name); put("unit",p.unit); put("category",p.category)
                put("weekly_milli",p.weeklyForFiveMilli); put("kcal",p.kcalPerUnit)
                put("step_milli",p.stepMilli); put("note",p.note); put("custom",0); put("visible",1)
            })
        }
    }

    private fun seedTasks(db: SQLiteDatabase) {
        BASE_TASKS.forEach { (id,title,period) ->
            db.insertOrThrow("task", null, ContentValues().apply {
                put("id",id); put("title",title); put("period",period.name); put("active",1); put("custom",0)
            })
        }
    }

    companion object {
        @Volatile private var instance: PrepperRepository? = null
        fun get(context: Context): PrepperRepository = instance ?: synchronized(this) {
            instance ?: PrepperRepository(context.applicationContext).also { instance = it }
        }

        private fun p(id:String,name:String,unit:String,category:String,week:Double,kcal:Int,step:Double,note:String="") =
            Product(id,name,unit,category,(week*1000).toLong(),kcal,(step*1000).toLong(),note)

        val BASE_PRODUCTS = listOf(
            p("agua","Agua","litros","Agua",140.0,0,5.0,"4 L por persona por día"),
            p("arroz","Arroz","kg","Base",2.0,3600,.5,"Frasco o tacho hermético"),
            p("fideos","Fideos secos","kg","Base",2.5,3500,.5),
            p("polenta","Polenta","kg","Base",1.0,3600,.5,"Cocción rápida"),
            p("legsecas","Lentejas o porotos secos","kg","Base",1.0,3400,.5,"Cocción larga"),
            p("avena","Avena","kg","Base",1.0,3800,.5,"Se come sin cocción"),
            p("harina","Harina","kg","Base",1.0,3600,1.0),
            p("leglata","Legumbres en lata","latas","Lista para comer",6.0,300,1.0,"Preferir abrefácil"),
            p("atun","Atún o caballa en lata","latas","Proteínas",8.0,200,1.0),
            p("carne","Carne o picadillo en lata","latas","Proteínas",4.0,350,1.0),
            p("leche","Leche en polvo","kg","Proteínas",2.0,4900,.5,"Rinde unos 8 L por kg"),
            p("mani","Mantequilla de maní","frascos 350 g","Proteínas",1.0,2100,1.0),
            p("aceite","Aceite","litros","Grasas",1.0,8800,.5,"Lejos de la luz"),
            p("frutos","Frutos secos","kg","Grasas",.5,6000,.25,"Rotar seguido"),
            p("galle","Galletitas","paquetes 300 g","Lista para comer",6.0,1350,1.0),
            p("barras","Barras de cereal","unidades","Lista para comer",15.0,100,1.0),
            p("fruta","Fruta en almíbar","latas","Lista para comer",4.0,300,1.0),
            p("tomate","Tomate triturado","cajas 520 g","Vegetales",4.0,120,1.0),
            p("choclo","Choclo o arvejas en lata","latas","Vegetales",6.0,200,1.0),
            p("azucar","Azúcar","kg","Despensa",1.0,4000,1.0),
            p("dulce","Dulce de leche o mermelada","frascos 400 g","Despensa",1.0,1200,1.0),
            p("sal","Sal","kg","Despensa",.5,0,.5),
            p("caldo","Caldo en cubos","cajas","Despensa",1.0,0,1.0),
            p("yerba","Yerba","kg","Despensa",1.0,0,.5),
            p("cafe","Café o té","paquetes","Despensa",1.0,0,1.0)
        )

        val BASE_TASKS = listOf(
            Triple("s1","Cargar powerbank y celulares",TaskPeriod.WEEKLY),
            Triple("s2","Tanque de nafta del auto por encima de la mitad",TaskPeriod.WEEKLY),
            Triple("s3","Revisar pronóstico extendido y alertas del SMN",TaskPeriod.WEEKLY),
            Triple("m1","Probar linternas, radio y anafe",TaskPeriod.MONTHLY),
            Triple("m2","Controlar las pilas",TaskPeriod.MONTHLY),
            Triple("m3","Reponer medicación habitual del botiquín",TaskPeriod.MONTHLY),
            Triple("m4","Revisar mochilas de evacuación",TaskPeriod.MONTHLY),
            Triple("t1","Rotar comida que vence pronto y reponer",TaskPeriod.QUARTERLY),
            Triple("t2","Controlar vencimientos del botiquín",TaskPeriod.QUARTERLY),
            Triple("t3","Revisar bidones: fisuras, olor o algas",TaskPeriod.QUARTERLY),
            Triple("t4","Práctica familiar del plan de encuentro",TaskPeriod.QUARTERLY),
            Triple("h1","Renovar agua de red y limpiar bidones",TaskPeriod.SEMIANNUAL),
            Triple("h2","Actualizar ropa de las mochilas",TaskPeriod.SEMIANNUAL),
            Triple("h3","Actualizar teléfonos y copias de documentos",TaskPeriod.SEMIANNUAL),
            Triple("h4","Renovar el efectivo de reserva",TaskPeriod.SEMIANNUAL),
            Triple("a1","Revisar y renovar pilas del stock",TaskPeriod.ANNUAL),
            Triple("a2","Revisar cartuchos de gas",TaskPeriod.ANNUAL),
            Triple("a3","Revisar el plan completo",TaskPeriod.ANNUAL),
            Triple("a4","Revisar mantas térmicas y bolsas herméticas",TaskPeriod.ANNUAL)
        )
    }
}
