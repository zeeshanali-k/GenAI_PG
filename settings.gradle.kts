pluginManagement {
    includeBuild("build-config")
    repositories {
        google {
            content {
//                includeGroupByRegex("com\\.android.*")
//                includeGroupByRegex("com\\.google.*")
//                includeGroupByRegex("androidx.*")

                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
//        google()
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "GenAI_PG"
include(":app")
include(":GenAI_PG_KMP:composeApp")
