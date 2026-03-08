package com.eventcalendar.app.ui.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventcalendar.app.data.local.entity.EventType
import com.eventcalendar.app.data.local.entity.EventRecord
import com.eventcalendar.app.data.repository.EventRepository
import com.eventcalendar.app.data.repository.EventWithType
import com.eventcalendar.app.ui.theme.EventColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class CalendarUiState(
    val currentYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val currentMonth: Int = Calendar.getInstance().get(Calendar.MONTH),
    val selectedDate: Int? = Calendar.getInstance().get(Calendar.DAY_OF_MONTH),
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH),
    val datesWithEvents: Set<Int> = emptySet(),
    val selectedDateEvents: List<EventWithType> = emptyList(),
    val eventTypes: List<EventType> = emptyList(),
    val showAddEventDialog: Boolean = false,
    val showAddEventTypeDialog: Boolean = false,
    val showMonthPickerDialog: Boolean = false,
    val showEditEventDialog: Boolean = false,
    val editingEvent: EventWithType? = null,
    val addEventYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val addEventMonth: Int = Calendar.getInstance().get(Calendar.MONTH),
    val addEventDay: Int = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: EventRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()
    private val _datesWithEvents = MutableStateFlow<Set<Int>>(emptySet())
    val datesWithEvents: StateFlow<Set<Int>> = _datesWithEvents.asStateFlow()
    private val _selectedDateEvents = MutableStateFlow<List<EventWithType>>(emptyList())
    val selectedDateEvents: StateFlow<List<EventWithType>> = _selectedDateEvents.asStateFlow()
    
    init {
        loadDatesWithEvents()
        loadEventTypes()
        loadTodayEvents()
    }

    private fun loadDatesWithEvents() {
        viewModelScope.launch {
            val state = _uiState.value
            repository.getDatesWithEvents(state.currentYear, state.currentMonth)
                .collect { dates ->
                    _datesWithEvents.value = dates
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

    private fun loadTodayEvents() {
        val today = Calendar.getInstance()
        loadEventsForSelectedDate(
            today.get(Calendar.YEAR),
            today.get(Calendar.MONTH),
            today.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun onMonthChange(year: Int, month: Int) {
        val currentState = _uiState.value
        val calendar = Calendar.getInstance().apply {
            set(year, month, 1)
        }
        val maxDayInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        val targetDay = if (currentState.selectedDate != null) {
            minOf(currentState.selectedDate, maxDayInMonth)
        } else {
            val today = Calendar.getInstance()
            if (year == today.get(Calendar.YEAR) && month == today.get(Calendar.MONTH)) {
                today.get(Calendar.DAY_OF_MONTH)
            } else {
                1
            }
        }
        
        _uiState.value = _uiState.value.copy(
            currentYear = year,
            currentMonth = month,
            selectedDate = targetDay,
            selectedYear = year,
            selectedMonth = month,
            showMonthPickerDialog = false
        )
        loadDatesWithEvents()
        loadEventsForSelectedDate(year, month, targetDay)
    }

    fun onDateSelected(year: Int, month: Int, day: Int) {
        _uiState.value = _uiState.value.copy(
            selectedDate = day,
            selectedYear = year,
            selectedMonth = month
        )
        loadEventsForSelectedDate(year, month, day)
    }

    private fun loadEventsForSelectedDate(year: Int, month: Int, day: Int) {
        viewModelScope.launch {
            repository.getRecordsByDate(year, month, day).collect { events ->
                _selectedDateEvents.value = events
            }
        }
    }

    fun onAddEventClick() {
        val state = _uiState.value
        _uiState.value = state.copy(
            showAddEventDialog = true,
            addEventYear = state.selectedYear,
            addEventMonth = state.selectedMonth,
            addEventDay = state.selectedDate ?: Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        )
    }

    fun onDismissAddEventDialog() {
        _uiState.value = _uiState.value.copy(showAddEventDialog = false)
    }

    fun onShowAddEventTypeDialog() {
        _uiState.value = _uiState.value.copy(showAddEventTypeDialog = true)
    }

    fun onDismissAddEventTypeDialog() {
        _uiState.value = _uiState.value.copy(showAddEventTypeDialog = false)
    }

    fun onShowMonthPickerDialog() {
        _uiState.value = _uiState.value.copy(showMonthPickerDialog = true)
    }

    fun onDismissMonthPickerDialog() {
        _uiState.value = _uiState.value.copy(showMonthPickerDialog = false)
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
            _uiState.value = _uiState.value.copy(showAddEventTypeDialog = false)
        }
    }

    private fun colorToLong(color: androidx.compose.ui.graphics.Color): Long {
        val argb = color.value.toLong()
        return if (argb < 0) argb + 0x100000000L else argb
    }

    fun addEventRecord(eventTypeId: Long, hour: Int, minute: Int, note: String) {
        val state = _uiState.value
        val calendar = Calendar.getInstance().apply {
            set(state.addEventYear, state.addEventMonth, state.addEventDay, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }

        viewModelScope.launch {
            val record = EventRecord(
                eventTypeId = eventTypeId,
                timestamp = calendar.timeInMillis,
                note = note
            )
            repository.insertEventRecord(record)
            _uiState.value = _uiState.value.copy(showAddEventDialog = false)
            loadEventsForSelectedDate(state.addEventYear, state.addEventMonth, state.addEventDay)
            loadDatesWithEvents()
        }
    }

    fun addEventRecordNow(eventTypeId: Long, note: String) {
        val state = _uiState.value
        viewModelScope.launch {
            val calendar = Calendar.getInstance().apply {
                set(state.addEventYear, state.addEventMonth, state.addEventDay, 
                    get(Calendar.HOUR_OF_DAY), get(Calendar.MINUTE), 0)
                set(Calendar.MILLISECOND, 0)
            }
            val record = EventRecord(
                eventTypeId = eventTypeId,
                timestamp = calendar.timeInMillis,
                note = note
            )
            repository.insertEventRecord(record)
            _uiState.value = _uiState.value.copy(showAddEventDialog = false)
            loadEventsForSelectedDate(state.addEventYear, state.addEventMonth, state.addEventDay)
            loadDatesWithEvents()
        }
    }

    fun deleteEvent(event: EventWithType) {
        viewModelScope.launch {
            repository.deleteEventRecord(event.record)
            val state = _uiState.value
            if (state.selectedDate != null) {
                loadEventsForSelectedDate(state.selectedYear, state.selectedMonth, state.selectedDate)
            }
            loadDatesWithEvents()
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
            if (state.selectedDate != null) {
                loadEventsForSelectedDate(state.selectedYear, state.selectedMonth, state.selectedDate)
            }
            loadDatesWithEvents()
        }
    }
}
