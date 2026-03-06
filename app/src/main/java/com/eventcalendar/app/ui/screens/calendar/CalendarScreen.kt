package com.eventcalendar.app.ui.screens.calendar

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.eventcalendar.app.data.local.entity.EventType
import com.eventcalendar.app.data.repository.EventWithType
import com.eventcalendar.app.ui.theme.EventColors
import com.eventcalendar.app.ui.theme.Primary
import com.eventcalendar.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CalendarScreen(
    navController: NavController,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val datesWithEvents by viewModel.datesWithEvents.collectAsState()
    val selectedDateEvents by viewModel.selectedDateEvents.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    
    var showDeleteConfirmDialog by remember { mutableStateOf<EventWithType?>(null) }
    
    val initialPage = 1200
    val pagerState = rememberPagerState(initialPage = initialPage) { 2400 }

    LaunchedEffect(pagerState.currentPage) {
        val pageOffset = pagerState.currentPage - initialPage
        val calendar = Calendar.getInstance().apply {
            add(Calendar.MONTH, pageOffset)
        }
        val newYear = calendar.get(Calendar.YEAR)
        val newMonth = calendar.get(Calendar.MONTH)
        if (newYear != uiState.currentYear || newMonth != uiState.currentMonth) {
            viewModel.onMonthChange(newYear, newMonth)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onAddEventClick() },
                containerColor = Primary,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp
                ),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "记录",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navController.navigate("menu") },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Rounded.MoreVert,
                        contentDescription = "菜单",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Rounded.ChevronLeft,
                            contentDescription = "上个月",
                            tint = TextSecondary
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable(onClick = { viewModel.onShowMonthPickerDialog() })
                    ) {
                        Text(
                            text = "${uiState.currentYear}",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                            letterSpacing = 2.sp
                        )
                        val monthNames = listOf(
                            "一月", "二月", "三月", "四月", "五月", "六月",
                            "七月", "八月", "九月", "十月", "十一月", "十二月"
                        )
                        Text(
                            text = monthNames[uiState.currentMonth],
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Rounded.ChevronRight,
                            contentDescription = "下个月",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(40.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            ModernWeekDayHeader()

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) { page ->
                val pageOffset = page - initialPage
                val calendar = Calendar.getInstance().apply {
                    add(Calendar.MONTH, pageOffset)
                }
                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH)

                ModernCalendarGrid(
                    year = year,
                    month = month,
                    datesWithEvents = if (year == uiState.currentYear && month == uiState.currentMonth) datesWithEvents else emptySet(),
                    selectedDate = if (year == uiState.currentYear && month == uiState.currentMonth) uiState.selectedDate else null,
                    onDateClick = { day ->
                        viewModel.onDateSelected(year, month, day)
                    }
                )
            }

            if (uiState.selectedDate != null) {
                Spacer(modifier = Modifier.height(16.dp))
                
                SelectedDateEventsSection(
                    year = uiState.selectedYear,
                    month = uiState.selectedMonth,
                    day = uiState.selectedDate ?: 1,
                    events = selectedDateEvents,
                    onDeleteEvent = { showDeleteConfirmDialog = it }
                )
            }
        }
    }

    showDeleteConfirmDialog?.let { eventToDelete ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "确认删除",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("确定要删除这条「${eventToDelete.eventType.name}」记录吗？")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteEvent(eventToDelete)
                        showDeleteConfirmDialog = null
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text("取消")
                }
            }
        )
    }

    if (uiState.showAddEventDialog) {
        ModernAddEventDialog(
            eventTypes = uiState.eventTypes,
            onDismiss = { viewModel.onDismissAddEventDialog() },
            onAddEventType = { viewModel.onShowAddEventTypeDialog() },
            onConfirm = { eventTypeId, hour, minute, note ->
                viewModel.addEventRecord(eventTypeId, hour, minute, note)
            }
        )
    }

    if (uiState.showAddEventTypeDialog) {
        AddEventTypeDialog(
            onDismiss = { viewModel.onDismissAddEventTypeDialog() },
            onConfirm = { name, colorIndex ->
                viewModel.addEventType(name, colorIndex)
            }
        )
    }

    if (uiState.showMonthPickerDialog) {
        MonthPickerDialog(
            currentYear = uiState.currentYear,
            currentMonth = uiState.currentMonth,
            onDismiss = { viewModel.onDismissMonthPickerDialog() },
            onConfirm = { year, month ->
                viewModel.onMonthChange(year, month)
                coroutineScope.launch {
                    val targetCalendar = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                    }
                    val currentCalendar = Calendar.getInstance()
                    val monthsDiff = (targetCalendar.get(Calendar.YEAR) - currentCalendar.get(Calendar.YEAR)) * 12 +
                            (targetCalendar.get(Calendar.MONTH) - currentCalendar.get(Calendar.MONTH))
                    pagerState.scrollToPage(initialPage + monthsDiff)
                }
            }
        )
    }
}

