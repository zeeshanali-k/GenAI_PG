package com.devscion.genai_pg_kmp.di

import com.devscion.genai_pg_kmp.data.model_managers.LiteRTLM_ModelManager
import com.devscion.genai_pg_kmp.data.model_managers.MediaPipeModelManager
import com.devscion.genai_pg_kmp.data.rag.AIEdgeRAGManager
import com.devscion.genai_pg_kmp.data.rag.LlamatikRAGManager
import com.devscion.genai_pg_kmp.domain.LLMModelManager
import com.devscion.genai_pg_kmp.domain.LlamatikPathProvider
import com.devscion.genai_pg_kmp.domain.LlamatikPathProviderAndroid
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
    factoryOf(::AIEdgeRAGManager) {
        qualifier = named(ModelManagerRuntime.MEDIA_PIPE)
    } bind RAGManager::class
    factoryOf(::LlamatikRAGManager) {
        qualifier = named(ModelManagerRuntime.LlamaTIK)
    } bind RAGManager::class


    //Model Managers
    factory(named(ModelManagerRuntime.LITE_RT_LM)) {
        LiteRTLM_ModelManager(get(), get(named(ModelManagerRuntime.LlamaTIK)))
    } bind LLMModelManager::class

    factory(named(ModelManagerRuntime.MEDIA_PIPE)) {
        MediaPipeModelManager(androidContext(), get(), get(named(ModelManagerRuntime.MEDIA_PIPE)))
    } bind LLMModelManager::class

    //Misc
    factoryOf(::LlamatikPathProviderAndroid) bind LlamatikPathProvider::class
    singleOf(::PlatformDetailProviderAndroid) bind PlatformDetailProvider::class
}