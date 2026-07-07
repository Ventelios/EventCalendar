package com.eventcalendar.app.ui.screens.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.eventcalendar.app.data.repository.EventWithType
import com.eventcalendar.app.ui.theme.Primary
import com.eventcalendar.app.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Refresh data each time the screen appears
    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // ── Title ─────────────────────────────────────────
            Text(
                text = "统计",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "查看事件统计与趋势",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ── Filter Row ────────────────────────────
                    item {
                        FilterRow(
                            dateRangeDays = uiState.dateRangeDays,
                            viewMode = uiState.viewMode,
                            onDateRangeChange = viewModel::onDateRangeChange,
                            onViewModeChange = viewModel::onViewModeChange
                        )
                    }

                    // ── Event Type Filters ────────────────────
                    if (uiState.allEventTypes.isNotEmpty()) {
                        item {
                            EventTypeFilterRow(
                                eventTypes = uiState.allEventTypes,
                                selectedIds = uiState.selectedTypeIds,
                                onToggle = viewModel::onEventTypeToggle
                            )
                        }
                    }

                    // ── Overview Cards ────────────────────────
                    if (uiState.overviewMetrics.isNotEmpty()) {
                        item {
                            OverviewCards(metrics = uiState.overviewMetrics)
                        }
                    }

                    // ── Trend Chart ───────────────────────────
                    item {
                        TrendChartCard(
                            trendData = uiState.trendData,
                            eventTypes = uiState.allEventTypes,
                            selectedTypeIds = uiState.selectedTypeIds
                        )
                    }

                    // ── Recent Events ─────────────────────────
                    item {
                        Text(
                            text = "近期事件",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    if (uiState.recentEvents.isEmpty()) {
                        item {
                            EmptyState()
                        }
                    } else {
                        items(uiState.recentEvents) { event ->
                            RecentEventCard(eventWithType = event)
                        }
                    }
                }
            }
        }
    }
}

// ── Filter Row ──────────────────────────────────────────────────

@Composable
private fun FilterRow(
    dateRangeDays: Int,
    viewMode: ViewMode,
    onDateRangeChange: (Int) -> Unit,
    onViewModeChange: (ViewMode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Date range chips — content depends on mode
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (viewMode == ViewMode.RECENT) {
                // Recent mode: 7d / 30d
                StatDateChip(
                    selected = dateRangeDays == 7,
                    label = "7天",
                    onClick = { onDateRangeChange(7) }
                )
                StatDateChip(
                    selected = dateRangeDays == 30,
                    label = "30天",
                    onClick = { onDateRangeChange(30) }
                )
            } else {
                // Cumulative mode: 7d / 30d / 365d
                StatDateChip(
                    selected = dateRangeDays == 7,
                    label = "7天",
                    onClick = { onDateRangeChange(7) }
                )
                StatDateChip(
                    selected = dateRangeDays == 30,
                    label = "30天",
                    onClick = { onDateRangeChange(30) }
                )
                StatDateChip(
                    selected = dateRangeDays == 365,
                    label = "365天",
                    onClick = { onDateRangeChange(365) }
                )
            }
        }

        SuggestionChip(
            onClick = {
                val next = if (viewMode == ViewMode.RECENT) ViewMode.CUMULATIVE else ViewMode.RECENT
                onViewModeChange(next)
            },
            label = {
                Text(
                    text = if (viewMode == ViewMode.RECENT) "近期模式" else "累计模式",
                    fontSize = 12.sp
                )
            },
            shape = RoundedCornerShape(20.dp),
            colors = SuggestionChipDefaults.suggestionChipColors(
                containerColor = Primary.copy(alpha = 0.12f),
                labelColor = Primary
            ),
            border = SuggestionChipDefaults.suggestionChipBorder(
                borderColor = Primary.copy(alpha = 0.3f),
                enabled = true
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatDateChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Primary.copy(alpha = 0.15f),
            selectedLabelColor = Primary
        ),
        shape = RoundedCornerShape(20.dp),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            selectedBorderColor = Primary,
            enabled = true,
            selected = selected
        )
    )
}

// ── Event Type Filter Row ────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventTypeFilterRow(
    eventTypes: List<com.eventcalendar.app.data.local.entity.EventType>,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit
) {
    // No "全部" chip — only per-type chips, each independently toggleable
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(eventTypes) { eventType ->
            val color = Color(eventType.color.toULong())
            val isSelected = selectedIds.contains(eventType.id)
            FilterChip(
                selected = isSelected,
                onClick = { onToggle(eventType.id) },
                label = {
                    Text(
                        text = eventType.name,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = color.copy(alpha = 0.15f),
                    selectedLabelColor = color
                ),
                shape = RoundedCornerShape(20.dp),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    selectedBorderColor = color,
                    enabled = true,
                    selected = isSelected
                )
            )
        }
    }
}

