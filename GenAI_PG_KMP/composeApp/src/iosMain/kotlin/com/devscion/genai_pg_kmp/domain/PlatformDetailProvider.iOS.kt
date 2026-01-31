package com.devscion.genai_pg_kmp.domain

import com.devscion.genai_pg_kmp.domain.model.Platform

class PlatformDetailProviderIOS : PlatformDetailProvider {

    override fun getPlatform(): Platform = Platform.IOS

}