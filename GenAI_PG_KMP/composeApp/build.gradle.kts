import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.kotlinCocoapods)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    applyDefaultHierarchyTemplate()
    androidLibrary {
        namespace = "com.devscion.genai_pg_kmp"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
        androidResources.enable = true
    }


    iosX64()
    iosArm64()
    iosSimulatorArm64()
//    listOf(
//    ).forEach { iosTarget ->
//        iosTarget.binaries.framework {
//            baseName = "ComposeApp"
//            isStatic = true
//        }
//    }


    cocoapods {
        summary = "Some description for the Shared Module"
        homepage = "Link to the Shared Module homepage"
        version = "1.0"
        ios.deploymentTarget = "15.0"
        podfile = project.file("../iosApp/Podfile")

        framework {
            baseName = "composeApp"
            isStatic = true
        }
//        pod("MediaPipeTasksGenAIC") {
//            version = "0.10.14"
//            extraOpts += listOf("-compiler-option", "-fmodules")
//        }
        pod("MediaPipeTasksGenAI") {
            version = "0.10.14"
            extraOpts += listOf("-compiler-option", "-fmodules")
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.koin.android)
            implementation(libs.koin.compose)

            /** Gen AI Libraries **/

            //  MediaPipe
            implementation(libs.mediapipe.tasks.genai)
            //    LiteRT-LM
            implementation(libs.litertlm.android)

            /** Gen AI Libraries **/
        }
        commonMain.dependencies {
            api(compose.runtime)
            api(compose.foundation)
            implementation(compose.material3)
            api(compose.ui)
            api(compose.components.resources)
            api(compose.preview)
            api(libs.ui.tooling.preview)

            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kermit.logger)
            implementation(libs.koin.core)
            implementation(libs.koin.compose.viewmodel.navigation)
            implementation(libs.material.icons.core)
        }
//        commonTest.dependencies {
//            implementation(libs.kotlin.test)
//        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.ui.tooling)
}

compose.resources {
    publicResClass = true
}