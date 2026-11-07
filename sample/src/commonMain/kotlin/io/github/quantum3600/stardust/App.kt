package io.github.quantum3600.stardust

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import io.github.quantum3600.stardust.utils.MeshData
import io.github.quantum3600.stardust.utils.generateMeshColors
import io.github.quantum3600.stardust.utils.generateMeshPoints
import io.github.quantum3600.stardust.utils.rememberSimplexAnimatedMesh
import io.github.quantum3600.stardust.utils.lerp
import kotlin.random.Random
import kotlinx.coroutines.launch
import kotlin.ranges.rangeTo

@Composable
fun App() {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
        Surface(Modifier.fillMaxSize()) {
            var tab by remember { mutableStateOf(0) }
            val tabs = listOf("Animated", "Interactive", "Generated", "Modifier API")
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                Text(
                    text = "Backend: ${platformInfo()}",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                TabRow(selectedTabIndex = tab) {
                    tabs.forEachIndexed { i, title ->
                        Tab(selected = tab == i, onClick = { tab = i }, text = { Text(title) })
                    }
                }
                Box(Modifier.fillMaxSize()) {
                    when (tab) {
                        0 -> AnimatedDemo()
                        1 -> InteractiveDemo()
                        2 -> GeneratedDemo()
                        else -> ModifierDemo()
                    }
                }
            }
        }
    }
}

private enum class Palette(val label: String) {
    Full("16 colours"), Stops("3 stops"), Duo("2 colours")
}

private val sunsetToNight = listOf(
    Color(0xffF36E21), Color(0xffF0A92A), Color(0xffE7D043), Color(0xffE7E95D),
    Color(0xffDFF168), Color(0xffB1E192), Color(0xff7FD3A9), Color(0xff5EC6B8),
    Color(0xff4AB8C2), Color(0xff4099B9), Color(0xff3B79A8), Color(0xff395B93),
    Color(0xff36447C), Color(0xff332E66), Color(0xff301E4E), Color(0xff2D1137),
)

private fun paletteColors(palette: Palette, w: Int, h: Int): List<Color> = when (palette) {
    // One explicit colour per control point (needs exactly w * h entries)
    Palette.Full -> resample(sunsetToNight, w * h)
    // Any number of stops, spread top-to-bottom
    Palette.Stops -> generateMeshColors(
        listOf(Color(0xFFFF9A8B), Color(0xFFFF6A88), Color(0xFF5B247A)), w, h, colorVariance = 0.5f, random = Random(3),
    ).toList()
    Palette.Duo -> generateMeshColors(
        listOf(Color(0xFF00C6FF), Color(0xFFF72585)), w, h, colorVariance = 0.8f, random = Random(7),
    ).toList()
}

/** Stretches/shrinks a palette to `count` colours, keeping its order (4x4 gives the original 16 exactly). */
private fun resample(src: List<Color>, count: Int): List<Color> = List(count) { i ->
    val pos = i / (count - 1f) * (src.size - 1)
    val a = pos.toInt().coerceAtMost(src.size - 2)
    lerp(src[a], src[a + 1], pos - a)
}
/** 1) Simplex-noise "lava lamp". Animation only invalidates drawing; nothing recomposes per frame. */
@Composable
private fun AnimatedDemo() {
    val w = 5
    val h = 5
    var palette by remember { mutableStateOf(Palette.Full) }
    val colors = remember(palette) { paletteColors(palette, w, h) }
    var speed by remember { mutableFloatStateOf(0.3f) }
    var intensity by remember { mutableFloatStateOf(0.08f) }

    val anim = rememberSimplexAnimatedMesh(w, h, colors, animationSpeed = speed, noiseIntensity = intensity)

    Box(Modifier.fillMaxSize()) {
        StardustMesh(Modifier.fillMaxSize(), width = w, height = h, state = anim.state)
        ControlPanel(Modifier.align(Alignment.BottomCenter)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Palette.entries.forEach { p ->
                    FilterChip(selected = palette == p, onClick = { palette = p }, label = { Text(p.label) })
                }
            }
            LabeledSlider("Speed", speed, 0f..1.5f) { speed = it }
            LabeledSlider("Intensity", intensity, 0f..0.12f) { intensity = it }
        }
    }
}

