package com.devscion.genai_pg_kmp.di

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.devscion.genai_pg_kmp.data.LlamatikModelManager
import com.devscion.genai_pg_kmp.data.database.AppDatabase
import com.devscion.genai_pg_kmp.data.database.DatabaseBuilder
import com.devscion.genai_pg_kmp.data.rag.LlamatikRAGManager
import com.devscion.genai_pg_kmp.data.repository.ChatRepositoryImpl
import com.devscion.genai_pg_kmp.domain.LLMModelManager
import com.devscion.genai_pg_kmp.domain.model.ModelManagerRuntime
import com.devscion.genai_pg_kmp.domain.rag.RAGManager
import com.devscion.genai_pg_kmp.domain.repository.ChatRepository
import com.devscion.genai_pg_kmp.ui.ChatViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module


expect val platformKoinModule: Module

fun startKoin(platformConfig: KoinApplication.() -> Unit) {
    startKoin {
        platformConfig()
        modules(
            platformKoinModule
                    + viewModelModule
                    + modelManagerModule
                    + databaseModule
                    + repositoryModule
        )
    }
}

val modelManagerModule = module {
    // RAG manager for Llamatik
    factoryOf(::LlamatikRAGManager) {
        qualifier = named(ModelManagerRuntime.LlamaTIK)
    } bind RAGManager::class

    // Llamatik model manager with RAG support
    factory<LLMModelManager>(named(ModelManagerRuntime.LlamaTIK)) {
        LlamatikModelManager(get(named(ModelManagerRuntime.LlamaTIK)))
    }
}

val viewModelModule = module {
    viewModelOf(::ChatViewModel)
}

val databaseModule = module {
    single<AppDatabase> {
        get<DatabaseBuilder>().get()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }
    single { get<AppDatabase>().chatDao() }
    single { get<AppDatabase>().messageDao() }
}

val repositoryModule = module {
    singleOf(::ChatRepositoryImpl) bind ChatRepository::class
}