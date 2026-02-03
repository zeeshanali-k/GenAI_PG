package com.devscion.genai_pg_kmp.ui.screens

import com.devscion.genai_pg_kmp.domain.model.ModelManagerOption
import kotlinx.serialization.Serializable


@Serializable
object Chat

//
//title = TODO(),
//onDismiss = TODO(),
//options = TODO(),
//getTitle = TODO(),
//isSelected = TODO(),
//selectedOption = TODO(),
//onRuntimeSelection = TODO()
@Serializable
data class RuntimeDialog(
    val title: String,
    val options: List<ModelManagerOption>,
    val selectedOption: ModelManagerOption?,
)