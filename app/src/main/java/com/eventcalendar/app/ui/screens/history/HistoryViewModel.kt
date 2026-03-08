package com.eventcalendar.app.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventcalendar.app.data.local.entity.EventType
import com.eventcalendar.app.data.local.entity.EventRecord
import com.eventcalendar.app.data.repository.EventRepository
import com.eventcalendar.app.data.repository.EventWithType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class HistoryUiState(
    val events: List<EventWithType> = emptyList(),
    val filteredEvents: List<EventWithType> = emptyList(),
    val eventTypes: List<EventType> = emptyList(),
    val selectedEventTypeId: Long? = null,
    val isLoading: Boolean = false,
    val showEditEventDialog: Boolean = false,
    val editingEvent: EventWithType? = null
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: EventRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadAllEvents()
        loadEventTypes()
    }

    private fun loadAllEvents() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.getAllRecords().collect { events ->
                _uiState.value = _uiState.value.copy(
                    events = events,
                    filteredEvents = events,
                    isLoading = false
                )
            }
        }
    }

    private fun loadEventTypes() {
        viewModelScope.launch {
            repository.getAllEventTypes().collect { types ->
                _uiState.value = _uiState.value.copy(eventTypes = types)
            }
        }
    }

    fun onEventTypeSelected(eventTypeId: Long?) {
        _uiState.value = _uiState.value.copy(selectedEventTypeId = eventTypeId)
        if (eventTypeId == null) {
            _uiState.value = _uiState.value.copy(filteredEvents = _uiState.value.events)
        } else {
            viewModelScope.launch {
                repository.getRecordsByEventType(eventTypeId).collect { events ->
                    _uiState.value = _uiState.value.copy(filteredEvents = events)
                }
            }
        }
    }

    fun deleteEvent(event: EventWithType) {
        viewModelScope.launch {
            repository.deleteEventRecord(event.record)
        }
    }

    fun onEditEventClick(event: EventWithType) {
        _uiState.value = _uiState.value.copy(
            showEditEventDialog = true,
            editingEvent = event
        )
    }

    fun onDismissEditEventDialog() {
        _uiState.value = _uiState.value.copy(
            showEditEventDialog = false,
            editingEvent = null
        )
    }

    fun updateEventRecord(eventTypeId: Long, hour: Int, minute: Int, note: String) {
        val state = _uiState.value
        val editingEvent = state.editingEvent ?: return

        val calendar = Calendar.getInstance().apply {
            timeInMillis = editingEvent.record.timestamp
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        viewModelScope.launch {
            val updatedRecord = editingEvent.record.copy(
                eventTypeId = eventTypeId,
                timestamp = calendar.timeInMillis,
                note = note
            )
            repository.updateEventRecord(updatedRecord)
            _uiState.value = _uiState.value.copy(
                showEditEventDialog = false,
                editingEvent = null
            )
        }
    }
}
