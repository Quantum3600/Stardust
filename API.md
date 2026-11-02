# Stardust API Reference

A comprehensive API documentation for **Stardust**, a Compose Multiplatform library for rendering high-performance, GPU-accelerated bicubic mesh gradients and procedural film grain on Android, iOS, Desktop (JVM), JS, and Wasm.

---

## Table of Contents

1. [Overview & Architecture](#overview--architecture)
2. [Core Composables](#core-composables)
    - [`StardustMesh`](#stardustmesh)
    - [`MeshGradient`](#meshgradient)
3. [State Management](#state-management)
    - [`StardustMeshState`](#stardustmeshstate)
    - [`MeshGradientState`](#meshgradientstate)
    - [`rememberStardustMeshState` / `rememberMeshGradientState`](#rememberstardustmeshstate--remembermeshgradientstate)
4. [Mesh Utilities & Generators](#mesh-utilities--generators)
    - [`generateMeshPoints`](#generatemeshpoints)
    - [`generateMeshColors`](#generatemeshcolors)
    - [`generateSpatialGridColors`](#generatespatialgridcolors)
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

Stardust renders fluid, multi-point color gradients by evaluating a bicubic surface per pixel using custom platform shaders (Android AGSL `RuntimeShader` on API 33+ or Skia `RuntimeEffect` on Desktop, iOS, JS, and Wasm).

### Key Features
- **GPU-Accelerated**: Per-pixel evaluation with zero CPU tessellation or mesh triangle overhead.
- **Explicit Bézier Tangents**: Full support for custom directional handle offsets per point via `BezierPoint`.
- **Procedural Film Grain**: Fast, non-repetitive GPU hash/dithering stage (`grainIntensity`, `grainSeed`).
- **Levenberg-Marquardt UV Inversion**: Solves inverse bicubic positions in shader code with Newton divergence guards.
- **State-Driven Animations**: Fine-grained point and color state holders (`StardustMeshState` / `MeshGradientState`) supporting 60+ FPS animations without object allocation churn.
- **Standalone Stardust Modifier**: Apply customizable film grain overlays directly to any Compose component via `Modifier.stardust`.

---

## Core Composables

### `StardustMesh` / `MeshGradient`

Primary composable functions for rendering mesh gradients in Stardust. `MeshGradient` is an exact alias for `StardustMesh`.

#### Overload 1: Explicit Bézier Control Points (`List<List<BezierPoint>>`)

```kotlin
@Composable
fun StardustMesh(
    bezierPoints: List<List<BezierPoint>>,
    colors: List<List<Color>>,
    modifier: Modifier = Modifier,
    grainIntensity: Float = 0f,
    grainSeed: Float = 0f,
)
```

##### Parameters

| Parameter | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `bezierPoints` | `List<List<BezierPoint>>` | *(Required)* | 2D grid list of `BezierPoint`s containing positions and custom tangent handles. |
| `colors` | `List<List<Color>>` | *(Required)* | 2D grid list of colors corresponding to each `BezierPoint`. |
| `modifier` | `Modifier` | `Modifier` | Layout modifier applied to the internal `Canvas`. |
| `grainIntensity` | `Float` | `0f` | Procedural stardust / film grain intensity (`0.0f` to `1.0f`). |
| `grainSeed` | `Float` | `0f` | Seed value or animation frame time for film grain noise generation. |

#### Overload 2: 2D Grid Position List (`List<List<Offset>>`)

```kotlin
@Composable
fun StardustMesh(
    points: List<List<Offset>>,
    colors: List<List<Color>>,
    modifier: Modifier = Modifier,
    grainIntensity: Float = 0f,
    grainSeed: Float = 0f,
)
```

Automatically generates neutral cardinal Catmull-Rom Bézier tangents for full backward compatibility.

#### Overload 3: Direct Arrays & Grid Dimensions

```kotlin
@Composable
fun StardustMesh(
    modifier: Modifier = Modifier,
    width: Int,
    height: Int,
    points: Array<Offset>,
    colors: Array<Color>,
    uTangents: FloatArray? = null,
    vTangents: FloatArray? = null,
    globalSubdivisions: Int = 32,
    dustIntensity: Float = 0f,
    dustAnimationSpeed: Float = 0f,
)
```

#### Overload 4: Driven by `StardustMeshState` / `MeshGradientState`

```kotlin
@Composable
fun StardustMesh(
    modifier: Modifier = Modifier,
    width: Int,
    height: Int,
    globalSubdivisions: Int = 32,
    dustIntensity: Float = 0f,
    dustAnimationSpeed: Float = 0f,
    state: StardustMeshState,
)
```

##### Exceptions Thrown
- `IllegalArgumentException` if `width <= 1` or `height <= 1`.
- `IllegalArgumentException` if `points.size != width * height` or `colors.size != width * height`.
- `IllegalArgumentException` if `width * height > MeshGradientShader.maxControlPoints` (max 25 points).
- `IllegalArgumentException` if `dustIntensity` / `grainIntensity` is not in range `0.0f..1.0f`.

---

## State Management

### `StardustMeshState` / `MeshGradientState`

A `@Stable` state holder designed for frame-by-frame manipulation and high-performance animation of control points and colors. `MeshGradientState` is a direct typealias for `StardustMeshState`.

```kotlin
@Stable
class StardustMeshState(
    initialPoints: Array<Offset>,
    initialColors: Array<Color>
)
```

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

### `rememberStardustMeshState` / `rememberMeshGradientState`

Creates and remembers a `StardustMeshState` / `MeshGradientState` instance across recompositions.

```kotlin
@Composable
fun rememberStardustMeshState(
    points: Array<Offset>,
    colors: Array<Color>
): StardustMeshState
```

---

## Mesh Utilities & Generators

### `generateSpatialGridColors`

Generates a full color array of size `width * height` using 2D spatial grid coordinates ($f_x = \text{col}/(\text{width}-1)$ and $f_y = \text{row}/(\text{height}-1)$) to smoothly blend hues across 4 corners (top-left, top-right, bottom-left, bottom-right).

```kotlin
fun generateSpatialGridColors(
    width: Int,
    height: Int,
    topLeft: Color,
    topRight: Color,
    bottomLeft: Color,
    bottomRight: Color,
    colorVariance: Float = 0.0f
): Array<Color>
```

---

### `Modifier.stardust`

A standalone modifier that applies procedural stardust/film grain noise overlay over any Compose component or layout.

```kotlin
fun Modifier.stardust(
    intensity: Float = 0.05f,
    seed: Float = 0f,
    blendMode: BlendMode = BlendMode.Overlay
): Modifier
```

##### Parameters

| Parameter | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `intensity` | `Float` | `0.05f` | Noise grain intensity (`0.0f` = disabled, `0.05f` = subtle grain, `0.2f` = heavy noise). |
| `seed` | `Float` | `0f` | Seed value or animation frame time phase for noise generation. |
| `blendMode` | `BlendMode` | `BlendMode.Overlay` | Blend mode used to layer noise over composable content. Defaults to `BlendMode.Overlay`. |

---

## Data Models

### `BezierPoint`

Representation of a control point with explicit directional tangent handle offsets (`uTangentLeading`, `uTangentTrailing`, `vTangentTop`, `vTangentBottom`), matching SwiftUI's `MeshGradient.BezierPoint`.

```kotlin
data class BezierPoint(
    val position: Offset,
    val leadingControlPoint: Offset = position,
    val topControlPoint: Offset = position,
    val trailingControlPoint: Offset = position,
    val bottomControlPoint: Offset = position
) {
    val uTangentLeading: Offset get() = leadingControlPoint - position
    val uTangentTrailing: Offset get() = trailingControlPoint - position
    val vTangentTop: Offset get() = topControlPoint - position
    val vTangentBottom: Offset get() = bottomControlPoint - position
}
```

---

## Usage Examples

### 1. Asymmetric Bézier Handles & Film Grain with Stardust
```kotlin
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
```

### 2. Applying Film Grain Overlay to Components
```kotlin
Card(
    modifier = Modifier
        .fillMaxWidth()
        .stardust(intensity = 0.08f, seed = time, blendMode = BlendMode.Overlay)
) {
    Text("Stardust Grain Overlay", modifier = Modifier.padding(16.dp))
}
```
