# Stardust: SwiftUI-like Mesh Gradients & Film Grain for Compose Multiplatform

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.quantum3600/composemeshgradient.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.github.quantum3600%22%20AND%20a:%22composemeshgradient%22)

**Stardust** is a lightweight, powerful Compose Multiplatform library that brings beautiful, animated, and static mesh gradients and procedural film grain to Android, iOS, Desktop/JVM, and Web, inspired by the stunning visuals of SwiftUI. Create fluid, vibrant, and interactive backgrounds with a simple and intuitive API.

The library uses a cached SkSL runtime shader through Compose's native Skia-backed `ShaderBrush` pipeline, so bicubic surface evaluation and dithering run per pixel without CPU tessellation seams. Android API 33+ uses `RuntimeShader`; Desktop, iOS, and Web use Skia's `RuntimeEffect`.

### Key Features
- **Explicit Bézier Tangent Controls**: Use `BezierPoint` with custom leading/trailing horizontal and top/bottom vertical tangent handles for asymmetric color warping.
- **Procedural Stardust & Film Grain**: Non-repetitive GPU hash/dithering stage (`grainIntensity`, `grainSeed`).
- **2D Spatial Grid Colors**: Generate multi-directional (diagonal, radial, and 4-corner) color blends using `generateSpatialGridColors`.
- **Standalone Stardust Modifier**: Apply customizable film grain noise overlays directly to any Compose component via `Modifier.stardust`.
- **GPU-Accelerated & Cross-Platform**: Android, iOS, Desktop (JVM), JS, and Wasm support.

---

## ✨ Demos

Here are some examples of what you can create with Stardust:

![github-1](https://github.com/user-attachments/assets/c0c78e75-77cb-49ba-ad34-b89712bd1b03)  ![Screen_Recording_20250811_110334_Compose Mesh Gradient](https://github.com/user-attachments/assets/f77f80af-1bdb-444d-817a-6bfa79ac2334)  ![Screen_Recording_20250811_223615_MeshGraadTest (1)](https://github.com/user-attachments/assets/f24e2a0d-a4a6-44c7-8038-04cdbf1f72cd)

---

## 💡 Showcase & Inspiration

Stardust provides the core tools to build a wide variety of dynamic and interactive effects. `StardustMeshState` / `MeshGradientState` allows you to update colors and control points on every frame, making it possible to connect gradients to external state or user input:

* **Interactive Backgrounds:** Create gradients that respond to touch input or gyroscope motion by updating `StardustMeshState`.
* **Data Visualization & Heatmaps:** Represent data dynamically by blending colors across 2D spatial grid coordinates (`generateSpatialGridColors`).
* **Themed Animations:** Design ambient, "lava lamp" or "aurora borealis" animations with organic motion and procedural film grain.
* **Component Noise Overlays:** Dust buttons, cards, or images with `Modifier.stardust` for a textured tactile aesthetic.

---

## 🚀 Installation

Add the dependency to your module's `build.gradle.kts` file from **Maven Central**:

```kotlin
dependencies {
    implementation("io.github.quantum3600:composemeshgradient:1.0.3")
}
```

---

## 🎨 Usage

The core composable functions are `StardustMesh` and `MeshGradient`. You define a grid size (`width` and `height`), provide control `points` (or `bezierPoints`), and assign `colors` to each point.

### Basic Concepts

* **Grid (`width`, `height`):** Defines the number of control points (e.g. 3x3 grid = 9 points).
* **Points (`Array<Offset>` or `List<List<BezierPoint>>`):** Positions or explicit Bézier points determining control point locations and tangent handles. Coordinates are normalized (`0f` to `1f`).
* **Colors (`Array<Color>` or `List<List<Color>>`):** Colors associated with each control point, smoothly interpolated across the mesh.

### Static Gradients

#### 2x2 Mesh Gradient (Static)

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
            Offset(0f, 0f), Offset(1f, 0f), // Top-left, Top-right
            Offset(0f, 1f), Offset(1f, 1f)  // Bottom-left, Bottom-right
        ),
        colors = arrayOf(
            Color(0xFFF09819), Color(0xFFEDDE5D),
            Color(0xFF8A2387), Color(0xFFE94057)
        ),
        modifier = Modifier.fillMaxSize()
    )
}
```

#### Asymmetric Bézier Handles & Film Grain

Use `BezierPoint` to specify custom directional tangent handles for asymmetric color pulling, alongside procedural film grain:

```kotlin
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import io.github.quantum3600.stardust.StardustMesh
import io.github.quantum3600.stardust.data.BezierPoint

