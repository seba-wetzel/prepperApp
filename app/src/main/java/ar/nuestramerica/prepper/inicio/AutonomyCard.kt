package ar.nuestramerica.prepper.inicio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ar.nuestramerica.prepper.asDays
import ar.nuestramerica.prepper.ui.theme.Bad
import ar.nuestramerica.prepper.ui.theme.Ok
import ar.nuestramerica.prepper.ui.theme.Warn

@Composable
internal fun AutonomyCard(label: String, days: Double, target: Int, modifier: Modifier = Modifier) {
    val ratio = if (target > 0) (days / target).coerceIn(0.0, 1.0).toFloat() else 0f
    val color = when {
        ratio >= 1f -> Ok
        ratio >= .4f -> Warn
        else -> Bad
    }
    OutlinedCard(
        modifier = modifier,
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.fillMaxWidth().height(5.dp).background(color, RoundedCornerShape(8.dp)))
            Text(label, style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(days.asDays(), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
                Text(" días", modifier = Modifier.padding(bottom = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LinearProgressIndicator(
                progress = { ratio },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = color,
                trackColor = MaterialTheme.colorScheme.outlineVariant
            )
            Text("Meta: $target días", style = MaterialTheme.typography.labelSmall)
        }
    }
}
