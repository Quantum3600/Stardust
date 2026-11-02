package io.github.quantum3600.stardust

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.quantum3600.stardust.utils.SimplexNoise
import io.github.quantum3600.stardust.utils.lerp
import kotlin.random.Random

/**
 * Color Presets for gradient buttons & background demo.
 */
enum class ColorPreset(
    val title: String,
    val colors: List<Color>
) {
    SUNSET("Sunset", listOf(Color(0xFFF36E21), Color(0xFFF0A92A), Color(0xFFE7D043), Color(0xFFE73562))),
    CYBERPUNK("Cyber", listOf(Color(0xFF00F0FF), Color(0xFF7000FF), Color(0xFFFF007A), Color(0xFF390066))),
    AURORA("Aurora", listOf(Color(0xFF00FF87), Color(0xFF60EFFF), Color(0xFF0061FF), Color(0xFF7B2CBF))),
    NEON("Neon", listOf(Color(0xFFFF0055), Color(0xFF7A00FF), Color(0xFF00E5FF), Color(0xFF00FF66))),
    COSMIC("Cosmic", listOf(Color(0xFF2E0854), Color(0xFF8A2BE2), Color(0xFFFF1493), Color(0xFF00BFFF)))
}

// Custom Vector Canvas Icons
@Composable
fun SparklesIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.5f, 0f)
            quadraticTo(w * 0.5f, h * 0.5f, w, h * 0.5f)
            quadraticTo(w * 0.5f, h * 0.5f, w * 0.5f, h)
            quadraticTo(w * 0.5f, h * 0.5f, 0f, h * 0.5f)
            quadraticTo(w * 0.5f, h * 0.5f, w * 0.5f, 0f)
        }
        drawPath(path, color = tint)
    }
}

@Composable
fun PlayIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.2f, h * 0.15f)
            lineTo(w * 0.85f, h * 0.5f)
            lineTo(w * 0.2f, h * 0.85f)
            close()
        }
        drawPath(path, color = tint)
    }
}

@Composable
fun PaletteIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier = modifier) {
        val r = size.minDimension / 2f
        drawCircle(color = tint, radius = r, style = Stroke(width = 3.dp.toPx()))
        drawCircle(color = tint, radius = r * 0.25f, center = Offset(center.x - r * 0.4f, center.y - r * 0.3f))
        drawCircle(color = tint, radius = r * 0.25f, center = Offset(center.x + r * 0.4f, center.y - r * 0.3f))
        drawCircle(color = tint, radius = r * 0.25f, center = Offset(center.x, center.y + r * 0.35f))
    }
}

@Composable
fun RefreshIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier = modifier) {
        val r = size.minDimension * 0.4f
        drawArc(
            color = tint,
            startAngle = 0f,
            sweepAngle = 280f,
            useCenter = false,
            topLeft = Offset(center.x - r, center.y - r),
            size = androidx.compose.ui.geometry.Size(r * 2f, r * 2f),
            style = Stroke(width = 3.dp.toPx())
        )
    }
}

@Composable
fun TouchIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier = modifier) {
        val r = size.minDimension / 2f
        drawCircle(color = tint.copy(alpha = 0.4f), radius = r)
        drawCircle(color = tint, radius = r * 0.4f)
    }
}

/**
 * A reusable interactive Button with a Mesh Gradient background.
 * Supports smooth press scaling, dynamic color state, and custom click actions.
 */
@Composable
fun MeshGradientButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: List<Color> = ColorPreset.SUNSET.colors,
    shape: Shape = RoundedCornerShape(16.dp),
    dustIntensity: Float = 0.08f,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "buttonScale"
    )

    var time by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var lastTime = 0L
        while (true) {
            withFrameNanos { frameTime ->
                if (lastTime != 0L) {
                    time += (frameTime - lastTime) / 1_000_000_000f
                }
                lastTime = frameTime
            }
        }
    }

    val animSpeed = if (isPressed) 1.5f else 0.4f

    val points = remember(time, animSpeed) {
        val width = 3
        val height = 3
        Array(width * height) { i ->
            val col = i % width
            val row = i / width
            val px = col / (width - 1f)
            val py = row / (height - 1f)

            if (row == 0 || row == height - 1 || col == 0 || col == width - 1) {
                Offset(px, py)
            } else {
                val nx = SimplexNoise.noise(px * 2f + time * animSpeed, i.toFloat()) * 0.15f
                val ny = SimplexNoise.noise(py * 2f + time * animSpeed, i + 50f) * 0.15f
                Offset(px + nx, py + ny)
            }
        }
    }

    val gridColors = remember(colors) {
        val width = 3
        val height = 3
        val c0 = colors.getOrElse(0) { Color.Red }
        val c1 = colors.getOrElse(1) { Color.Yellow }
        val c2 = colors.getOrElse(2) { Color.Green }
        val c3 = colors.getOrElse(3) { Color.Blue }

        Array(width * height) { i ->
            val col = i % width
            val row = i / width
            val fx = col / (width - 1f)
            val fy = row / (height - 1f)
            val top = lerp(c0, c1, fx)
            val bottom = lerp(c2, c3, fx)
            lerp(top, bottom, fy)
        }
    }

    Surface(
        onClick = onClick,
        modifier = modifier
            .scale(scale)
            .clip(shape),
        shape = shape,
        interactionSource = interactionSource,
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.2f))
                .padding(horizontal = 20.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            StardustMesh(
                modifier = Modifier.matchParentSize(),
                width = 3,
                height = 3,
                points = points,
                colors = gridColors,
                dustIntensity = dustIntensity,
                dustAnimationSpeed = 0f
            )

            // Gloss overlay
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Box(contentAlignment = Alignment.Center) {
                content()
            }
        }
    }
}

