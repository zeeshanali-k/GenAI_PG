plugins {
    id("common-config")
}

kotlin {

    androidLibrary {
        namespace = "com.devscion.genai_pg_kmp"
    }



    cocoapods {

        framework {
            baseName = "composeApp"
        }

        pod("MediaPipeTasksGenAI") {
            version = "0.10.18"
            extraOpts += listOf("-compiler-option", "-fmodules")
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.koin.android)
            implementation(libs.koin.compose)

            /** Gen AI Libraries **/

            //  MediaPipe
            implementation(libs.mediapipe.tasks.genai)
            //    LiteRT-LM
            implementation(libs.litertlm.android)
            //  Google AI Edge RAG SDK (for MediaPipe & LiteRT-LM)
            implementation(libs.localagents.rag)


            /** Gen AI Libraries **/
        }
        commonMain.dependencies {
//            api(compose.runtime)
//            api(compose.foundation)
//            implementation(compose.material3)
//            api(compose.ui)
//            api(compose.components.resources)
//            api(compose.preview)
//            api(libs.ui.tooling.preview)

            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kermit.logger)
            implementation(libs.koin.core)
            implementation(libs.koin.compose.viewmodel.navigation)
            implementation(libs.material.icons.core)
            //Llamatik

            implementation(libs.llamatik)
        }
//        commonTest.dependencies {
//            implementation(libs.kotlin.test)
//        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.ui.tooling)
}

//compose.resources {
//    publicResClass = true
//}