import io.github.keiyoushi.gradle.api.ContentWarning

plugins {
    alias(kei.plugins.extension)
}

keiyoushi {
    name = "Anikoto"
    versionCode = 1
    contentWarning = ContentWarning.SAFE
    libVersion = "1.5"
    source {
        lang = "en"
        baseUrl = "https://anikototv.to"
        id = 8129301482049182390L
    }
    deeplink {
        host("anikototv.to")
        host("anikoto.bz")
        host("anikoto.net")
        path("/watch/..*")
        path("/anime/..*")
    }
}

// Fallback legacy properties for older Gradle plugins & Dartotsu extension bridge
ext {
    extName = "Anikoto"
    extClass = ".Anikoto"
    extVersionCode = 1
    isNsfw = false
}
