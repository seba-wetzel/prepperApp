package ar.nuestramerica.prepper.ajustes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ar.nuestramerica.prepper.ui.components.NumberField
import ar.nuestramerica.prepper.ui.format.toMilli

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddProductDialog(
    onDismiss: () -> Unit,
    save: (String, String, String, Long, Int, Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf(productUnits.first()) }
    var unitExpanded by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf("Agregados por vos") }
    var weekly by remember { mutableStateOf("1") }
    var kcal by remember { mutableStateOf("0") }
    var step by remember { mutableStateOf("1") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar producto") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name,{name=it},label={Text("Nombre")},modifier=Modifier.fillMaxWidth())
                ExposedDropdownMenuBox(
                    expanded = unitExpanded,
                    onExpandedChange = { unitExpanded = it },
                ) {
                    OutlinedTextField(
                        value = unit,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Unidad") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(unitExpanded) },
                        modifier = Modifier.fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(
                        expanded = unitExpanded,
                        onDismissRequest = { unitExpanded = false },
                    ) {
                        productUnits.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    unit = option
                                    unitExpanded = false
                                },
                            )
                        }
                    }
                }
                OutlinedTextField(category,{category=it},label={Text("Categoría")},modifier=Modifier.fillMaxWidth())
                NumberField("Por semana para 5 personas",weekly){weekly=it}
                NumberField("Kcal por unidad",kcal){kcal=it}
                NumberField("Sumar de a",step){step=it}
            }
        },
        confirmButton = {
            TextButton(
                onClick = { save(name,unit,category,weekly.toMilli(),kcal.toIntOrNull() ?: 0,step.toMilli()) },
                enabled = name.isNotBlank() && unit.isNotBlank() && weekly.toMilli() >= 0 && step.toMilli() > 0
            ) { Text("Agregar") }
        },
        dismissButton = { TextButton(onClick=onDismiss){Text("Cancelar")} }
    )
}

private val productUnits = listOf(
    "unidades",
    "kg",
    "litros",
    "latas",
    "paquetes",
    "frascos",
    "cajas",
    "botellas",
    "bidones",
)
