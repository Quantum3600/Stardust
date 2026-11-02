package io.github.quantum3600.stardust

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SampleApp() {
    MaterialTheme {
        Box(Modifier.fillMaxSize()) {
            BezierAndGrainDemo(Modifier.fillMaxSize())
        }
    }
}
