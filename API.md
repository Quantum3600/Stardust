# Stardust API Reference & Usage Guide

Complete API documentation for **Stardust** (`StardustMesh` / `MeshGradient`), a Compose Multiplatform library for GPU-accelerated bicubic mesh gradients and film grain textures across Android, iOS, Desktop (JVM), JS, and Wasm.

---

## Table of Contents

1. [Core Composables](#core-composables)
   - [`StardustMesh` / `MeshGradient`](#stardustmesh--meshgradient)
   - [`AnimatedStardustMesh` / `AnimatedMeshGradient`](#animatedstardustmesh--animatedmeshgradient)
2. [State Holders](#state-holders)
   - [`StardustMeshState` / `MeshGradientState`](#stardustmeshstate--meshgradientstate)
   - [`AnimatedStardustMeshState`](#animatedstardustmeshstate)
3. [Utilities & Extensions](#utilities--extensions)
   - [`rememberSimplexAnimatedMesh`](#remembersimplexanimatedmesh)
   - [`InfiniteTransition.animateOffset`](#infinitetransitionanimateoffset)
   - [`Modifier.stardust`](#modifierstardust)
4. [Usage Examples](#usage-examples)
   - [Static Gradients (2x2, 3x3, 4x4)](#static-gradients)
   - [Animating Colors](#animating-colors)
   - [Animating Control Points](#animating-control-points)
   - [Fluid Simplex Noise Animation](#fluid-simplex-noise-animation)

---

## Core Composables

### `StardustMesh` / `MeshGradient`

The primary composable for rendering bicubic mesh gradients with explicit point arrays or a state holder. `MeshGradient` is an exact alias for `StardustMesh`.

```kotlin
@Composable
fun StardustMesh(
    width: Int,
    height: Int,
    points: Array<Offset>,
    colors: Array<Color>,
    modifier: Modifier = Modifier,
    globalSubdivisions: Int = 32,
    dustIntensity: Float = 0f
)

@Composable
fun StardustMesh(
    width: Int,
    height: Int,
    modifier: Modifier = Modifier,
    globalSubdivisions: Int = 32,
    dustIntensity: Float = 0f,
    state: StardustMeshState
)
```

#### Parameters
| Parameter | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `width` | `Int` | *(Required)* | Number of control points horizontally (e.g. `2`, `3`, `4`). |
| `height` | `Int` | *(Required)* | Number of control points vertically (e.g. `2`, `3`, `4`). |
| `points` | `Array<Offset>` | *(Required)* | Control point positions in normalized `[0.0, 1.0]` coordinates (`width * height` items). |
| `colors` | `Array<Color>` | *(Required)* | Colors for each control point (`width * height` items). |
| `modifier` | `Modifier` | `Modifier` | Layout modifier applied to the composable. |
| `globalSubdivisions` | `Int` | `32` | Tessellation density for legacy rendering pipelines. |
| `dustIntensity` | `Float` | `0f` | Static procedural film grain intensity (`0.0f` = disabled, `0.08f` = subtle grain). |
| `state` | `StardustMeshState` | *(Required)* | State holder for programmatic animation or frame-by-frame point updates. |

---

### `AnimatedStardustMesh` / `AnimatedMeshGradient`

High-level, zero-boilerplate composable that drives continuous, fluid Simplex noise mesh animations with zero heap allocations on VSYNC.

```kotlin
@Composable
fun AnimatedStardustMesh(
    colors: List<Color>,
    modifier: Modifier = Modifier,
    width: Int = 4,
    height: Int = 4,
    speed: Float = 0.3f,
    wobbleAmount: Float = 0.2f,
    dustIntensity: Float = 0f,
    state: AnimatedStardustMeshState = rememberAnimatedStardustMeshState(...)
)
```

#### Parameters
| Parameter | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `colors` | `List<Color>` | *(Required)* | 4 corner colors or a grid of `width * height` colors. |
| `width` | `Int` | `4` | Number of horizontal grid points. |
| `height` | `Int` | `4` | Number of vertical grid points. |
| `speed` | `Float` | `0.3f` | Flow speed of interior point motion (`0f` to freeze motion). |
| `wobbleAmount` | `Float` | `0.2f` | Distortion amplitude of interior point movement (`0.0f` to `0.35f`). |
| `dustIntensity` | `Float` | `0f` | Static film grain overlay intensity (`0.0f` to `0.25f`). |
| `state` | `AnimatedStardustMeshState` | *(Default)* | Optional state holder to control speed, wobble, or colors programmatically. |

---

## State Holders

### `StardustMeshState` / `MeshGradientState`

State holder for programmatic animation and frame-by-frame control point updates.

```kotlin
@Stable
class StardustMeshState(
    initialPoints: Array<Offset>,
    initialColors: Array<Color> = emptyArray()
) {
    val points: List<Offset>
    val colors: List<Color>

    suspend fun animatePoint(index: Int, target: Offset, spec: AnimationSpec<Offset>)
    fun setColor(index: Int, color: Color)
    suspend fun snapAllPoints(newOffsets: List<Offset>)
    fun snapAllPoints(newOffsets: FloatArray)
}

@Composable
fun rememberStardustMeshState(
    points: Array<Offset>,
    colors: Array<Color> = emptyArray()
): StardustMeshState
```

---

### `AnimatedStardustMeshState`

State holder for controlling `AnimatedStardustMesh` parameters dynamically.

```kotlin
@Stable
class AnimatedStardustMeshState(
    val width: Int = 4,
    val height: Int = 4,
    initialColors: List<Color> = defaultColors,
    initialSpeed: Float = 0.3f,
    initialWobbleAmount: Float = 0.2f
) {
    var speed: Float
    var wobbleAmount: Float
    var isPaused: Boolean

    fun setColors(colors: List<Color>)
}

@Composable
fun rememberAnimatedStardustMeshState(
    width: Int = 4,
    height: Int = 4,
    colors: List<Color> = AnimatedStardustMeshState.defaultColors,
    speed: Float = 0.3f,
    wobbleAmount: Float = 0.2f
): AnimatedStardustMeshState
```

---

## Utilities & Extensions

### `rememberSimplexAnimatedMesh`

Composable utility that manages Simplex noise animation state for custom setups.

```kotlin
@Composable
fun rememberSimplexAnimatedMesh(
    width: Int,
    height: Int,
    colors: List<Color>,
    animationSpeed: Float = 0.3f,
    noiseIntensity: Float = 0.2f
): AnimatedMeshState
```

---

### `InfiniteTransition.animateOffset`

Animate individual `Offset` control points within an `InfiniteTransition`.

```kotlin
@Composable
fun InfiniteTransition.animateOffset(
    initialValue: Offset,
    targetValue: Offset,
    animationSpec: InfiniteRepeatableSpec<Float>
): State<Offset>
```

---

### `Modifier.stardust`

Apply procedural static film grain texture overlays directly over any Compose component.

```kotlin
fun Modifier.stardust(
    intensity: Float = 0.05f,
    seed: Float = 0f,
    blendMode: BlendMode = BlendMode.Overlay
): Modifier
```

---

## Usage Examples

### Static Gradients

#### Static 2x2 Mesh Gradient
```kotlin
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

### Animating Colors

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

---

### Animating Control Points

```kotlin
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

---

### Fluid Simplex Noise Animation

#### High-Level 1-Liner (`AnimatedStardustMesh`)
```kotlin
@Composable
fun SimpleSimplexMesh() {
    AnimatedStardustMesh(
        colors = listOf(
            Color(0xffF36E21), Color(0xffF0A92A), Color(0xffE7D043), Color(0xffE7E95D),
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

#### Custom State Loop (`rememberStardustMeshState`)
```kotlin
@Composable
fun SimplexNoiseMeshExample(modifier: Modifier = Modifier) {
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

    val meshState = rememberStardustMeshState(points = initialPoints)

    LaunchedEffect(Unit) {
        var time = 0f
        val basePoints = initialPoints.toList()
        val currentPoints = initialPoints.toMutableList()
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
```
