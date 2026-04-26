// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinCocoapods) apply false
    alias(libs.plugins.serialization) apply false
}


//task downloadMobileBertModel(type: Download) {
//    src 'https://storage.googleapis.com/mediapipe-models/text_embedder/bert_embedder/float32/1/bert_embedder.tflite'
//    dest project.ext.ASSET_DIR + '/mobile_bert.tflite'
//    overwrite false
//}
//
//task downloadAverageWordModel(type: Download) {
//    src 'https://storage.googleapis.com/mediapipe-models/text_embedder/average_word_embedder/float32/1/average_word_embedder.tflite'
//    dest project.ext.ASSET_DIR + '/average_word.tflite'
//    overwrite false
//}
//
//preBuild.dependsOn downloadMobileBertModel, downloadAverageWordModel