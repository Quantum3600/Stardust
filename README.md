<p align="center">
  <img src=".github/assets/Stardust Gradient Orb.png" width="250" alt="Stardust Logo" />
</p>
<p align="center">
  <img src=".github/assets/Stardust Grainy Gradient Wordmark.png" width="250" alt="Stardust Logo" />
</p>
<h2 align="center">Ultra-Intuitive Mesh Gradients & Film Grain for Compose Multiplatform</h2>

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.quantum3600/stardust.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.github.quantum3600%22%20AND%20a:%22stardust%22)

**Stardust** (`StardustMesh` / `MeshGradient`) is a high-performance Compose Multiplatform library for rendering static and animated bicubic mesh gradients with procedural film grain across **Android, iOS, Desktop/JVM, and Web**. Inspired by SwiftUI's MeshGradient, Stardust provides intuitive, clean composables alongside zero-allocation GPU shaders.

---

## 🎨 Usage Examples

### 1. Static Gradients

#### Static 2x2 Mesh Gradient
```kotlin
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import io.github.quantum3600.stardust.StardustMesh

@Composable
fun Static2x2Mesh() {
    StardustMesh(
        width = 2,
        height = 2,
        points = arrayOf(
            Offset(0f, 0f), Offset(1f, 0f),
            Offset(0f, 1f), Offset(1f, 1f)
        ),
        colors = arrayOf(
            Color(0xFFF09819), Color(0xFFEDDE5D),
            Color(0xFF8A2387), Color(0xFFE94057)
        ),
        modifier = Modifier.fillMaxSize()
    )
}
```

#### Static 3x3 Mesh Gradient
```kotlin
@Composable
fun Static3x3Mesh() {
    StardustMesh(
        width = 3,
        height = 3,
        points = arrayOf(
            Offset(0f, 0f),   Offset(0.5f, 0f),   Offset(1f, 0f),
            Offset(0f, 0.5f), Offset(0.5f, 0.5f), Offset(1f, 0.5f),
            Offset(0f, 1f),   Offset(0.5f, 1f),   Offset(1f, 1f)
        ),
        colors = arrayOf(
            Color.Red,  Color.Yellow, Color.Green,
            Color.Cyan, Color.Blue,   Color.Magenta,
            Color.Black, Color.Gray,  Color.White
        ),
        modifier = Modifier.fillMaxSize()
    )
}
```

#### Static 4x4 Mesh Gradient
```kotlin
@Composable
fun Static4x4Mesh() {
    StardustMesh(
        width = 4,
        height = 4,
        points = Array(16) { i ->
            val col = i % 4
            val row = i / 4
            Offset(x = col / 3f, y = row / 3f)
        },
        colors = arrayOf(
            Color(0xffF36E21), Color(0xffF0A92A), Color(0xffE7D043), Color(0xffE7E95D),
            Color(0xffDFF168), Color(0xffB1E192), Color(0xff7FD3A9), Color(0xff5EC6B8),
            Color(0xff4AB8C2), Color(0xff4099B9), Color(0xff3B79A8), Color(0xff395B93),
            Color(0xff36447C), Color(0xff332E66), Color(0xff301E4E), Color(0xff2D1137)
        ),
        modifier = Modifier.fillMaxSize()
    )
}
```

---

### 2. Animated Gradients

#### Animating Colors
```kotlin
@Composable
fun AnimatedColorMesh() {
    val infiniteTransition = rememberInfiniteTransition(label = "color-transition")

    val animatedColor1 = infiniteTransition.animateColor(
        initialValue = Color(0xFF8A2387),
        targetValue = Color(0xFFF09819),
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "color1"
    )

    val animatedColor2 = infiniteTransition.animateColor(
        initialValue = Color(0xFFEDDE5D),
        targetValue = Color(0xFFE94057),
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "color2"
    )

    StardustMesh(
        width = 2,
        height = 2,
        points = arrayOf(
            Offset(0f, 0f), Offset(1f, 0f),
            Offset(0f, 1f), Offset(1f, 1f)
        ),
        colors = arrayOf(
            animatedColor1.value, animatedColor2.value,
            animatedColor2.value, animatedColor1.value
        ),
        modifier = Modifier.fillMaxSize()
    )
}
```

#### Animating Control Points (`animateOffset`)
```kotlin
import io.github.quantum3600.stardust.utils.animateOffset

@Composable
fun AnimatedPointMesh() {
    val infiniteTransition = rememberInfiniteTransition(label = "point-transition")

    val animatedMiddlePoint = infiniteTransition.animateOffset(
        initialValue = Offset(0.2f, 0.2f),
        targetValue = Offset(0.8f, 0.8f),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    StardustMesh(
        width = 3,
        height = 3,
        points = arrayOf(
            Offset(0f, 0f),   Offset(0.5f, 0f),       Offset(1f, 0f),
            Offset(0f, 0.5f), animatedMiddlePoint.value, Offset(1f, 0.5f),
            Offset(0f, 1f),   Offset(0.5f, 1f),       Offset(1f, 1f)
        ),
        colors = arrayOf(
            Color.Red,    Color.Magenta, Color.Blue,
            Color.Yellow, Color.White,   Color.Green,
            Color.Cyan,   Color.DarkGray, Color.Black
        ),
        modifier = Modifier.fillMaxSize()
    )
}
```

#### Fluid Simplex Noise Animation (`AnimatedStardustMesh` / `rememberStardustMeshState`)
```kotlin
@Composable
fun SimpleSimplexMesh() {
    AnimatedStardustMesh(
        colors = listOf(
            Color(0xffF36E21), Color(0xffF0A92A), Color(0xffE7D043), Color(0xffE7D043),
            Color(0xffDFF168), Color(0xffB1E192), Color(0xff7FD3A9), Color(0xff5EC6B8),
            Color(0xff4AB8C2), Color(0xff4099B9), Color(0xff3B79A8), Color(0xff395B93),
            Color(0xff36447C), Color(0xff332E66), Color(0xff301E4E), Color(0xff2D1137)
        ),
        width = 4,
        height = 4,
        speed = 0.3f,
        wobbleAmount = 0.2f,
        dustIntensity = 0.05f,
        modifier = Modifier.fillMaxSize()
    )
}
```

---

### 3. Standalone Film Grain Overlay (`Modifier.stardust`)

Apply procedural static film grain texture directly over any Compose component:

```kotlin
import androidx.compose.ui.graphics.BlendMode
import io.github.quantum3600.stardust.stardust

Card(
    modifier = Modifier
        .fillMaxWidth()
        .stardust(
            intensity = 0.08f,
            blendMode = BlendMode.Overlay
        )
) {
    Text("Tactile Grain Overlay", modifier = Modifier.padding(16.dp))
}
```

---

## 📦 Installation

Add Stardust to your module's `build.gradle.kts` from **Maven Central**:

```kotlin
dependencies {
    implementation("io.github.quantum3600:stardust:1.0.5-alpha05")
}
```

---

## ⚡ Key Features

- **Primary Composables**: `StardustMesh` and `MeshGradient` (exact alias).
- **State Holders**: `StardustMeshState` and `MeshGradientState` (exact alias).
- **High-Level Composable**: `AnimatedStardustMesh` for zero-boilerplate Simplex noise flow.
- **Cross-Platform GPU Performance**: Native AGSL shaders on Android API 33+, OpenGL ES hardware buffers on Android API 26-32, and Skia SkSL `RuntimeEffect` on iOS, Desktop, JS, and Wasm.
