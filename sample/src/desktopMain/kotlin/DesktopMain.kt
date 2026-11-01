import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import github.om252345.composemeshgradient.SampleApp

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Compose Mesh Gradient") {
        SampleApp()
    }
}
