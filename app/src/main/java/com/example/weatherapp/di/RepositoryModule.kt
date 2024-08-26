package com.example.weatherapp.di

import com.example.weatherapp.data.network.repository.WeatherDataRepository
import org.koin.dsl.module

val repositoryModule = module {
    single { WeatherDataRepository(weatherApi = get()) }
}