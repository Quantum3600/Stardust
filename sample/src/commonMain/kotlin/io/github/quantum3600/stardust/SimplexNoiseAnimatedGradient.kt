package io.github.quantum3600.stardust

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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

/**
 * Full-screen sample demonstrating [AnimatedStardustMesh] with 16 rainbow palette colors (4x4)
 * matching the exact original Simplex noise animation and live controls for:
 * 1. Animation Speed
 * 2. Mesh Wobble
 * 3. Static Film Grain Amount
 */
@Composable
fun SimplexNoiseMeshExample(modifier: Modifier = Modifier) {
    var animSpeed by remember { mutableFloatStateOf(0.3f) }
    var wobbleAmount by remember { mutableFloatStateOf(0.2f) }
    var grainAmount by remember { mutableFloatStateOf(0.05f) }

    val colors = remember {
        listOf(
            Color(0xffF36E21), Color(0xffF0A92A), Color(0xffE7D043), Color(0xffE7E95D),
            Color(0xffDFF168), Color(0xffB1E192), Color(0xff7FD3A9), Color(0xff5EC6B8),
            Color(0xff4AB8C2), Color(0xff4099B9), Color(0xff3B79A8), Color(0xff395B93),
            Color(0xff36447C), Color(0xff332E66), Color(0xff301E4E), Color(0xff2D1137)
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedStardustMesh(
            colors = colors,
            width = 4,
            height = 4,
            speed = animSpeed,
            wobbleAmount = wobbleAmount,
            dustIntensity = grainAmount,
            modifier = Modifier.fillMaxSize()
        )

        // Bottom Control Panel
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.75f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Stardust Simplex Flow (4x4)",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "GPU-Accelerated 16-Color Rainbow Mesh",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 12.sp
                )

                Spacer(Modifier.height(12.dp))

                // Speed Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val speedDisplay = (animSpeed * 100f).roundToInt() / 100f
                    Text(
                        text = "Speed: ${speedDisplay}x",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.width(110.dp)
                    )
                    Slider(
                        value = animSpeed,
                        onValueChange = { animSpeed = it },
                        valueRange = 0.0f..0.8f,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Wobble Amount Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Wobble: ${(wobbleAmount * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.width(110.dp)
                    )
                    Slider(
                        value = wobbleAmount,
                        onValueChange = { wobbleAmount = it },
                        valueRange = 0f..0.50f,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Grain Amount Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Static Grain: ${(grainAmount * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.width(110.dp)
                    )
                    Slider(
                        value = grainAmount,
                        onValueChange = { grainAmount = it },
                        valueRange = 0f..0.25f,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
