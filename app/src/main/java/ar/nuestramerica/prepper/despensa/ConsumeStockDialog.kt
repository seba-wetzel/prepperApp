package ar.nuestramerica.prepper.despensa

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
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
import ar.nuestramerica.prepper.Product
import ar.nuestramerica.prepper.asQuantity
import ar.nuestramerica.prepper.ui.components.NumberField
import ar.nuestramerica.prepper.ui.components.StatusChip
import ar.nuestramerica.prepper.ui.format.dateFormat
import ar.nuestramerica.prepper.ui.format.toMilli
import ar.nuestramerica.prepper.ui.theme.Bad
import ar.nuestramerica.prepper.ui.theme.EmergencyRedDark
import ar.nuestramerica.prepper.ui.theme.Ok

@Composable
internal fun ConsumeStockDialog(
    state: PrepperState,
    product: Product,
    onDismiss: () -> Unit,
    consume: (Long, Long) -> Unit,
    focusLotId: Long? = null,
) {
    val lots = state.productLots(product.id).filter { focusLotId == null || it.id == focusLotId }
    val suggested = if (focusLotId == null) state.suggestedLot(product.id) else lots.firstOrNull()
    var selectedId by remember(product.id, suggested?.id) { mutableStateOf(suggested?.id) }
    var amount by remember(product.id, suggested?.id) {
        mutableStateOf(minOf(product.stepMilli, suggested?.quantityMilli ?: 0).asQuantity())
    }
    val selectedLot = lots.firstOrNull { it.id == selectedId }
    val amountMilli = amount.toMilli()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Consumir ${product.name}") },
        text = {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    if (focusLotId == null) "Elegí de cuál existencia descontar. Sugerimos la que vence o se rota antes." else "Lote indicado en el aviso.",
                    style = MaterialTheme.typography.bodySmall,
                )
                lots.forEach { lot ->
                    val isSuggested = lot.id == suggested?.id
                    OutlinedCard(
                        onClick = {
                            selectedId = lot.id
                            amount = minOf(product.stepMilli, lot.quantityMilli).asQuantity()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = selectedId == lot.id,
                                onClick = null,
                            )
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column {
                                        Text(
                                            lot.code,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = EmergencyRedDark,
                                        )
                                        Text(
                                            "${lot.quantityMilli.asQuantity()} ${product.unit}",
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                    if (isSuggested && focusLotId == null) StatusChip("Sugerida", Ok)
                                }
                                Text(
                                    lot.date?.let { "${lot.dateKind.label}: ${it.format(dateFormat)}" }
                                        ?: "Sin fecha informada",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                if (lot.location.isNotBlank()) {
                                    Text(
                                        "Ubicación: ${lot.location}",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }
                }
                NumberField("Cantidad a consumir (${product.unit})", amount) { amount = it }
                selectedLot?.let { lot ->
                    if (amountMilli > lot.quantityMilli) {
                        Text("La cantidad supera la existencia elegida.", color = Bad)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedLot?.let { consume(it.id, amountMilli) }
                    onDismiss()
                },
                enabled = selectedLot != null && amountMilli in 1..selectedLot.quantityMilli,
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
