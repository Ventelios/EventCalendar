package com.eventcalendar.app.di

import android.content.Context
import androidx.room.Room
import com.eventcalendar.app.data.local.EventDatabase
import com.eventcalendar.app.data.local.dao.EventTypeDao
import com.eventcalendar.app.data.local.dao.EventRecordDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideEventDatabase(@ApplicationContext context: Context): EventDatabase {
        return Room.databaseBuilder(
            context,
            EventDatabase::class.java,
            "event_database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideEventTypeDao(database: EventDatabase): EventTypeDao {
        return database.eventTypeDao()
    }

    @Provides
    @Singleton
    fun provideEventRecordDao(database: EventDatabase): EventRecordDao {
        return database.eventRecordDao()
    }
}
