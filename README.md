<p align="center">
  <img src=".github/assets/Stardust Gradient Orb.png" width="250" alt="Stardust Logo" />
</p>
<p align="center">
  <img src=".github/assets/Stardust Grainy Gradient Wordmark.png" width="250" alt="Stardust Logo" />
</p>
<h2 align="center">Ultra-Intuitive Mesh Gradients & Film Grain for Compose Multiplatform</h2>

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.quantum3600/stardust.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.github.quantum3600%22%20AND%20a:%22stardust%22)

**Stardust** is a high-performance Compose Multiplatform library for rendering animated, static, and Bézier mesh gradients with procedural film grain across **Android, iOS, Desktop/JVM, and Web**. Inspired by SwiftUI's MeshGradient, Stardust completely eliminates low-level frame loop boilerplate, providing intuitive, clean composables alongside zero-allocation GPU shaders.

---

## 🚀 Quick Start Tutorial

### 1. Animated Mesh Gradient (Instant 1-Liner)

Create a fluid, continuously flowing mesh gradient with zero setup math or manual VSYNC frame loops:

```kotlin
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.quantum3600.stardust.AnimatedStardustMesh

@Composable
fun FlowingMeshBackground() {
    AnimatedStardustMesh(
        colors = listOf(
            Color(0xFFF36E21), Color(0xFFF0A92A),
            Color(0xFF36447C), Color(0xFF2D1137)
        ),
        width = 3,
        height = 3,
        speed = 0.15f,           // Mesh motion speed (set to 0f to pause)
        wobbleAmount = 0.12f,    // Motion amplitude/randomness
        dustIntensity = 0.05f,   // Procedural film grain intensity
        modifier = Modifier.fillMaxSize()
    )
}
```

---

### 2. Animated Bézier Mesh Gradient (Organic Color Pulling & Tangents)

For smooth, organic curves and dynamic tangent handle warping:

```kotlin
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.quantum3600.stardust.AnimatedBezierStardustMesh

@Composable
fun OrganicBezierMesh() {
    AnimatedBezierStardustMesh(
        colors = listOf(Color.Cyan, Color.Magenta, Color.Blue, Color.Yellow),
        width = 3,
        height = 3,
        speed = 1.0f,
        randomness = 0.18f,
        dustIntensity = 0.10f,
        modifier = Modifier.fillMaxSize()
    )
}
```

---

### 3. Static Mesh Gradient

Pass explicit point positions or 2D grid lists for static layouts:

```kotlin
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import io.github.quantum3600.stardust.StardustMesh

@Composable
fun StaticMesh() {
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
        dustIntensity = 0.08f,
        modifier = Modifier.fillMaxSize()
    )
}
```

---

### 4. Custom State & Programmatic Controls (`StardustMeshState`)

For interactive drag gestures, physics, or manual point snapping:

```kotlin
import io.github.quantum3600.stardust.StardustMesh
import io.github.quantum3600.stardust.rememberStardustMeshState

@Composable
fun InteractiveMesh(points: Array<Offset>, colors: Array<Color>) {
    val meshState = rememberStardustMeshState(points, colors)

    StardustMesh(
        width = 3,
        height = 3,
        state = meshState,
        dustIntensity = 0.05f,
        modifier = Modifier.fillMaxSize()
    )
}
```

---

### 5. Standalone Film Grain Overlay (`Modifier.stardust`)

Apply procedural film grain texture directly over any Compose component (Cards, Buttons, Images, Text):

```kotlin
import androidx.compose.ui.graphics.BlendMode
import io.github.quantum3600.stardust.stardust

Card(
    modifier = Modifier
        .fillMaxWidth()
        .stardust(
            intensity = 0.08f,
            seed = animTime,
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
    implementation("io.github.quantum3600:stardust:1.0.5-alpha01")
}
```

---

## ⚡ Key Features

- **Intuitive Composables**: Single-line calls for animated, static, and Bézier mesh gradients.
- **Unified Parameters**: Consistent parameter names (`colors`, `speed`, `dustIntensity`, `wobbleAmount`, `randomness`).
- **Zero Allocation VSYNC Engine**: Continuous frame animation loop runs with zero heap allocations or recomposition passes.
- **Automatic Spatial Grid Colors**: Pass 4 corner colors to automatically blend rich 2D multi-directional gradients across any grid size.
- **Cross-Platform GPU Performance**: Native AGSL shaders on Android API 33+, OpenGL ES hardware buffers on Android API 26-32, and Skia SkSL `RuntimeEffect` on iOS, Desktop, JS, and Wasm.
