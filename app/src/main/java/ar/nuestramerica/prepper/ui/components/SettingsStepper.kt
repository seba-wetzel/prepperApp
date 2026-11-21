package ar.nuestramerica.prepper.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun SettingsStepper(
    title:String, subtitle:String, value:Int, min:Int, max:Int, step:Int=1, update:(Int)->Unit
) {
    Row(Modifier.fillMaxWidth().padding(vertical=4.dp),verticalAlignment=Alignment.CenterVertically){
        Column(Modifier.weight(1f)){
            Text(title,fontWeight=FontWeight.Bold)
            Text(subtitle,style=MaterialTheme.typography.bodySmall,
                color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedButton(onClick={update((value-step).coerceAtLeast(min))},
            enabled=value>min,contentPadding=androidx.compose.foundation.layout.PaddingValues(0.dp),
            modifier=Modifier.size(44.dp)){Text("−")}
        Text(value.toString(),modifier=Modifier.width(58.dp),
            textAlign=androidx.compose.ui.text.style.TextAlign.Center,fontWeight=FontWeight.Bold)
        OutlinedButton(onClick={update((value+step).coerceAtMost(max))},
            enabled=value<max,contentPadding=androidx.compose.foundation.layout.PaddingValues(0.dp),
            modifier=Modifier.size(44.dp)){Text("+")}
    }
}
