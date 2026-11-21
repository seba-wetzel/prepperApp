package ar.nuestramerica.prepper.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ar.nuestramerica.prepper.ui.theme.Ink
import ar.nuestramerica.prepper.ui.theme.Paper

@Composable
internal fun SectionTitle(text: String) {
    Text(
        text.uppercase(),
        modifier = Modifier.background(Ink, RoundedCornerShape(2.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        color = Paper,
        style = MaterialTheme.typography.labelLarge,
    )
}
