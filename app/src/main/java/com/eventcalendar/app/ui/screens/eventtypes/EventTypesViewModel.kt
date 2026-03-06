package com.eventcalendar.app.ui.screens.eventtypes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventcalendar.app.data.local.entity.EventType
import com.eventcalendar.app.data.repository.EventRepository
import com.eventcalendar.app.ui.theme.EventColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EventTypesViewModel @Inject constructor(
    private val repository: EventRepository
) : ViewModel() {

    private val _eventTypes = MutableStateFlow<List<EventType>>(emptyList())
    val eventTypes: StateFlow<List<EventType>> = _eventTypes.asStateFlow()

    init {
        loadEventTypes()
    }

    private fun loadEventTypes() {
        viewModelScope.launch {
            repository.getAllEventTypes().collect { types ->
                _eventTypes.value = types
            }
        }
    }

    fun addEventType(name: String, colorIndex: Int) {
        viewModelScope.launch {
            val color = EventColors.getOrElse(colorIndex) { EventColors[0] }
            val colorLong = colorToLong(color)
            val eventType = EventType(
                name = name,
                color = colorLong
            )
            repository.insertEventType(eventType)
        }
    }

    fun updateEventType(originalEventType: EventType, name: String, colorIndex: Int) {
        viewModelScope.launch {
            val color = EventColors.getOrElse(colorIndex) { EventColors[0] }
            val colorLong = colorToLong(color)
            val eventType = originalEventType.copy(
                name = name,
                color = colorLong
            )
            repository.updateEventType(eventType)
        }
    }

    fun deleteEventType(eventType: EventType) {
        viewModelScope.launch {
            repository.deleteEventType(eventType)
        }
    }

    private fun colorToLong(color: androidx.compose.ui.graphics.Color): Long {
        val argb = color.value.toLong()
        return if (argb < 0) argb + 0x100000000L else argb
    }
}
