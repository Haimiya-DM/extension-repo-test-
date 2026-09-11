import io.github.keiyoushi.gradle.api.ContentWarning

plugins {
    alias(kei.plugins.extension)
}

keiyoushi {
    name = "Comix"
    versionCode = 1
    contentWarning = ContentWarning.SAFE
    libVersion = "1.6"
    source {
        lang = "en"
        baseUrl = "https://comix.to"
        id = 6821940182947182941L
    }
    deeplink {
        host("comix.to")
        path("/manga/..*")
        path("/read/..*")
    }
}

// Fallback legacy properties for older Gradle plugins & Dartotsu extension bridge
ext {
    extName = "Comix"
    extClass = ".Comix"
    extVersionCode = 1
    isNsfw = false
}