@Composable
fun SelectedDateEventsSection(
    year: Int,
    month: Int,
    day: Int,
    events: List<EventWithType>,
    onDeleteEvent: (EventWithType) -> Unit
) {
    val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val dateString = "${month + 1}月${day}日"

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${year}年",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Text(
                text = "${events.size} 条记录",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (events.isEmpty()) {
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
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Rounded.EventNote,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = TextSecondary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "暂无记录",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "点击右下角 + 添加记录",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.height(200.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(events) { eventWithType ->
                    SelectedDateEventItem(
                        eventWithType = eventWithType,
                        timeString = dateFormat.format(eventWithType.record.timestamp),
                        onDelete = { onDeleteEvent(eventWithType) }
                    )
                }
            }
        }
    }
}

@Composable
fun SelectedDateEventItem(
    eventWithType: EventWithType,
    timeString: String,
    onDelete: () -> Unit
) {
    val eventColor = Color(eventWithType.eventType.color.toULong())

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = eventColor.copy(alpha = 0.1f)
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
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = TextSecondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = timeString,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                if (eventWithType.record.note.isNotEmpty()) {
                    Text(
                        text = eventWithType.record.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun ModernWeekDayHeader() {
    val weekDays = listOf("日", "一", "二", "三", "四", "五", "六")

    Row(modifier = Modifier.fillMaxWidth()) {
        weekDays.forEachIndexed { index, day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = if (index == 0 || index == 6) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                } else {
                    TextSecondary
                }
            )
        }
    }
}

@Composable
fun ModernCalendarGrid(
    year: Int,
    month: Int,
    datesWithEvents: Set<Int>,
    selectedDate: Int?,
    onDateClick: (Int) -> Unit
) {
    val calendar = Calendar.getInstance().apply {
        set(year, month, 1)
    }
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1

    val totalCells = firstDayOfWeek + daysInMonth
    val rows = (totalCells + 6) / 7

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(20.dp)
            )
            .padding(12.dp)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
    ) {
        repeat(rows) { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { col ->
                    val cellIndex = row * 7 + col
                    val day = cellIndex - firstDayOfWeek + 1

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (day in 1..daysInMonth) {
                            val isSelected = day == selectedDate
                            val hasEvents = datesWithEvents.contains(day)
                            val isToday = isToday(year, month, day)

                            ModernCalendarDayCell(
                                day = day,
                                isSelected = isSelected,
                                hasEvents = hasEvents,
                                isToday = isToday,
                                onClick = { onDateClick(day) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModernCalendarDayCell(
    day: Int,
    isSelected: Boolean,
    hasEvents: Boolean,
    isToday: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isSelected -> Primary
        isToday -> Primary.copy(alpha = 0.15f)
        else -> Color.Transparent
    }

    val textColor = when {
        isSelected -> Color.White
        isToday -> Primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    val borderColor = if (isToday && !isSelected) {
        Primary.copy(alpha = 0.3f)
    } else {
        Color.Transparent
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(1.dp, borderColor, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.toString(),
                color = textColor,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
            )
            if (hasEvents && !isSelected) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(Primary, CircleShape)
                )
            }
        }
    }
}

fun isToday(year: Int, month: Int, day: Int): Boolean {
    val today = Calendar.getInstance()
    return today.get(Calendar.YEAR) == year &&
            today.get(Calendar.MONTH) == month &&
            today.get(Calendar.DAY_OF_MONTH) == day
}

@Composable
fun ModernAddEventDialog(
    eventTypes: List<EventType>,
    onDismiss: () -> Unit,
    onAddEventType: () -> Unit,
    onConfirm: (Long, Int, Int, String) -> Unit
) {
    var selectedEventTypeId by remember { mutableStateOf<Long?>(null) }
    var hour by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) }
    var minute by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.MINUTE)) }
    var note by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "记录事件",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "选择事件类型",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (eventTypes.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "还没有创建事件类型",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = onAddEventType,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("创建事件类型")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.height(200.dp)
                    ) {
                        items(eventTypes) { eventType ->
                            val isSelected = selectedEventTypeId == eventType.id
                            val eventColor = Color(eventType.color.toULong())

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { selectedEventTypeId = eventType.id },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) {
                                        eventColor.copy(alpha = 0.2f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    }
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
                                            .size(12.dp)
                                            .background(eventColor, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = eventType.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                if (eventTypes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "时间",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "时",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { if (hour > 0) hour-- },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            CircleShape
                                        )
                                ) {
                                    Icon(
                                        Icons.Rounded.ChevronLeft,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = hour.toString().padStart(2, '0'),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                                IconButton(
                                    onClick = { if (hour < 23) hour++ },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            CircleShape
                                        )
                                ) {
                                    Icon(
                                        Icons.Rounded.ChevronRight,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "分",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { if (minute > 0) minute-- else minute = 59 },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            CircleShape
                                        )
                                ) {
                                    Icon(
                                        Icons.Rounded.ChevronLeft,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = minute.toString().padStart(2, '0'),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                                IconButton(
                                    onClick = { if (minute < 59) minute++ else minute = 0 },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            CircleShape
                                        )
                                ) {
                                    Icon(
                                        Icons.Rounded.ChevronRight,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("备注 (可选)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        maxLines = 2
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("取消")
                    }

                    Button(
                        onClick = {
                            selectedEventTypeId?.let { id ->
                                onConfirm(id, hour, minute, note)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = selectedEventTypeId != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Primary
                        )
                    ) {
                        Text("确定")
                    }
                }
            }
        }
    }
}

@Composable
fun AddEventTypeDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedColorIndex by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "新建事件类型",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "选择颜色",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EventColors.forEachIndexed { index, color ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(color, CircleShape)
                                .clickable { selectedColorIndex = index }
                                .then(
                                    if (selectedColorIndex == index) {
                                        Modifier.border(
                                            3.dp,
                                            MaterialTheme.colorScheme.onSurface,
                                            CircleShape
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, selectedColorIndex) },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun MonthPickerDialog(
    currentYear: Int,
    currentMonth: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val currentCalendar = Calendar.getInstance()
    val currentYearNow = currentCalendar.get(Calendar.YEAR)
    
    var selectedYear by remember { mutableIntStateOf(currentYear) }
    var selectedMonth by remember { mutableIntStateOf(currentMonth) }
    
    val monthNames = listOf(
        "一月", "二月", "三月", "四月", "五月", "六月",
        "七月", "八月", "九月", "十月", "十一月", "十二月"
    )
    
    val years = (currentYearNow - 10..currentYearNow + 10).toList()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "选择月份",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Rounded.ChevronLeft,
                            contentDescription = "关闭",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "年份",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (selectedYear > years.first()) selectedYear-- },
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Rounded.ChevronLeft,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = "${selectedYear}年",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    IconButton(
                        onClick = { if (selectedYear < years.last()) selectedYear++ },
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "月份",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(0 to 3, 3 to 6, 6 to 9, 9 to 12).forEach { range ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            for (index in range.first until range.second) {
                                val isSelected = selectedMonth == index
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clickable { selectedMonth = index },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) {
                                            Primary
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = monthNames[index],
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) {
                                                Color.White
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("取消")
                    }

                    Button(
                        onClick = { onConfirm(selectedYear, selectedMonth) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Primary
                        )
                    ) {
                        Text("确定")
                    }
                }
            }
        }
    }
}