// ── Overview Cards ───────────────────────────────────────────────

@Composable
private fun OverviewCards(metrics: List<OverviewMetric>) {
    if (metrics.size <= 3) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            metrics.forEach { metric ->
                OverviewCard(
                    metric = metric,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for (i in metrics.indices step 2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OverviewCard(
                        metric = metrics[i],
                        modifier = Modifier.weight(1f)
                    )
                    if (i + 1 < metrics.size) {
                        OverviewCard(
                            metric = metrics[i + 1],
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewCard(
    metric: OverviewMetric,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = metric.label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "${metric.value}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = metric.unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
            if (metric.changePercent != null) {
                Spacer(modifier = Modifier.height(6.dp))
                ChangeIndicator(
                    percent = metric.changePercent,
                    isPositive = metric.isPositive
                )
            }
        }
    }
}

@Composable
private fun ChangeIndicator(percent: Float, isPositive: Boolean) {
    // Red for increase (上升), green for decrease (下降)
    val color = if (percent == 0f) {
        TextSecondary
    } else if (isPositive) {
        Color(0xFFE57373)  // red — 上升
    } else {
        Color(0xFF4CAF50)  // green — 下降
    }
    val icon = if (percent == 0f) {
        Icons.Rounded.Remove
    } else if (isPositive) {
        Icons.Rounded.ArrowUpward
    } else {
        Icons.Rounded.ArrowDownward
    }
    val absPercent = kotlin.math.abs(percent)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = "${String.format(Locale.US, "%.1f", absPercent)}%",
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

// ── Trend Chart ──────────────────────────────────────────────────

@Composable
private fun TrendChartCard(
    trendData: List<DaySeriesPoint>,
    eventTypes: List<com.eventcalendar.app.data.local.entity.EventType>,
    selectedTypeIds: Set<Long>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ShowChart,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "趋势图",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Active types for the chart
            val activeTypes = eventTypes.filter { selectedTypeIds.contains(it.id) }

            Spacer(modifier = Modifier.height(8.dp))

            // Legend
            if (activeTypes.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    activeTypes.take(5).forEach { type ->
                        val color = Color(type.color.toULong())
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(color, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = type.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                    if (activeTypes.size > 5) {
                        Text(
                            text = "+${activeTypes.size - 5}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (trendData.isEmpty() || activeTypes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无数据",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            } else {
                MultiLineChart(
                    trendData = trendData,
                    activeTypes = activeTypes,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
        }
    }
}

/**
 * Calculate a nice round max value for the Y-axis scale.
 * Minimum is 4 so low-volume charts still have meaningful grid lines.
 * E.g. 0→4, 1→4, 3→4, 5→5, 7→10, 12→15, 23→25, 45→50, 87→100
 */
private fun calcNiceMax(raw: Int): Int {
    if (raw <= 0) return 4
    if (raw <= 2) return 4
    val magnitude = Math.pow(10.0, Math.floor(Math.log10(raw.toDouble()))).toInt()
    val residual = raw.toDouble() / magnitude
    val nice = when {
        residual <= 1.0 -> 1 * magnitude
        residual <= 2.0 -> 2 * magnitude
        residual <= 5.0 -> 5 * magnitude
        else -> 10 * magnitude
    }
    return nice.coerceAtLeast(4)
}

/**
 * Multi-line trend chart with proper Y-axis scale labels and per-type colored lines.
 * Labels are drawn inside Canvas for exact alignment with grid lines.
 * No total/aggregate line — each event type is shown in its own color.
 */
@Composable
private fun MultiLineChart(
    trendData: List<DaySeriesPoint>,
    activeTypes: List<com.eventcalendar.app.data.local.entity.EventType>,
    modifier: Modifier = Modifier
) {
    if (trendData.isEmpty()) return

    val globalMax = trendData.maxOfOrNull { it.totalValue }?.coerceAtLeast(1) ?: 1
    val niceMax = calcNiceMax(globalMax)

    // Build Y-axis values from top (max) to bottom (0)
    val yScaleValues = remember(niceMax) {
        listOf(niceMax, niceMax * 3 / 4, niceMax / 2, niceMax / 4, 0)
    }
    val yLabels = remember(niceMax) {
        yScaleValues.map { it.toString() }
    }

    val textMeasurer = rememberTextMeasurer()
    val labelTextStyle = remember {
        TextStyle(fontSize = 10.sp, color = TextSecondary)
    }

    // Pre-measure all labels so we can draw them inside Canvas with exact positioning
    val measuredLabels = remember(yLabels) {
        yLabels.map { textMeasurer.measure(AnnotatedString(it), labelTextStyle) }
    }

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (measuredLabels.isEmpty()) return@Canvas

            // Reserve space for Y-axis labels on the left
            val maxLabelW = measuredLabels.maxOf { it.size.width }.toFloat()
            val labelRightMargin = 6f  // gap between label text and chart area
            val paddingLeft = maxLabelW + labelRightMargin
            val paddingRight = 12f
            // Vertical padding: half the label height so the top/bottom labels
            // are vertically centered on the first/last grid lines
            val halfLabelH = measuredLabels.first().size.height.toFloat() / 2f
            val paddingTop = halfLabelH
            val paddingBottom = halfLabelH
            val chartWidth = (size.width - paddingLeft - paddingRight).coerceAtLeast(1f)
            val chartHeight = (size.height - paddingTop - paddingBottom).coerceAtLeast(1f)

            // ── Grid lines + Y-axis labels (drawn together for perfect alignment) ──
            val gridColor = TextSecondary.copy(alpha = 0.15f)
            for (i in 0..4) {
                val y = paddingTop + (chartHeight / 4f) * i

                // Grid line
                drawLine(
                    color = gridColor,
                    start = Offset(paddingLeft, y),
                    end = Offset(size.width - paddingRight, y),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                )

                // Y-axis label — centered vertically on the grid line
                val label = measuredLabels[i]  // i=0 is top (niceMax), i=4 is bottom (0)
                drawText(
                    textLayoutResult = label,
                    topLeft = Offset(
                        paddingLeft - label.size.width - labelRightMargin,
                        y - label.size.height / 2f
                    )
                )
            }

            // ── Projection helpers ───────────────────────────────
            fun projectX(index: Int): Float {
                return paddingLeft + (chartWidth / (trendData.size - 1).coerceAtLeast(1)) * index
            }

            fun projectY(value: Int): Float {
                val ratio = (value.toFloat() / niceMax).coerceIn(0f, 1f)
                return paddingTop + chartHeight * (1f - ratio)
            }

            // ── Per-type colored lines ───────────────────────────
            // Line width is slightly thicker when only one type is visible
            val perLineWidth = if (activeTypes.size == 1) 3f else 2.5f
            val perDotRadius = if (activeTypes.size == 1) 3.5f else 3f

            for (type in activeTypes) {
                val typeColor = Color(type.color.toULong())

                val points = trendData.mapIndexed { index, dp ->
                    Offset(projectX(index), projectY(dp.perType[type.id] ?: 0))
                }

                // Draw curved line
                if (points.size > 1) {
                    val path = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (j in 1 until points.size) {
                            val prev = points[j - 1]
                            val curr = points[j]
                            val midX = (prev.x + curr.x) / 2f
                            cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = typeColor,
                        style = Stroke(width = perLineWidth)
                    )
                }

                // Dots on non-zero points
                trendData.forEachIndexed { index, dp ->
                    val count = dp.perType[type.id] ?: 0
                    if (count > 0) {
                        val center = Offset(projectX(index), projectY(count))
                        drawCircle(color = typeColor, radius = perDotRadius, center = center)
                        drawCircle(color = Color.White, radius = perDotRadius * 0.5f, center = center)
                    }
                }
            }
        }

        // ── X-axis date labels ───────────────────────────────────
        // Only render visible labels (no Spacers) so text isn't squeezed on wide ranges
        val labelStep = when {
            trendData.size <= 7 -> 1
            trendData.size <= 14 -> 2
            trendData.size <= 21 -> 3
            else -> 5
        }
        val visibleLabels = trendData.filterIndexed { index, _ ->
            index % labelStep == 0 || index == trendData.size - 1
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 34.dp, top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            visibleLabels.forEach { point ->
                Text(
                    text = point.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    fontSize = 10.sp
                )
            }
        }
    }
}

// ── Recent Events ────────────────────────────────────────────────

@Composable
private fun RecentEventCard(eventWithType: EventWithType) {
    val dateFormat = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
    val eventColor = Color(eventWithType.eventType.color.toULong())

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(eventColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = eventWithType.eventType.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = TextSecondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = dateFormat.format(Date(eventWithType.record.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
            if (eventWithType.record.note.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = eventWithType.record.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(80.dp),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

// ── Empty State ──────────────────────────────────────────────────

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Rounded.BarChart,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = TextSecondary.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "暂无近期事件",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}
