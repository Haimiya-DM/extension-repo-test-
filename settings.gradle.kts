@file:Suppress("ktlint:standard:kdoc")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven(url = "https://www.jitpack.io")
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    @Suppress("UnstableApiUsage")
    repositories {
        google()
        mavenCentral()
        maven(url = "https://www.jitpack.io")
    }
}

rootProject.name = "shonenx-extensions"

// Active extension source modules
include(":src:en:miruro")
include(":src:en:anikoto")
include(":src:en:allmanga")
include(":src:en:comix")
