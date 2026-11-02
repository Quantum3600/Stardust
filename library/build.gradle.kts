import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose)
    alias(libs.plugins.vanniktech.maven.publish)
}

group = "io.github.quantum3600"
version = "1.0.4"

android {
    namespace = "io.github.quantum3600.stardust"
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
    @OptIn(ExperimentalWasmDsl::class)
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
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        val desktopTest by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

mavenPublishing {
    publishToMavenCentral()

    // Sign all publications if keys are present
    if (project.hasProperty("signingInMemoryKey") || project.hasProperty("signing.keyId")) {
        signAllPublications()
    }
    
    coordinates("io.github.quantum3600", "stardust", version.toString())

    pom {
        name.set("Stardust - Mesh Gradient for Multiplatform")
        description.set("A dynamic mesh gradient library for Compose Multiplatform.")
        url.set("https://github.com/quantum3600/stardust")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("quantum3600")
                name.set("Trishit Majumdar")
                email.set("trishitquantum360@gmail.com")
            }
        }
        scm {
            connection.set("scm:git:github.com/quantum3600/stardust.git")
            developerConnection.set("scm:git:ssh://github.com/quantum3600/stardust.git")
            url.set("https://github.com/quantum3600/stardust")
        }
    }
}