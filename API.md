# Stardust API Reference & Usage Tutorial

Comprehensive API reference and tutorial for **Stardust**, a Compose Multiplatform library for GPU-accelerated bicubic mesh gradients and procedural film grain.

---

## Core Composables

### `AnimatedStardustMesh`
High-level composable for continuous Simplex noise mesh gradient animations.

```kotlin
@Composable
fun AnimatedStardustMesh(
    colors: List<Color>,
    modifier: Modifier = Modifier,
    width: Int = 3,
    height: Int = 3,
    speed: Float = 0.15f,
    wobbleAmount: Float = 0.12f,
    dustIntensity: Float = 0f,
    grainIntensity: Float = dustIntensity,
    dustAnimationSpeed: Float = 10f,
    state: AnimatedStardustMeshState = rememberAnimatedStardustMeshState(...)
)
```

#### Key Parameters
- `colors`: 4 corner colors or a grid list of `width * height` colors. Automatically interpolated spatially across the grid.
- `width`: Horizontal grid count (default `3`).
- `height`: Vertical grid count (default `3`).
- `speed`: Animation speed of interior point movement (default `0.15f`). Set to `0f` to freeze motion.
- `wobbleAmount`: Intensity of point movement/distortion (default `0.12f`).
- `dustIntensity`: Procedural film grain overlay intensity (`0.0f` to `1.0f`).
- `grainIntensity`: Alias for `dustIntensity`.
- `dustAnimationSpeed`: Speed of film grain noise animation (default `10f`).

---

### `AnimatedBezierStardustMesh`
High-level composable for animated Bézier mesh gradients with smooth tangent handle motion and organic curves.

```kotlin
@Composable
fun AnimatedBezierStardustMesh(
    colors: List<Color>,
    modifier: Modifier = Modifier,
    width: Int = 3,
    height: Int = 3,
    speed: Float = 1.0f,
    randomness: Float = 0.18f,
    tangentWobble: Float = 0.15f,
    dustIntensity: Float = 0f,
    grainIntensity: Float = dustIntensity,
    dustAnimationSpeed: Float = 60f,
    state: AnimatedBezierStardustMeshState = rememberAnimatedBezierStardustMeshState(...)
)
```

---

### `StardustMesh`
Base rendering composable for static grids, custom `BezierPoint` grids, or state-driven meshes.

```kotlin
@Composable
fun StardustMesh(
    modifier: Modifier = Modifier,
    width: Int,
    height: Int,
    points: Array<Offset>,
    colors: Array<Color>,
    dustIntensity: Float = 0f,
    dustAnimationSpeed: Float = 0f,
)

@Composable
fun StardustMesh(
    modifier: Modifier = Modifier,
    width: Int,
    height: Int,
    state: StardustMeshState,
    dustIntensity: Float = 0f,
    dustAnimationSpeed: Float = 0f,
)

@Composable
fun StardustMesh(
    modifier: Modifier = Modifier,
    state: StardustBezierMeshState,
    dustIntensity: Float = 0f,
    dustAnimationSpeed: Float = 0f,
)
```

---

## State Holders

### `StardustMeshState`
State holder for custom point animations and manual point manipulation.

```kotlin
val meshState = rememberStardustMeshState(points, colors)
```

### `StardustBezierMeshState`
State holder for custom 2D grid `BezierPoint` manipulation and tangent handles.

```kotlin
val bezierState = rememberStardustBezierMeshState(bezierPoints, colors)
```

---

## Modifiers

### `Modifier.stardust`
Applies procedural film grain overlays to any Compose element.

```kotlin
Modifier.stardust(
    intensity = 0.08f,
    seed = animTime,
    blendMode = BlendMode.Overlay
)
```
