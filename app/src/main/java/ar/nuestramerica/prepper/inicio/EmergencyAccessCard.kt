package ar.nuestramerica.prepper.inicio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ar.nuestramerica.prepper.ui.theme.Ink
import ar.nuestramerica.prepper.ui.theme.Paper

@Composable
internal fun EmergencyAccessCard(open: () -> Unit) {
    OutlinedCard(
        onClick = open,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.primary,
        ),
        border = androidx.compose.foundation.BorderStroke(2.dp, Ink),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier.size(46.dp).background(Ink, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Phone, contentDescription = null, tint = Paper)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "EMERGENCIAS",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    "Números importantes sin conexión",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Text("ABRIR", color = Color.White, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
internal fun EmergencyNumbersDialog(dismiss: () -> Unit, dial: (String) -> Unit) {
    val numbers = listOf(
        Triple("911", "Emergencias generales", "Policía y asistencia inmediata"),
        Triple("100", "Bomberos", "Incendios y rescates"),
        Triple("103", "Defensa Civil", "Evacuación, inundaciones y temporales"),
        Triple("107", "Emergencias médicas", "SAME y servicios locales"),
    )
    AlertDialog(
        onDismissRequest = dismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("NÚMEROS DE EMERGENCIA", style = MaterialTheme.typography.labelLarge)
                Text("Elegí a quién llamar", style = MaterialTheme.typography.headlineSmall)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                numbers.forEach { (number, name, detail) ->
                    OutlinedButton(
                        onClick = {
                            dismiss()
                            dial(number)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(4.dp),
                    ) {
                        Icon(Icons.Filled.Phone, contentDescription = null)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                            Text("$number · $name", fontWeight = FontWeight.Bold)
                            Text(detail, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Text(
                    "La disponibilidad puede variar según la localidad.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = dismiss) { Text("Cerrar") }
        },
    )
}
