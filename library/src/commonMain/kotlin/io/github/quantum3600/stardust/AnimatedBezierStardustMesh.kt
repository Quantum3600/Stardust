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
 * State holder for [AnimatedBezierStardustMesh] that manages smooth, zero-allocation Bézier handle and position motion.
 */
@Stable
class AnimatedBezierStardustMeshState(
    val width: Int = 3,
    val height: Int = 3,
    initialColors: List<Color> = defaultColors,
    initialSpeed: Float = 1.0f,
    initialRandomness: Float = 0.28f,
    initialTangentWobble: Float = 0.30f
) {
    var speed by mutableFloatStateOf(initialSpeed)
    var randomness by mutableFloatStateOf(initialRandomness)
    var tangentWobble by mutableFloatStateOf(initialTangentWobble)
    var isPaused by mutableStateOf(false)

    var time: Float = 0f
        private set

    var dustPhase by mutableFloatStateOf(0f)
        private set

    internal val framePoints = FloatArray(width * height * 2)
    internal val frameColors = FloatArray(width * height * 4)
    internal val frameUTangents = FloatArray(StardustMeshShader.maxControlPoints * 4)
    internal val frameVTangents = FloatArray(StardustMeshShader.maxControlPoints * 4)

    private var version by mutableIntStateOf(0)

    init {
        setColors(initialColors)
        updateFrame(0f, 0f)
    }

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
        version++
    }

    fun updateFrame(deltaTime: Float, dustAnimationSpeed: Float = 0f) {
        if (isPaused) return

        time += deltaTime

        if (speed > 0f) {
            val r = randomness
            val tw = tangentWobble
            val animSpeed = speed

            for (row in 0 until height) {
                for (col in 0 until width) {
                    val idx = row * width + col
                    val posX = col / (width - 1f).coerceAtLeast(1f)
                    val posY = row / (height - 1f).coerceAtLeast(1f)

                    val isBorder = row == 0 || row == height - 1 || col == 0 || col == width - 1

                    val activeX = if (isBorder) posX else posX + SimplexNoise.noise(posX * 2f + time * 0.5f * animSpeed, idx.toFloat()) * r
                    val activeY = if (isBorder) posY else posY + SimplexNoise.noise(posY * 2f + time * 0.5f * animSpeed, idx + 50f) * r

                    framePoints[idx * 2] = activeX
                    framePoints[idx * 2 + 1] = activeY

                    val tldX = -0.22f + SimplexNoise.noise(time * 0.9f * animSpeed, idx + 10f) * tw
                    val tldY = SimplexNoise.noise(time * 0.9f * animSpeed, idx + 20f) * tw

                    val ttrX = 0.22f + SimplexNoise.noise(time * 0.9f * animSpeed, idx + 30f) * tw
                    val ttrY = SimplexNoise.noise(time * 0.9f * animSpeed, idx + 40f) * tw

                    val ttopX = SimplexNoise.noise(time * 0.9f * animSpeed, idx + 60f) * tw
                    val ttopY = -0.22f + SimplexNoise.noise(time * 0.9f * animSpeed, idx + 70f) * tw

                    val tbotX = SimplexNoise.noise(time * 0.9f * animSpeed, idx + 80f) * tw
                    val tbotY = 0.22f + SimplexNoise.noise(time * 0.9f * animSpeed, idx + 90f) * tw

                    val uBase = idx * 4
                    frameUTangents[uBase + 0] = tldX
                    frameUTangents[uBase + 1] = tldY
                    frameUTangents[uBase + 2] = ttrX
                    frameUTangents[uBase + 3] = ttrY

                    val vBase = idx * 4
                    frameVTangents[vBase + 0] = ttopX
                    frameVTangents[vBase + 1] = ttopY
                    frameVTangents[vBase + 2] = tbotX
                    frameVTangents[vBase + 3] = tbotY
                }
            }
            version++
        }

        if (dustAnimationSpeed > 0f) {
            dustPhase += deltaTime * dustAnimationSpeed
        }
    }

    fun pointsArray(): FloatArray {
        @Suppress("UNUSED_EXPRESSION")
        version
        return framePoints
    }

    fun colorsArray(): FloatArray {
        @Suppress("UNUSED_EXPRESSION")
        version
        return frameColors
    }

    fun uTangentsArray(): FloatArray {
        @Suppress("UNUSED_EXPRESSION")
        version
        return frameUTangents
    }

    fun vTangentsArray(): FloatArray {
        @Suppress("UNUSED_EXPRESSION")
        version
        return frameVTangents
    }

    companion object {
        val defaultColors = listOf(
            Color(0xFFF36E21), Color(0xFFF0A92A),
            Color(0xFF36447C), Color(0xFF2D1137)
        )
    }
}

/** Backward-compatible typealias for [AnimatedBezierStardustMeshState]. */
typealias AnimatedBezierMeshGradientState = AnimatedBezierStardustMeshState

/**
 * Creates and remembers an [AnimatedBezierStardustMeshState].
 */
@Composable
fun rememberAnimatedBezierStardustMeshState(
    width: Int = 3,
    height: Int = 3,
    colors: List<Color> = AnimatedBezierStardustMeshState.defaultColors,
    speed: Float = 1.0f,
    randomness: Float = 0.28f,
    tangentWobble: Float = 0.30f
): AnimatedBezierStardustMeshState {
    return remember(width, height) {
        AnimatedBezierStardustMeshState(
            width = width,
            height = height,
            initialColors = colors,
            initialSpeed = speed,
            initialRandomness = randomness,
            initialTangentWobble = tangentWobble
        )
    }
}

