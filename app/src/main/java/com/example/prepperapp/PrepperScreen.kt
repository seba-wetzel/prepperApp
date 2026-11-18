package com.example.prepperapp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class Section(val label: String) {
    HOME("Inicio"), PANTRY("Despensa"), TASKS("Tareas"), SETTINGS("Ajustes")
}

private val timeFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("es-AR"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PrepperScreen(
    model: ReviewViewModel,
    taskLaunch: Int,
    permission: Boolean,
    channel: Boolean,
    appNotifications: Boolean,
    restriction: String,
    requestPermission: () -> Unit,
    openNotifications: () -> Unit,
    openRestrictions: () -> Unit,
    testNotice: () -> Unit,
) {
    var selected by rememberSaveable { mutableStateOf(Section.HOME) }
    val schedule by model.schedule.collectAsState()
    val message by model.message.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(taskLaunch) {
        if (taskLaunch > 0) selected = Section.TASKS
    }
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            model.clearMessage()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Mi kit de emergencia") }) },
        bottomBar = {
            NavigationBar {
                Section.entries.forEach { section ->
                    NavigationBarItem(
                        selected = section == selected,
                        onClick = { selected = section },
                        icon = { SectionIcon(section) },
                        label = { Text(section.label) }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            key(selected) {
                Column(
                    Modifier.widthIn(max = 560.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (selected) {
                        Section.HOME -> {
                            Text("Datos guardados en este dispositivo", style = MaterialTheme.typography.bodyMedium)
                            KitCard {
                                Text("Tareas pendientes", style = MaterialTheme.typography.titleLarge)
                                Text(schedule?.let { if (it.pending) "1 pendiente" else "Todo al día" } ?: "Cargando…")
                                TextButton(onClick = { selected = Section.TASKS }) { Text("Ver tareas") }
                            }
                        }
                        Section.PANTRY -> KitCard {
                            Text("Despensa", style = MaterialTheme.typography.titleLarge)
                            Text("El inventario por lotes y las reservas de agua estarán disponibles en la próxima etapa.")
                        }
                        Section.TASKS -> ReviewPanel(schedule, model::complete, model::undo)
                        Section.SETTINGS -> KitCard {
                            Text("Avisos de revisión", style = MaterialTheme.typography.titleLarge)
                            Text("Permiso de notificación: ${if (permission) "Concedido" else "Sin conceder"}")
                            Text("Notificaciones de la app: ${if (appNotifications) "Habilitadas" else "Deshabilitadas"}")
                            Text("Canal de revisiones: ${if (channel) "Habilitado" else "Deshabilitado"}")
                            Text("Restricciones por desuso: $restriction")
                            Text("Anticipación: $REVIEW_NOTICE_DAYS días")
                            Text("Próximo evento: ${schedule?.due?.displayDate() ?: "—"}")
                            val check = schedule?.lastCheck?.atZone(ZoneId.systemDefault())?.format(timeFormat)
                            Text("Última comprobación: ${check ?: "Aún sin registro"}")
                            if (!permission) {
                                Button(onClick = requestPermission) { Text("Habilitar notificaciones") }
                            }
                            OutlinedButton(onClick = openNotifications) { Text("Ajustes de notificaciones") }
                            OutlinedButton(onClick = openRestrictions) { Text("Ajustes por desuso") }
                            Text(
                                "Android puede retrasar las comprobaciones o suspenderlas por desuso. El aviso de prueba comprueba la visualización inmediata.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Button(onClick = testNotice, enabled = permission && appNotifications && channel) {
                                Text("Enviar aviso de prueba")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewPanel(schedule: ReviewSchedule?, onComplete: () -> Unit, onUndo: (Long) -> Unit) {
    if (schedule == null) {
        Text("Cargando revisión…")
        return
    }
    var undoId by rememberSaveable { mutableStateOf<Long?>(null) }
    val latest = schedule.history.firstOrNull()
    Text("Trimestral · ${if (schedule.pending) "0" else "1"}/1 al día",
        style = MaterialTheme.typography.titleMedium)
    KitCard {
        Text(REVIEW_TITLE, style = MaterialTheme.typography.titleLarge)
        Text("Revisión de los recipientes de agua")
        val status = when {
            schedule.daysLeft < 0 -> "Atrasada ${-schedule.daysLeft} días"
            schedule.pending -> "Toca hoy"
            else -> "Al día"
        }
        Text(status, color = if (schedule.pending) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurface)
        Text("Próxima revisión: ${schedule.due.displayDate()}")
        Text("Última realización: ${latest?.date?.displayDate() ?: "Sin registro"}")
        Button(onClick = onComplete, enabled = latest?.date != schedule.today) { Text("Marcar como realizada") }
    }
    KitCard {
        Text("Historial", style = MaterialTheme.typography.titleMedium)
        if (latest == null) Text("Todavía no hay revisiones registradas.")
        schedule.history.forEach { completion ->
            Text("Revisión realizada · ${completion.date.displayDate()}")
        }
        if (latest != null) {
            TextButton(onClick = { undoId = latest.id }) { Text("Deshacer última realización") }
        }
    }
    undoId?.let { id ->
        AlertDialog(
            onDismissRequest = { undoId = null },
            title = { Text("¿Deshacer la última realización?") },
            text = { Text("Se quitará ese registro y se recalculará la próxima revisión.") },
            confirmButton = { TextButton(onClick = { onUndo(id); undoId = null }) { Text("Deshacer") } },
            dismissButton = { TextButton(onClick = { undoId = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun KitCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
private fun SectionIcon(section: Section) {
    val color = LocalContentColor.current
    Canvas(Modifier.size(24.dp)) {
        val unit = size.width / 24
        val stroke = 1.75.dp.toPx()
        fun point(x: Int, y: Int) = Offset(x * unit, y * unit)
        fun line(x: Int, y: Int, toX: Int, toY: Int) =
            drawLine(color, point(x, y), point(toX, toY), strokeWidth = stroke, cap = StrokeCap.Round)
        when (section) {
            Section.HOME -> {
                line(3, 11, 12, 4)
                line(12, 4, 21, 11)
                line(6, 10, 6, 20)
                line(6, 20, 18, 20)
                line(18, 20, 18, 10)
            }
            Section.PANTRY -> {
                drawRect(color, point(5, 4), Size(14 * unit, 16 * unit), style = Stroke(stroke))
                line(5, 10, 19, 10)
                line(5, 15, 19, 15)
            }
            Section.TASKS -> for (y in listOf(6, 12, 18)) {
                line(4, y, 6, y + 1)
                line(6, y + 1, 8, y - 2)
                line(11, y, 20, y)
            }
            Section.SETTINGS -> {
                drawCircle(color, 4 * unit, style = Stroke(stroke))
                line(12, 3, 12, 6)
                line(12, 18, 12, 21)
                line(3, 12, 6, 12)
                line(18, 12, 21, 12)
                line(6, 6, 8, 8)
                line(16, 16, 18, 18)
                line(6, 18, 8, 16)
                line(16, 8, 18, 6)
            }
        }
    }
}
