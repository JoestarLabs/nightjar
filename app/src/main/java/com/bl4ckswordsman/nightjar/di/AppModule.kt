package com.bl4ckswordsman.nightjar.di

import com.bl4ckswordsman.nightjar.service.AndroidSunsetAudioPlayer
import com.bl4ckswordsman.nightjar.service.SunsetAudioPlayer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindSunsetAudioPlayer(
        impl: AndroidSunsetAudioPlayer
    ): SunsetAudioPlayer
}
