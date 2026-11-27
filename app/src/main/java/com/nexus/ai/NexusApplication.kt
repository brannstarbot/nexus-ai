package com.nexus.ai

import android.app.Application
import com.nexus.ai.di.AppContainer

class NexusApplication : Application() {

    /** Manual dependency container — sederhana, tanpa Hilt, mudah diganti. */
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
