package com.example.weatherapp.utils

import android.app.Application
import com.example.weatherapp.di.networkModule
import com.example.weatherapp.di.repositoryModule
import com.example.weatherapp.di.serializerModule
import com.example.weatherapp.di.storageModule
import com.example.weatherapp.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@App)
            modules(
                listOf(
                    repositoryModule,
                    viewModelModule,
                    storageModule,
                    serializerModule,
                    networkModule
                )
            )
        }
    }
}