/** Backward-compatible helper alias for [rememberAnimatedBezierStardustMeshState]. */
@Composable
fun rememberAnimatedBezierMeshGradientState(
    width: Int = 3,
    height: Int = 3,
    colors: List<Color> = AnimatedBezierStardustMeshState.defaultColors,
    speed: Float = 1.0f,
    randomness: Float = 0.28f,
    tangentWobble: Float = 0.30f
): AnimatedBezierStardustMeshState = rememberAnimatedBezierStardustMeshState(
    width, height, colors, speed, randomness, tangentWobble
)

/**
 * An intuitive, ready-to-use animated Bézier mesh gradient composable with smooth tangent handle motion.
 * Produces organic, fluid color pulling and curves without requiring manual Bézier handle calculations.
 *
 * @param colors List of colors for the mesh. Can be 4 corner colors or a grid of [width] * [height] colors.
 * @param modifier Composable modifier.
 * @param width Horizontal grid dimension (default 3).
 * @param height Vertical grid dimension (default 3).
 * @param speed Animation speed for mesh motion (default 1.0f).
 * @param randomness Intensity of point movement/distortion (default 0.28f).
 * @param tangentWobble Intensity of Bézier tangent handle motion (default 0.30f).
 * @param dustIntensity Procedural film grain/dust overlay intensity (0.0f to 1.0f).
 * @param grainIntensity Alias for [dustIntensity].
 * @param dustAnimationSpeed Speed of dust particle animation (default 60f).
 * @param state Optional state holder to control animation programmatically.
 */
@Composable
fun AnimatedBezierStardustMesh(
    colors: List<Color>,
    modifier: Modifier = Modifier,
    width: Int = 3,
    height: Int = 3,
    speed: Float = 1.0f,
    randomness: Float = 0.28f,
    tangentWobble: Float = 0.30f,
    dustIntensity: Float = 0f,
    grainIntensity: Float = dustIntensity,
    dustAnimationSpeed: Float = 60f,
    state: AnimatedBezierStardustMeshState = rememberAnimatedBezierStardustMeshState(
        width = width,
        height = height,
        colors = colors,
        speed = speed,
        randomness = randomness,
        tangentWobble = tangentWobble
    )
) {
    val effectiveDustIntensity = if (grainIntensity != 0f) grainIntensity else dustIntensity

    LaunchedEffect(colors, speed, randomness, tangentWobble) {
        state.speed = speed
        state.randomness = randomness
        state.tangentWobble = tangentWobble
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
        uTangentsProvider = { state.uTangentsArray() },
        vTangentsProvider = { state.vTangentsArray() },
        dustIntensityProvider = { effectiveDustIntensity },
        dustPhaseProvider = { state.dustPhase }
    )
}

/**
 * Overload of [AnimatedBezierStardustMesh] taking a 2D list of grid colors.
 */
@Composable
fun AnimatedBezierStardustMesh(
    colorsGrid: List<List<Color>>,
    modifier: Modifier = Modifier,
    speed: Float = 1.0f,
    randomness: Float = 0.28f,
    tangentWobble: Float = 0.30f,
    dustIntensity: Float = 0f,
    grainIntensity: Float = dustIntensity,
    dustAnimationSpeed: Float = 60f,
) {
    val height = colorsGrid.size
    val width = colorsGrid.firstOrNull()?.size ?: 0
    val flatColors = remember(colorsGrid) { colorsGrid.flatten() }

    AnimatedBezierStardustMesh(
        colors = flatColors,
        modifier = modifier,
        width = width,
        height = height,
        speed = speed,
        randomness = randomness,
        tangentWobble = tangentWobble,
        dustIntensity = dustIntensity,
        grainIntensity = grainIntensity,
        dustAnimationSpeed = dustAnimationSpeed
    )
}

/** Alias for [AnimatedBezierStardustMesh]. */
@Composable
fun AnimatedBezierMeshGradient(
    colors: List<Color>,
    modifier: Modifier = Modifier,
    width: Int = 3,
    height: Int = 3,
    speed: Float = 1.0f,
    randomness: Float = 0.28f,
    tangentWobble: Float = 0.30f,
    dustIntensity: Float = 0f,
    grainIntensity: Float = dustIntensity,
    dustAnimationSpeed: Float = 60f,
    state: AnimatedBezierStardustMeshState = rememberAnimatedBezierStardustMeshState(
        width = width,
        height = height,
        colors = colors,
        speed = speed,
        randomness = randomness,
        tangentWobble = tangentWobble
    )
) {
    AnimatedBezierStardustMesh(
        colors = colors,
        modifier = modifier,
        width = width,
        height = height,
        speed = speed,
        randomness = randomness,
        tangentWobble = tangentWobble,
        dustIntensity = dustIntensity,
        grainIntensity = grainIntensity,
        dustAnimationSpeed = dustAnimationSpeed,
        state = state
    )
}

/** Alias for [AnimatedBezierStardustMesh]. */
@Composable
fun AnimatedBezierMeshGradient(
    colorsGrid: List<List<Color>>,
    modifier: Modifier = Modifier,
    speed: Float = 1.0f,
    randomness: Float = 0.28f,
    tangentWobble: Float = 0.30f,
    dustIntensity: Float = 0f,
    grainIntensity: Float = dustIntensity,
    dustAnimationSpeed: Float = 60f,
) {
    AnimatedBezierStardustMesh(
        colorsGrid = colorsGrid,
        modifier = modifier,
        speed = speed,
        randomness = randomness,
        tangentWobble = tangentWobble,
        dustIntensity = dustIntensity,
        grainIntensity = grainIntensity,
        dustAnimationSpeed = dustAnimationSpeed
    )
}
