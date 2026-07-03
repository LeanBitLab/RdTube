package com.example.reddittube

import android.app.Application
import com.example.reddittube.di.AppContainer

// ponytail: Application subclass for manual DI container initialization
class RedditTubeApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
