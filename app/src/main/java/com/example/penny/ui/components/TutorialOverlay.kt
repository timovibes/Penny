package com.example.penny.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class TutorialStep(
    val targetBounds: Rect?,
    val title: String,
    val description: String,
    val tooltipAlignment: Alignment,
    val tooltipPadding: PaddingValues = PaddingValues(24.dp)
)

@Composable
fun TutorialOverlay(
    steps: List<TutorialStep>,
    currentStepIndex: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    val step = steps.getOrNull(currentStepIndex) ?: return
    val bounds = step.targetBounds
    val isLastStep = currentStepIndex == steps.lastIndex
    val density = LocalDensity.current

    Box(modifier = Modifier.fillMaxSize()) {

        // Dimmed background with a see-through hole cut around the target button.
        // graphicsLayer(alpha = 0.99f) forces the clear blend mode below to actually
        // punch a transparent hole instead of just drawing black over black.
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = 0.99f)
        ) {
            drawRect(color = Color.Black.copy(alpha = 0.78f))

            if (bounds != null) {
                val holePadding = with(density) { 10.dp.toPx() }
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = Offset(bounds.left - holePadding, bounds.top - holePadding),
                    size = Size(bounds.width + holePadding * 2, bounds.height + holePadding * 2),
                    cornerRadius = CornerRadius(24f, 24f),
                    blendMode = BlendMode.Clear
                )
            }
        }

        // The instruction card, positioned near the highlighted button
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(step.tooltipPadding),
            contentAlignment = step.tooltipAlignment
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.widthIn(max = 320.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = step.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = step.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${currentStepIndex + 1}/${steps.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row {
                            TextButton(onClick = onSkip) {
                                Text("Skip")
                            }
                            Button(
                                onClick = onNext,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text(if (isLastStep) "Got it" else "Next")
                            }
                        }
                    }
                }
            }
        }
    }
}