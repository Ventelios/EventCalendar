package com.eventcalendar.app.ui.screens.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventcalendar.app.data.local.PreferencesManager
import com.eventcalendar.app.data.local.entity.EventType
import com.eventcalendar.app.data.repository.EventRepository
import com.eventcalendar.app.data.repository.EventWithType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

enum class ViewMode { CUMULATIVE, RECENT }

data class OverviewMetric(
    val label: String,
    val value: Int,
    val unit: String = "次",
    val changePercent: Float? = null,
    val isPositive: Boolean = true
)

data class DaySeriesPoint(
    val label: String,
    val timestamp: Long,
    val totalValue: Int,
    val perType: Map<Long, Int> = emptyMap()
)

data class StatisticsUiState(
    val viewMode: ViewMode = ViewMode.RECENT,
    val dateRangeDays: Int = 7,
    val overviewMetrics: List<OverviewMetric> = emptyList(),
    val trendData: List<DaySeriesPoint> = emptyList(),
    val recentEvents: List<EventWithType> = emptyList(),
    val allEventTypes: List<EventType> = emptyList(),
    val selectedTypeIds: Set<Long> = emptySet(),
    val isLoading: Boolean = true
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val repository: EventRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        StatisticsUiState(
            viewMode = if (preferencesManager.defaultStatisticsMode == "cumulative")
                ViewMode.CUMULATIVE else ViewMode.RECENT
        )
    )
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    /** True until the very first data load completes; prevents spinner flicker on refreshes. */
    private var initialLoadDone = false

    init {
        loadAllData()
    }

    /** Public refresh entry point — called by Screen on each appearance. */
    fun refresh() {
        loadAllData()
    }

    fun onViewModeChange(mode: ViewMode) {
        // Reset to a sensible default when switching modes
        val defaultDays = if (mode == ViewMode.CUMULATIVE) 7 else 7
        _uiState.update { it.copy(viewMode = mode, dateRangeDays = defaultDays) }
        loadAllData()
    }

    fun onDateRangeChange(days: Int) {
        _uiState.update { it.copy(dateRangeDays = days) }
        loadAllData()
    }

    fun onEventTypeToggle(typeId: Long) {
        _uiState.update { state ->
            val newSet = state.selectedTypeIds.toMutableSet()
            if (newSet.contains(typeId)) {
                // Don't allow deselecting the last selected type
                if (newSet.size > 1) {
                    newSet.remove(typeId)
                }
            } else {
                newSet.add(typeId)
            }
            state.copy(selectedTypeIds = newSet)
        }
        loadAllData()
    }

    /** Select all event types (used when new types are discovered). */
    fun selectAllTypes() {
        _uiState.update { state ->
            state.copy(selectedTypeIds = state.allEventTypes.map { it.id }.toSet())
        }
        loadAllData()
    }

    // ── Data Loading ───────────────────────────────────────────────

    private fun loadAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            // Only show full-screen spinner on the very first load to avoid flicker
            if (!initialLoadDone) {
                _uiState.update { it.copy(isLoading = true) }
            }

            // Always reload event types so newly created types appear
            val eventTypes = repository.getAllEventTypesOnce()

            val state = _uiState.value
            val now = System.currentTimeMillis()
            val cal = Calendar.getInstance()

            // Build the effective set of selected type IDs
            val existingIds = eventTypes.map { it.id }.toSet()
            val effectiveSelectedIds = if (state.selectedTypeIds.isEmpty() && existingIds.isNotEmpty()) {
                existingIds
            } else {
                state.selectedTypeIds.intersect(existingIds).ifEmpty { existingIds }
            }

            val overviewMetrics = calculateOverviewMetrics(
                mode = state.viewMode,
                dateRangeDays = state.dateRangeDays,
                now = now,
                cal = cal,
                selectedTypeIds = effectiveSelectedIds
            )
            val trendData = calculateTrendData(
                viewMode = state.viewMode,
                dateRangeDays = state.dateRangeDays,
                now = now,
                cal = cal,
                selectedTypeIds = effectiveSelectedIds
            )
            val recentEvents = loadRecentEvents(now, effectiveSelectedIds)

            _uiState.update {
                it.copy(
                    allEventTypes = eventTypes,
                    selectedTypeIds = effectiveSelectedIds,
                    overviewMetrics = overviewMetrics,
                    trendData = trendData,
                    recentEvents = recentEvents,
                    isLoading = false
                )
            }

            initialLoadDone = true
        }
    }

    // ── Overview Metrics ──────────────────────────────────────────

    private suspend fun calculateOverviewMetrics(
        mode: ViewMode,
        dateRangeDays: Int,
        now: Long,
        cal: Calendar,
        selectedTypeIds: Set<Long>
    ): List<OverviewMetric> {
        return if (mode == ViewMode.CUMULATIVE) {
            calculateCumulativeMetrics(now, cal, selectedTypeIds)
        } else {
            calculateRecentMetrics(now, cal, dateRangeDays, selectedTypeIds)
        }
    }

    private suspend fun getFilteredCount(start: Long, end: Long, typeIds: Set<Long>): Int {
        if (typeIds.isEmpty()) return 0
        var total = 0
        for (typeId in typeIds) {
            total += repository.getEventCountByTypeAndTimeRange(start, end, typeId)
        }
        return total
    }

    private suspend fun calculateCumulativeMetrics(
        now: Long,
        cal: Calendar,
        typeIds: Set<Long>
    ): List<OverviewMetric> {
        // Today
        cal.timeInMillis = now
        val todayStart = startOfDay(cal)
        val todayEnd = now
        val todayCount = getFilteredCount(todayStart, todayEnd, typeIds)

        cal.timeInMillis = todayStart
        cal.add(Calendar.DAY_OF_MONTH, -1)
        val yesterdayStart = cal.timeInMillis
        val yesterdayEnd = todayStart
        val yesterdayCount = getFilteredCount(yesterdayStart, yesterdayEnd, typeIds)

        val todayMetric = OverviewMetric(
            label = "今日",
            value = todayCount,
            changePercent = calcChange(todayCount, yesterdayCount),
            isPositive = todayCount >= yesterdayCount
        )

        // This week (Monday to now)
        cal.timeInMillis = now
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else (dayOfWeek - Calendar.MONDAY)
        cal.add(Calendar.DAY_OF_MONTH, -daysFromMonday)
        val thisWeekStart = cal.timeInMillis
        val thisWeekCount = getFilteredCount(thisWeekStart, now, typeIds)

        cal.timeInMillis = thisWeekStart
        cal.add(Calendar.DAY_OF_MONTH, -7)
        val lastWeekStart = cal.timeInMillis
        val lastWeekCount = getFilteredCount(lastWeekStart, thisWeekStart, typeIds)

        val weekMetric = OverviewMetric(
            label = "本周",
            value = thisWeekCount,
            changePercent = calcChange(thisWeekCount, lastWeekCount),
            isPositive = thisWeekCount >= lastWeekCount
        )

        // This month (1st to now)
        cal.timeInMillis = now
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val thisMonthStart = startOfDay(cal)
        val thisMonthCount = getFilteredCount(thisMonthStart, now, typeIds)

        cal.timeInMillis = thisMonthStart
        cal.add(Calendar.MONTH, -1)
        val lastMonthStart = cal.timeInMillis
        val lastMonthCount = getFilteredCount(lastMonthStart, thisMonthStart, typeIds)

        val monthMetric = OverviewMetric(
            label = "本月",
            value = thisMonthCount,
            changePercent = calcChange(thisMonthCount, lastMonthCount),
            isPositive = thisMonthCount >= lastMonthCount
        )

        return listOf(todayMetric, weekMetric, monthMetric)
    }

    private suspend fun calculateRecentMetrics(
        now: Long,
        cal: Calendar,
        days: Int,
        typeIds: Set<Long>
    ): List<OverviewMetric> {
        // Current period: [now - days, now]
        cal.timeInMillis = now
        cal.add(Calendar.DAY_OF_MONTH, -days)
        val currentStart = startOfDay(cal)
        val currentCount = getFilteredCount(currentStart, now, typeIds)

        // Previous period: [now - 2*days, now - days]
        cal.timeInMillis = currentStart
        cal.add(Calendar.DAY_OF_MONTH, -days)
        val previousStart = cal.timeInMillis
        val previousCount = getFilteredCount(previousStart, currentStart, typeIds)

        val dailyAvg = if (days > 0) currentCount.toFloat() / days else 0f
        val prevDailyAvg = if (days > 0) previousCount.toFloat() / days else 0f
        val avgAsInt = Math.round(dailyAvg)

        val labelPrefix = if (days == 7) "过去7天" else "过去30天"

        return listOf(
            OverviewMetric(
                label = "$labelPrefix 总计",
                value = currentCount,
                changePercent = calcChange(currentCount, previousCount),
                isPositive = currentCount >= previousCount
            ),
            OverviewMetric(
                label = "$labelPrefix 日均",
                value = avgAsInt,
                unit = "次/天",
                changePercent = calcChangeFloat(dailyAvg, prevDailyAvg),
                isPositive = dailyAvg >= prevDailyAvg
            )
        )
    }

    // ── Trend Data ────────────────────────────────────────────────

    private suspend fun calculateTrendData(
        viewMode: ViewMode,
        dateRangeDays: Int,
        now: Long,
        cal: Calendar,
        selectedTypeIds: Set<Long>
    ): List<DaySeriesPoint> {
        return if (viewMode == ViewMode.CUMULATIVE) {
            calculateCumulativeTrend(dateRangeDays, now, cal, selectedTypeIds)
        } else {
            calculateRecentDailyTrend(dateRangeDays, now, cal, selectedTypeIds)
        }
    }

    /**
     * Cumulative-mode trend: unit depends on the selected range.
     *   7d  → daily  (this week, Mon–Sun)
     *  30d  → weekly (past ~5 weeks, Mon–Sun buckets)
     * 365d  → monthly (Jan through current month)
     */
    private suspend fun calculateCumulativeTrend(
        dateRangeDays: Int,
        now: Long,
        cal: Calendar,
        selectedTypeIds: Set<Long>
    ): List<DaySeriesPoint> {
        return when (dateRangeDays) {
            7 -> buildWeekDaily(now, cal, selectedTypeIds)
            30 -> buildWeekly(now, cal, selectedTypeIds)
            365 -> buildMonthly(now, cal, selectedTypeIds)
            else -> buildWeekDaily(now, cal, selectedTypeIds)
        }
    }

    // ── 7d cumulative: full Mon–Sun week (7 daily points) ──────────

    private suspend fun buildWeekDaily(
        now: Long,
        cal: Calendar,
        selectedTypeIds: Set<Long>
    ): List<DaySeriesPoint> {
        cal.timeInMillis = now
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        // Navigate to Monday of this week
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else (dayOfWeek - Calendar.MONDAY)
        cal.add(Calendar.DAY_OF_MONTH, -daysFromMonday)
        val monday = cal.timeInMillis

        val dateFormat = SimpleDateFormat("M/d", Locale.getDefault())
        val dayFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())

        // Fetch records covering the full Mon–Sun week
        cal.timeInMillis = monday
        cal.add(Calendar.DAY_OF_MONTH, 7)
        val weekEnd = cal.timeInMillis
        val allRecords = repository.getRecordsByTimeRange(monday, weekEnd)

        val dayBuckets = mutableMapOf<String, MutableList<com.eventcalendar.app.data.local.entity.EventRecord>>()
        for (record in allRecords) {
            val key = dayFormat.format(Date(record.timestamp))
            dayBuckets.getOrPut(key) { mutableListOf() }.add(record)
        }

        // Build 7 daily points (Mon=0 … Sun=6)
        val result = mutableListOf<DaySeriesPoint>()
        cal.timeInMillis = monday
        for (i in 0 until 7) {
            val dayStart = cal.timeInMillis
            cal.add(Calendar.DAY_OF_MONTH, 1)
            val dayKey = dayFormat.format(Date(dayStart))
            val dayRecords = dayBuckets[dayKey] ?: emptyList()

            val perType = mutableMapOf<Long, Int>()
            var total = 0
            for (record in dayRecords) {
                if (selectedTypeIds.isEmpty() || selectedTypeIds.contains(record.eventTypeId)) {
                    perType[record.eventTypeId] = (perType[record.eventTypeId] ?: 0) + 1
                    total++
                }
            }

            result.add(
                DaySeriesPoint(
                    label = dateFormat.format(Date(dayStart)),
                    timestamp = dayStart,
                    totalValue = total,
                    perType = perType
                )
            )
        }

        return result
    }

    // ── 30d cumulative: weekly buckets (Mon–Sun) for ~5 weeks ──────

    private suspend fun buildWeekly(
        now: Long,
        cal: Calendar,
        selectedTypeIds: Set<Long>
    ): List<DaySeriesPoint> {
        cal.timeInMillis = now
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        // Navigate to Monday of this week
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else (dayOfWeek - Calendar.MONDAY)
        cal.add(Calendar.DAY_OF_MONTH, -daysFromMonday)
        val thisMonday = cal.timeInMillis

        // Go back 5 weeks (cover ~35 days)
        cal.timeInMillis = thisMonday
        cal.add(Calendar.DAY_OF_MONTH, -35)
        val fetchStart = cal.timeInMillis

        // Fetch all records in range
        cal.timeInMillis = thisMonday
        cal.add(Calendar.DAY_OF_MONTH, 7)
        val fetchEnd = cal.timeInMillis
        val allRecords = repository.getRecordsByTimeRange(fetchStart, fetchEnd)

        val dayFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val dayBuckets = mutableMapOf<String, MutableList<com.eventcalendar.app.data.local.entity.EventRecord>>()
        for (record in allRecords) {
            val key = dayFormat.format(Date(record.timestamp))
            dayBuckets.getOrPut(key) { mutableListOf() }.add(record)
        }

        val labelFormat = SimpleDateFormat("M/d", Locale.getDefault())
        val result = mutableListOf<DaySeriesPoint>()

        // Generate 5 weekly points, oldest → newest
        cal.timeInMillis = thisMonday
        for (w in 0 until 5) {
            cal.timeInMillis = thisMonday
            cal.add(Calendar.DAY_OF_MONTH, -7 * (4 - w))
            val weekMonday = cal.timeInMillis

            // Sunday = Monday + 6 days
            cal.add(Calendar.DAY_OF_MONTH, 6)
            val weekSunday = cal.timeInMillis

            // Label: "M/d-M/d" e.g. "7/6-7/12"
            val label = "${labelFormat.format(Date(weekMonday))}-${labelFormat.format(Date(weekSunday))}"

            val perType = mutableMapOf<Long, Int>()
            var total = 0

            cal.timeInMillis = weekMonday
            for (d in 0 until 7) {
                val dayStart = cal.timeInMillis
                cal.add(Calendar.DAY_OF_MONTH, 1)
                val key = dayFormat.format(Date(dayStart))
                val dayRecords = dayBuckets[key] ?: emptyList()
                for (record in dayRecords) {
                    if (selectedTypeIds.isEmpty() || selectedTypeIds.contains(record.eventTypeId)) {
                        perType[record.eventTypeId] = (perType[record.eventTypeId] ?: 0) + 1
                        total++
                    }
                }
            }

            result.add(
                DaySeriesPoint(
                    label = label,
                    timestamp = weekMonday,
                    totalValue = total,
                    perType = perType
                )
            )
        }

        return result
    }

    // ── 365d cumulative: monthly buckets (Jan through current month) ─

    private suspend fun buildMonthly(
        now: Long,
        cal: Calendar,
        selectedTypeIds: Set<Long>
    ): List<DaySeriesPoint> {
        cal.timeInMillis = now

        // Jan 1 of the current year
        cal.set(Calendar.MONTH, Calendar.JANUARY)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val yearStart = startOfDay(cal)

        // End of current month (fetch up to now)
        cal.timeInMillis = now
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val fetchEnd = cal.timeInMillis

        val allRecords = repository.getRecordsByTimeRange(yearStart, fetchEnd)

        // Group records by month
        val monthBuckets = mutableMapOf<Int, MutableList<com.eventcalendar.app.data.local.entity.EventRecord>>()
        cal.timeInMillis = now
        val currentMonth = cal.get(Calendar.MONTH)  // 0=Jan … 11=Dec

        for (record in allRecords) {
            cal.timeInMillis = record.timestamp
            val month = cal.get(Calendar.MONTH)
            monthBuckets.getOrPut(month) { mutableListOf() }.add(record)
        }

        val monthNames = listOf("1月", "2月", "3月", "4月", "5月", "6月",
            "7月", "8月", "9月", "10月", "11月", "12月")

        val result = mutableListOf<DaySeriesPoint>()
        for (m in 0..currentMonth) {
            val monthRecords = monthBuckets[m] ?: emptyList()
            val perType = mutableMapOf<Long, Int>()
            var total = 0
            for (record in monthRecords) {
                if (selectedTypeIds.isEmpty() || selectedTypeIds.contains(record.eventTypeId)) {
                    perType[record.eventTypeId] = (perType[record.eventTypeId] ?: 0) + 1
                    total++
                }
            }

            // Use the 1st of each month as the timestamp
            cal.timeInMillis = yearStart
            cal.set(Calendar.MONTH, m)
            val monthTs = cal.timeInMillis

            result.add(
                DaySeriesPoint(
                    label = monthNames[m],
                    timestamp = monthTs,
                    totalValue = total,
                    perType = perType
                )
            )
        }

        return result
    }

    // ── Recent-mode: daily points for the selected range ───────────

    private suspend fun calculateRecentDailyTrend(
        days: Int,
        now: Long,
        cal: Calendar,
        selectedTypeIds: Set<Long>
    ): List<DaySeriesPoint> {
        cal.timeInMillis = now
        cal.add(Calendar.DAY_OF_MONTH, -days)
        val startTime = startOfDay(cal)

        val allRecords = repository.getRecordsByTimeRange(startTime, now)
        val dateFormat = SimpleDateFormat("M/d", Locale.getDefault())
        val dayFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())

        val dayBuckets = mutableMapOf<String, MutableList<com.eventcalendar.app.data.local.entity.EventRecord>>()
        for (record in allRecords) {
            val dayKey = dayFormat.format(Date(record.timestamp))
            dayBuckets.getOrPut(dayKey) { mutableListOf() }.add(record)
        }

        val result = mutableListOf<DaySeriesPoint>()
        cal.timeInMillis = startTime
        for (i in 0 until days) {
            val dayStart = cal.timeInMillis
            cal.add(Calendar.DAY_OF_MONTH, 1)
            val dayKey = dayFormat.format(Date(dayStart))
            val dayRecords = dayBuckets[dayKey] ?: emptyList()

            val perType = mutableMapOf<Long, Int>()
            var total = 0
            for (record in dayRecords) {
                if (selectedTypeIds.isEmpty() || selectedTypeIds.contains(record.eventTypeId)) {
                    perType[record.eventTypeId] = (perType[record.eventTypeId] ?: 0) + 1
                    total++
                }
            }

            result.add(
                DaySeriesPoint(
                    label = dateFormat.format(Date(dayStart)),
                    timestamp = dayStart,
                    totalValue = total,
                    perType = perType
                )
            )
        }

        return result
    }

    // ── Recent Events ─────────────────────────────────────────────

    private suspend fun loadRecentEvents(
        now: Long,
        selectedTypeIds: Set<Long>
    ): List<EventWithType> {
        val cal = Calendar.getInstance()
        cal.timeInMillis = now
        cal.add(Calendar.DAY_OF_MONTH, -30)
        val startTime = startOfDay(cal)

        val events = repository.getRecentRecordsWithTypes(startTime, now, 50)
        return if (selectedTypeIds.isEmpty()) {
            events.take(30)
        } else {
            events.filter { selectedTypeIds.contains(it.eventType.id) }.take(30)
        }
    }

    // ── Helpers ───────────────────────────────────────────────────

    private fun startOfDay(cal: Calendar): Long {
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun calcChange(current: Int, previous: Int): Float? {
        if (previous == 0 && current == 0) return null
        if (previous == 0) return 100f
        return ((current - previous).toFloat() / previous * 100f)
    }

    private fun calcChangeFloat(current: Float, previous: Float): Float? {
        if (previous == 0f && current == 0f) return null
        if (previous == 0f) return 100f
        return ((current - previous) / previous * 100f)
    }
}
