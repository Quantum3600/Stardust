# API reference

Package: `io.github.quantum3600.stardust` (utils in `.utils`, data in `.data`).

- [Concepts](#concepts)
- [Composables](#composables)
- [Modifiers](#modifiers-modifierstardustmesh)
- [StardustMeshState](#stardustmeshstate)
- [Utilities](#utilities)
- [Animation helpers](#animation-helpers)
- [Platform backends](#platform-backends)
- [Performance and limits](#performance-and-limits)
- [Threading](#threading)
- [Troubleshooting](#troubleshooting)
- [Migrating from the Android-only library](#migrating-from-the-android-only-library)

---

## Concepts

A mesh is a `width x height` grid of **control points**. Each point has a **position** and a **colour**.
Arrays are flattened row-major: `index = row * width + column`.

| Convention | Value |
|---|---|
| Coordinate space | Normalized `[0, 1]` on both axes, relative to the composable's bounds |
| Origin | **Top-left**, y grows downward (Compose convention) |
| Minimum grid | 2 x 2 |
| Interpolation | Catmull-Rom (bicubic) between control points; edge neighbours are clamped |
| Border points | Normally kept on the edges (`0` or `1`) so the mesh fills the composable. Areas not covered by the mesh are transparent |

---

## Composables

### `StardustMesh` (array overload)

```kotlin
@Composable
fun StardustMesh(
    modifier: Modifier = Modifier,
    width: Int,
    height: Int,
    points: Array<Offset>,
    colors: Array<Color>,
    globalSubdivisions: Int = 32,
)
```

Renders a mesh from fixed arrays. Recomposes when you pass new arrays.

| Parameter | Description |
|---|---|
| `modifier` | Standard modifier. Size it (e.g. `fillMaxSize()`); the mesh fills the bounds. |
| `width`, `height` | Number of control points per row / column (>= 2). |
| `points` | `width * height` normalized positions. Throws `IllegalArgumentException` on size mismatch. |
| `colors` | `width * height` colours. Throws `IllegalArgumentException` on size mismatch. |
| `globalSubdivisions` | Tessellation per cell. **Only used by the OpenGL fallback** (Android < 33). Ignored by AGSL/SkSL, which are per-pixel. |

Use this overload for static meshes or when you drive the arrays yourself.

### `StardustMesh` (state overload)

```kotlin
@Composable
fun StardustMesh(
    modifier: Modifier = Modifier,
    width: Int,
    height: Int,
    globalSubdivisions: Int = 32,
    state: StardustMeshState,
)
```

Renders from a [`StardustMeshState`](#stardustmeshstate). On AGSL/SkSL the state is read in the **draw phase**,
so animating points or colours only invalidates drawing: no recomposition and no per-frame allocation.
Prefer this overload for anything animated.

### `rememberStardustMeshState`

```kotlin
@Composable
fun rememberStardustMeshState(points: Array<Offset>, colors: Array<Color>): StardustMeshState
```

Remembers a state keyed on the **contents** of `points` and `colors` (not array identity), so an inline
`arrayOf(...)` doesn't reset it on every recomposition. Changing the contents creates a fresh state.

### Modifiers (`Modifier.stardustMesh` & `Modifier.stardustMeshAnimation`)

```kotlin
@Composable
fun Modifier.stardustMesh(
    width: Int,
    height: Int,
    points: Array<Offset>,
    colors: Array<Color>,
    globalSubdivisions: Int = 32,
): Modifier

@Composable
fun Modifier.stardustMesh(
    width: Int,
    height: Int,
    state: StardustMeshState,
    globalSubdivisions: Int = 32,
): Modifier

@Composable
fun Modifier.stardustMesh(
    width: Int,
    height: Int,
    animatedState: AnimatedMeshState,
    globalSubdivisions: Int = 32,
): Modifier

@Composable
fun Modifier.stardustMeshAnimation(
    width: Int,
    height: Int,
    colors: List<Color>,
    animationSpeed: Float = 0.3f,
    noiseIntensity: Float = 0.2f,
    globalSubdivisions: Int = 32,
): Modifier
```

Applies a static, state-driven, or simplex-noise animated mesh gradient directly behind the content of any composable (e.g. `Box`, `Button`, `Text`, `Surface`).

---

## StardustMeshState

```kotlin
@Stable
class StardustMeshState(initialPoints: Array<Offset>, initialColors: Array<Color>)
```

Holds one `Animatable<Offset>` per point and a state list of colours. `initialPoints.size` must equal
`initialColors.size`.

| Member | Description |
|---|---|
| `val points: List<Offset>` | Current positions. Allocates a new list per call; use for reading/inspection. |
| `val colors: List<Color>` | Current colours (observable). |
| `fun pointsArray(): FloatArray` | Reusable `[x0,y0,x1,y1,...]` snapshot. Reads Compose state; call from draw/snapshot scope. Do not retain. |
| `fun colorsArray(): FloatArray` | Reusable `[r,g,b,a,...]` snapshot. Same rules. |
| `suspend fun animatePoint(index, target, spec: AnimationSpec<Offset>)` | Animates one control point. |
| `fun setColor(index, color)` | Sets a colour immediately (triggers a redraw). |
| `suspend fun snapAllPoints(newOffsets: List<Offset>)` | Jumps all points; extra entries are ignored. |

Example:

```kotlin
val state = rememberStardustMeshState(generateMeshPoints(3, 3, 0f), colors)
val scope = rememberCoroutineScope()

MeshGradient(Modifier.fillMaxSize(), width = 3, height = 3, state = state)

Button(onClick = {
    scope.launch {
        state.animatePoint(4, Offset(0.7f, 0.3f), spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessLow))
    }
}) { Text("Move centre") }
```

---

## Utilities

Package `io.github.quantum3600.stardust.utils`.

### `MeshData`

```kotlin
class MeshData(val points: Array<Offset>, val colors: Array<Color>)
```
Container with content-based `equals`/`hashCode`, plus `component1/2` and `copy`.

### `generateMeshPoints`

```kotlin
fun generateMeshPoints(width: Int, height: Int, randomness: Float = 0.1f, random: Random = Random.Default): Array<Offset>
```
Regular grid; **inner** points are jittered by up to `randomness` (`0f..1f`, useful range `0..0.2`), border points are not.
Pass a seeded `Random` for reproducible output.

### `generateMeshColors`

```kotlin
fun generateMeshColors(
    baseColors: List<Color>, // 1 or 2 colours
    width: Int, height: Int,
    colorVariance: Float = 0f, // 0..1
    random: Random = Random.Default,
): Array<Color>
```
One colour fills the mesh; two colours make a top-to-bottom linear (sRGB) gradient. `colorVariance` jitters hue (+/-10 deg) and
HSV value (+/-0.2) per point; alpha is preserved.

### `rememberThemedMeshData`

```kotlin
@Composable
fun rememberThemedMeshData(
    lightThemeColors: List<Color>, darkThemeColors: List<Color>,
    width: Int, height: Int,
    pointRandomness: Float = 0.1f, colorVariance: Float = 0f,
): MeshData
```
Remembers generated data and switches colours with `isSystemInDarkTheme()`. Regenerates when any argument or the theme changes.

### `lerp`

```kotlin
fun lerp(start: Color, stop: Color, fraction: Float): Color   // linear sRGB
fun lerp(start: Offset, stop: Offset, fraction: Float): Offset // fraction clamped to 0..1
```

### `MeshInterpolation` (internal)

CPU reference implementation of the interpolation (`generateMeshVerticesAndColors`, `catmullRom`). Output is normalized `[0,1]`.
Useful for tests or a future CPU renderer; not used by the renderers.

### `SimplexNoise`

```kotlin
object SimplexNoise { fun noise(xin: Float, yin: Float): Float } // [-1, 1]
```
2D simplex noise. The permutation table is randomized once per process.

### `BezierPoint` (`.data`)

Position plus four control points (leading/top/trailing/bottom). **Not used by the renderers yet.**
Factories: `BezierPoint.simple(x, y)`, `BezierPoint.symmetric(position, controlPointOffset)`, `BezierPoint.withControls(...)`.

---

## Animation helpers

### `rememberSimplexAnimatedMesh`

```kotlin
@Composable
fun rememberSimplexAnimatedMesh(
    width: Int, height: Int,
    colors: List<Color>,            // size == width * height
    animationSpeed: Float = 0.3f,
    noiseIntensity: Float = 0.2f,
): AnimatedMeshState

interface AnimatedMeshState {
    val state: StardustMeshState    // pass to MeshGradient(state = ...)
    val meshData: MeshData          // allocating snapshot
}
```
Moves inner points with simplex noise (border points fixed), smoothed toward their targets each frame.
`animationSpeed` and `noiseIntensity` can change live without restarting the loop. Changing `width`, `height` or the
colours' contents restarts the animation.

```kotlin
val anim = rememberSimplexAnimatedMesh(4, 4, colors)
MeshGradient(Modifier.fillMaxSize(), width = 4, height = 4, state = anim.state)
```

### `InfiniteTransition.animateOffset`

```kotlin
@Composable
fun InfiniteTransition.animateOffset(initialValue: Offset, targetValue: Offset, animationSpec: InfiniteRepeatableSpec<Float>): State<Offset>
```
Convenience for infinitely animating an `Offset`.

---

## Platform backends

| Target | Backend | Notes |
|---|---|---|
| Android 33+ | AGSL `RuntimeShader` | If shader creation throws, the library falls back to OpenGL automatically. |
| Android 21-32 | OpenGL ES 3.0 `GLSurfaceView` | Requires an ES 3.0 context. Uses `globalSubdivisions`. |
| Desktop (JVM), iOS, Web (js / wasmJs) | SkSL `RuntimeEffect` (Skiko) | Same shader source as AGSL. |

How the shader backends work: AGSL/SkSL have no vertex stage, so for each pixel the shader finds the mesh cell containing it
(inverse bilinear mapping over the control points) and evaluates the colour with bicubic Catmull-Rom. Cell geometry uses
straight-edged quads through the control points; colour interpolation is fully bicubic. The OpenGL path deforms a tessellated
grid with Catmull-Rom positions, so on strongly bent meshes the two can differ slightly at cell edges.

Output is premultiplied alpha. Pixels outside every cell are transparent.

---

## Performance and limits

- **Grid size:** the shader is unrolled per cell, so GPU cost per pixel grows with the number of cells. 3x3 to 5x5 is the intended range;
  go larger only after profiling on your lowest-end target.
- **Animation:** use the state overload. The array overload rebuilds float arrays on every recomposition.
- **Subdivisions (OpenGL only):** vertex count is `((w-1)*s+1) * ((h-1)*s+1)`. Indices are 32-bit, so there is no 65 535 vertex cap, but cost still scales with it.
- **Folded meshes:** if control points cross so that cells overlap, the shader paths draw the first matching cell; the OpenGL path draws all triangles without culling.

---

## Threading

- `StardustMeshState` and the composables are main-thread APIs.
- On the OpenGL path, arrays are copied on the UI thread before being handed to the GL thread.
- `pointsArray()`/`colorsArray()` return shared buffers; consume them immediately.

---

## Troubleshooting

| Symptom | Cause / fix |
|---|---|
| `IllegalArgumentException: points must have width*height entries` | Array sizes don't match the grid. |
| Blank output on Android < 33 | Device lacks ES 3.0, or shader compile failed; check Logcat for "Mesh gradient shader compile failed". |
| Mesh upside-down versus the old library | The old library drew `y = 0` at the bottom. Now `y = 0` is the top. Use `1f - y` to restore. |
| Animation restarts on recomposition | You pass a new colour list each time. Remember it, or keep its contents stable. |
| iOS build can't find `ComposeApp` | Run `./gradlew :sample:embedAndSignAppleFrameworkForXcode` or build from Xcode (see the README). |
| Web page is blank | Serve via `./gradlew :sample:wasmJsBrowserDevelopmentRun`; opening `index.html` from disk won't work. |

---

## Migrating from the Android-only library

| Before | Now |
|---|---|
| `y = 0` at bottom | `y = 0` at top |
| `StardustMeshRenderer`/`Helper` public | Internal (Android OpenGL fallback only) |
| `MeshData` was a `data class` | Plain class with `component1/2`, `copy`, content `equals` |
| `AnimatedMeshState.meshData` only | Also `state` (preferred) |
| `generateMeshPoints`/animation used `i / height` for the row | Fixed to `i / width` (non-square grids) |
| `BezierPoint` in `github.om252345...` | `io.github.quantum3600.stardust.data` |
| `BezierPoint(position, controlPointOffset)` | `BezierPoint.symmetric(...)` |
