package com.devscion.genai_pg_kmp.di

import com.devscion.genai_pg_kmp.data.model_managers.LiteRT_LMModelManager
import com.devscion.genai_pg_kmp.data.model_managers.MediaPipeModelManager
import com.devscion.genai_pg_kmp.domain.LLMModelManager
import com.devscion.genai_pg_kmp.domain.PlatformDetailProvider
import com.devscion.genai_pg_kmp.domain.PlatformDetailProviderIOS
import com.devscion.genai_pg_kmp.domain.model.ModelManagerType
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

actual val platformKoinModule = module {

    singleOf(::LiteRT_LMModelManager) {
        qualifier = named(ModelManagerType.LITE_RT_LM)
    } bind LLMModelManager::class

    single(qualifier = named(ModelManagerType.MEDIA_PIPE)) {
        MediaPipeModelManager(get())
    } bind LLMModelManager::class

    singleOf(::PlatformDetailProviderIOS) bind PlatformDetailProvider::class
}