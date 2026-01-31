package com.devscion.genai_pg_kmp.di

import com.devscion.genai_pg_kmp.ui.ChatViewModel
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module


expect val platformKoinModule: Module

fun startKoin(platformConfig: KoinApplication.() -> Unit) {
    startKoin {
        platformConfig()
        modules(
            platformKoinModule
                    + viewModelModule
        )
    }
}


val viewModelModule = module {
    viewModelOf(::ChatViewModel)
}