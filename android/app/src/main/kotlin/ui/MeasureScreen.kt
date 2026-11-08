package oyoon.app.ui

import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import oyoon.app.R
import oyoon.core.gate.RejectReason

private val ChipGreen = Color(0xFF2E7D32)
private val ChipRed = Color(0xFFC62828)
private val ChipGrey = Color(0xFF424242)
private val Scrim = Color(0x99000000)

/**
 * Live measurement screen (S2-AND-2): camera preview with the face mesh, a
 * status chip and the live PD. Debug builds also get timing text, a
 * resolution switcher and a landmark label toggle.
 */
@Composable
fun MeasureScreen(
    state: MeasureUiState,
    previewView: PreviewView,
    isDebugBuild: Boolean,
    showLabels: Boolean,
    resolution: AnalysisResolution,
    onResolutionSelected: (AnalysisResolution) -> Unit,
    onToggleLabels: () -> Unit,
) {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
            FaceOverlay(
                result = state.face,
                imageWidth = state.imageWidth,
                imageHeight = state.imageHeight,
                showLabels = isDebugBuild && showLabels,
                modifier = Modifier.fillMaxSize(),
            )

            Column(
                Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.message != null) {
                        StatusChip(state.message, ChipRed)
                    } else {
                        StatusChip(chipText(state.status), chipColour(state.status))
                    }
                    if (isDebugBuild && state.debugLines.isNotEmpty()) DebugText(state.debugLines)
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PdReadout(state.pdMm, live = state.status == ChipStatus.Accepted)
                    if (isDebugBuild) {
                        DebugBar(resolution, showLabels, onResolutionSelected, onToggleLabels)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(text: String, colour: Color) {
    Surface(color = colour, shape = RoundedCornerShape(50)) {
        Text(
            text,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun chipText(status: ChipStatus): String = stringResource(
    when (status) {
        ChipStatus.Searching -> R.string.chip_searching
        ChipStatus.Accepted -> R.string.chip_accepted
        is ChipStatus.Rejected -> when (status.reason) {
            RejectReason.YAW -> R.string.chip_yaw
            RejectReason.PITCH -> R.string.chip_pitch
            RejectReason.ROLL -> R.string.chip_roll
            RejectReason.TOO_FAR -> R.string.chip_too_far
            RejectReason.IRIS_MISMATCH -> R.string.chip_iris_mismatch
            RejectReason.UNSTABLE -> R.string.chip_unstable
            RejectReason.TOO_DARK -> R.string.chip_too_dark
        }
    }
)

private fun chipColour(status: ChipStatus): Color = when (status) {
    ChipStatus.Searching -> ChipGrey
    ChipStatus.Accepted -> ChipGreen
    is ChipStatus.Rejected -> ChipRed
}

/** PD in mm; dimmed while the current frame is not accepted (the number is then the last good one). */
@Composable
private fun PdReadout(pdMm: Double?, live: Boolean) {
    Column(
        Modifier
            .background(Scrim, RoundedCornerShape(16.dp))
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .alpha(if (live) 1f else 0.6f),
    ) {
        Text(stringResource(R.string.pd_label), color = Color.White, fontSize = 14.sp)
        Text(
            if (pdMm != null) stringResource(R.string.pd_value, pdMm) else stringResource(R.string.pd_none),
            color = Color.White,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun DebugText(lines: List<String>) {
    Text(
        lines.joinToString("\n"),
        color = Color.White,
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        modifier = Modifier.background(Scrim).padding(8.dp),
    )
}

@Composable
private fun DebugBar(
    resolution: AnalysisResolution,
    showLabels: Boolean,
    onResolutionSelected: (AnalysisResolution) -> Unit,
    onToggleLabels: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().background(Scrim, RoundedCornerShape(12.dp)),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val labels = mapOf(
            AnalysisResolution.VGA to R.string.res_640,
            AnalysisResolution.HD to R.string.res_1280,
            AnalysisResolution.FULL_HD to R.string.res_1920,
        )
        for ((option, label) in labels) {
            DebugButton(stringResource(label), selected = option == resolution) { onResolutionSelected(option) }
        }
        DebugButton(stringResource(R.string.labels), selected = showLabels, onClick = onToggleLabels)
    }
}

@Composable
private fun DebugButton(text: String, selected: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.alpha(if (selected) 1f else 0.45f)) {
        Text(text, color = Color.White, fontSize = 12.sp)
    }
}
