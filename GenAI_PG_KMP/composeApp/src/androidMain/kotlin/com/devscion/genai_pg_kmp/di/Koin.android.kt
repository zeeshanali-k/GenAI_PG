package com.devscion.genai_pg_kmp.di

import com.devscion.genai_pg_kmp.data.model_managers.LiteRTLM_ModelManager
import com.devscion.genai_pg_kmp.data.model_managers.MediaPipeModelManager
import com.devscion.genai_pg_kmp.domain.LLMModelManager
import com.devscion.genai_pg_kmp.domain.LlamatikPathProvider
import com.devscion.genai_pg_kmp.domain.LlamatikPathProviderAndroid
import com.devscion.genai_pg_kmp.domain.PlatformDetailProvider
import com.devscion.genai_pg_kmp.domain.PlatformDetailProviderAndroid
import com.devscion.genai_pg_kmp.domain.model.ModelManagerRuntime
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

actual val platformKoinModule = module {

    factoryOf(::LiteRTLM_ModelManager) {
        qualifier = named(ModelManagerRuntime.LITE_RT_LM)
    } bind LLMModelManager::class

    factoryOf(::MediaPipeModelManager) {
        qualifier = named(ModelManagerRuntime.MEDIA_PIPE)
    } bind LLMModelManager::class

    factoryOf(::LlamatikPathProviderAndroid) bind LlamatikPathProvider::class


    singleOf(::PlatformDetailProviderAndroid) bind PlatformDetailProvider::class
}