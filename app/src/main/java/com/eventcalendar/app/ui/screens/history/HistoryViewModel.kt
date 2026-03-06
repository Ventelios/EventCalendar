package com.eventcalendar.app.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventcalendar.app.data.local.entity.EventType
import com.eventcalendar.app.data.repository.EventRepository
import com.eventcalendar.app.data.repository.EventWithType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val events: List<EventWithType> = emptyList(),
    val filteredEvents: List<EventWithType> = emptyList(),
    val eventTypes: List<EventType> = emptyList(),
    val selectedEventTypeId: Long? = null,
    val isLoading: Boolean = false
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
}
