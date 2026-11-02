package io.github.quantum3600.stardust

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.quantum3600.stardust.data.BezierPoint
import io.github.quantum3600.stardust.utils.SimplexNoise
import io.github.quantum3600.stardust.utils.lerp
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * An interactive demo showcasing:
 * 1. Asymmetric color pulling with explicit [BezierPoint] tangent handles animated via Simplex noise.
 * 2. 2D spatial grid coordinate color generation (fx = col/(w-1), fy = row/(h-1)) for multi-directional gradients.
 * 3. Animated procedural film grain / stardust dithering by updating grainSeed continuously.
 */
@Composable
fun BezierAndGrainDemo(modifier: Modifier = Modifier) {
    var grainIntensity by remember { mutableFloatStateOf(0.12f) }
    var animSpeed by remember { mutableFloatStateOf(1.0f) }
    var pointRandomness by remember { mutableFloatStateOf(0.18f) }

    var grainSeed by remember { mutableFloatStateOf(0f) }
    var time by remember { mutableFloatStateOf(0f) }

    var paletteSeed by remember { mutableFloatStateOf(Random.nextFloat() * 360f) }

    // Continuous animation loop for time and grainSeed
    LaunchedEffect(animSpeed) {
        var lastTime = 0L
        while (true) {
            withFrameNanos { frameTime ->
                if (lastTime != 0L) {
                    val dt = (frameTime - lastTime) / 1_000_000_000f
                    time += dt * animSpeed
                    grainSeed += dt * 60f
                }
                lastTime = frameTime
            }
        }
    }

    val gridWidth = 3
    val gridHeight = 3

    val bezierPoints = remember(time, pointRandomness) {
        List(gridHeight) { row ->
            List(gridWidth) { col ->
                val posX = col / (gridWidth - 1f)
                val posY = row / (gridHeight - 1f)
                val pos = Offset(posX, posY)

                val pointIdx = row * gridWidth + col
                val isBorder = row == 0 || row == gridHeight - 1 || col == 0 || col == gridWidth - 1

                val activePos = if (isBorder) pos else {
                    val nx = SimplexNoise.noise(posX * 2f + time * 0.5f, pointIdx.toFloat()) * pointRandomness
                    val ny = SimplexNoise.noise(posY * 2f + time * 0.5f, pointIdx + 50f) * pointRandomness
                    Offset(posX + nx, posY + ny)
                }

                // Asymmetric tangent handle motion
                val tldX = -0.18f + SimplexNoise.noise(time * 0.9f, pointIdx + 10f) * 0.15f
                val tldY = SimplexNoise.noise(time * 0.9f, pointIdx + 20f) * 0.15f

                val ttrX = 0.18f + SimplexNoise.noise(time * 0.9f, pointIdx + 30f) * 0.15f
                val ttrY = SimplexNoise.noise(time * 0.9f, pointIdx + 40f) * 0.15f

                val ttopX = SimplexNoise.noise(time * 0.9f, pointIdx + 60f) * 0.15f
                val ttopY = -0.18f + SimplexNoise.noise(time * 0.9f, pointIdx + 70f) * 0.15f

                val tbotX = SimplexNoise.noise(time * 0.9f, pointIdx + 80f) * 0.15f
                val tbotY = 0.18f + SimplexNoise.noise(time * 0.9f, pointIdx + 90f) * 0.15f

                BezierPoint.withTangents(
                    position = activePos,
                    uTangentLeading = Offset(tldX, tldY),
                    uTangentTrailing = Offset(ttrX, ttrY),
                    vTangentTop = Offset(ttopX, ttopY),
                    vTangentBottom = Offset(tbotX, tbotY)
                )
            }
        }
    }

    // Generate colors using 2D spatial grid coordinates fx = col/(w-1) and fy = row/(h-1)
    val colors = remember(time, paletteSeed) {
        val colorTL = Color.hsv((paletteSeed + time * 15f) % 360f, 0.85f, 0.95f)
        val colorTR = Color.hsv((paletteSeed + 90f + time * 15f) % 360f, 0.85f, 0.95f)
        val colorBL = Color.hsv((paletteSeed + 180f + time * 15f) % 360f, 0.85f, 0.95f)
        val colorBR = Color.hsv((paletteSeed + 270f + time * 15f) % 360f, 0.85f, 0.95f)

        List(gridHeight) { row ->
            List(gridWidth) { col ->
                val fx = col / (gridWidth - 1f)
                val fy = row / (gridHeight - 1f)
                val topColor = lerp(colorTL, colorTR, fx)
                val bottomColor = lerp(colorBL, colorBR, fx)
                lerp(topColor, bottomColor, fy)
            }
        }
    }

    Card(
        modifier = modifier.padding(8.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            StardustMesh(
                bezierPoints = bezierPoints,
                colors = colors,
                modifier = Modifier.fillMaxSize(),
                grainIntensity = grainIntensity,
                grainSeed = grainSeed
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Asymmetric Bézier & Spatial Colors",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Custom Tangents + 2D Spatial Grid Colors + Film Grain",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 12.sp
                        )
                    }

                    Button(
                        onClick = { paletteSeed = Random.nextFloat() * 360f },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Random Colors", fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val speedDisplay = (animSpeed * 10f).roundToInt() / 10f
                    Text(
                        text = "Speed: ${speedDisplay}x",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.width(90.dp)
                    )
                    Slider(
                        value = animSpeed,
                        onValueChange = { animSpeed = it },
                        valueRange = 0.1f..3.0f,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Randomness: ${(pointRandomness * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.width(90.dp)
                    )
                    Slider(
                        value = pointRandomness,
                        onValueChange = { pointRandomness = it },
                        valueRange = 0f..0.35f,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Grain: ${(grainIntensity * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.width(90.dp)
                    )
                    Slider(
                        value = grainIntensity,
                        onValueChange = { grainIntensity = it },
                        valueRange = 0f..0.3f,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
