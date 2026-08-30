package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LyraCyan
import com.example.ui.theme.LyraPink
import com.example.ui.theme.LyraViolet
import com.example.voice.VoiceState
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HolographicOrb(
    voiceState: VoiceState,
    audioAmplitude: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_transition")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val ampScale = remember { Animatable(1f) }
    LaunchedEffect(audioAmplitude) {
        ampScale.animateTo(
            targetValue = 1f + (audioAmplitude * 0.45f),
            animationSpec = tween(80)
        )
    }

    val baseColors = when (voiceState) {
        VoiceState.LISTENING -> listOf(LyraCyan, Color(0xFF38BDF8), LyraViolet)
        VoiceState.PROCESSING -> listOf(LyraViolet, LyraPink, LyraCyan)
        VoiceState.SPEAKING -> listOf(LyraPink, LyraCyan, Color(0xFF67E8F9))
        VoiceState.ERROR -> listOf(Color(0xFFEF4444), Color(0xFFF87171), LyraViolet)
        VoiceState.IDLE -> listOf(Color(0xFF0284C7), LyraViolet, Color(0xFF1E1B4B))
    }

    Box(
        modifier = modifier
            .size(190.dp)
            .testTag("holographic_orb")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 95.dp),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = (size.minDimension / 2.6f) * pulseScale * ampScale.value

            // 1. Outer Diffuse Glow Aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        baseColors[0].copy(alpha = if (voiceState == VoiceState.IDLE) 0.25f else 0.45f),
                        baseColors[1].copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.5f
                ),
                radius = baseRadius * 1.5f,
                center = center
            )

            // 2. Concentric Orbit Rings
            val ringCount = 3
            for (i in 1..ringCount) {
                val ringRadius = baseRadius * (0.85f + (i * 0.18f))
                val ringAlpha = 0.25f / i
                drawCircle(
                    color = baseColors[i % baseColors.size].copy(alpha = ringAlpha),
                    radius = ringRadius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // 3. Rotating energy orbital nodes
            val rad = Math.toRadians(rotationAngle.toDouble())
            for (k in 0 until 4) {
                val angle = rad + (k * Math.PI / 2)
                val orbitRadius = baseRadius * 1.05f
                val dotX = center.x + (orbitRadius * cos(angle)).toFloat()
                val dotY = center.y + (orbitRadius * sin(angle)).toFloat()
                drawCircle(
                    color = baseColors[k % baseColors.size].copy(alpha = 0.85f),
                    radius = 3.5.dp.toPx(),
                    center = Offset(dotX, dotY)
                )
            }

            // 4. Core Holographic Sphere
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.9f),
                        baseColors[0],
                        baseColors[1],
                        baseColors[2].copy(alpha = 0.8f)
                    ),
                    center = Offset(center.x - baseRadius * 0.2f, center.y - baseRadius * 0.2f),
                    radius = baseRadius * 0.88f
                ),
                radius = baseRadius * 0.78f,
                center = center
            )

            // 5. Inner Core Specular Highlight
            drawCircle(
                color = Color.White.copy(alpha = 0.65f),
                radius = baseRadius * 0.22f,
                center = Offset(center.x - baseRadius * 0.25f, center.y - baseRadius * 0.25f)
            )
        }
    }
}
