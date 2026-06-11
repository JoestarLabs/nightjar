package com.bl4ckswordsman.nightjar.di

import android.content.Context
import com.bl4ckswordsman.nightjar.data.TimerPreferencesDataSource
import com.bl4ckswordsman.nightjar.data.TimerRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideTimerPreferencesDataSource(
        @ApplicationContext context: Context
    ): TimerPreferencesDataSource = TimerPreferencesDataSource(context)

    @Provides
    @Singleton
    fun provideTimerRepository(
        preferencesDataSource: TimerPreferencesDataSource
    ): TimerRepository = TimerRepository(preferencesDataSource)
}
