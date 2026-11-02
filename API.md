# ComposeMeshGradient API Reference

A comprehensive API documentation for **ComposeMeshGradient** (`stardust`), a Compose Multiplatform library for rendering high-performance, GPU-accelerated bicubic mesh gradients on Android, iOS, Desktop (JVM), JS, and Wasm.

---

## Table of Contents

1. [Overview & Architecture](#overview--architecture)
2. [Core Composables](#core-composables)
    - [`StardustMesh`](#stardustmesh)
3. [State Management](#state-management)
    - [`MeshGradientState`](#meshgradientstate)
    - [`rememberMeshGradientState`](#remembermeshgradientstate)
4. [Mesh Utilities & Generators](#mesh-utilities--generators)
    - [`generateMeshPoints`](#generatemeshpoints)
    - [`generateMeshColors`](#generatemeshcolors)
    - [`rememberThemedMeshData`](#rememberthemedmeshdata)
    - [`rememberSimplexAnimatedMesh`](#remembersimplexanimatedmesh)
    - [`InfiniteTransition.animateOffset`](#infinitetransitionanimateoffset)
    - [`Modifier.stardust`](#modifierstardust)
5. [Data Models](#data-models)
    - [`MeshData`](#meshdata)
    - [`BezierPoint`](#bezierpoint)
6. [Procedural Noise Engine](#procedural-noise-engine)
    - [`SimplexNoise`](#simplexnoise)
7. [Shader & Technical Specifications](#shader--technical-specifications)
    - [Bicubic Interpolation & Solver](#bicubic-interpolation--solver)
    - [Constraints & Limits](#constraints--limits)

---

## Overview & Architecture

ComposeMeshGradient renders fluid, multi-point color gradients by evaluating a bicubic Catmull-Rom surface per pixel using custom platform shaders (Android AGSL `RuntimeShader` on API 33+ or Skia `RuntimeEffect` on Desktop, iOS, JS, and Wasm).

### Key Features
- **GPU-Accelerated**: Per-pixel evaluation with zero CPU tessellation or mesh triangle overhead.
- **Levenberg-Marquardt UV Inversion**: Solves inverse bicubic positions in shader code to support complex, smooth surface warping.
- **Dust Noise Grain**: Integrated Skia shader noise overlay with customizable intensity and animation phase.
- **State-Driven Animations**: Fine-grained point and color state holders supporting 60+ FPS animations without object allocation churn.

---

## Core Composables

### `StardustMesh`

The primary composable component for rendering mesh gradients.

#### Overload 1: Direct Arrays (Static / Explicit State)

```kotlin
@Composable
fun StardustMesh(
    modifier: Modifier = Modifier,
    width: Int,
    height: Int,
    points: Array<Offset>,
    colors: Array<Color>,
    globalSubdivisions: Int = 32,
    dustIntensity: Float = 0f,
    dustAnimationSpeed: Float = 0f,
)
```

##### Parameters

| Parameter | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `modifier` | `Modifier` | `Modifier` | Layout modifier applied to the internal `Canvas`. |
| `width` | `Int` | *(Required)* | Number of horizontal grid control points. Must be `> 1`. |
| `height` | `Int` | *(Required)* | Number of vertical grid control points. Must be `> 1`. |
| `points` | `Array<Offset>` | *(Required)* | Array of normalized coordinates (`0f` to `1f`) in row-major order. Must contain `width * height` elements. |
| `colors` | `Array<Color>` | *(Required)* | Array of colors associated with each control point in row-major order. Must contain `width * height` elements. |
| `globalSubdivisions` | `Int` | `32` | Retained for backward compatibility. |
| `dustIntensity` | `Float` | `0f` | Grain noise intensity (`0.0f` to `1.0f`). Defaults to `0f` (disabled). |
| `dustAnimationSpeed` | `Float` | `0f` | Speed factor for continuous noise phase progression. Set to `> 0f` to animate dust particles. |

#### Overload 2: Driven by `MeshGradientState` (Animated)

```kotlin
@Composable
fun StardustMesh(
    modifier: Modifier = Modifier,
    width: Int,
    height: Int,
    globalSubdivisions: Int = 32,
    dustIntensity: Float = 0f,
    dustAnimationSpeed: Float = 0f,
    state: MeshGradientState,
)
```

##### Parameters

| Parameter | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `modifier` | `Modifier` | `Modifier` | Layout modifier applied to the canvas. |
| `width` | `Int` | *(Required)* | Number of horizontal grid control points. |
| `height` | `Int` | *(Required)* | Number of vertical grid control points. |
| `globalSubdivisions` | `Int` | `32` | Retained for backward compatibility. |
| `dustIntensity` | `Float` | `0f` | Grain noise intensity (`0.0f` to `1.0f`). |
| `dustAnimationSpeed` | `Float` | `0f` | Speed factor for continuous noise phase progression. |
| `state` | `MeshGradientState` | *(Required)* | State object managing control points and colors for programmatic animation. |

##### Exceptions Thrown
- `IllegalArgumentException` if `width <= 1` or `height <= 1`.
- `IllegalArgumentException` if `points.size != width * height` or `colors.size != width * height`.
- `IllegalArgumentException` if `width * height > MeshGradientShader.maxControlPoints` (max 25 points).
- `IllegalArgumentException` if `dustIntensity` is not in range `0.0f..1.0f`.

---

## State Management

### `MeshGradientState`

A `@Stable` state holder designed for frame-by-frame manipulation and high-performance animation of control points and colors.

```kotlin
@Stable
class MeshGradientState(
    initialPoints: Array<Offset>,
    initialColors: Array<Color>
)
```

#### Properties

| Property | Type | Description |
| :--- | :--- | :--- |
| `points` | `List<Offset>` | Current list of control point offsets (allocates list on read for backward compatibility). |
| `colors` | `List<Color>` | Current list of colors for control points. |

#### Methods

```kotlin
/** Returns a zero-allocation snapshot float array of points [x0, y0, x1, y1, ...]. */
fun pointsArray(): FloatArray

/** Returns a zero-allocation snapshot float array of colors [r0, g0, b0, a0, ...]. */
fun colorsArray(): FloatArray

/** Smoothly animates a single control point at [index] to a [target] offset using [spec]. */
suspend fun animatePoint(index: Int, target: Offset, spec: AnimationSpec<Offset>)

/** Mutates the color at [index] instantly. */
fun setColor(index: Int, color: Color)

/** Instantly snaps all control points to [newOffsets]. Useful inside frame loops. */
suspend fun snapAllPoints(newOffsets: List<Offset>)
```

---

### `rememberMeshGradientState`

Creates and remembers a `MeshGradientState` instance across recompositions.

```kotlin
@Composable
fun rememberMeshGradientState(
    points: Array<Offset>,
    colors: Array<Color>
): MeshGradientState
```

---

## Mesh Utilities & Generators

### `generateMeshPoints`

Generates an array of normalized control points for a rectangular grid. Boundary points remain anchored at the edges (`0.0` or `1.0`), while interior points can optionally be perturbed by `randomness`.

```kotlin
fun generateMeshPoints(
    width: Int,
    height: Int,
    randomness: Float = 0.1f
): Array<Offset>
```

- **`randomness`**: Float between `0.0f` (perfect uniform grid) and `1.0f` (maximum inner point displacement). Recommended range: `0.0f` to `0.2f`.

---

### `generateMeshColors`

Generates a full color array of size `width * height` by linearly interpolating across a list of 1 or 2 base colors along rows, with optional random hue/lightness variance.

```kotlin
fun generateMeshColors(
    baseColors: List<Color>,
    width: Int,
    height: Int,
    colorVariance: Float = 0.0f
): Array<Color>
```

- **`baseColors`**: List of 1 or 2 `Color` objects.
- **`colorVariance`**: Random color shift applied to HSL components (`0.0f` to `1.0f`).

---

### `rememberThemedMeshData`

Creates and remembers a `MeshData` configuration that automatically adapts colors when the system switches between Light and Dark themes.

```kotlin
@Composable
fun rememberThemedMeshData(
    lightThemeColors: List<Color>,
    darkThemeColors: List<Color>,
    width: Int,
    height: Int,
    pointRandomness: Float = 0.1f,
    colorVariance: Float = 0.0f
): MeshData
```

---

### `rememberSimplexAnimatedMesh`

Convenience composable helper that creates a continuous, organic "lava lamp" animation driven by 2D Simplex Noise without explicit animation loop boilerplate.

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

#### `AnimatedMeshState` Interface
```kotlin
interface AnimatedMeshState {
    val meshData: MeshData
}
```

---

### `InfiniteTransition.animateOffset`

An extension on `InfiniteTransition` for animating 2D `Offset` targets.

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

Standalone modifier for applying a noise overlay to existing Compose layouts.

```kotlin
fun Modifier.stardust(
    intensity: Float = 0.05f,
    blendMode: BlendMode = BlendMode.Overlay
): Modifier
```

---

## Data Models

### `MeshData`

Immutable container grouping grid control points and their matching colors.

```kotlin
data class MeshData(
    val points: Array<Offset>,
    val colors: Array<Color>
)
```

---

### `BezierPoint`

Representation of a control point with optional directional tangent handles, designed for advanced cubic mesh control (matching SwiftUI's `MeshGradient.BezierPoint`).

```kotlin
data class BezierPoint(
    val position: Offset,
    val leadingControlPoint: Offset = position,
    val topControlPoint: Offset = position,
    val trailingControlPoint: Offset = position,
    val bottomControlPoint: Offset = position
)
```

#### Secondary Constructors & Factory Methods

```kotlin
// Constructor with simple x, y coordinates
constructor(x: Float, y: Float)

// Constructor with symmetric control point offset
constructor(position: Offset, controlPointOffset: Offset)

companion object {
    fun simple(x: Float, y: Float): BezierPoint
    
    fun withControls(
        x: Float,
        y: Float,
        leadingOffset: Offset = Offset.Zero,
        topOffset: Offset = Offset.Zero,
        trailingOffset: Offset = Offset.Zero,
        bottomOffset: Offset = Offset.Zero
    ): BezierPoint
}
```

---

## Procedural Noise Engine

### `SimplexNoise`

An optimized 2D implementation of Ken Perlin's Simplex Noise algorithm.

```kotlin
object SimplexNoise {
    /** Generates a 2D noise value in range [-1.0, 1.0] for coordinates (xin, yin). */
    fun noise(xin: Float, yin: Float): Float
}
```

---

## Shader & Technical Specifications

### Bicubic Interpolation & Solver

- **Catmull-Rom Surface Evaluation**: Shader calculates spatial coordinates and color values across grid cells using 2D Catmull-Rom spline interpolation for smooth derivatives across control points.
- **Levenberg-Marquardt (LM) Inversion**: Inverts non-linear bicubic point displacements in GPU shader execution using 5 damped Gauss-Newton iterations.
- **sRGB Color Preservation**: Color mixing occurs in perceptual gamma space and converts to linear RGB prior to framebuffer write, preventing dark seam artifacts.

### Constraints & Limits

| Constraint | Limit | Reason |
| :--- | :--- | :--- |
| Max Control Points | `25` (e.g., 5x5 grid) | Fixed uniform buffer packing in shader for cross-platform compatibility (`uPackedPoints[13]`, `uColors[25]`). |
| Min Grid Dimensions | `2 x 2` | Minimum grid size required for bicubic patch evaluation. |
| Normalized Coordinate Bounds | `[0.0, 1.0]` | Control points represent normalized fraction of container size. |

---

## Usage Examples

### 1. Simple 2x2 Static Mesh
```kotlin
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
```

### 2. Animated Control Point
```kotlin
val transition = rememberInfiniteTransition()
val animatedCenter = transition.animateOffset(
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
        Offset(0f, 0f), Offset(0.5f, 0f), Offset(1f, 0f),
        Offset(0f, 0.5f), animatedCenter.value, Offset(1f, 0.5f),
        Offset(0f, 1f), Offset(0.5f, 1f), Offset(1f, 1f)
    ),
    colors = arrayOf(
        Color.Red, Color.Yellow, Color.Green,
        Color.Cyan, Color.Blue, Color.Magenta,
        Color.Black, Color.Gray, Color.White
    ),
    modifier = Modifier.fillMaxSize()
)
```

### 3. Simplex Noise Fluid Mesh Gradient with Dust Grain
```kotlin
val animatedMesh = rememberSimplexAnimatedMesh(
    width = 4,
    height = 4,
    colors = listOf(
        Color(0xFFF36E21), Color(0xFFF0A92A), Color(0xFFE7D043), Color(0xFFE7E95D),
        Color(0xFFDFF168), Color(0xFFB1E192), Color(0xFF7FD3A9), Color(0xFF5EC6B8),
        Color(0xFF4AB8C2), Color(0xFF4099B9), Color(0xFF3B79A8), Color(0xFF395B93),
        Color(0xFF36447C), Color(0xFF332E66), Color(0xFF301E4E), Color(0xFF2D1137)
    ),
    animationSpeed = 0.15f,
    noiseIntensity = 0.12f
)

StardustMesh(
    width = 4,
    height = 4,
    points = animatedMesh.meshData.points,
    colors = animatedMesh.meshData.colors,
    dustIntensity = 0.05f,
    dustAnimationSpeed = 1.0f,
    modifier = Modifier.fillMaxSize()
)
```
