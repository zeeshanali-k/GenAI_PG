package com.devscion.genai_pg_kmp.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.devscion.genai_pg_kmp.data.AndroidFilePicker
import com.devscion.genai_pg_kmp.domain.FilePicker

@Composable
actual fun BindFilePicker(filePicker: FilePicker) {
    if (filePicker is AndroidFilePicker) {
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->
            filePicker.onFilePicked(uri)
        }
        
        LaunchedEffect(filePicker) {
            filePicker.bind(launcher)
        }
    }
}
