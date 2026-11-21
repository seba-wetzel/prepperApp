package ar.nuestramerica.prepper.despensa

import androidx.compose.runtime.Composable
import ar.nuestramerica.prepper.DateKind
import ar.nuestramerica.prepper.StockLot
import ar.nuestramerica.prepper.ui.components.StatusChip
import ar.nuestramerica.prepper.ui.format.dateFormat
import ar.nuestramerica.prepper.ui.theme.Bad
import ar.nuestramerica.prepper.ui.theme.Ok
import ar.nuestramerica.prepper.ui.theme.Warn
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
internal fun ExpiryChip(lot: StockLot, today: LocalDate) {
    val date = lot.date ?: return
    val days = ChronoUnit.DAYS.between(today, date)
    val text = if (lot.dateKind == DateKind.ROTATION) {
        when {
            days < 0 -> "Rotación vencida"
            days == 0L -> "Rotar hoy"
            days <= 60 -> "Rotar en $days d"
            else -> "Rotar ${date.format(dateFormat)}"
        }
    } else {
        when {
            days < 0 -> "Vencido"
            days == 0L -> "Vence hoy"
            days <= 60 -> "Vence en $days d"
            else -> date.format(dateFormat)
        }
    }
    StatusChip(text, when { days < 0 -> Bad; days <= 60 -> Warn; else -> Ok })
}