@Composable
fun BezierAndGrainMesh() {
    val bezierPoints = listOf(
        listOf(
            BezierPoint.withTangents(Offset(0f, 0f)),
            BezierPoint.withTangents(Offset(0.5f, 0f), uTangentTrailing = Offset(0.1f, 0.05f)),
            BezierPoint.withTangents(Offset(1f, 0f))
        ),
        listOf(
            BezierPoint.withTangents(Offset(0f, 0.5f)),
            BezierPoint.withTangents(
                position = Offset(0.5f, 0.5f),
                uTangentLeading = Offset(-0.2f, 0.1f),
                uTangentTrailing = Offset(0.2f, -0.1f),
                vTangentTop = Offset(0.05f, -0.2f),
                vTangentBottom = Offset(-0.05f, 0.2f)
            ),
            BezierPoint.withTangents(Offset(1f, 0.5f))
        ),
        listOf(
            BezierPoint.withTangents(Offset(0f, 1f)),
            BezierPoint.withTangents(Offset(0.5f, 1f)),
            BezierPoint.withTangents(Offset(1f, 1f))
        )
    )

    StardustMesh(
        bezierPoints = bezierPoints,
        colors = colors,
        grainIntensity = 0.12f,
        grainSeed = frameTime
    )
}
```

#### Standalone Stardust Modifier

Apply procedural film grain overlay to any Compose layout element (such as Cards, Buttons, Images, or Text) using `Modifier.stardust`:

```kotlin
import androidx.compose.ui.Modifier
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
    Text("Stardust Texture Overlay", modifier = Modifier.padding(16.dp))
}
```

---

## 🎬 Animated Gradients

### Simplex Noise Animation (Fluid Gradient)

For a truly dynamic and organic feel, use `SimplexNoise` to continuously animate inner control points with `rememberStardustMeshState` / `rememberMeshGradientState`:

```kotlin
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import io.github.quantum3600.stardust.StardustMesh
import io.github.quantum3600.stardust.rememberStardustMeshState
import io.github.quantum3600.stardust.utils.SimplexNoise
import kotlinx.coroutines.launch

@Composable
fun SimplexNoiseMeshExample(modifier: Modifier) {
    val width = 4
    val height = 4

    val colors = remember {
        listOf(
            Color(0xffF36E21), Color(0xffF0A92A), Color(0xffE7D043), Color(0xffE7E95D),
            Color(0xffDFF168), Color(0xffB1E192), Color(0xff7FD3A9), Color(0xff5EC6B8),
            Color(0xff4AB8C2), Color(0xff4099B9), Color(0xff3B79A8), Color(0xff395B93),
            Color(0xff36447C), Color(0xff332E66), Color(0xff301E4E), Color(0xff2D1137)
        )
    }

    val initialPoints = remember {
        Array(width * height) { i ->
            val col = i % width
            val row = i / width
            Offset(x = col / (width - 1f), y = row / (height - 1f))
        }
    }
    
    val meshState = rememberStardustMeshState(points = initialPoints, colors = colors.toTypedArray())

    LaunchedEffect(Unit) {
        var time = 0f
        val basePoints = initialPoints.toList()
        var currentPoints = initialPoints.toMutableList()
        val targetPoints = initialPoints.toMutableList()
        var lastFrameTime = 0L

        while (true) {
            withFrameNanos { frameTime ->
                if (lastFrameTime == 0L) lastFrameTime = frameTime
                val deltaTime = (frameTime - lastFrameTime) / 1_000_000_000.0f
                lastFrameTime = frameTime
                time += deltaTime * 0.3f

                for (i in targetPoints.indices) {
                    val col = i % width
                    val row = i / width
                    val isBorder = row == 0 || row == height - 1 || col == 0 || col == width - 1

                    if (!isBorder) {
                        val bp = basePoints[i]
                        val noiseX = SimplexNoise.noise(bp.x * 1.5f, time + i) * 0.2f
                        val noiseY = SimplexNoise.noise(bp.y * 1.5f, time + i + 100f) * 0.2f
                        targetPoints[i] = Offset(bp.x + noiseX, bp.y + noiseY)
                    }
                }

                for (i in currentPoints.indices) {
                    val lerped = lerp(currentPoints[i], targetPoints[i], (8f * deltaTime))
                    currentPoints[i] = lerped
                }

                launch {
                    meshState.snapAllPoints(currentPoints.toList())
                }
            }
        }
    }

    StardustMesh(
        modifier = modifier,
        width = width,
        height = height,
        state = meshState,
        dustIntensity = 0.05f
    )
}

private fun lerp(start: Offset, stop: Offset, fraction: Float): Offset {
    return start + (stop - start) * fraction.coerceIn(0f, 1f)
}
```

---

## 🙏 Credits & Acknowledgments

This project is built upon and inspired by [ComposeMeshGradient](https://github.com/om252345/composemeshgradient) created by [Omkar Deshmukh](https://github.com/om252345). We extend our sincere gratitude to Omkar Deshmukh for the original foundation.

---

## 📜 License

```
Copyright 2025 Omkar Deshmukh

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
