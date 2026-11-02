<p align="center">
  <img src=".github/assets/Stardust Gradient Orb.png" width="250" alt="Stardust Logo" />
</p>
<p align="center">
  <img src=".github/assets/Stardust Grainy Gradient Wordmark.png" width="250" alt="Stardust Logo" />
</p>
<h2 align="center">SwiftUI-like Mesh Gradients & Film Grain for Compose Multiplatform</h2>

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.quantum3600/stardust.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.github.quantum3600%22%20AND%20a:%22stardust%22)

**Stardust** is a lightweight, powerful Compose Multiplatform library that brings beautiful, animated, and static mesh gradients and procedural film grain to Android, iOS, Desktop/JVM, and Web, inspired by the stunning visuals of SwiftUI. Create fluid, vibrant, and interactive backgrounds with a simple and intuitive API.

The library uses a cached SkSL runtime shader through Compose's native Skia-backed `ShaderBrush` pipeline on Desktop, iOS, and Web (`RuntimeEffect`). On Android API 33+, it uses native `RuntimeShader` (AGSL); on Android API 26–32, it automatically routes to an offscreen OpenGL ES hardware buffer pipeline (`HardwareBuffer` + `ImageReader`) with zero-copy Canvas integration (and high-resolution CPU bicubic patch tessellation as a safety fallback).

### Key Features
- **Explicit Bézier Tangent Controls**: Use `BezierPoint` with custom leading/trailing horizontal and top/bottom vertical tangent handles for asymmetric color warping.
- **Procedural Stardust & Film Grain**: Non-repetitive GPU hash/dithering stage (`grainIntensity`, `grainSeed`).
- **2D Spatial Grid Colors**: Generate multi-directional (diagonal, radial, and 4-corner) color blends using `generateSpatialGridColors`.
- **Standalone Stardust Modifier**: Apply customizable film grain noise overlays directly to any Compose component via `Modifier.stardust`.
- **GPU-Accelerated & Cross-Platform**: Android (API 26+), iOS, Desktop (JVM), JS, and Wasm support.

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
    implementation("io.github.quantum3600:stardust:1.0.5-alpha01")
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

### 🔘 Gradient Buttons (Static & Interactive)

Stardust mesh gradients can be wrapped around buttons or interactive containers to build custom UI components.

#### 1. Static Mesh Gradient Button

Wrap `StardustMesh` inside a Compose `Surface` or `Box` to create static gradient buttons:

```kotlin
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.quantum3600.stardust.StardustMesh

@Composable
fun StaticGradientButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)

    Surface(
        onClick = onClick,
        modifier = modifier.clip(shape),
        shape = shape,
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            StardustMesh(
                modifier = Modifier.matchParentSize(),
                width = 2,
                height = 2,
                points = arrayOf(
                    Offset(0f, 0f), Offset(1f, 0f),
                    Offset(0f, 1f), Offset(1f, 1f)
                ),
                colors = arrayOf(
                    Color(0xFFF36E21), Color(0xFFF0A92A),
                    Color(0xFFE73562), Color(0xFF7000FF)
                ),
                dustIntensity = 0.08f
            )

            Text(
                text = text,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
```

#### 2. Interactive Press-Scaling Mesh Gradient Button

Add interactive feedback like spring scale on press and animated mesh flow:

```kotlin
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.quantum3600.stardust.StardustMesh
import io.github.quantum3600.stardust.utils.SimplexNoise

@Composable
fun InteractiveGradientButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(Color(0xFF00F0FF), Color(0xFF7000FF), Color(0xFFFF007A), Color(0xFF390066))
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Smooth press scaling effect
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "buttonScale"
    )

    var time by remember { mutableFloatStateOf(0f) }
    val speed = if (isPressed) 1.5f else 0.4f

    LaunchedEffect(Unit) {
        var lastTime = 0L
        while (true) {
            withFrameNanos { frameTime ->
                if (lastTime != 0L) {
                    time += (frameTime - lastTime) / 1_000_000_000f * speed
                }
                lastTime = frameTime
            }
        }
    }

    val points = remember(time) {
        val w = 3; val h = 3
        Array(w * h) { i ->
            val col = i % w; val row = i / h
            val px = col / (w - 1f); val py = row / (h - 1f)
            if (row == 0 || row == h - 1 || col == 0 || col == w - 1) {
                Offset(px, py)
            } else {
                val nx = SimplexNoise.noise(px * 2f + time, i.toFloat()) * 0.15f
                val ny = SimplexNoise.noise(py * 2f + time, i + 50f) * 0.15f
                Offset(px + nx, py + ny)
            }
        }
    }

    val gridColors = remember(colors) {
        val c0 = colors[0]; val c1 = colors[1]; val c2 = colors[2]; val c3 = colors[3]
        Array(9) { i ->
            val fx = (i % 3) / 2f; val fy = (i / 3) / 2f
            io.github.quantum3600.stardust.utils.lerp(
                io.github.quantum3600.stardust.utils.lerp(c0, c1, fx),
                io.github.quantum3600.stardust.utils.lerp(c2, c3, fx),
                fy
            )
        }
    }

    val shape = RoundedCornerShape(16.dp)

    Surface(
        onClick = onClick,
        modifier = modifier.scale(scale).clip(shape),
        shape = shape,
        interactionSource = interactionSource,
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            StardustMesh(
                modifier = Modifier.matchParentSize(),
                width = 3,
                height = 3,
                points = points,
                colors = gridColors,
                dustIntensity = 0.08f
            )

            Text(
                text = text,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
```

#### 3. Touch-Deformable Touch Canvas Button

