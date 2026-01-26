package com.devscion.genai_pg_kmp.di

import com.devscion.genai_pg_kmp.data.model_managers.LiteRTLM_ModelManager
import com.devscion.genai_pg_kmp.data.model_managers.MediaPipeModelManager
import com.devscion.genai_pg_kmp.domain.LLMModelManager
import com.devscion.genai_pg_kmp.domain.PlatformDetailProvider
import com.devscion.genai_pg_kmp.domain.PlatformDetailProviderAndroid
import com.devscion.genai_pg_kmp.domain.model.ModelManagerType
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

actual val platformKoinModule = module {

    singleOf(::LiteRTLM_ModelManager) {
        qualifier = named(ModelManagerType.LITE_RT_LM)
    } bind LLMModelManager::class

    singleOf(::MediaPipeModelManager) {
        qualifier = named(ModelManagerType.MEDIA_PIPE)
    } bind LLMModelManager::class


    singleOf(::PlatformDetailProviderAndroid) bind PlatformDetailProvider::class
}