plugins {
    `kotlin-dsl`
}


dependencies {
//    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.0")
    implementation(libs.plugins.kotlinMultiplatform.dependency)
    implementation(libs.plugins.composeCompiler.dependency)
    implementation(libs.plugins.composeMultiplatform.dependency)
    implementation(libs.plugins.androidLibrary.dependency)
    implementation(libs.plugins.kotlinCocoapods.dependency)
    implementation(libs.plugins.serialization.dependency)
}


val Provider<PluginDependency>.dependency
    get() = map { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" }