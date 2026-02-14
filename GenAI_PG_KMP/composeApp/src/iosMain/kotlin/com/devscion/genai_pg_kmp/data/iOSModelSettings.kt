package com.devscion.genai_pg_kmp.data

import com.devscion.genai_pg_kmp.domain.ModelSettings
import platform.Foundation.NSUserDefaults

class iOSModelSettings : ModelSettings {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun savePath(id: String, path: String?) {
        if (path == null) {
            defaults.removeObjectForKey(id)
        } else {
            defaults.setObject(path, id)
        }
    }

    override fun getPath(id: String): String? {
        return defaults.stringForKey(id)
    }
}
