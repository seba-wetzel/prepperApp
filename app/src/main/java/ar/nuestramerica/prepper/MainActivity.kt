package ar.nuestramerica.prepper

import ar.nuestramerica.prepper.app.PrepperApp
import ar.nuestramerica.prepper.avisos.ReviewReminders

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import ar.nuestramerica.prepper.ui.theme.PrepperAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    private val model: PrepperViewModel by viewModels()
    private var reviewKey by mutableStateOf<String?>(null)
    private var rationaleVisible by mutableStateOf(false)
    private var restoreText by mutableStateOf<String?>(null)
    private var backupText: String? = null

    private val permissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && model.state.value.settings.configured) ReviewReminders.checkNow(this)
        else model.notify("Podés habilitar los avisos desde los ajustes de Android")
    }

    private val createBackup = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val text = backupText
        backupText = null
        if (uri == null || text == null) return@registerForActivityResult
        lifecycleScope.launch {
            val saved = withContext(Dispatchers.IO) {
                runCatching {
                    contentResolver.openOutputStream(uri)?.bufferedWriter().use { writer ->
                        requireNotNull(writer).write(text)
                    }
                }.isSuccess
            }
            if (saved) model.backupSaved() else model.notify("No se pudo guardar la copia")
        }
    }

    private val openBackup = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch {
            restoreText = withContext(Dispatchers.IO) {
                runCatching { contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }.getOrNull()
            }
            if (restoreText == null) model.notify("No se pudo leer la copia")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        reviewKey = if (savedInstanceState != null) savedInstanceState.getString("reviewKey") else target(intent)
        lifecycleScope.launch {
            model.state
                .filter { !it.loading }
                .map { it.settings.configured }
                .distinctUntilChanged()
                .collect { configured ->
                    if (configured) {
                        ReviewReminders.registerChannel(this@MainActivity)
                        ReviewReminders.schedule(this@MainActivity)
                        ReviewReminders.checkNow(this@MainActivity)
                    } else {
                        ReviewReminders.stop(this@MainActivity)
                    }
                }
        }
        setContent {
            PrepperAppTheme {
                PrepperApp(
                    model = model,
                    reviewKey = reviewKey,
                    selectReview = { reviewKey = it },
                    notificationsAllowed = ReviewReminders.hasPermission(this),
                    notificationsEnabled = ReviewReminders.appEnabled(this) && ReviewReminders.channelEnabled(this),
                    requestNotifications = ::requestNotifications,
                    openNotificationSettings = ::openNotificationSettings,
                    sendTestNotification = ::sendTestNotification,
                    shareShoppingList = ::shareShoppingList,
                    exportBackup = ::exportBackup,
                    importBackup = { openBackup.launch(arrayOf("application/json", "text/plain")) },
                    dialEmergency = ::dialEmergency,
                )
                if (rationaleVisible) {
                    AlertDialog(
                        onDismissRequest = { rationaleVisible = false },
                        title = { Text("Avisos de revisión") },
                        text = { Text("El permiso permite mostrar tareas pendientes y vencimientos aunque la interfaz no esté abierta.") },
                        confirmButton = {
                            TextButton(onClick = {
                                rationaleVisible = false
                                launchNotificationPermission()
                            }) { Text("Continuar") }
                        },
                        dismissButton = {
                            TextButton(onClick = { rationaleVisible = false }) { Text("Ahora no") }
                        }
                    )
                }
                restoreText?.let { text ->
                    AlertDialog(
                        onDismissRequest = { restoreText = null },
                        title = { Text("¿Restaurar la copia?") },
                        text = { Text("Se reemplazarán los productos, lotes, tareas y ajustes guardados en este teléfono.") },
                        confirmButton = {
                            TextButton(onClick = {
                                restoreText = null
                                model.restore(text)
                            }) { Text("Restaurar") }
                        },
                        dismissButton = {
                            TextButton(onClick = { restoreText = null }) { Text("Cancelar") }
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        model.refresh()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        reviewKey = target(intent)
    }

    private fun target(intent: Intent): String? =
        if (intent.action == ReviewReminders.OPEN_TASKS) intent.data?.lastPathSegment ?: ReviewReminders.SUMMARY else null

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("reviewKey", reviewKey)
    }

    private fun requestNotifications() {
        when {
            ReviewReminders.hasPermission(this) -> Unit
            Build.VERSION.SDK_INT < 33 -> Unit
            shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> rationaleVisible = true
            getPreferences(MODE_PRIVATE).getBoolean("notices_requested", false) -> openAppSettings()
            else -> launchNotificationPermission()
        }
    }

    private fun launchNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            getPreferences(MODE_PRIVATE).edit { putBoolean("notices_requested", true) }
            permissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun sendTestNotification() {
        val sent = ReviewReminders.send(this, "Los avisos inmediatos están funcionando.", test = true)
        model.notify(if (sent) "Aviso de prueba enviado" else "Revisá el permiso y el canal de avisos")
    }

    private fun shareShoppingList() {
        val state = model.state.value
        val lines = state.visibleProducts.mapNotNull { product ->
            val missing = state.missingMilli(product)
            if (missing > 0) "• ${product.name}: ${missing.asQuantity()} ${product.unit}" else null
        }
        val text = if (lines.isEmpty()) "No falta nada." else
            "Lista de compras · kit de emergencia\n" + lines.joinToString("\n")
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }, "Compartir lista"))
    }

    private fun exportBackup() {
        model.exportSnapshot { json ->
            backupText = json
            createBackup.launch("prepper-copia-${LocalDate.now()}.json")
        }
    }

    private fun dialEmergency(number: String) {
        try {
            startActivity(Intent(Intent.ACTION_DIAL, "tel:$number".toUri()))
        } catch (_: ActivityNotFoundException) {
            model.notify("No se encontró una aplicación para realizar llamadas")
        }
    }

    private fun openNotificationSettings() {
        try {
            startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
        } catch (_: ActivityNotFoundException) {
            openAppSettings()
        }
    }

    private fun openAppSettings() {
        try {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null)))
        } catch (_: ActivityNotFoundException) {
            model.notify("Abrí la información de Prepper App desde Ajustes")
        }
    }
}
