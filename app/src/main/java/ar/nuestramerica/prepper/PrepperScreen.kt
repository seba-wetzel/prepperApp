package ar.nuestramerica.prepper

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import ar.nuestramerica.prepper.ui.theme.EmergencyRedDark
import ar.nuestramerica.prepper.ui.theme.FieldGreen
import ar.nuestramerica.prepper.ui.theme.Ink
import ar.nuestramerica.prepper.ui.theme.Paper
import ar.nuestramerica.prepper.ui.theme.SignalAmber
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private enum class Section(val label: String, val icon: ImageVector) {
    HOME("Inicio", Icons.Filled.Home),
    PANTRY("Despensa", Icons.AutoMirrored.Filled.List),
    TASKS("Tareas", Icons.Filled.CheckCircle),
    SETTINGS("Ajustes", Icons.Filled.Settings)
}

private enum class PantryFilter(val label: String) {
    ALL("Todo"), SHOPPING("Para comprar"), EXPIRING("Vence pronto")
}

private enum class PantryAction {
    LOTS,
    ADD,
    CONSUME,
}

private data class PantrySelection(val product: Product, val action: PantryAction)

private val dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")
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
private val Ok = FieldGreen
private val Warn = SignalAmber
private val Bad = EmergencyRedDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PrepperApp(
    model: PrepperViewModel,
    reviewKey: String?,
    selectReview: (String?) -> Unit,
    notificationsAllowed: Boolean,
    notificationsEnabled: Boolean,
    requestNotifications: () -> Unit,
    openNotificationSettings: () -> Unit,
    sendTestNotification: () -> Unit,
    shareShoppingList: () -> Unit,
    exportBackup: () -> Unit,
    importBackup: () -> Unit,
    dialEmergency: (String) -> Unit,
) {
    val state by model.state.collectAsState()
    val message by model.message.collectAsState()
    var section by rememberSaveable { mutableStateOf(Section.HOME) }
    var pantryFilter by rememberSaveable { mutableStateOf(PantryFilter.ALL) }
    val snackbar = remember { SnackbarHostState() }

    if (reviewKey != null && !state.loading && state.settings.configured) {
        val alerts = remember(state) { state.reviewAlerts() }
        val alert = alerts.firstOrNull { it.key == reviewKey }
        val dismiss = { selectReview(null) }
        if (alert?.lot != null && alert.product != null) {
            androidx.compose.runtime.key(reviewKey) {
                ConsumeStockDialog(state, alert.product, dismiss, model::consumeLot, alert.lot.id)
            }
        } else {
            AlertDialog(
                onDismissRequest = dismiss,
                title = { Text(alert?.title ?: if (reviewKey == ReviewReminders.SUMMARY) "Avisos pendientes" else "Aviso resuelto") },
                text = {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        when {
                            alert != null -> Text(alert.detail)
                            reviewKey == ReviewReminders.SUMMARY -> {
                                if (alerts.isEmpty()) Text("No hay avisos pendientes.")
                                alerts.forEach { item ->
                                    TextButton(onClick = { selectReview(item.key) }) {
                                        Text("${item.title}\n${item.detail}")
                                    }
                                }
                            }
                            reviewKey.startsWith("lot/") -> Text("Este lote ya no tiene una revisión pendiente.")
                            else -> Text("Esta tarea ya no está pendiente para esa fecha.")
                        }
                    }
                },
                confirmButton = {
                    alert?.task?.let { task ->
                        TextButton(onClick = { dismiss(); model.completeTask(task.id) }) { Text("Marcar como hecha") }
                    } ?: TextButton(onClick = dismiss) { Text("Cerrar") }
                },
                dismissButton = {
                    if (alert?.task != null) TextButton(onClick = dismiss) { Text("Cerrar") }
                },
            )
        }
    }
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            model.messageShown()
        }
    }

    if (!state.loading && !state.settings.configured) {
        OnboardingScreen(state.settings) { settings, enableNotifications ->
            model.finishOnboarding(settings)
            if (enableNotifications) requestNotifications()
        }
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            "PREPARACIÓN DEL HOGAR",
                            modifier = Modifier.background(Ink, RoundedCornerShape(2.dp))
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                            color = Paper,
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Text("Mi kit de emergencia", style = MaterialTheme.typography.titleLarge)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.navigationBarsPadding(),
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                Section.entries.forEach { item ->
                    NavigationBarItem(
                        selected = section == item,
                        onClick = { section = item },
                        icon = {
                            Box {
                                Icon(item.icon, contentDescription = item.label)
                                val badge = when (item) {
                                    Section.PANTRY -> state.shoppingCount
                                    Section.TASKS -> state.pendingTasks.size
                                    else -> 0
                                }
                                if (badge > 0) {
                                    Text(
                                        badge.coerceAtMost(99).toString(),
                                        modifier = Modifier.align(Alignment.TopEnd)
                                            .background(Bad, RoundedCornerShape(50)).padding(horizontal = 4.dp),
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        },
                        label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (section) {
                        Section.HOME -> HomeScreen(
                            state,
                            goShopping = {
                                pantryFilter = PantryFilter.SHOPPING
                                section = Section.PANTRY
                            },
                            goTasks = { section = Section.TASKS },
                            dialEmergency = dialEmergency,
                        )
                        Section.PANTRY -> PantryScreen(
                            state, model, pantryFilter,
                            changeFilter = { pantryFilter = it },
                            shareShoppingList = shareShoppingList
                        )
                        Section.TASKS -> TasksScreen(state, model)
                        Section.SETTINGS -> SettingsScreen(
                            state, model, notificationsAllowed, notificationsEnabled,
                            requestNotifications, openNotificationSettings, sendTestNotification,
                            exportBackup, importBackup
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun OnboardingScreen(
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

@Composable
private fun HomeScreen(
    state: PrepperState,
    goShopping: () -> Unit,
    goTasks: () -> Unit,
    dialEmergency: (String) -> Unit,
) {
    var emergencyVisible by rememberSaveable { mutableStateOf(false) }

    EmergencyAccessCard { emergencyVisible = true }
    SectionTitle("Autonomía del hogar")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        GaugeCard("Comida", state.foodDays, state.targetDays, Modifier.weight(1f))
        GaugeCard("Agua", state.waterDays, state.targetDays, Modifier.weight(1f))
    }
    SummaryCard(
        title = "Para comprar",
        subtitle = if (state.shoppingCount == 0) "Todo el stock está completo" else "Productos por debajo de la meta",
        count = state.shoppingCount,
        onClick = goShopping
    )
    SummaryCard(
        title = "Tareas pendientes",
        subtitle = if (state.pendingTasks.isEmpty()) "Todo al día" else "Mantenimiento que toca ahora",
        count = state.pendingTasks.size,
        onClick = goTasks
    )
    SectionTitle("Vencen pronto")
    KitCard {
        if (state.expiringLots.isEmpty()) {
            Text("Nada vence en los próximos 60 días. Cargá lotes con fecha para seguirlos acá.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            state.expiringLots.take(6).forEachIndexed { index, (product, lot) ->
                if (index > 0) HorizontalDivider()
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(product.name, fontWeight = FontWeight.SemiBold)
                        Text("${lot.quantityMilli.asQuantity()} ${product.unit}",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    ExpiryChip(lot, state.today)
                }
            }
        }
    }
    val backup = state.settings.lastBackup
    Text(
        if (backup == null) "Todavía no guardaste una copia de seguridad. Podés hacerlo desde Ajustes."
        else "Última copia confirmada: ${backup.atZone(java.time.ZoneId.systemDefault()).toLocalDate().format(dateFormat)}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    if (emergencyVisible) {
        EmergencyNumbersDialog(
            dismiss = { emergencyVisible = false },
            dial = dialEmergency,
        )
    }
}

@Composable
private fun EmergencyAccessCard(open: () -> Unit) {
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
private fun EmergencyNumbersDialog(dismiss: () -> Unit, dial: (String) -> Unit) {
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

@Composable
private fun GaugeCard(label: String, days: Double, target: Int, modifier: Modifier = Modifier) {
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

@Composable
private fun SummaryCard(title: String, subtitle: String, count: Int, onClick: () -> Unit) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(count.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            Text("  ›", style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun PantryScreen(
    state: PrepperState,
    model: PrepperViewModel,
    filter: PantryFilter,
    changeFilter: (PantryFilter) -> Unit,
    shareShoppingList: () -> Unit
) {
    var selection by remember { mutableStateOf<PantrySelection?>(null) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        PantryFilter.entries.forEach { item ->
            FilterChip(
                selected = filter == item,
                onClick = { changeFilter(item) },
                label = { Text(item.label) },
                modifier = Modifier.weight(1f)
            )
        }
    }

    val shown = state.visibleProducts.filter { product ->
        when (filter) {
            PantryFilter.ALL -> true
            PantryFilter.SHOPPING -> state.missingMilli(product) > 0
            PantryFilter.EXPIRING -> state.productLots(product.id).any {
                it.date?.let { date -> ChronoUnit.DAYS.between(state.today, date) <= 60 } == true
            }
        }
    }
    if (shown.isEmpty()) {
        val message = when (filter) {
            PantryFilter.ALL -> "No hay productos visibles. Podés elegirlos desde Ajustes."
            PantryFilter.SHOPPING -> "No falta nada: el stock cubre la meta."
            PantryFilter.EXPIRING -> "Nada vence en los próximos 60 días."
        }
        KitCard { Text(message) }
    }
    shown.groupBy { it.category }.forEach { (category, products) ->
        SectionTitle(category)
        products.forEach { product ->
            ProductCard(state, product) { action -> selection = PantrySelection(product, action) }
        }
    }
    OutlinedButton(onClick = shareShoppingList, modifier = Modifier.fillMaxWidth()) {
        Text("Compartir lista de compras")
    }
    selection?.let { selected ->
        ProductLotsDialog(
            state = state,
            product = selected.product,
            initialAction = selected.action,
            onDismiss = { selection = null },
            addLot = model::addLot,
            consume = model::consumeLot,
            deleteLot = model::deleteLot,
            deleteProduct = model::deleteCustomProduct,
        )
    }
}

@Composable
private fun ProductCard(
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

@Composable
private fun ProductLotsDialog(
    state: PrepperState,
    product: Product,
    initialAction: PantryAction,
    onDismiss: () -> Unit,
    addLot: (String, Long, LocalDate?, DateKind, String) -> Unit,
    consume: (Long, Long) -> Unit,
    deleteLot: (Long) -> Unit,
    deleteProduct: (String) -> Unit,
) {
    when (initialAction) {
        PantryAction.ADD -> AddStockDialog(product, onDismiss, addLot)
        PantryAction.CONSUME -> ConsumeStockDialog(state, product, onDismiss, consume)
        PantryAction.LOTS -> LotsDialog(state, product, onDismiss, deleteLot, deleteProduct)
    }
}

@Composable
private fun AddStockDialog(
    product: Product,
    onDismiss: () -> Unit,
    addLot: (String, Long, LocalDate?, DateKind, String) -> Unit,
) {
    val isWater = product.id == "agua"
    val defaultDate = remember(product.id) {
        product.defaultLotDate(LocalDate.now())?.format(dateFormat).orEmpty()
    }
    var amount by remember(product.id) { mutableStateOf(product.stepMilli.asQuantity()) }
    var date by remember(product.id) { mutableStateOf(defaultDate) }
    var location by remember(product.id) { mutableStateOf("") }
    var kind by remember(product.id) { mutableStateOf(product.defaultDateKind()) }
    val amountMilli = amount.toMilli()
    val parsedDate = date.parseDateOrNull()
    val validDate = (date.isBlank() || parsedDate != null)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar ${product.name}") },
        text = {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    if (isWater) {
                        "Cargá los litros almacenados. La fecha de rotación se propone a 3 meses."
                    } else {
                        "Cada carga queda separada para poder consumir primero lo que vence antes."
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "Al guardar te daremos un código corto para marcar e identificar este envase.",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                )
                NumberField("Cantidad (${product.unit})", amount) { amount = it }
                ExpirationDateField(
                    value = date,
                    label = if (isWater) "Próxima rotación" else "Fecha del envase (opcional)",
                    guidance = if (isWater) {
                        "El material indica rotar o volver a tratar el agua entre 3 y 6 meses."
                    } else {
                        "Escribí DD/MM/AA o elegí una fecha."
                    },
                    onValueChange = { date = it },
                )
                if (!isWater && date.isNotBlank()) {
                    Text("La etiqueta dice", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(DateKind.EXPIRATION, DateKind.BEST_BEFORE).forEach { dateKind ->
                            FilterChip(
                                selected = kind == dateKind,
                                onClick = { kind = dateKind },
                                label = { Text(dateKind.label) },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Ubicación (opcional)") },
                    placeholder = { Text(if (isWater) "Ej.: lavadero" else "Ej.: alacena") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    addLot(product.id, amountMilli, parsedDate, kind, location)
                    onDismiss()
                },
                enabled = amountMilli > 0 && validDate,
            ) {
                Text("Agregar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun ConsumeStockDialog(
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

@Composable
private fun LotsDialog(
    state: PrepperState,
    product: Product,
    onDismiss: () -> Unit,
    deleteLot: (Long) -> Unit,
    deleteProduct: (String) -> Unit,
) {
    var deleteId by remember { mutableStateOf<Long?>(null) }
    var confirmProductDelete by remember { mutableStateOf(false) }
    val lots = state.productLots(product.id)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Existencias de ${product.name}") },
        text = {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "Total: ${state.quantityMilli(product.id).asQuantity()} ${product.unit}",
                    fontWeight = FontWeight.Bold,
                )
                if (lots.isEmpty()) {
                    Text("Todavía no hay existencias cargadas.")
                }
                lots.forEachIndexed { index, lot ->
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                lot.code.ifBlank { "Existencia ${index + 1}" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = EmergencyRedDark,
                            )
                            Text(
                                "${lot.quantityMilli.asQuantity()} ${product.unit}",
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                lot.date?.let { "${lot.dateKind.label}: ${it.format(dateFormat)}" }
                                    ?: "Fecha no informada",
                            )
                            if (lot.location.isNotBlank()) Text("Ubicación: ${lot.location}")
                            TextButton(onClick = { deleteId = lot.id }) { Text("Eliminar") }
                        }
                    }
                }
                if (product.custom) {
                    TextButton(onClick = { confirmProductDelete = true }) {
                        Text("Quitar producto personalizado", color = Bad)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
    )
    if (confirmProductDelete) {
        AlertDialog(
            onDismissRequest = { confirmProductDelete = false },
            title = { Text("¿Quitar este producto?") },
            text = { Text("También se eliminarán todas sus existencias. Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    deleteProduct(product.id)
                    confirmProductDelete = false
                    onDismiss()
                }) { Text("Quitar", color = Bad) }
            },
            dismissButton = {
                TextButton(onClick = { confirmProductDelete = false }) { Text("Cancelar") }
            },
        )
    }
    deleteId?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteId = null },
            title = { Text("¿Eliminar esta existencia?") },
            text = { Text("La cantidad registrada se quitará de la despensa.") },
            confirmButton = {
                TextButton(onClick = { deleteLot(id); deleteId = null }) { Text("Eliminar", color = Bad) }
            },
            dismissButton = { TextButton(onClick = { deleteId = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun ExpirationDateField(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddProductDialog(
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

@Composable
private fun TasksScreen(state: PrepperState, model: PrepperViewModel) {
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<MaintenanceTask?>(null) }
    var undoTask by remember { mutableStateOf<MaintenanceTask?>(null) }
    TaskPeriod.entries.forEach { period ->
        val tasks = state.tasks.filter { it.period == period }
        if (tasks.isNotEmpty()) {
            val pendingCount = tasks.count {
                it.isPending(state.today, state.settings.taskScheduleStart)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom) {
                SectionTitle(period.label)
                Text("$pendingCount pendientes", style = MaterialTheme.typography.bodySmall)
            }
            tasks.forEach { task ->
                val pending = task.isPending(state.today, state.settings.taskScheduleStart)
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.Top) {
                        Checkbox(
                            checked = task.completions.isNotEmpty() && !pending,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    model.completeTask(task.id)
                                } else if (task.completions.isNotEmpty()) {
                                    undoTask = task
                                }
                            },
                            enabled = task.active
                        )
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(task.title, fontWeight = FontWeight.SemiBold)
                            val days = state.taskDaysUntil(task)
                            val label = when {
                                !task.active -> "Desactivada"
                                days < 0 -> "Atrasada ${-days} días"
                                days == 0L -> "Toca hoy"
                                task.completions.isEmpty() ->
                                    "Primera revisión: ${state.taskDue(task).format(dateFormat)}"
                                else -> "Próxima: ${state.taskDue(task).format(dateFormat)}"
                            }
                            StatusChip(label, when {
                                !task.active -> MaterialTheme.colorScheme.onSurfaceVariant
                                days <= 0 -> Bad
                                else -> Ok
                            })
                            task.completions.firstOrNull()?.let {
                                Text("Última realización: ${it.date.format(dateFormat)}",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                            TextButton(onClick = { editing = task }) { Text("Editar") }
                        }
                        Switch(task.active, { model.setTaskActive(task.id,it) },
                            modifier = Modifier.semantics { contentDescription = "Activar ${task.title}" })
                    }
                }
            }
        }
    }
    OutlinedButton(onClick = { adding = true }, modifier = Modifier.fillMaxWidth()) {
        Text("Agregar tarea")
    }
    if (adding) TaskDialog(
        dialogTitle = "Agregar tarea",
        initialTitle = "",
        initialPeriod = TaskPeriod.MONTHLY,
        onDismiss = { adding = false },
        save = { title, period -> model.addTask(title,period); adding = false }
    )
    editing?.let { task ->
        TaskDialog(
            dialogTitle = "Editar tarea",
            initialTitle = task.title,
            initialPeriod = task.period,
            onDismiss = { editing = null },
            save = { title, period -> model.updateTask(task.id,title,period); editing = null }
        )
    }
    undoTask?.let { task ->
        AlertDialog(
            onDismissRequest = { undoTask = null },
            title = { Text("¿Deshacer la última realización?") },
            text = { Text("Se quitará el registro más reciente y se recalculará la próxima fecha.") },
            confirmButton = {
                TextButton(onClick = { model.undoTask(task.id); undoTask = null }) { Text("Deshacer") }
            },
            dismissButton = { TextButton(onClick = { undoTask = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun TaskDialog(
    dialogTitle: String,
    initialTitle: String,
    initialPeriod: TaskPeriod,
    onDismiss: () -> Unit,
    save: (String, TaskPeriod) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var period by remember { mutableStateOf(initialPeriod) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialogTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title,{title=it},label={Text("Tarea")},modifier=Modifier.fillMaxWidth())
                TaskPeriod.entries.forEach {
                    FilterChip(period==it,{period=it},{Text(it.label)},modifier=Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            TextButton(onClick={save(title,period)},enabled=title.isNotBlank()){Text("Guardar")}
        },
        dismissButton = { TextButton(onClick=onDismiss){Text("Cancelar")} }
    )
}

@Composable
private fun SettingsScreen(
    state: PrepperState,
    model: PrepperViewModel,
    notificationsAllowed: Boolean,
    notificationsEnabled: Boolean,
    requestNotifications: () -> Unit,
    openNotificationSettings: () -> Unit,
    sendTestNotification: () -> Unit,
    exportBackup: () -> Unit,
    importBackup: () -> Unit
) {
    val settings = state.settings
    var confirmReset by remember { mutableStateOf(false) }
    var addingProduct by remember { mutableStateOf(false) }
    KitCard {
        SettingsStepper("Personas","Integrantes del hogar",settings.people,1,30) {
            model.updateSettings(settings.copy(people=it))
        }
        HorizontalDivider()
        SettingsStepper("Semanas","Autonomía objetivo",settings.weeks,1,52) {
            model.updateSettings(settings.copy(weeks=it))
        }
        HorizontalDivider()
        SettingsStepper("Kcal diarias","Promedio por persona",settings.dailyKcal,1000,5000,100) {
            model.updateSettings(settings.copy(dailyKcal=it))
        }
        HorizontalDivider()
        val water = (settings.dailyWaterLitresMilli / 1000).toInt()
        SettingsStepper("Agua diaria","Litros por persona",water,1,20) {
            model.updateSettings(settings.copy(dailyWaterLitresMilli=it*1000L))
        }
    }
    Text("Las metas de producto se ajustan según las personas y semanas. La autonomía es una estimación.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

    SectionTitle("Alimentos de la despensa")
    Text(
        "Marcá los productos que consume tu hogar. Los desmarcados conservan sus existencias, pero no aparecen en la despensa, las compras, la autonomía ni los avisos.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    KitCard {
        OutlinedButton(onClick = { addingProduct = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Agregar producto nuevo")
        }
        HorizontalDivider()
        state.products.forEachIndexed { index, product ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = product.visible,
                    onCheckedChange = { model.setProductVisible(product.id, it) },
                )
                Column(Modifier.weight(1f)) {
                    Text(product.name, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (product.custom) "Producto agregado por vos" else product.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (index < state.products.lastIndex) HorizontalDivider()
        }
    }

    SectionTitle("Avisos")
    KitCard {
        SettingStatus("Permiso de notificación", if(notificationsAllowed)"Concedido" else "Sin conceder")
        SettingStatus("Canal de avisos", if(notificationsEnabled)"Habilitado" else "Deshabilitado")
        SettingStatus("Tareas pendientes",state.pendingTasks.size.toString())
        if (!notificationsAllowed) Button(onClick=requestNotifications,modifier=Modifier.fillMaxWidth()){
            Text("Habilitar notificaciones")
        }
        OutlinedButton(onClick=openNotificationSettings,modifier=Modifier.fillMaxWidth()){
            Text("Ajustes de notificaciones")
        }
        OutlinedButton(onClick=sendTestNotification,enabled=notificationsAllowed&&notificationsEnabled,
            modifier=Modifier.fillMaxWidth()){Text("Enviar aviso de prueba")}
        Text("La comprobación diaria puede demorarse por ahorro de batería o hibernación.",
            style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }

    SectionTitle("Copia de seguridad")
    KitCard {
        Text("Exportá un archivo portable para recuperar productos, lotes, tareas e historial.")
        Button(onClick=exportBackup,modifier=Modifier.fillMaxWidth()){Text("Guardar copia de seguridad")}
        OutlinedButton(onClick=importBackup,modifier=Modifier.fillMaxWidth()){Text("Restaurar desde una copia")}
    }
    OutlinedButton(onClick={confirmReset=true},modifier=Modifier.fillMaxWidth()){
        Text("Borrar todos los datos", color=Bad)
    }
    if (addingProduct) {
        AddProductDialog(onDismiss = { addingProduct = false }) { name, unit, category, weekly, kcal, step ->
            model.addProduct(name, unit, category, weekly, kcal, step)
            addingProduct = false
        }
    }
    if(confirmReset){
        AlertDialog(
            onDismissRequest={confirmReset=false},
            title={Text("¿Borrar todos los datos?")},
            text={Text("Se perderán el stock, los lotes, los productos agregados y el historial de tareas.")},
            confirmButton={
                TextButton(onClick={model.resetAll();confirmReset=false}){Text("Borrar todo",color=Bad)}
            },
            dismissButton={TextButton(onClick={confirmReset=false}){Text("Cancelar")}}
        )
    }
}

@Composable
private fun SettingsStepper(
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

@Composable
private fun SettingStatus(label:String,value:String){
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
        Text(label)
        Text(value,fontWeight=FontWeight.SemiBold)
    }
}

@Composable
private fun NumberField(label:String,value:String,onValue:(String)->Unit){
    OutlinedTextField(
        value,onValue,label={Text(label)},modifier=Modifier.fillMaxWidth(),singleLine=true,
        keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal)
    )
}

@Composable
private fun ExpiryChip(lot: StockLot, today: LocalDate) {
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

@Composable
private fun StatusChip(text: String, color: Color, background: Color = color.copy(alpha = .12f)) {
    Text(
        text.uppercase(),
        modifier = Modifier
            .background(background, RoundedCornerShape(2.dp))
            .border(1.dp, color.copy(alpha = .55f), RoundedCornerShape(2.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        color = color,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text.uppercase(),
        modifier = Modifier.background(Ink, RoundedCornerShape(2.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        color = Paper,
        style = MaterialTheme.typography.labelLarge,
    )
}

@Composable
private fun KitCard(content:@Composable ColumnScope.()->Unit){
    OutlinedCard(
        modifier=Modifier.fillMaxWidth(),
        colors=CardDefaults.outlinedCardColors(containerColor=MaterialTheme.colorScheme.surface),
        shape=RoundedCornerShape(8.dp)
    ){
        Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp),
            content=content)
    }
}

private fun String.toMilli():Long =
    (replace(',','.').toDoubleOrNull()?.times(1000)?.toLong() ?: 0L).coerceAtLeast(0)

internal fun String.toArgentineDateInput(): String {
    val digits = filter(Char::isDigit).take(8)
    return buildString {
        append(digits.take(2))
        if (digits.length > 2) append('/').append(digits.substring(2, minOf(4, digits.length)))
        if (digits.length > 4) append('/').append(digits.substring(4))
    }
}

internal fun String.parseDateOrNull(): LocalDate? {
    if (isBlank()) return null
    val digits = filter(Char::isDigit)
    if (digits.length != 6 && digits.length != 8) return null
    val day = digits.substring(0, 2).toInt()
    val month = digits.substring(2, 4).toInt()
    val rawYear = digits.substring(4).toInt()
    val year = if (digits.length == 6) 2000 + rawYear else rawYear
    return runCatching { LocalDate.of(year, month, day) }.getOrNull()
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Inicio · teléfono",
    showBackground = true,
    widthDp = 412,
    heightDp = 850
)
@Composable
private fun HomePreview() {
    ar.nuestramerica.prepper.ui.theme.PrepperAppTheme(darkTheme = false) {
        val products = PrepperRepository.BASE_PRODUCTS
        val state = PrepperState(
            products = products,
            lots = listOf(
                StockLot(1, "agua", 48_000, null, DateKind.EXPIRATION, "Lavadero", true),
                StockLot(2, "arroz", 3_000, LocalDate.now().plusDays(20), DateKind.EXPIRATION, "Despensa", true),
                StockLot(3, "atun", 5_000, LocalDate.now().minusDays(2), DateKind.EXPIRATION, "Despensa", true)
            ),
            tasks = PrepperRepository.BASE_TASKS.map { (id, title, period) ->
                MaintenanceTask(id, title, period, true, emptyList())
            },
            today = LocalDate.now(),
            loading = false
        )
        Column(
            Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            HomeScreen(state, {}, {}, {})
        }
    }
}
