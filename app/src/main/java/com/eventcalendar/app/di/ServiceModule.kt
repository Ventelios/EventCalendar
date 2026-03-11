package com.eventcalendar.app.di

import com.eventcalendar.app.data.service.GitHubReleaseService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {
    
    @Provides
    @Singleton
    fun provideGitHubReleaseService(): GitHubReleaseService {
        return GitHubReleaseService()
    }
}
