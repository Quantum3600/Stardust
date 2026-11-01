import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.quantum3600.stardust.SampleApp

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Compose Mesh Gradient") {
        SampleApp()
    }
}
