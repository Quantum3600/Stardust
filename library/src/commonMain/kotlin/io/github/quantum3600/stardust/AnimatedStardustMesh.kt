package io.github.quantum3600.stardust

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.quantum3600.stardust.utils.SimplexNoise
import io.github.quantum3600.stardust.utils.normalizeColorGrid

/**
 * State holder for [AnimatedStardustMesh] that manages smooth, zero-allocation Simplex noise mesh animations.
 */
@Stable
class AnimatedStardustMeshState(
    val width: Int = 3,
    val height: Int = 3,
    initialColors: List<Color> = defaultColors,
    initialSpeed: Float = 0.15f,
    initialWobbleAmount: Float = 0.12f
) {
    var speed by mutableFloatStateOf(initialSpeed)
    var wobbleAmount by mutableFloatStateOf(initialWobbleAmount)
    var isPaused by mutableStateOf(false)

    var time: Float = 0f
        private set

    var dustPhase: Float = 0f
        private set

    // Base grid positions (normalized 0..1)
    private val basePointsX = FloatArray(width * height) { i ->
        val w = (width - 1).coerceAtLeast(1)
        (i % width) / w.toFloat()
    }
    private val basePointsY = FloatArray(width * height) { i ->
        val h = (height - 1).coerceAtLeast(1)
        (i / width) / h.toFloat()
    }

    // Running positions for smooth interpolation (zero allocations during animation)
    private val currentPointsX = basePointsX.copyOf()
    private val currentPointsY = basePointsY.copyOf()
    private val targetPointsX = basePointsX.copyOf()
    private val targetPointsY = basePointsY.copyOf()

    // Output FloatArrays consumed directly by drawing pass
    internal val framePoints = FloatArray(width * height * 2)
    internal val frameColors = FloatArray(width * height * 4)

    private var pointsVersion by mutableIntStateOf(0)
    private var colorsVersion by mutableIntStateOf(0)

    init {
        setColors(initialColors)
        for (i in 0 until (width * height)) {
            framePoints[i * 2] = basePointsX[i]
            framePoints[i * 2 + 1] = basePointsY[i]
        }
    }

    /**
     * Updates the colors of the mesh gradient.
     * Can accept 4 corner colors, a flat list of [width] * [height] colors, or any base colors.
     */
    fun setColors(colors: List<Color>) {
        val normalized = normalizeColorGrid(colors, width, height)
        val limit = minOf(normalized.size, width * height)
        for (i in 0 until limit) {
            val c = normalized[i]
            frameColors[i * 4 + 0] = c.red
            frameColors[i * 4 + 1] = c.green
            frameColors[i * 4 + 2] = c.blue
            frameColors[i * 4 + 3] = c.alpha
        }
        colorsVersion++
    }

    /**
     * Ticks the animation frame synchronized to VSYNC with zero allocations.
     */
    fun updateFrame(deltaTime: Float, dustAnimationSpeed: Float = 0f) {
        if (isPaused) return

        if (speed > 0f) {
            time += deltaTime * speed
            val numPoints = width * height
            val currentWobble = wobbleAmount

            for (i in 0 until numPoints) {
                val col = i % width
                val row = i / width
                val isBorder = row == 0 || row == height - 1 || col == 0 || col == width - 1

                if (isBorder) {
                    targetPointsX[i] = basePointsX[i]
                    targetPointsY[i] = basePointsY[i]
                } else {
                    val bx = basePointsX[i]
                    val by = basePointsY[i]
                    val noiseX = SimplexNoise.noise(bx * 1.5f, time + i) * currentWobble
                    val noiseY = SimplexNoise.noise(by * 1.5f, time + i + 100f) * currentWobble
                    targetPointsX[i] = bx + noiseX
                    targetPointsY[i] = by + noiseY
                }
            }

            val factor = (4f * deltaTime).coerceIn(0f, 1f)
            for (i in 0 until numPoints) {
                currentPointsX[i] = currentPointsX[i] + (targetPointsX[i] - currentPointsX[i]) * factor
                currentPointsY[i] = currentPointsY[i] + (targetPointsY[i] - currentPointsY[i]) * factor
                framePoints[i * 2] = currentPointsX[i]
                framePoints[i * 2 + 1] = currentPointsY[i]
            }
            pointsVersion++
        }

        if (dustAnimationSpeed > 0f) {
            dustPhase += deltaTime * dustAnimationSpeed
        }
    }

    fun pointsArray(): FloatArray {
        @Suppress("UNUSED_EXPRESSION")
        pointsVersion
        return framePoints
    }

    fun colorsArray(): FloatArray {
        @Suppress("UNUSED_EXPRESSION")
        colorsVersion
        return frameColors
    }

    companion object {
        val defaultColors = listOf(
            Color(0xFFF36E21), Color(0xFFF0A92A),
            Color(0xFF36447C), Color(0xFF2D1137)
        )
    }
}

/** Backward-compatible typealias for [AnimatedStardustMeshState]. */
typealias AnimatedMeshGradientState = AnimatedStardustMeshState

/**
 * Creates and remembers an [AnimatedStardustMeshState].
 */
@Composable
fun rememberAnimatedStardustMeshState(
    width: Int = 3,
    height: Int = 3,
    colors: List<Color> = AnimatedStardustMeshState.defaultColors,
    speed: Float = 0.15f,
    wobbleAmount: Float = 0.12f
): AnimatedStardustMeshState {
    return remember(width, height) {
        AnimatedStardustMeshState(
            width = width,
            height = height,
            initialColors = colors,
            initialSpeed = speed,
            initialWobbleAmount = wobbleAmount
        )
    }
}

