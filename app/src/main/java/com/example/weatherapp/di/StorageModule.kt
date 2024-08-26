package com.example.weatherapp.di

import com.example.weatherapp.storage.SharedPreferenceManager
import org.koin.dsl.module

val storageModule = module {
    single { SharedPreferenceManager(context = get(), gson = get()) }
}