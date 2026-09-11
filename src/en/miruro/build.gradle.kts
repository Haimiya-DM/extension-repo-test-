import io.github.keiyoushi.gradle.api.ContentWarning

plugins {
    alias(kei.plugins.extension)
}

keiyoushi {
    name = "Miruro"
    versionCode = 1
    contentWarning = ContentWarning.SAFE
    libVersion = "1.5"
    source {
        lang = "en"
        baseUrl = "https://miruro.to"
        id = 7392184910248102381L
    }
    deeplink {
        host("miruro.to")
        path("/watch/..*")
        path("/anime/..*")
    }
}

// Fallback legacy properties for older Gradle plugins & Dartotsu extension bridge
ext {
    extName = "Miruro"
    extClass = ".Miruro"
    extVersionCode = 1
    isNsfw = false
}
