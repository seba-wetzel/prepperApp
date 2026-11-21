package ar.nuestramerica.prepper.despensa

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ar.nuestramerica.prepper.PrepperState
import ar.nuestramerica.prepper.Product
import ar.nuestramerica.prepper.asQuantity
import ar.nuestramerica.prepper.ui.components.StatusChip
import ar.nuestramerica.prepper.ui.theme.Bad
import ar.nuestramerica.prepper.ui.theme.Ok
import ar.nuestramerica.prepper.ui.theme.Warn

internal enum class PantryAction {
    LOTS,
    ADD,
    CONSUME,
}

@Composable
internal fun ProductCard(
    state: PrepperState,
    product: Product,
    onAction: (PantryAction) -> Unit,
) {
    val quantity = state.quantityMilli(product.id)
    val goal = state.goalMilli(product)
    val missing = state.missingMilli(product)
    val lots = state.productLots(product.id)
    val color = when {
        missing == 0L -> Ok
        quantity > 0 -> Warn
        else -> Bad
    }
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Box(Modifier.width(5.dp).height(48.dp).background(color, RoundedCornerShape(5.dp)))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(product.name, fontWeight = FontWeight.Bold)
                    Text(
                        buildString {
                            append(product.unit)
                            if (product.note.isNotBlank()) append(" · ${product.note}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val scheme = MaterialTheme.colorScheme
                val chipColors = when {
                    missing == 0L -> scheme.secondaryContainer to scheme.onSecondaryContainer
                    quantity > 0 -> scheme.tertiaryContainer to scheme.onTertiaryContainer
                    else -> scheme.primaryContainer to scheme.onPrimaryContainer
                }
                StatusChip(
                    if (missing == 0L) "Completo" else "Faltan ${missing.asQuantity()}",
                    color = chipColors.second,
                    background = chipColors.first,
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("En despensa", style = MaterialTheme.typography.labelMedium)
                    Text(
                        "${quantity.asQuantity()} ${product.unit}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Objetivo", style = MaterialTheme.typography.labelMedium)
                    Text("${goal.asQuantity()} ${product.unit}", fontWeight = FontWeight.Bold)
                }
            }
            val nearest = lots.firstOrNull { it.date != null }
            if (nearest != null) {
                ExpiryChip(nearest, state.today)
            } else {
                Text(
                    if (lots.isEmpty()) "Todavía no cargaste existencias" else "Existencias sin fecha cargada",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onAction(PantryAction.ADD) }, modifier = Modifier.weight(1f)) {
                    Text("Agregar")
                }
                OutlinedButton(
                    onClick = { onAction(PantryAction.CONSUME) },
                    enabled = quantity > 0,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Consumir")
                }
            }
            TextButton(onClick = { onAction(PantryAction.LOTS) }, modifier = Modifier.fillMaxWidth()) {
                Text("Ver existencias (${lots.size})")
            }
        }
    }
}
