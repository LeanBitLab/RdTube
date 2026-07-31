package com.lean.reddittube.di

import android.content.Context
import com.lean.reddittube.data.DataRepository
import com.lean.reddittube.data.DefaultDataRepository

// ponytail: lightweight manual DI — no framework dependencies
class AppContainer(context: Context) {
    val repository: DataRepository by lazy {
        DefaultDataRepository(context.applicationContext)
    }
}
