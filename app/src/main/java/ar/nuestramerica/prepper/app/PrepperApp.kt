package ar.nuestramerica.prepper.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import ar.nuestramerica.prepper.PrepperViewModel
import ar.nuestramerica.prepper.ajustes.SettingsScreen
import ar.nuestramerica.prepper.avisos.ReviewReminders
import ar.nuestramerica.prepper.avisos.reviewAlerts
import ar.nuestramerica.prepper.avisos.reviewKey
import ar.nuestramerica.prepper.despensa.ConsumeStockDialog
import ar.nuestramerica.prepper.despensa.PantryFilter
import ar.nuestramerica.prepper.despensa.PantryScreen
import ar.nuestramerica.prepper.inicio.HomeScreen
import ar.nuestramerica.prepper.onboarding.OnboardingScreen
import ar.nuestramerica.prepper.tareas.TasksScreen
import ar.nuestramerica.prepper.ui.theme.Bad
import ar.nuestramerica.prepper.ui.theme.Ink
import ar.nuestramerica.prepper.ui.theme.Paper

private enum class Section(val label: String, val icon: ImageVector) {
    HOME("Inicio", Icons.Filled.Home),
    PANTRY("Despensa", Icons.AutoMirrored.Filled.List),
    TASKS("Tareas", Icons.Filled.CheckCircle),
    SETTINGS("Ajustes", Icons.Filled.Settings)
}

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
                            openLot = { selectReview(it.reviewKey()) },
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
