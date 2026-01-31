package com.devscion.genai_pg_kmp.domain

import com.devscion.genai_pg_kmp.domain.model.Platform

interface PlatformDetailProvider {

    fun getPlatform() : Platform

}