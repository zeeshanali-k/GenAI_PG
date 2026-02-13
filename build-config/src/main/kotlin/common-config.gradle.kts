import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.compose")
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.native.cocoapods")
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    applyDefaultHierarchyTemplate()

    androidLibrary {
        compileSdk = 36
        minSdk = 24

        compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
        androidResources.enable = true
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        summary = "Some description for the Shared Module"
        homepage = "Link to the Shared Module homepage"
        version = "1.0"
        ios.deploymentTarget = "15.0"
        podfile = project.file("../iosApp/Podfile")

        framework {
            isStatic = true
        }
    }

    sourceSets {

        androidMain.dependencies {
            implementation(compose.preview)
        }


        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)

            implementation("org.jetbrains.compose.ui:ui-tooling-preview:1.10.0")
        }
    }

}

compose.resources {
    publicResClass = true
}