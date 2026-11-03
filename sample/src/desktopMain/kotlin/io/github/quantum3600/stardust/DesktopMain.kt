package io.github.quantum3600.stardust

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Stardust Mesh Gradient") {
        SampleApp()
    }
}
