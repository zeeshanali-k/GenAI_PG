plugins {
    id("common-config")
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.serialization)
}

room {
    schemaDirectory("$projectDir/schemas")
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
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)

            //Llamatik
            implementation(libs.llamatik)

            // Moko Permissions
            implementation(libs.moko.permissions.compose)
            implementation(libs.moko.permissions.storage)

            // Room
            implementation(libs.room.runtime)
            implementation(libs.sqlite.bundled)
        }
//        commonTest.dependencies {
//            implementation(libs.kotlin.test)
//        }
    }
}

dependencies {

    add("kspAndroid", libs.room.compiler)
    add("kspIosX64", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
}

//compose.resources {
//    publicResClass = true
//}