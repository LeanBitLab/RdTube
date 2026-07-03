package com.example.reddittube.di

import android.content.Context
import com.example.reddittube.data.DataRepository
import com.example.reddittube.data.DefaultDataRepository

// ponytail: lightweight manual DI — no framework dependencies
class AppContainer(context: Context) {
    val repository: DataRepository by lazy {
        DefaultDataRepository(context.applicationContext)
    }
}