Deform the control points under the user's touch location dynamically:

```kotlin
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import io.github.quantum3600.stardust.StardustMesh

@Composable
fun TouchDeformableGradientButton(
    modifier: Modifier = Modifier
) {
    var touchPos by remember { mutableStateOf<Offset?>(null) }
    val width = 3; val height = 3

    val basePoints = remember {
        Array(9) { i -> Offset((i % 3) / 2f, (i / 3) / 2f) }
    }

    val points = remember(touchPos) {
        val touch = touchPos
        basePoints.mapIndexed { i, base ->
            val col = i % 3; val row = i / 3
            if (row == 0 || row == 2 || col == 0 || col == 2 || touch == null) {
                base
            } else {
                val dist = (base - touch).getDistance()
                val pull = (1f - (dist / 1.2f).coerceIn(0f, 1f)) * 0.25f
                val dir = if (dist > 0.001f) (touch - base) / dist else Offset.Zero
                Offset((base.x + dir.x * pull).coerceIn(0.1f, 0.9f), (base.y + dir.y * pull).coerceIn(0.1f, 0.9f))
            }
        }.toTypedArray()
    }

    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(shape)
            .border(1.dp, Color.White.copy(alpha = 0.3f), shape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        touchPos = Offset(offset.x / size.width, offset.y / size.height)
                        tryAwaitRelease()
                        touchPos = null
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { touchPos = Offset(it.x / size.width, it.y / size.height) },
                    onDrag = { change, _ -> touchPos = Offset(change.position.x / size.width, change.position.y / size.height) },
                    onDragEnd = { touchPos = null },
                    onDragCancel = { touchPos = null }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        StardustMesh(
            modifier = Modifier.matchParentSize(),
            width = width,
            height = height,
            points = points,
            colors = arrayOf(
                Color(0xFF00F0FF), Color(0xFF7000FF), Color(0xFFFF007A),
                Color(0xFF0061FF), Color(0xFF60EFFF), Color(0xFF00FF87),
                Color(0xFF390066), Color(0xFF8A2BE2), Color(0xFF00BFFF)
            ),
            dustIntensity = 0.1f
        )

        Text(
            text = if (touchPos != null) "Mesh Deforming..." else "Touch & Drag Me",
            color = Color.White
        )
    }
}
```

#### 4. Loading State Mesh Gradient Card

Animate animation speed and colors dynamically when transitioning between loading and done states:

```kotlin
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.quantum3600.stardust.StardustMesh
import io.github.quantum3600.stardust.utils.SimplexNoise
import io.github.quantum3600.stardust.utils.lerp

@Composable
fun LoadingMeshGradientCard(
    isLoading: Boolean,
    onToggleLoading: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Fast animation speed when loading (2.2x), slow/calm when done (0.25x)
    val animSpeed by animateFloatAsState(
        targetValue = if (isLoading) 2.2f else 0.25f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "loadingSpeed"
    )

    // Smooth color palette transition from vibrant loading colors to fixed theme colors
    val targetC0 = if (isLoading) Color(0xFF00F0FF) else Color(0xFF1E1E2E)
    val targetC1 = if (isLoading) Color(0xFF7000FF) else Color(0xFF2A2A40)
    val targetC2 = if (isLoading) Color(0xFFFF007A) else Color(0xFF323250)
    val targetC3 = if (isLoading) Color(0xFF390066) else Color(0xFF181825)

    val c0 by animateColorAsState(targetC0, tween(1000), label = "c0")
    val c1 by animateColorAsState(targetC1, tween(1000), label = "c1")
    val c2 by animateColorAsState(targetC2, tween(1000), label = "c2")
    val c3 by animateColorAsState(targetC3, tween(1000), label = "c3")

    var time by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var lastTime = 0L
        while (true) {
            withFrameNanos { frameTime ->
                if (lastTime != 0L) {
                    val dt = (frameTime - lastTime) / 1_000_000_000f
                    time += dt * animSpeed
                }
                lastTime = frameTime
            }
        }
    }

    val points = remember(time) {
        val w = 3; val h = 3
        Array(w * h) { i ->
            val col = i % w; val row = i / h
            val px = col / (w - 1f); val py = row / (h - 1f)
            if (row == 0 || row == h - 1 || col == 0 || col == w - 1) {
                Offset(px, py)
            } else {
                val nx = SimplexNoise.noise(px * 1.5f + time, i.toFloat()) * 0.16f
                val ny = SimplexNoise.noise(py * 1.5f + time, i + 50f) * 0.16f
                Offset(px + nx, py + ny)
            }
        }
    }

    val gridColors = remember(c0, c1, c2, c3) {
        Array(9) { i ->
            val fx = (i % 3) / 2f; val fy = (i / 3) / 2f
            lerp(lerp(c0, c1, fx), lerp(c2, c3, fx), fy)
        }
    }

    val shape = RoundedCornerShape(20.dp)

    Surface(
        onClick = onToggleLoading,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.5.dp, if (isLoading) Color.Cyan.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.2f), shape),
        shape = shape,
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier.padding(20.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            StardustMesh(
                modifier = Modifier.matchParentSize(),
                width = 3,
                height = 3,
                points = points,
                colors = gridColors,
                dustIntensity = if (isLoading) 0.15f else 0.04f
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = if (isLoading) "Loading Content..." else "Content Loaded",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = if (isLoading) "Rapid mesh flow active..." else "Slow motion & fixed theme palette",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.4f)
                ) {
                    Text(
                        text = if (isLoading) "Simulate Done" else "Simulate Load",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
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
