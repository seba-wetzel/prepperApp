package com.example.prepperapp

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
import androidx.core.content.IntentCompat
import androidx.core.content.edit
import com.example.prepperapp.ui.theme.PrepperAppTheme

class MainActivity : ComponentActivity() {
    private val model: ReviewViewModel by viewModels()
    private var taskLaunch by mutableStateOf(0)
    private var rationale by mutableStateOf(false)
    private var allowed by mutableStateOf(false)
    private var channelEnabled by mutableStateOf(false)
    private var appEnabled by mutableStateOf(false)
    private var unusedRestrictions by mutableStateOf("Consultando…")

    private val permissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        refreshSettings()
        if (granted) ReviewReminders.checkNow(this)
        else if (!shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
            model.showMessage("Podés habilitar los avisos en los ajustes de Android")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null && intent.action == ReviewReminders.OPEN_TASKS) taskLaunch++
        ReviewReminders.registerChannel(this)
        ReviewReminders.schedule(this)
        setContent {
            PrepperAppTheme {
                PrepperScreen(
                    model = model,
                    taskLaunch = taskLaunch,
                    permission = allowed,
                    channel = channelEnabled,
                    appNotifications = appEnabled,
                    restriction = unusedRestrictions,
                    requestPermission = ::requestNotices,
                    openNotifications = ::openNotificationSettings,
                    openRestrictions = ::openRestrictionSettings,
                    testNotice = ::testNotice
                )
                if (rationale) {
                    AlertDialog(
                        onDismissRequest = { rationale = false },
                        title = { Text("Avisos de revisión") },
                        text = { Text("Activá las notificaciones para recibir avisos sobre la próxima revisión de bidones.") },
                        confirmButton = {
                            TextButton(onClick = {
                                rationale = false
                                launchPermission()
                            }) { Text("Continuar") }
                        },
                        dismissButton = {
                            TextButton(onClick = { rationale = false }) { Text("Cancelar") }
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        model.refresh()
        refreshSettings()
        ReviewReminders.checkNow(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == ReviewReminders.OPEN_TASKS) taskLaunch++
    }

    private fun refreshSettings() {
        allowed = ReviewReminders.hasPermission(this)
        channelEnabled = ReviewReminders.channelEnabled(this)
        appEnabled = ReviewReminders.appEnabled(this)
        unusedRestrictions = if (packageManager.isAutoRevokeWhitelisted) "Desactivadas" else "Activadas"
    }

    private fun requestNotices() {
        when {
            ReviewReminders.hasPermission(this) -> openNotificationSettings()
            shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> rationale = true
            getPreferences(MODE_PRIVATE).getBoolean("notices_requested", false) -> openAppSettings()
            else -> launchPermission()
        }
    }

    private fun launchPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            getPreferences(MODE_PRIVATE).edit { putBoolean("notices_requested", true) }
            permissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun testNotice() {
        if (ReviewReminders.send(this, "Se pueden mostrar avisos en este dispositivo.", test = true)) {
            model.showMessage("Aviso de prueba enviado")
        } else {
            refreshSettings()
            model.showMessage("Revisá el permiso y el canal de avisos en Android")
        }
    }

    private fun openNotificationSettings() = openSettings(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    )

    private fun openRestrictionSettings() {
        try {
            openSettings(IntentCompat.createManageUnusedAppRestrictionsIntent(this, packageName))
        } catch (_: UnsupportedOperationException) {
            openAppSettings()
        }
    }

    private fun openSettings(intent: Intent) {
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            openAppSettings()
        }
    }

    private fun openAppSettings() {
        try {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
        } catch (_: ActivityNotFoundException) {
            model.showMessage("Abrí la información de Prepper App desde los ajustes de Android")
        }
    }
}
