package com.devscion.genai_pg_kmp.domain

import com.devscion.genai_pg_kmp.domain.model.Platform

class PlatformDetailProviderAndroid : PlatformDetailProvider {

    override fun getPlatform(): Platform = Platform.ANDROID

}