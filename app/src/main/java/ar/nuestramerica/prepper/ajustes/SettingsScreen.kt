package ar.nuestramerica.prepper.ajustes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ar.nuestramerica.prepper.PrepperState
import ar.nuestramerica.prepper.PrepperViewModel
import ar.nuestramerica.prepper.ui.components.KitCard
import ar.nuestramerica.prepper.ui.components.SectionTitle
import ar.nuestramerica.prepper.ui.components.SettingsStepper
import ar.nuestramerica.prepper.ui.theme.Bad

@Composable
internal fun SettingsScreen(
    state: PrepperState,
    model: PrepperViewModel,
    notificationsAllowed: Boolean,
    notificationsEnabled: Boolean,
    requestNotifications: () -> Unit,
    openNotificationSettings: () -> Unit,
    sendTestNotification: () -> Unit,
    exportBackup: () -> Unit,
    importBackup: () -> Unit
) {
    val settings = state.settings
    var confirmReset by remember { mutableStateOf(false) }
    var addingProduct by remember { mutableStateOf(false) }
    KitCard {
        SettingsStepper("Personas","Integrantes del hogar",settings.people,1,30) {
            model.updateSettings(settings.copy(people=it))
        }
        HorizontalDivider()
        SettingsStepper("Semanas","Autonomía objetivo",settings.weeks,1,52) {
            model.updateSettings(settings.copy(weeks=it))
        }
        HorizontalDivider()
        SettingsStepper("Kcal diarias","Promedio por persona",settings.dailyKcal,1000,5000,100) {
            model.updateSettings(settings.copy(dailyKcal=it))
        }
        HorizontalDivider()
        val water = (settings.dailyWaterLitresMilli / 1000).toInt()
        SettingsStepper("Agua diaria","Litros por persona",water,1,20) {
            model.updateSettings(settings.copy(dailyWaterLitresMilli=it*1000L))
        }
    }
    Text("Las metas de producto se ajustan según las personas y semanas. La autonomía es una estimación.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

    SectionTitle("Alimentos de la despensa")
    Text(
        "Marcá los productos que consume tu hogar. Los desmarcados conservan sus existencias, pero no aparecen en la despensa, las compras, la autonomía ni los avisos.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    KitCard {
        OutlinedButton(onClick = { addingProduct = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Agregar producto nuevo")
        }
        HorizontalDivider()
        state.products.forEachIndexed { index, product ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = product.visible,
                    onCheckedChange = { model.setProductVisible(product.id, it) },
                )
                Column(Modifier.weight(1f)) {
                    Text(product.name, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (product.custom) "Producto agregado por vos" else product.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (index < state.products.lastIndex) HorizontalDivider()
        }
    }

    SectionTitle("Avisos")
    KitCard {
        SettingStatus("Permiso de notificación", if(notificationsAllowed)"Concedido" else "Sin conceder")
        SettingStatus("Canal de avisos", if(notificationsEnabled)"Habilitado" else "Deshabilitado")
        SettingStatus("Tareas pendientes",state.pendingTasks.size.toString())
        if (!notificationsAllowed) Button(onClick=requestNotifications,modifier=Modifier.fillMaxWidth()){
            Text("Habilitar notificaciones")
        }
        OutlinedButton(onClick=openNotificationSettings,modifier=Modifier.fillMaxWidth()){
            Text("Ajustes de notificaciones")
        }
        OutlinedButton(onClick=sendTestNotification,enabled=notificationsAllowed&&notificationsEnabled,
            modifier=Modifier.fillMaxWidth()){Text("Enviar aviso de prueba")}
        Text("La comprobación diaria puede demorarse por ahorro de batería o hibernación.",
            style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }

    SectionTitle("Copia de seguridad")
    KitCard {
        Text("Exportá un archivo portable para recuperar productos, lotes, tareas e historial.")
        Button(onClick=exportBackup,modifier=Modifier.fillMaxWidth()){Text("Guardar copia de seguridad")}
        OutlinedButton(onClick=importBackup,modifier=Modifier.fillMaxWidth()){Text("Restaurar desde una copia")}
    }
    OutlinedButton(onClick={confirmReset=true},modifier=Modifier.fillMaxWidth()){
        Text("Borrar todos los datos", color=Bad)
    }
    if (addingProduct) {
        AddProductDialog(onDismiss = { addingProduct = false }) { name, unit, category, weekly, kcal, step ->
            model.addProduct(name, unit, category, weekly, kcal, step)
            addingProduct = false
        }
    }
    if(confirmReset){
        AlertDialog(
            onDismissRequest={confirmReset=false},
            title={Text("¿Borrar todos los datos?")},
            text={Text("Se perderán el stock, los lotes, los productos agregados y el historial de tareas.")},
            confirmButton={
                TextButton(onClick={model.resetAll();confirmReset=false}){Text("Borrar todo",color=Bad)}
            },
            dismissButton={TextButton(onClick={confirmReset=false}){Text("Cancelar")}}
        )
    }
}

@Composable
private fun SettingStatus(label:String,value:String){
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
        Text(label)
        Text(value,fontWeight=FontWeight.SemiBold)
    }
}
