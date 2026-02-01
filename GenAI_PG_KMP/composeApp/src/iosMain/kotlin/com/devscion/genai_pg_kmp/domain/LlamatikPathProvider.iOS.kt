package com.devscion.genai_pg_kmp.domain

import platform.Foundation.NSBundle

class LlamatikPathProviderIOS() : LlamatikPathProvider {

    override fun getPath(modelName: String): String? {
        return try {
            val extension = modelName.split(".").lastOrNull() ?: return null
            println(
                "LlamatikModelManager-> getPath: ${modelName.removeSuffix(extension)} :: $extension"
            )
            return NSBundle.mainBundle.pathForResource(
                modelName.removeSuffix(".${extension}"),
                ofType = extension
            )
        } catch (e: Exception) {
            println("LlamatikModelManager-> Error: ${e.message} :: ${e.cause}")
            null
        }
    }

}