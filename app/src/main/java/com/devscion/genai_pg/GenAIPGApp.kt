package com.devscion.genai_pg

import android.app.Application
import com.devscion.genai_pg_kmp.di.startKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.logger.Level

class GenAIPGApp : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(applicationContext)
            androidLogger(Level.DEBUG)
        }
    }

}