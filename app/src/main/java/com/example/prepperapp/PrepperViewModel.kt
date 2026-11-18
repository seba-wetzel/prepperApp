package com.example.prepperapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

internal class PrepperViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PrepperRepository.get(application)
    private val mutableState = MutableStateFlow(PrepperState())
    val state = mutableState.asStateFlow()

    private val mutableMessage = MutableStateFlow<String?>(null)
    val message = mutableMessage.asStateFlow()

    init { refresh() }

    fun refresh() = run { repository.load() }

    fun adjust(product: Product, deltaMilli: Long) = run {
        repository.adjustProduct(product, deltaMilli)
        repository.load()
    }

    fun addLot(productId: String, quantityMilli: Long, date: LocalDate?, kind: DateKind, location: String) =
        run("Lote guardado") {
            repository.addLot(productId, quantityMilli, date, kind, location)
            repository.load()
        }

    fun consumeLot(lotId: Long, quantityMilli: Long) = run("Stock actualizado") {
        repository.consumeLot(lotId, quantityMilli)
        repository.load()
    }

    fun deleteLot(id: Long) = run("Lote eliminado") {
        repository.deleteLot(id)
        repository.load()
    }

    fun addProduct(name: String, unit: String, category: String, weeklyMilli: Long, kcal: Int, stepMilli: Long) =
        run("Producto agregado") {
            repository.addProduct(name, unit, category, weeklyMilli, kcal, stepMilli)
            repository.load()
        }

    fun deleteCustomProduct(id: String) = run("Producto eliminado") {
        repository.deleteCustomProduct(id)
        repository.load()
    }

    fun completeTask(taskId: String) = run("Tarea registrada") {
        repository.completeTask(taskId)
        repository.load()
    }

    fun undoTask(taskId: String) = run("Registro deshecho") {
        repository.undoTask(taskId)
        repository.load()
    }

    fun addTask(title: String, period: TaskPeriod) = run("Tarea agregada") {
        repository.addTask(title, period)
        repository.load()
    }

    fun updateTask(id: String, title: String, period: TaskPeriod) = run("Tarea actualizada") {
        repository.updateTask(id, title, period)
        repository.load()
    }

    fun setTaskActive(id: String, active: Boolean) = run {
        repository.setTaskActive(id, active)
        repository.load()
    }

    fun finishOnboarding(settings: HouseholdSettings) = run("Configuración guardada") {
        repository.updateSettings(settings.copy(
            configured = true,
            taskScheduleStart = LocalDate.now(),
        ))
        repository.load()
    }

    fun updateSettings(settings: HouseholdSettings) = run {
        repository.updateSettings(settings)
        repository.load()
    }

    fun exportSnapshot(deliver: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val json = withContext(Dispatchers.IO) { repository.exportJson() }
                deliver(json)
            } catch (error: Exception) {
                mutableMessage.value = error.message ?: "No se pudo preparar la copia"
            }
        }
    }

    fun resetAll() = run("Datos restablecidos") {
        repository.resetAll()
        repository.load()
    }

    fun backupSaved() = run("Copia guardada") {
        repository.markBackup()
        repository.load()
    }

    fun restore(text: String) = run("Copia restaurada") {
        repository.importJson(text)
        repository.load()
    }

    fun notify(message: String) { mutableMessage.value = message }
    fun messageShown() { mutableMessage.value = null }

    private fun run(success: String? = null, operation: () -> PrepperState) {
        viewModelScope.launch {
            try {
                mutableState.value = withContext(Dispatchers.IO) { operation() }
                if (success != null) mutableMessage.value = success
            } catch (error: Exception) {
                mutableMessage.value = error.message ?: "No se pudo guardar el cambio"
            }
        }
    }
}
