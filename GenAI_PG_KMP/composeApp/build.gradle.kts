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

//        pod("MediaPipeTasksGenAI") {
//            version = "0.10.24"
//            extraOpts += listOf("-compiler-option", "-fmodules")
//        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.koin.android)
            implementation(libs.koin.compose)

            /** Gen AI Libraries **/

            //  MediaPipe
            implementation(libs.mediapipe.tasks.genai)
            implementation(libs.localagents.rag)
            implementation(libs.tasks.vision)
//            implementation(libs.tasks.text)
//            implementation(libs.tasks.audio)
            //    LiteRT-LM
            implementation(libs.litertlm.android)
            //  Google AI Edge RAG SDK for MediaPipe


            /** Gen AI Libraries **/
        }
        commonMain.dependencies {

            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kermit.logger)
            implementation(libs.koin.core)
            implementation(libs.koin.compose.viewmodel.navigation)
            implementation(libs.material.icons.core)
            //Llamatik

            implementation(libs.llamatik)

            // Moko Permissions
            implementation("dev.icerock.moko:permissions-compose:0.18.0")
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