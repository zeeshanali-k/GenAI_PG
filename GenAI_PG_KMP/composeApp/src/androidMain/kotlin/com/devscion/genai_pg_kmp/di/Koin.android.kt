package com.devscion.genai_pg_kmp.di

import com.devscion.genai_pg_kmp.data.AndroidFilePicker
import com.devscion.genai_pg_kmp.data.AndroidModelSettings
import com.devscion.genai_pg_kmp.data.model_managers.LiteRTLM_ModelManager
import com.devscion.genai_pg_kmp.data.model_managers.MediaPipeModelManager
import com.devscion.genai_pg_kmp.data.rag.MediaPipeRAGManager
import com.devscion.genai_pg_kmp.domain.FilePicker
import com.devscion.genai_pg_kmp.domain.LLMModelManager
import com.devscion.genai_pg_kmp.domain.ModelPathProvider
import com.devscion.genai_pg_kmp.domain.ModelPathProviderAndroid
import com.devscion.genai_pg_kmp.domain.ModelSettings
import com.devscion.genai_pg_kmp.domain.PlatformDetailProvider
import com.devscion.genai_pg_kmp.domain.PlatformDetailProviderAndroid
import com.devscion.genai_pg_kmp.domain.model.ModelManagerRuntime
import com.devscion.genai_pg_kmp.domain.rag.RAGManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

actual val platformKoinModule = module {

    // RAG manager for Android (MediaPipe and LiteRT-LM)
    factoryOf(::MediaPipeRAGManager) {
        qualifier = named(ModelManagerRuntime.MEDIA_PIPE)
    } bind RAGManager::class

    //Model Managers
    factory(named(ModelManagerRuntime.LITE_RT_LM)) {
        LiteRTLM_ModelManager(get(named(ModelManagerRuntime.LlamaTIK)))
    } bind LLMModelManager::class

    factory(named(ModelManagerRuntime.MEDIA_PIPE)) {
        MediaPipeModelManager(androidContext(), get(named(ModelManagerRuntime.MEDIA_PIPE)), get())
    } bind LLMModelManager::class

    //Misc
    factoryOf(::ModelPathProviderAndroid) bind ModelPathProvider::class
    singleOf(::PlatformDetailProviderAndroid) bind PlatformDetailProvider::class

    // File Picker
    single { AndroidFilePicker(androidContext()) } bind FilePicker::class

    // Settings
    single<ModelSettings> { AndroidModelSettings(androidContext()) }
}