package com.devscion.genai_pg_kmp.di

import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import co.touchlab.kermit.Logger
import com.devscion.genai_pg_kmp.data.LlamatikModelManager
import com.devscion.genai_pg_kmp.data.database.AppDatabase
import com.devscion.genai_pg_kmp.data.database.DatabaseBuilder
import com.devscion.genai_pg_kmp.data.document.DefaultDocumentTextParser
import com.devscion.genai_pg_kmp.data.rag.LlamatikRAGManager
import com.devscion.genai_pg_kmp.data.repository.ChatRepositoryImpl
import com.devscion.genai_pg_kmp.data.repository.VectorDBRepositoryImpl
import com.devscion.genai_pg_kmp.domain.LLMRuntimeManager
import com.devscion.genai_pg_kmp.domain.document.DocumentTextParser
import com.devscion.genai_pg_kmp.domain.model.ModelManagerRuntime
import com.devscion.genai_pg_kmp.domain.rag.RAGManager
import com.devscion.genai_pg_kmp.domain.repository.ChatRepository
import com.devscion.genai_pg_kmp.domain.repository.VectorDBRepository
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
    factory<LLMRuntimeManager>(named(ModelManagerRuntime.LlamaTIK)) {
        LlamatikModelManager(get(named(ModelManagerRuntime.LlamaTIK)))
    }
}

val viewModelModule = module {
    viewModelOf(::ChatViewModel)
}

val databaseModule = module {
    single<AppDatabase> {
        get<DatabaseBuilder>().get()
            .setQueryCoroutineContext(Dispatchers.IO)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(connection: SQLiteConnection) {
                    Logger.d(null, "RoomDBCallBack") {
                        "onCreate-> ${connection.inTransaction()}"
                    }
                    // vec0 virtual tables are NOT managed by Room's schema system
                    // you can use Room with some additional logic and a blob column for embeddings
                    connection.execSQL(
                        """CREATE VIRTUAL TABLE IF NOT EXISTS doc_embeddings USING vec0(       
                           embedding float[512],
                           +content TEXT,
                           chat_id TEXT,
                           file_name TEXT
                        )"""
                    )
                    Logger.d(null, "RoomDBCallBack") {
                        "onCreate-> execSQL done    "
                    }
                }
            })
            .build()
    }
    single { get<AppDatabase>().chatDao() }
    single { get<AppDatabase>().messageDao() }
    single { get<AppDatabase>().vectorEmbeddingsDao() }
}

val repositoryModule = module {
    singleOf(::DefaultDocumentTextParser) bind DocumentTextParser::class
    singleOf(::ChatRepositoryImpl) bind ChatRepository::class
    singleOf(::VectorDBRepositoryImpl) bind VectorDBRepository::class
}
