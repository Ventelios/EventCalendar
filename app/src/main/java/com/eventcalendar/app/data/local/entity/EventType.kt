package com.eventcalendar.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "event_types")
data class EventType(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val color: Long = 0xFF6B7280L,
    val icon: String = "event",
    val createdAt: Long = System.currentTimeMillis()
)
