package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.actions.MultiStepExecutionState
import com.example.actions.TaskStep
import com.example.actions.TaskStepStatus
import com.example.ui.theme.LyraBorderGlow
import com.example.ui.theme.LyraCyan
import com.example.ui.theme.LyraError
import com.example.ui.theme.LyraSuccess
import com.example.ui.theme.LyraSurfaceCard
import com.example.ui.theme.LyraViolet

@Composable
fun TaskExecutionCard(
    state: MultiStepExecutionState,
    modifier: Modifier = Modifier
) {
    if (state.steps.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LyraSurfaceCard)
            .border(1.dp, if (state.isVerified) LyraSuccess.copy(alpha = 0.6f) else LyraBorderGlow, RoundedCornerShape(16.dp))
            .padding(14.dp)
            .testTag("task_execution_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = null,
                    tint = LyraCyan,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = state.overallTaskName.ifBlank { "Multi-Step Execution" },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (state.isRunning) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = LyraCyan
                    )
                    Text("Executing...", fontSize = 11.sp, color = LyraCyan)
                }
            } else if (state.isVerified) {
                Text("Verified Complete", fontSize = 11.sp, color = LyraSuccess, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        state.steps.forEachIndexed { index, step ->
            StepItemRow(step = step, isLast = index == state.steps.lastIndex)
        }

        if (state.finalMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (state.isVerified) LyraSuccess.copy(alpha = 0.15f) else LyraViolet.copy(alpha = 0.15f))
                    .padding(8.dp)
            ) {
                Text(
                    text = state.finalMessage,
                    fontSize = 12.sp,
                    color = if (state.isVerified) LyraSuccess else LyraCyan
                )
            }
        }
    }
}

@Composable
private fun StepItemRow(step: TaskStep, isLast: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Step Status Dot
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(
                    when (step.status) {
                        TaskStepStatus.SUCCESS -> LyraSuccess.copy(alpha = 0.2f)
                        TaskStepStatus.RUNNING -> LyraCyan.copy(alpha = 0.2f)
                        TaskStepStatus.FAILED -> LyraError.copy(alpha = 0.2f)
                        TaskStepStatus.PENDING -> Color(0xFF334155)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            when (step.status) {
                TaskStepStatus.SUCCESS -> Icon(Icons.Default.Check, contentDescription = null, tint = LyraSuccess, modifier = Modifier.size(12.dp))
                TaskStepStatus.RUNNING -> CircularProgressIndicator(modifier = Modifier.size(10.dp), strokeWidth = 1.5.dp, color = LyraCyan)
                TaskStepStatus.FAILED -> Icon(Icons.Default.Close, contentDescription = null, tint = LyraError, modifier = Modifier.size(12.dp))
                TaskStepStatus.PENDING -> Text("${step.stepNumber}", fontSize = 10.sp, color = Color(0xFF94A3B8))
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = step.description,
                fontSize = 12.sp,
                fontWeight = if (step.status == TaskStepStatus.RUNNING) FontWeight.SemiBold else FontWeight.Normal,
                color = when (step.status) {
                    TaskStepStatus.SUCCESS -> MaterialTheme.colorScheme.onSurface
                    TaskStepStatus.RUNNING -> LyraCyan
                    TaskStepStatus.FAILED -> LyraError
                    TaskStepStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                }
            )
            if (!step.detail.isNullOrBlank() && step.status != TaskStepStatus.PENDING) {
                Text(
                    text = step.detail,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
