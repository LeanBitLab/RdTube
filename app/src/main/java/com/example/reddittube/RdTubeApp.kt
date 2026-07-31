package com.lean.reddittube

import android.app.Application
import com.lean.reddittube.di.AppContainer

// ponytail: Application subclass for manual DI container initialization
class RdTubeApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