/** Backward-compatible helper alias for [rememberAnimatedStardustMeshState]. */
@Composable
fun rememberAnimatedMeshGradientState(
    width: Int = 3,
    height: Int = 3,
    colors: List<Color> = AnimatedStardustMeshState.defaultColors,
    speed: Float = 0.15f,
    wobbleAmount: Float = 0.12f
): AnimatedStardustMeshState = rememberAnimatedStardustMeshState(width, height, colors, speed, wobbleAmount)

/**
 * An intuitive, ready-to-use animated mesh gradient composable driven by continuous Simplex noise.
 * Completely hides low-level VSYNC loops, frame callbacks, and point calculations while preserving maximum performance.
 *
 * @param colors List of colors for the mesh. Can be 4 corner colors or a flat grid of [width] * [height] colors.
 * @param modifier Composable modifier.
 * @param width Horizontal grid dimension (default 3).
 * @param height Vertical grid dimension (default 3).
 * @param speed Animation speed for mesh point movement (default 0.15f). Set to 0f to freeze motion.
 * @param wobbleAmount Intensity of interior point movement/distortion (default 0.12f).
 * @param dustIntensity Procedural film grain/dust overlay intensity (0.0f to 1.0f).
 * @param grainIntensity Alias for [dustIntensity].
 * @param dustAnimationSpeed Speed of dust particle animation (default 10f).
 * @param state Optional state holder to control animation programmatically.
 */
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
    state: AnimatedStardustMeshState = rememberAnimatedStardustMeshState(
        width = width,
        height = height,
        colors = colors,
        speed = speed,
        wobbleAmount = wobbleAmount
    )
) {
    val effectiveDustIntensity = if (grainIntensity != 0f) grainIntensity else dustIntensity

    LaunchedEffect(colors, speed, wobbleAmount) {
        state.speed = speed
        state.wobbleAmount = wobbleAmount
        state.setColors(colors)
    }

    LaunchedEffect(state, dustAnimationSpeed) {
        var lastTime = 0L
        while (true) {
            withFrameNanos { frameTime ->
                if (lastTime != 0L) {
                    val dt = (frameTime - lastTime) / 1_000_000_000f
                    state.updateFrame(dt, dustAnimationSpeed)
                }
                lastTime = frameTime
            }
        }
    }

    StardustMesh(
        width = state.width,
        height = state.height,
        pointsProvider = { state.pointsArray() },
        colorsProvider = { state.colorsArray() },
        modifier = modifier,
        dustIntensityProvider = { effectiveDustIntensity },
        dustPhaseProvider = { state.dustPhase }
    )
}

/**
 * Overload of [AnimatedStardustMesh] taking a 2D list of grid colors.
 */
@Composable
fun AnimatedStardustMesh(
    colorsGrid: List<List<Color>>,
    modifier: Modifier = Modifier,
    speed: Float = 0.15f,
    wobbleAmount: Float = 0.12f,
    dustIntensity: Float = 0f,
    grainIntensity: Float = dustIntensity,
    dustAnimationSpeed: Float = 10f,
) {
    val height = colorsGrid.size
    val width = colorsGrid.firstOrNull()?.size ?: 0
    val flatColors = remember(colorsGrid) { colorsGrid.flatten() }

    AnimatedStardustMesh(
        colors = flatColors,
        modifier = modifier,
        width = width,
        height = height,
        speed = speed,
        wobbleAmount = wobbleAmount,
        dustIntensity = dustIntensity,
        grainIntensity = grainIntensity,
        dustAnimationSpeed = dustAnimationSpeed
    )
}

/** Alias for [AnimatedStardustMesh]. */
@Composable
fun AnimatedMeshGradient(
    colors: List<Color>,
    modifier: Modifier = Modifier,
    width: Int = 3,
    height: Int = 3,
    speed: Float = 0.15f,
    wobbleAmount: Float = 0.12f,
    dustIntensity: Float = 0f,
    grainIntensity: Float = dustIntensity,
    dustAnimationSpeed: Float = 10f,
    state: AnimatedStardustMeshState = rememberAnimatedStardustMeshState(
        width = width,
        height = height,
        colors = colors,
        speed = speed,
        wobbleAmount = wobbleAmount
    )
) {
    AnimatedStardustMesh(
        colors = colors,
        modifier = modifier,
        width = width,
        height = height,
        speed = speed,
        wobbleAmount = wobbleAmount,
        dustIntensity = dustIntensity,
        grainIntensity = grainIntensity,
        dustAnimationSpeed = dustAnimationSpeed,
        state = state
    )
}

/** Alias for [AnimatedStardustMesh]. */
@Composable
fun AnimatedMeshGradient(
    colorsGrid: List<List<Color>>,
    modifier: Modifier = Modifier,
    speed: Float = 0.15f,
    wobbleAmount: Float = 0.12f,
    dustIntensity: Float = 0f,
    grainIntensity: Float = dustIntensity,
    dustAnimationSpeed: Float = 10f,
) {
    AnimatedStardustMesh(
        colorsGrid = colorsGrid,
        modifier = modifier,
        speed = speed,
        wobbleAmount = wobbleAmount,
        dustIntensity = dustIntensity,
        grainIntensity = grainIntensity,
        dustAnimationSpeed = dustAnimationSpeed
    )
}
