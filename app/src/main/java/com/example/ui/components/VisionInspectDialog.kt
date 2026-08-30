package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.LyraBorderGlow
import com.example.ui.theme.LyraCyan
import com.example.ui.theme.LyraSurfaceCard
import com.example.ui.theme.LyraSurfaceDark
import com.example.ui.theme.LyraViolet

@Composable
fun VisionInspectDialog(
    latestBitmap: Bitmap?,
    isSharing: Boolean,
    onDismiss: () -> Unit,
    onExecuteSampleQuery: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, LyraCyan.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .testTag("vision_inspect_dialog"),
            colors = CardDefaults.cardColors(containerColor = LyraSurfaceDark)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Header
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
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = LyraCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "LYRA Screen Observer",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Frame Preview Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(LyraSurfaceCard)
                        .border(1.dp, LyraBorderGlow, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (latestBitmap != null) {
                        Image(
                            bitmap = latestBitmap.asImageBitmap(),
                            contentDescription = "Screen Capture Preview",
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = LyraCyan,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = "Screen Vision Active (Latest Frame Ready)",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Quick Screen Actions & Tests",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LyraCyan
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SampleActionRow(
                        query = "What is on my screen?",
                        onClick = {
                            onDismiss()
                            onExecuteSampleQuery("What is on my screen?")
                        }
                    )
                    SampleActionRow(
                        query = "Center video open karo",
                        onClick = {
                            onDismiss()
                            onExecuteSampleQuery("Center video open karo")
                        }
                    )
                    SampleActionRow(
                        query = "Scroll down and open the second video",
                        onClick = {
                            onDismiss()
                            onExecuteSampleQuery("Scroll down and open the second video")
                        }
                    )
                    SampleActionRow(
                        query = "Read visible text and explain code",
                        onClick = {
                            onDismiss()
                            onExecuteSampleQuery("Read visible text and explain code")
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SampleActionRow(query: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = LyraSurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, LyraBorderGlow)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = query, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
            Icon(Icons.Default.PlayCircle, contentDescription = null, tint = LyraViolet, modifier = Modifier.size(16.dp))
        }
    }
}
