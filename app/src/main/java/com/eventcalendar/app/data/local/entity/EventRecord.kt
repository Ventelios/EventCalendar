package com.eventcalendar.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "event_records",
    foreignKeys = [
        ForeignKey(
            entity = EventType::class,
            parentColumns = ["id"],
            childColumns = ["eventTypeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("eventTypeId"), Index("timestamp")]
)
data class EventRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val eventTypeId: Long,
    val timestamp: Long,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
