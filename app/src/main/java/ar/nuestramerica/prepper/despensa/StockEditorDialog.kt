package ar.nuestramerica.prepper.despensa

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ar.nuestramerica.prepper.DateKind
import ar.nuestramerica.prepper.Product
import ar.nuestramerica.prepper.StockLot
import ar.nuestramerica.prepper.defaultDateKind
import ar.nuestramerica.prepper.defaultLotDate
import ar.nuestramerica.prepper.ui.components.NumberField
import ar.nuestramerica.prepper.ui.format.dateFormat
import ar.nuestramerica.prepper.ui.format.parseDateOrNull
import ar.nuestramerica.prepper.ui.format.toMilli
import java.time.LocalDate

@Composable
internal fun StockEditorDialog(
    product: Product,
    lot: StockLot? = null,
    onDismiss: () -> Unit,
    save: (Long, LocalDate?, DateKind, String) -> Unit,
) {
    val isWater = product.id == "agua"
    var amount by rememberSaveable(product.id, lot?.id) {
        val quantity = lot?.quantityMilli ?: product.stepMilli
        mutableStateOf(java.math.BigDecimal.valueOf(quantity, 3).stripTrailingZeros().toPlainString().replace('.', ','))
    }
    var date by rememberSaveable(product.id, lot?.id) {
        val initial = if (lot == null) product.defaultLotDate(LocalDate.now()) else lot.date
        mutableStateOf(initial?.format(dateFormat).orEmpty())
    }
    var location by rememberSaveable(product.id, lot?.id) { mutableStateOf(lot?.location.orEmpty()) }
    var kind by rememberSaveable(product.id, lot?.id) { mutableStateOf(lot?.dateKind ?: product.defaultDateKind()) }
    val amountMilli = amount.toMilli()
    val parsedDate = date.parseDateOrNull()
    val validDate = (date.isBlank() || parsedDate != null)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (lot == null) "Agregar ${product.name}" else "Editar ${product.name} · ${lot.code}") },
        text = {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (lot == null) Text(
                    if (isWater) {
                        "Cargá los litros almacenados. La fecha de rotación se propone a 3 meses."
                    } else {
                        "Cada carga queda separada para poder consumir primero lo que vence antes."
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
                if (lot == null) Text(
                    "Al guardar te daremos un código corto para marcar e identificar este envase.",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                )
                NumberField("Cantidad (${product.unit})", amount) { amount = it }
                ExpirationDateField(
                    value = date,
                    label = if (isWater) "Próxima rotación" else "Fecha del envase (opcional)",
                    guidance = if (isWater) {
                        "El material indica rotar o volver a tratar el agua entre 3 y 6 meses."
                    } else {
                        "Escribí DD/MM/AA o elegí una fecha."
                    },
                    onValueChange = { date = it },
                )
                if (!isWater && date.isNotBlank()) {
                    Text("La etiqueta dice", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(DateKind.EXPIRATION, DateKind.BEST_BEFORE).forEach { dateKind ->
                            FilterChip(
                                selected = kind == dateKind,
                                onClick = { kind = dateKind },
                                label = { Text(dateKind.label) },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Ubicación (opcional)") },
                    placeholder = { Text(if (isWater) "Ej.: lavadero" else "Ej.: alacena") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    save(amountMilli, parsedDate, kind, location)
                    onDismiss()
                },
                enabled = amountMilli > 0 && validDate,
            ) {
                Text(if (lot == null) "Agregar" else "Guardar cambios")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
