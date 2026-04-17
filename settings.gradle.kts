pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "stack"

include(
    ":app",
    ":core:common",
    ":core:design",
    ":core:ui",
    ":core:database",
    ":core:datastore",
    ":core:domain",
    ":core:audio",
    ":feature:library",
    ":feature:player",
    ":feature:playlist",
    ":feature:search",
    ":feature:settings",
)