/** 2) Tap anywhere: the centre control point springs there. Colours can be shuffled. */
@Composable
private fun InteractiveDemo() {
    val initialPoints = remember { generateMeshPoints(3, 3, randomness = 0f) }
    val initialColors = remember {
        arrayOf(
            Color(0xFFFF5F6D), Color(0xFFFFC371), Color(0xFF47B8E0),
            Color(0xFF845EC2), Color(0xFFF9F871), Color(0xFF00C9A7),
            Color(0xFF2C73D2), Color(0xFFFF9671), Color(0xFFD65DB1),
        )
    }
    val state = rememberStardustMeshState(initialPoints, initialColors)
    val scope = rememberCoroutineScope()

    Box(
        Modifier.fillMaxSize().pointerInput(Unit) {
            detectTapGestures { tap ->
                scope.launch {
                    state.animatePoint(
                        index = 4, // centre of a 3x3 grid
                        target = Offset(
                            (tap.x / size.width).coerceIn(0.05f, 0.95f),
                            (tap.y / size.height).coerceIn(0.05f, 0.95f),
                        ),
                        spec = spring<Offset>(dampingRatio = 0.4f, stiffness = Spring.StiffnessLow),
                    )
                }
            }
        },
    ) {
        StardustMesh(Modifier.fillMaxSize(), width = 3, height = 3, state = state)
        ControlPanel(Modifier.align(Alignment.BottomCenter)) {
            Text("Tap anywhere to move the centre point", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    repeat(9) { state.setColor(it, Color(Random.nextFloat(), Random.nextFloat(), Random.nextFloat())) }
                }) { Text("Shuffle colours") }
                OutlinedButton(onClick = {
                    scope.launch { state.snapAllPoints(initialPoints.toList()) }
                }) { Text("Reset") }
            }
        }
    }
}

/** 3) Static, generated mesh using the array overload of MeshGradient. */
@Composable
private fun GeneratedDemo() {
    val dark = isSystemInDarkTheme()
    var grid by remember { mutableIntStateOf(4) }
    var randomness by remember { mutableFloatStateOf(0.12f) }
    var variance by remember { mutableFloatStateOf(0.4f) }
    var seed by remember { mutableIntStateOf(1) }

    val mesh = remember(grid, randomness, variance, seed, dark) {
        val base = if (dark) listOf(Color(0xFF0F2027), Color(0xFF2C5364))
        else listOf(Color(0xFFFFE259), Color(0xFFFFA751))
        val rnd = Random(seed)
        MeshData(
            points = generateMeshPoints(grid, grid, randomness, rnd),
            colors = generateMeshColors(base, grid, grid, variance, rnd),
        )
    }

    Box(Modifier.fillMaxSize()) {
        StardustMesh(
            Modifier.fillMaxSize(),
            width = grid, height = grid,
            points = mesh.points, colors = mesh.colors,
        )
        ControlPanel(Modifier.align(Alignment.BottomCenter)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf(3, 4, 5).forEach { n ->
                    FilterChip(selected = grid == n, onClick = { grid = n }, label = { Text("$n×$n") })
                }
                Button(onClick = { seed++ }) { Text("Regenerate") }
            }
            LabeledSlider("Randomness", randomness, 0f..0.3f) { randomness = it }
            LabeledSlider("Variance", variance, 0f..1f) { variance = it }
        }
    }
}

@Composable
private fun ControlPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.padding(16.dp).fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp), content = content)
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.width(96.dp), style = MaterialTheme.typography.bodyMedium)
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

/** 4) Modifier API demo: applying mesh gradient to containers/composables via Modifier.stardustMesh / Modifier.stardustMeshAnimation */
@Composable
private fun ModifierDemo() {
    val initialPoints = remember { generateMeshPoints(3, 3, randomness = 0.1f) }
    val initialColors = remember {
        arrayOf(
            Color(0xFF8A2387), Color(0xFFE94057), Color(0xFFF27121),
            Color(0xFF00F260), Color(0xFF0575E6), Color(0xFF8A2387),
            Color(0xFFF12711), Color(0xFFF5AF19), Color(0xFF00F260),
        )
    }

    Box(
        Modifier
            .fillMaxSize()
            .stardustMeshAnimation(width = 3, height = 3, colors = initialColors.toList(), animationSpeed = 0.4f),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .stardustMesh(width = 3, height = 3, points = initialPoints, colors = initialColors),
            shape = MaterialTheme.shapes.extraLarge,
            color = Color.Transparent,
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Composable Modifier API",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                )
                Text(
                    text = "Mesh gradients applied directly via Modifier.stardustMesh(...) and Modifier.stardustMeshAnimation(...)",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
        }
    }
}
