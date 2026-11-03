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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * An interactive demo showcasing the high-level [AnimatedBezierMeshGradient] API:
 * 1. Fluid Bézier curves and organic tangent motion.
 * 2. Real-time control of animation speed, point randomness, and film grain.
 * 3. Dynamic color palette updates with spatial grid blending.
 */
@Composable
fun BezierAndGrainDemo(modifier: Modifier = Modifier) {
    var grainIntensity by remember { mutableFloatStateOf(0.12f) }
    var animSpeed by remember { mutableFloatStateOf(1.0f) }
    var pointRandomness by remember { mutableFloatStateOf(0.18f) }
    var paletteSeed by remember { mutableFloatStateOf(Random.nextFloat() * 360f) }

    val cornerColors = remember(paletteSeed) {
        listOf(
            Color.hsv((paletteSeed) % 360f, 0.85f, 0.95f),
            Color.hsv((paletteSeed + 90f) % 360f, 0.85f, 0.95f),
            Color.hsv((paletteSeed + 180f) % 360f, 0.85f, 0.95f),
            Color.hsv((paletteSeed + 270f) % 360f, 0.85f, 0.95f)
        )
    }

    Card(
        modifier = modifier.padding(8.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedBezierStardustMesh(
                colors = cornerColors,
                width = 3,
                height = 3,
                speed = animSpeed,
                randomness = pointRandomness,
                dustIntensity = grainIntensity,
                modifier = Modifier.fillMaxSize()
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
