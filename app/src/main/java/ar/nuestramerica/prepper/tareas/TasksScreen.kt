package ar.nuestramerica.prepper.tareas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ar.nuestramerica.prepper.MaintenanceTask
import ar.nuestramerica.prepper.PrepperState
import ar.nuestramerica.prepper.PrepperViewModel
import ar.nuestramerica.prepper.TaskPeriod
import ar.nuestramerica.prepper.ui.components.SectionTitle
import ar.nuestramerica.prepper.ui.components.StatusChip
import ar.nuestramerica.prepper.ui.format.dateFormat
import ar.nuestramerica.prepper.ui.theme.Bad
import ar.nuestramerica.prepper.ui.theme.Ok

@Composable
internal fun TasksScreen(state: PrepperState, model: PrepperViewModel) {
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<MaintenanceTask?>(null) }
    var undoTask by remember { mutableStateOf<MaintenanceTask?>(null) }
    TaskPeriod.entries.forEach { period ->
        val tasks = state.tasks.filter { it.period == period }
        if (tasks.isNotEmpty()) {
            val pendingCount = tasks.count {
                it.isPending(state.today, state.settings.taskScheduleStart)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom) {
                SectionTitle(period.label)
                Text("$pendingCount pendientes", style = MaterialTheme.typography.bodySmall)
            }
            tasks.forEach { task ->
                val pending = task.isPending(state.today, state.settings.taskScheduleStart)
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.Top) {
                        Checkbox(
                            checked = task.completions.isNotEmpty() && !pending,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    model.completeTask(task.id)
                                } else if (task.completions.isNotEmpty()) {
                                    undoTask = task
                                }
                            },
                            enabled = task.active
                        )
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(task.title, fontWeight = FontWeight.SemiBold)
                            val days = state.taskDaysUntil(task)
                            val label = when {
                                !task.active -> "Desactivada"
                                days < 0 -> "Atrasada ${-days} días"
                                days == 0L -> "Toca hoy"
                                task.completions.isEmpty() ->
                                    "Primera revisión: ${state.taskDue(task).format(dateFormat)}"
                                else -> "Próxima: ${state.taskDue(task).format(dateFormat)}"
                            }
                            StatusChip(label, when {
                                !task.active -> MaterialTheme.colorScheme.onSurfaceVariant
                                days <= 0 -> Bad
                                else -> Ok
                            })
                            task.completions.firstOrNull()?.let {
                                Text("Última realización: ${it.date.format(dateFormat)}",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                            TextButton(onClick = { editing = task }) { Text("Editar") }
                        }
                        Switch(task.active, { model.setTaskActive(task.id,it) },
                            modifier = Modifier.semantics { contentDescription = "Activar ${task.title}" })
                    }
                }
            }
        }
    }
    OutlinedButton(onClick = { adding = true }, modifier = Modifier.fillMaxWidth()) {
        Text("Agregar tarea")
    }
    if (adding) TaskDialog(
        dialogTitle = "Agregar tarea",
        initialTitle = "",
        initialPeriod = TaskPeriod.MONTHLY,
        onDismiss = { adding = false },
        save = { title, period -> model.addTask(title,period); adding = false }
    )
    editing?.let { task ->
        TaskDialog(
            dialogTitle = "Editar tarea",
            initialTitle = task.title,
            initialPeriod = task.period,
            onDismiss = { editing = null },
            save = { title, period -> model.updateTask(task.id,title,period); editing = null }
        )
    }
    undoTask?.let { task ->
        AlertDialog(
            onDismissRequest = { undoTask = null },
            title = { Text("¿Deshacer la última realización?") },
            text = { Text("Se quitará el registro más reciente y se recalculará la próxima fecha.") },
            confirmButton = {
                TextButton(onClick = { model.undoTask(task.id); undoTask = null }) { Text("Deshacer") }
            },
            dismissButton = { TextButton(onClick = { undoTask = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun TaskDialog(
    dialogTitle: String,
    initialTitle: String,
    initialPeriod: TaskPeriod,
    onDismiss: () -> Unit,
    save: (String, TaskPeriod) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var period by remember { mutableStateOf(initialPeriod) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialogTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title,{title=it},label={Text("Tarea")},modifier=Modifier.fillMaxWidth())
                TaskPeriod.entries.forEach {
                    FilterChip(period==it,{period=it},{Text(it.label)},modifier=Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            TextButton(onClick={save(title,period)},enabled=title.isNotBlank()){Text("Guardar")}
        },
        dismissButton = { TextButton(onClick=onDismiss){Text("Cancelar")} }
    )
}
