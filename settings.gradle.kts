@file:Suppress("ktlint:standard:kdoc")

pluginManagement {
    includeBuild("gradle/build-logic")
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
    versionCatalogs {
        create("libs") {
            from(files("gradle/libs.versions.toml"))
        }
        create("kei") {
            from(files("gradle/kei.versions.toml"))
        }
    }
}

rootProject.name = "shonenx-extensions"

// Shared build-logic support modules (from keiyoushi/extensions-source,
// Apache-2.0 — see ATTRIBUTION.md)
include(":core")
include(":common")
include(":compiler")

// Shared library modules required by extension sources below
include(":lib:unpacker")

// Active extension source modules
include(":src:eu:kanade:tachiyomi:animeextension:en:anikoto")
include(":src:eu:kanade:tachiyomi:animeextension:en:animepahe")
include(":src:eu:kanade:tachiyomi:extension:en:allanime")
include(":src:eu:kanade:tachiyomi:extension:en:comix")

project(":src:eu:kanade:tachiyomi:animeextension:en:anikoto").projectDir =
    file("src/eu/kanade/tachiyomi/animeextension/en/anikoto")
project(":src:eu:kanade:tachiyomi:animeextension:en:animepahe").projectDir =
    file("src/eu/kanade/tachiyomi/animeextension/en/animepahe")
project(":src:eu:kanade:tachiyomi:extension:en:allanime").projectDir =
    file("src/eu/kanade/tachiyomi/extension/en/allanime")
project(":src:eu:kanade:tachiyomi:extension:en:comix").projectDir =
    file("src/eu/kanade/tachiyomi/extension/en/comix")
