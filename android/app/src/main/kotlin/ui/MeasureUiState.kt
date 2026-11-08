package oyoon.app.ui

import androidx.camera.core.AspectRatio
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import oyoon.core.gate.RejectReason

/** Everything the measurement screen shows. Replaced as a whole on every frame. */
data class MeasureUiState(
    /** Latest landmarks for the overlay, or null when no face is visible. */
    val face: FaceLandmarkerResult? = null,
    val imageWidth: Int = 1,
    val imageHeight: Int = 1,
    val status: ChipStatus = ChipStatus.Searching,
    /** Live PD from accepted frames only; keeps its last value while frames are rejected. */
    val pdMm: Double? = null,
    /** Error or permission message, shown instead of the normal status. */
    val message: String? = null,
    /** Debug-build only text lines (timing, resolution, iris, pose). */
    val debugLines: List<String> = emptyList(),
)

sealed interface ChipStatus {
    data object Searching : ChipStatus
    data object Accepted : ChipStatus
    data class Rejected(val reason: RejectReason) : ChipStatus
}

/** Analysis resolutions selectable from the debug bar, in landscape (sensor) terms. */
enum class AnalysisResolution(val width: Int, val height: Int, val aspectRatio: Int) {
    VGA(640, 480, AspectRatio.RATIO_4_3),
    HD(1280, 720, AspectRatio.RATIO_16_9),
    FULL_HD(1920, 1080, AspectRatio.RATIO_16_9),
}
