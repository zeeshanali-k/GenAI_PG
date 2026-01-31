package com.devscion.genai_pg_kmp

import androidx.compose.ui.window.ComposeUIViewController
import com.devscion.genai_pg_kmp.di.startKoin
import com.devscion.genai_pg_kmp.domain.SwiftModelManager
import org.koin.dsl.module

fun MainViewController(swiftModelManager: SwiftModelManager) = ComposeUIViewController {
    startKoin {
        modules(
            module {
                single { swiftModelManager }
            }
        )
    }
    App()
}