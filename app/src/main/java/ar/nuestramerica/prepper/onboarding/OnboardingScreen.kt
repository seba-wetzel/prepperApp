package ar.nuestramerica.prepper.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ar.nuestramerica.prepper.HouseholdSettings
import ar.nuestramerica.prepper.ui.components.KitCard
import ar.nuestramerica.prepper.ui.components.SettingsStepper
import ar.nuestramerica.prepper.ui.theme.Ink
import ar.nuestramerica.prepper.ui.theme.Paper

@Composable
internal fun OnboardingScreen(
    initial: HouseholdSettings,
    finish: (HouseholdSettings, Boolean) -> Unit,
) {
    var people by rememberSaveable { mutableStateOf(initial.people) }
    var weeks by rememberSaveable { mutableStateOf(initial.weeks) }
    var dailyKcal by rememberSaveable { mutableStateOf(initial.dailyKcal) }
    var waterLitres by rememberSaveable {
        mutableStateOf((initial.dailyWaterLitresMilli / 1000).toInt())
    }
    var enableNotifications by rememberSaveable { mutableStateOf(true) }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OnboardingHero()
            Text(
                "Definí la autonomía de tu hogar",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                "Estos datos calculan tus reservas. Podés cambiarlos después desde Ajustes.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            KitCard {
                SettingsStepper("Personas", "Integrantes del hogar", people, 1, 30) { people = it }
                HorizontalDivider()
                SettingsStepper("Semanas", "Autonomía objetivo", weeks, 1, 52) { weeks = it }
                HorizontalDivider()
                SettingsStepper("Kcal diarias", "Promedio por persona", dailyKcal, 1000, 5000, 100) {
                    dailyKcal = it
                }
                HorizontalDivider()
                SettingsStepper("Agua diaria", "Litros por persona", waterLitres, 1, 20) {
                    waterLitres = it
                }
            }
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(enableNotifications, { enableNotifications = it })
                    Column(Modifier.weight(1f)) {
                        Text("Avisos de mantenimiento", fontWeight = FontWeight.Bold)
                        Text(
                            "Las primeras tareas vencerán después de su período: una semana, un mes o más.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Button(
                onClick = {
                    finish(
                        initial.copy(
                            people = people,
                            weeks = weeks,
                            dailyKcal = dailyKcal,
                            dailyWaterLitresMilli = waterLitres * 1000L,
                        ),
                        enableNotifications,
                    )
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text("Guardar y comenzar")
            }
            Text(
                "La app funciona sin cuenta y guarda los datos localmente en este dispositivo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun OnboardingHero() {
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
            .border(2.dp, Ink, RoundedCornerShape(8.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Text(
            "PREPARACIONISMO COMUNITARIO",
            modifier = Modifier.background(Ink).padding(horizontal = 8.dp, vertical = 3.dp),
            color = Paper,
            style = MaterialTheme.typography.labelLarge,
        )
        Text(
            "PREPARÁ TU HOGAR",
            color = Color.White,
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(
            "Organizá tus reservas y mantené listo lo esencial para las primeras 72 horas.",
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
