package ar.nuestramerica.prepper

import android.app.Application
import android.database.sqlite.SQLiteException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class ReviewViewModel(application: Application) : AndroidViewModel(application) {
    private val store = ReviewStore.get(application)
    val schedule = store.schedule

    private val feedback = MutableStateFlow<String?>(null)
    val message = feedback.asStateFlow()

    fun refresh() = useStore { store.refresh() }

    fun complete() = useStore {
        if (store.complete { ReviewReminders.clearReview(getApplication()) }) {
            feedback.value = "Revisión registrada"
        }
    }

    fun undo(id: Long) = useStore {
        if (store.undo(id) { ReviewReminders.clearReview(getApplication()) }) {
            ReviewReminders.checkNow(getApplication())
            feedback.value = "Registro deshecho"
        }
    }

    fun showMessage(message: String) { feedback.value = message }
    fun clearMessage() { feedback.value = null }

    private fun useStore(action: () -> Unit) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { action() }
            } catch (_: SQLiteException) {
                feedback.value = "No se pudo acceder a los datos locales"
            }
        }
    }
}
