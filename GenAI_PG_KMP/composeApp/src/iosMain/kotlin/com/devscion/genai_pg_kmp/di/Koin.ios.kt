package com.devscion.genai_pg_kmp.di

import com.devscion.genai_pg_kmp.data.database.DatabaseBuilder
import com.devscion.genai_pg_kmp.data.iOSFilePicker
import com.devscion.genai_pg_kmp.data.iOSModelSettings
import com.devscion.genai_pg_kmp.data.model_managers.LiteRT_LMModelManager
import com.devscion.genai_pg_kmp.data.model_managers.MediaPipeModelManager
import com.devscion.genai_pg_kmp.data.rag.MediaPipeRAGManager
import com.devscion.genai_pg_kmp.domain.FilePicker
import com.devscion.genai_pg_kmp.domain.LLMRuntimeManager
import com.devscion.genai_pg_kmp.domain.ModelPathProvider
import com.devscion.genai_pg_kmp.domain.ModelPathProviderIOS
import com.devscion.genai_pg_kmp.domain.ModelSettings
import com.devscion.genai_pg_kmp.domain.PlatformDetailProvider
import com.devscion.genai_pg_kmp.domain.PlatformDetailProviderIOS
import com.devscion.genai_pg_kmp.domain.model.ModelManagerRuntime
import com.devscion.genai_pg_kmp.domain.rag.RAGManager
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

actual val platformKoinModule: Module = module {

    singleOf(::DatabaseBuilder)


    // RAG manager for Android (MediaPipe and LiteRT-LM)
    factoryOf(::MediaPipeRAGManager) {
        qualifier = named(ModelManagerRuntime.MEDIA_PIPE)
    } bind RAGManager::class

    factoryOf(::LiteRT_LMModelManager) {
        qualifier = named(ModelManagerRuntime.LITE_RT_LM)
    } bind LLMRuntimeManager::class

    factory(named(ModelManagerRuntime.MEDIA_PIPE)) {
        MediaPipeModelManager(get(), get(named(ModelManagerRuntime.MEDIA_PIPE)))
    } bind LLMRuntimeManager::class

    factoryOf(::ModelPathProviderIOS) bind ModelPathProvider::class

    singleOf(::PlatformDetailProviderIOS) bind PlatformDetailProvider::class

    // File Picker
    factoryOf(::iOSFilePicker) bind FilePicker::class

    // Settings
    single<ModelSettings> { iOSModelSettings() }
}