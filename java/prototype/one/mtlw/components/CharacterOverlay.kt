package prototype.one.mtlw.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import prototype.one.mtlw.R

@Composable
fun CharacterOverlay(
    modifier: Modifier = Modifier,
    characterResId: Int = R.drawable.repo_main,
    align: Alignment = Alignment.TopStart,
    label: String? = null,
    confidence: Float? = null,
    visible: Boolean = true,
    showSmall: Boolean = true,
    onPrimary: (() -> Unit)? = null,
    onSecondary: (() -> Unit)? = null,
    primaryText: String? = null,
    secondaryText: String? = null,
    cloudScale: Float = 0.8f // scale
) {
    val infinite = rememberInfiniteTransition(label = "charFloat")
    val bob by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bob"
    )

    val strong = (confidence ?: 0f) >= 0.6f
    val scale by animateFloatAsState(
        targetValue = if (strong) 1.15f else 1.0f,
        animationSpec = tween(durationMillis = 220, easing = LinearEasing),
        label = "scale"
    )
    val glowPulse by infinite.animateFloat(
        initialValue = if (strong) 0.2f else 0f,
        targetValue = if (strong) 0.8f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    if (!visible) return
    Box(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier
            .align(align)
            .offset(x = (100).dp, y = (5 + bob).dp) // Moved bee
        ) {
            // Thought bubble (cloud) with centered content and optional actions
            if (!label.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = (-130).dp, y = (-20).dp) // Moved cloud
                        .scale(cloudScale + .7f) // Made cloud bigger to prevent text overflow
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.cloud),
                        contentDescription = null,
                        colorFilter = null,
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 6.dp, vertical = 3.dp), // Reduced padding even more to prevent overflow
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = label,
                            maxLines = 2, // Reduced to 2 lines to prevent overflow
                            overflow = TextOverflow.Ellipsis,
                            color = Color(0xFF222222),
                            style = androidx.compose.material3.MaterialTheme.typography.labelLarge, // Use smallest available typography
                            modifier = Modifier.offset(y = (-1).dp) // Adjusted position
                        )
                        if (primaryText != null || secondaryText != null) {
                            Row(
                                modifier = Modifier
                                    .padding(top = 0.dp) // No top padding to bring buttons closer to text
                                    .fillMaxWidth(), // Make row fill width for centering
                                horizontalArrangement = Arrangement.Center, // Center the buttons
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (primaryText != null && onPrimary != null) {
                                    androidx.compose.material3.TextButton( // Changed from Button to TextButton
                                        onClick = onPrimary,
                                        content = { 
                                            androidx.compose.material3.Text(
                                                primaryText,
                                                style = androidx.compose.material3.MaterialTheme.typography.labelSmall // Use smallest available typography
                                            ) 
                                        },
                                        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                            contentColor = Color(0xFF1E824C) // Green text color
                                        ),
                                        modifier = Modifier.padding(end = 1.dp), // Reduced spacing between buttons even more
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 3.dp, vertical = 2.dp) // Made buttons even smaller
                                    )
                                }
                                if (secondaryText != null && onSecondary != null) {
                                    androidx.compose.material3.TextButton( // Changed from Button to TextButton
                                        onClick = onSecondary,
                                        content = { 
                                            androidx.compose.material3.Text(
                                                secondaryText,
                                                style = androidx.compose.material3.MaterialTheme.typography.labelSmall // Use smallest available typography
                                            ) 
                                        },
                                        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                            contentColor = Color(0xFF666666) // Gray text color
                                        ),
                                        modifier = Modifier.padding(start = 1.dp), // Reduced spacing between buttons even more
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 3.dp, vertical = 2.dp) // Made buttons even smaller
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Image(
                painter = painterResource(id = characterResId),
                contentDescription = null,
                modifier = Modifier.scale(if (showSmall) 0.4f * scale else scale * 0.7f) // Made character even smaller
            )
        }
    }
}



