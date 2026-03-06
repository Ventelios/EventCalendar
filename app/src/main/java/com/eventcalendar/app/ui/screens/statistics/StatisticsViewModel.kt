package com.eventcalendar.app.ui.screens.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventcalendar.app.data.repository.EventRepository
import com.eventcalendar.app.data.repository.EventWithType
import com.eventcalendar.app.data.repository.EventTypeWithCount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

enum class TimeRange {
    ALL, TODAY, THIS_WEEK, THIS_MONTH
}

data class StatisticsUiState(
    val timeRange: TimeRange = TimeRange.TODAY,
    val frequencies: List<EventTypeWithCount> = emptyList(),
    val selectedEventTypeId: Long? = null,
    val selectedEvents: List<EventWithType> = emptyList(),
    val showDetailDialog: Boolean = false,
    val trendData: List<TrendPoint> = emptyList()
)

data class TrendPoint(
    val label: String,
    val value: Int
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val repository: EventRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        loadFrequency()
    }

    private fun loadFrequency() {
        viewModelScope.launch {
            val (startTime, endTime) = getTimeRangeMillis(_uiState.value.timeRange)

            if (startTime == null && endTime == null) {
                repository.getAllEventFrequency().collect { frequencies ->
                    val trendData = generateAllTimeTrendData()
                    _uiState.value = _uiState.value.copy(
                        frequencies = frequencies,
                        trendData = trendData
                    )
                }
            } else if (startTime != null && endTime != null) {
                repository.getEventFrequency(startTime, endTime).collect { frequencies ->
                    val trendData = generateTrendDataForTimeRange(_uiState.value.timeRange, startTime, endTime)
                    _uiState.value = _uiState.value.copy(
                        frequencies = frequencies,
                        trendData = trendData
                    )
                }
            }
        }
    }

    private suspend fun generateAllTimeTrendData(): List<TrendPoint> {
        val result = mutableListOf<TrendPoint>()
        val calendar = Calendar.getInstance()
        
        for (i in 5 downTo 0) {
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(Calendar.MONTH, -i)
            
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfMonth = calendar.timeInMillis
            
            calendar.add(Calendar.MONTH, 1)
            val endOfMonth = calendar.timeInMillis
            
            val count = repository.getEventCountByTimeRange(startOfMonth, endOfMonth)
            val month = (calendar.get(Calendar.MONTH) + 1).let { if (i == 0) (it - 1).coerceAtLeast(1) else it }
            result.add(TrendPoint("${month}月", count))
        }
        
        return result
    }

    private suspend fun generateTrendDataForTimeRange(
        timeRange: TimeRange,
        startTime: Long,
        endTime: Long
    ): List<TrendPoint> {
        return when (timeRange) {
            TimeRange.ALL -> generateAllTimeTrendData()
            TimeRange.TODAY -> generateTodayTrendData()
            TimeRange.THIS_WEEK -> generateWeekTrendData()
            TimeRange.THIS_MONTH -> generateMonthTrendData()
        }
    }

    private suspend fun generateTodayTrendData(): List<TrendPoint> {
        val result = mutableListOf<TrendPoint>()
        val calendar = Calendar.getInstance()
        
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        for (hour in 0..23) {
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            val startOfHour = calendar.timeInMillis
            
            calendar.add(Calendar.HOUR_OF_DAY, 1)
            val endOfHour = calendar.timeInMillis
            
            val count = repository.getEventCountByTimeRange(startOfHour, endOfHour)
            result.add(TrendPoint("${hour}时", count))
        }
        
        return result
    }

    private suspend fun generateWeekTrendData(): List<TrendPoint> {
        val result = mutableListOf<TrendPoint>()
        val calendar = Calendar.getInstance()
        val weekDays = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
        
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val daysToSubtract = if (dayOfWeek == Calendar.SUNDAY) 6 else (dayOfWeek - Calendar.MONDAY)
        calendar.add(Calendar.DAY_OF_MONTH, -daysToSubtract)
        
        for (i in 0..6) {
            val startOfDay = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            val endOfDay = calendar.timeInMillis
            
            val count = repository.getEventCountByTimeRange(startOfDay, endOfDay)
            result.add(TrendPoint(weekDays[i], count))
        }
        
        return result
    }

    private suspend fun generateMonthTrendData(): List<TrendPoint> {
        val result = mutableListOf<TrendPoint>()
        val calendar = Calendar.getInstance()
        
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val weeksInMonth = ((daysInMonth - 1) / 7) + 1
        
        for (week in 1..weeksInMonth.coerceAtMost(5)) {
            val startOfWeek = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_MONTH, 7)
            val endOfWeek = calendar.timeInMillis.coerceAtMost(System.currentTimeMillis() + 86400000)
            
            val count = repository.getEventCountByTimeRange(startOfWeek, endOfWeek)
            result.add(TrendPoint("第${week}周", count))
        }
        
        return result
    }

    fun onTimeRangeChange(timeRange: TimeRange) {
        _uiState.value = _uiState.value.copy(timeRange = timeRange)
        loadFrequency()
    }

    fun onEventTypeClick(eventTypeId: Long) {
        _uiState.value = _uiState.value.copy(selectedEventTypeId = eventTypeId, showDetailDialog = true)
        viewModelScope.launch {
            repository.getRecordsByEventType(eventTypeId).collect { events ->
                _uiState.value = _uiState.value.copy(selectedEvents = events)
            }
        }
    }

    fun onDismissDialog() {
        _uiState.value = _uiState.value.copy(showDetailDialog = false, selectedEventTypeId = null, selectedEvents = emptyList())
    }

    private fun getTimeRangeMillis(timeRange: TimeRange): Pair<Long?, Long?> {
        val calendar = Calendar.getInstance()
        return when (timeRange) {
            TimeRange.ALL -> Pair(null, null)
            TimeRange.TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                val end = calendar.timeInMillis
                Pair(start, end)
            }
            TimeRange.THIS_WEEK -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                val start = calendar.timeInMillis
                calendar.add(Calendar.WEEK_OF_YEAR, 1)
                val end = calendar.timeInMillis
                Pair(start, end)
            }
            TimeRange.THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                calendar.add(Calendar.MONTH, 1)
                val end = calendar.timeInMillis
                Pair(start, end)
            }
        }
    }
}