/**
 * An interactive touch button that physically deforms the mesh gradient
 * as the user touches and drags on the button!
 */
@Composable
fun InteractiveTouchMeshButton(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    colors: List<Color> = ColorPreset.CYBERPUNK.colors
) {
    var touchPos by remember { mutableStateOf<Offset?>(null) }
    var touchCount by remember { mutableStateOf(0) }

    val width = 3
    val height = 3

    val basePoints = remember {
        Array(width * height) { i ->
            val col = i % width
            val row = i / width
            Offset(col / (width - 1f), row / (height - 1f))
        }
    }

    val animatedPoints = remember(touchPos) {
        val currentTouch = touchPos
        basePoints.mapIndexed { i, base ->
            val col = i % width
            val row = i / width
            val isBorder = row == 0 || row == height - 1 || col == 0 || col == width - 1

            if (isBorder || currentTouch == null) {
                base
            } else {
                val dist = (base - currentTouch).getDistance()
                val pullFactor = (1.0f - (dist / 1.2f).coerceIn(0f, 1f)) * 0.25f
                val dir = if (dist > 0.001f) (currentTouch - base) / dist else Offset.Zero
                Offset(
                    (base.x + dir.x * pullFactor).coerceIn(0.1f, 0.9f),
                    (base.y + dir.y * pullFactor).coerceIn(0.1f, 0.9f)
                )
            }
        }.toTypedArray()
    }

    val gridColors = remember(colors) {
        val c0 = colors.getOrElse(0) { Color.Cyan }
        val c1 = colors.getOrElse(1) { Color.Magenta }
        val c2 = colors.getOrElse(2) { Color.Blue }
        val c3 = colors.getOrElse(3) { Color.Yellow }

        Array(width * height) { i ->
            val col = i % width
            val row = i / width
            val fx = col / (width - 1f)
            val fy = row / (height - 1f)
            val top = lerp(c0, c1, fx)
            val bottom = lerp(c2, c3, fx)
            lerp(top, bottom, fy)
        }
    }

    Box(
        modifier = modifier
            .clip(shape)
            .border(1.5.dp, Color.White.copy(alpha = 0.3f), shape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        val normX = (offset.x / size.width).coerceIn(0f, 1f)
                        val normY = (offset.y / size.height).coerceIn(0f, 1f)
                        touchPos = Offset(normX, normY)
                        touchCount++
                        tryAwaitRelease()
                        touchPos = null
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val normX = (offset.x / size.width).coerceIn(0f, 1f)
                        val normY = (offset.y / size.height).coerceIn(0f, 1f)
                        touchPos = Offset(normX, normY)
                    },
                    onDrag = { change, _ ->
                        val normX = (change.position.x / size.width).coerceIn(0f, 1f)
                        val normY = (change.position.y / size.height).coerceIn(0f, 1f)
                        touchPos = Offset(normX, normY)
                    },
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
            points = animatedPoints,
            colors = gridColors,
            dustIntensity = 0.1f
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            TouchIcon(modifier = Modifier.size(20.dp), tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = "Touch & Drag Mesh Canvas",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = if (touchPos != null) "Deforming mesh..." else "Tap or drag to pull control points ($touchCount taps)",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

/**
 * A Loading Card component where the mesh gradient flows rapidly with intense colors while loading,
 * and smoothly slows down to fixed colors when loading finishes!
 */
@Composable
fun LoadingMeshGradientCard(
    isLoading: Boolean,
    onToggleLoading: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animSpeed by animateFloatAsState(
        targetValue = if (isLoading) 2.2f else 0.25f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "loadingSpeed"
    )

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

/**
 * A Circular Floating Action Button styled with animated Mesh Gradient.
 */
@Composable
fun MeshGradientFAB(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: List<Color> = ColorPreset.AURORA.colors,
    size: Dp = 64.dp,
    icon: @Composable () -> Unit = { SparklesIcon(Modifier.size(22.dp), Color.White) }
) {
    MeshGradientButton(
        onClick = onClick,
        modifier = modifier.size(size),
        colors = colors,
        shape = CircleShape
    ) {
        icon()
    }
}

/**
 * Comprehensive Demo showing smooth background mesh gradient, interactive gradient buttons,
 * palette switchers, and touch canvas!
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GradientButtonsDemo(modifier: Modifier = Modifier) {
    var activePreset by remember { mutableStateOf(ColorPreset.SUNSET) }
    var animSpeed by remember { mutableFloatStateOf(0.8f) }
    var clickCount by remember { mutableStateOf(0) }
    var isLoadingCard by remember { mutableStateOf(true) }

    // Smoothly animated background mesh gradient colors
    val bgC0 by animateColorAsState(activePreset.colors[0], tween(800, easing = FastOutSlowInEasing), label = "c0")
    val bgC1 by animateColorAsState(activePreset.colors[1], tween(800, easing = FastOutSlowInEasing), label = "c1")
    val bgC2 by animateColorAsState(activePreset.colors[2], tween(800, easing = FastOutSlowInEasing), label = "c2")
    val bgC3 by animateColorAsState(activePreset.colors[3], tween(800, easing = FastOutSlowInEasing), label = "c3")

    var time by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(animSpeed) {
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

    val bgPoints = remember(time) {
        val w = 3
        val h = 3
        Array(w * h) { i ->
            val col = i % w
            val row = i / h
            val px = col / (w - 1f)
            val py = row / (h - 1f)
            if (row == 0 || row == h - 1 || col == 0 || col == h - 1) {
                Offset(px, py)
            } else {
                val nx = SimplexNoise.noise(px * 1.5f + time * 0.4f, i.toFloat()) * 0.16f
                val ny = SimplexNoise.noise(py * 1.5f + time * 0.4f, i + 50f) * 0.16f
                Offset(px + nx, py + ny)
            }
        }
    }

    val bgGridColors = remember(bgC0, bgC1, bgC2, bgC3) {
        val w = 3
        val h = 3
        Array(w * h) { i ->
            val col = i % w
            val row = i / h
            val fx = col / (w - 1f)
            val fy = row / (h - 1f)
            val top = lerp(bgC0, bgC1, fx)
            val bottom = lerp(bgC2, bgC3, fx)
            lerp(top, bottom, fy)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Smooth background mesh
        StardustMesh(
            modifier = Modifier.fillMaxSize(),
            width = 3,
            height = 3,
            points = bgPoints,
            colors = bgGridColors,
            dustIntensity = 0.08f
        )

        // Overlay Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.65f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Stardust Mesh Gradient",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Ultra-Smooth AGSL Shader • Interactive Gradient Buttons",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 12.sp
                    )
                }
            }

            // Interactive Touch Canvas Button
            InteractiveTouchMeshButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                colors = activePreset.colors
            )

            // Loading State Mesh Card
            LoadingMeshGradientCard(
                isLoading = isLoadingCard,
                onToggleLoading = { isLoadingCard = !isLoadingCard }
            )

            // Demo Buttons Row & Actions
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.65f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Gradient Preset Palette",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ColorPreset.entries.forEach { preset ->
                            val isSelected = preset == activePreset
                            Surface(
                                onClick = { activePreset = preset },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.1f),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, Color.White) else null
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(preset.colors.first())
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = preset.title,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "Interactive Gradient Buttons ($clickCount clicks)",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MeshGradientButton(
                            onClick = { clickCount++ },
                            colors = ColorPreset.CYBERPUNK.colors,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PlayIcon(Modifier.size(16.dp), Color.White)
                                Spacer(Modifier.width(6.dp))
                                Text("Primary CTA", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        MeshGradientButton(
                            onClick = {
                                activePreset = ColorPreset.entries[Random.nextInt(ColorPreset.entries.size)]
                            },
                            colors = ColorPreset.AURORA.colors,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PaletteIcon(Modifier.size(16.dp), Color.White)
                                Spacer(Modifier.width(6.dp))
                                Text("Randomize", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        MeshGradientFAB(
                            onClick = {
                                animSpeed = if (animSpeed > 1.2f) 0.5f else 1.8f
                            },
                            colors = ColorPreset.COSMIC.colors,
                            size = 48.dp
                        ) {
                            RefreshIcon(Modifier.size(20.dp), Color.White)
                        }
                    }
                }
            }
        }
    }
}
