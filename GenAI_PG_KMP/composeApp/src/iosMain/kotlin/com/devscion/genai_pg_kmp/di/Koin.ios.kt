package com.devscion.genai_pg_kmp.di

import com.devscion.genai_pg_kmp.data.iOSFilePicker
import com.devscion.genai_pg_kmp.data.iOSModelSettings
import com.devscion.genai_pg_kmp.data.model_managers.LiteRT_LMModelManager
import com.devscion.genai_pg_kmp.data.model_managers.MediaPipeModelManager
import com.devscion.genai_pg_kmp.domain.FilePicker
import com.devscion.genai_pg_kmp.domain.LLMModelManager
import com.devscion.genai_pg_kmp.domain.ModelPathProvider
import com.devscion.genai_pg_kmp.domain.ModelPathProviderIOS
import com.devscion.genai_pg_kmp.domain.ModelSettings
import com.devscion.genai_pg_kmp.domain.PlatformDetailProvider
import com.devscion.genai_pg_kmp.domain.PlatformDetailProviderIOS
import com.devscion.genai_pg_kmp.domain.model.ModelManagerRuntime
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

actual val platformKoinModule = module {

    factoryOf(::LiteRT_LMModelManager) {
        qualifier = named(ModelManagerRuntime.LITE_RT_LM)
    } bind LLMModelManager::class

    factoryOf(::MediaPipeModelManager) {
        qualifier = named(ModelManagerRuntime.MEDIA_PIPE)
    } bind LLMModelManager::class

    factoryOf(::ModelPathProviderIOS) bind ModelPathProvider::class

    singleOf(::PlatformDetailProviderIOS) bind PlatformDetailProvider::class

    // File Picker
    factoryOf(::iOSFilePicker) bind FilePicker::class

    // Settings
    single<ModelSettings> { iOSModelSettings() }
}