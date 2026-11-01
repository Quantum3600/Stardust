plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose)
    alias(libs.plugins.vanniktech.maven.publish)
}

group = "io.github.om252345"
version = "0.3.0"
android {
    namespace = "io.github.om252345.composemeshgradient"
    compileSdk = 35
    defaultConfig {
        minSdk = 21
    }
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
    jvm("desktop")
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    wasmJs {
        browser()
    }
    js(IR) {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.animation)
        }
    }
}


// If you are using the vanniktech mavenPublishing plugin:
mavenPublishing {
    // Only require signing if keys are present (or disable signing for JitPack builds)
    if (project.hasProperty("signingInMemoryKey") || project.hasProperty("signing.keyId")) {
        signAllPublications()
    }
    coordinates("com.github.quantum3600", "composemeshgradientcmp", version.toString())

    pom {
        name.set("Compose Mesh Gradient Multiplatform")
        description.set("A dynamic mesh gradient library for Compose Multiplatform.")
        url.set("https://github.com/quantum3600/composemeshgradient")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
    }
}