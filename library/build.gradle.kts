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


mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates(group.toString(), "composemeshgradient", version.toString())

    pom {
        name.set("Compose Mesh Gradient")
        description.set("A Compose Multiplatform library for mesh gradients.")
        url.set("https://github.com/om252345/composemeshgradient")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("om252345")
                name.set("Omkar Deshmukh")
                email.set("omkard@gmail.com")
            }
        }
        scm {
            connection.set("scm:git:github.com/om252345/composemeshgradient.git")
            developerConnection.set("scm:git:ssh://github.com/om252345/composemeshgradient.git")
            url.set("https://github.com/om252345/composemeshgradient")
        }
    }
}