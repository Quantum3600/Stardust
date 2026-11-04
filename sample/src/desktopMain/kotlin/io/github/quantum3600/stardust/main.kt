package io.github.quantum3600.stardust

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application


import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Mesh Gradient Sample",
        state = rememberWindowState(width = 1000.dp, height = 720.dp),
    ) {
        App()
    }
}
