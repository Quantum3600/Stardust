plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.vanniktech.maven.publish) apply false
}

tasks.register("notifyKlibsIo") {
    group = "publishing"
    description = "Notifies klibs.io to index the published library version."
    dependsOn(":library:notifyKlibsIo")
}