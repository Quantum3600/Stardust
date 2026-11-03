import java.net.HttpURLConnection
import java.net.URI
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose)
    alias(libs.plugins.vanniktech.maven.publish)
}

group = "io.github.quantum3600"
version = "1.0.5-alpha05"

android {
    namespace = "io.github.quantum3600.stardust"
    compileSdk = 35
    defaultConfig {
        minSdk = 26
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

tasks.register("notifyKlibsIo") {
    group = "publishing"
    description = "Checks Google Maven Central Mirror and provides klibs.io indexing links."
    doLast {
        val artifactId = "stardust"
        val groupPath = project.group.toString().replace('.', '/')
        val artifactCoordinate = "${project.group}:$artifactId:${project.version}"
        val mirrorPomUrlStr = "https://maven-central.storage-download.googleapis.com/maven2/$groupPath/$artifactId/${project.version}/$artifactId-${project.version}.pom"

        println("Checking Google Maven Central Mirror for $artifactCoordinate...")
        val isAvailableOnMirror = runCatching {
            val mirrorConn = URI.create(mirrorPomUrlStr).toURL().openConnection() as HttpURLConnection
            mirrorConn.requestMethod = "HEAD"
            mirrorConn.responseCode == 200
        }.getOrDefault(false)

        if (!isAvailableOnMirror) {
            println("WARNING: $artifactCoordinate is NOT found on Google's mirror yet ($mirrorPomUrlStr).")
            println("klibs.io cannot index files that are absent from the mirror. Please publish to Maven Central first and wait for mirror propagation.")
        } else {
            println("SUCCESS: $artifactCoordinate is verified and live on Google's Maven Central mirror!")
            println("Mirror POM: $mirrorPomUrlStr")
            println("\nTo index this version on klibs.io immediately:")
            println("1. Open https://klibs.io")
            println("2. Submit an indexing request for coordinates: $artifactCoordinate (or GitHub URL: https://github.com/quantum3600/stardust)")
        }
    }
}