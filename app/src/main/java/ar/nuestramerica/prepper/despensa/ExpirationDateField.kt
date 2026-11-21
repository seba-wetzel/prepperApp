package ar.nuestramerica.prepper.despensa

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.input.KeyboardType
import ar.nuestramerica.prepper.ui.format.dateFormat
import ar.nuestramerica.prepper.ui.format.parseDateOrNull
import ar.nuestramerica.prepper.ui.format.toArgentineDateInput
import java.time.LocalDate

@Composable
internal fun ExpirationDateField(
    value: String,
    label: String,
    guidance: String,
    onValueChange: (String) -> Unit,
) {
    val context = LocalContext.current
    val parsed = value.parseDateOrNull()
    val digits = value.filter(Char::isDigit)
    val complete = digits.length == 6 || digits.length == 8
    val invalid = complete && parsed == null
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.toArgentineDateInput()) },
        label = { Text(label) },
        placeholder = { Text("DD/MM/AAAA") },
        supportingText = {
            Text(if (invalid) "Revisá el día y el mes." else guidance)
        },
        isError = invalid,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        trailingIcon = {
            IconButton(
                onClick = {
                    val initial = parsed ?: LocalDate.now()
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            onValueChange(LocalDate.of(year, month + 1, day).format(dateFormat))
                        },
                        initial.year,
                        initial.monthValue - 1,
                        initial.dayOfMonth,
                    ).show()
                },
            ) {
                Icon(Icons.Filled.DateRange, contentDescription = "Elegir fecha en el calendario")
            }
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
    if (value.isNotBlank()) {
        TextButton(onClick = { onValueChange("") }) { Text("Quitar fecha") }
    }
}
