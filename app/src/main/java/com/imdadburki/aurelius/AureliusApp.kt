package com.imdadburki.aurelius

import android.app.Application
import com.imdadburki.aurelius.core.di.AppContainer

class AureliusApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
