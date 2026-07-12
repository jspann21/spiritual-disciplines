package com.spiritualdisciplines.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/** Semantic, device-tuned feedback for the app's recurring interaction types. */
@Stable
class ExpressiveHaptics internal constructor(private val feedback: HapticFeedback) {
    fun press() = feedback.performHapticFeedback(HapticFeedbackType.VirtualKey)
    fun select() = feedback.performHapticFeedback(HapticFeedbackType.SegmentTick)
    fun toggle(enabled: Boolean) = feedback.performHapticFeedback(
        if (enabled) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff
    )
    fun confirm() = feedback.performHapticFeedback(HapticFeedbackType.Confirm)
    fun reject() = feedback.performHapticFeedback(HapticFeedbackType.Reject)

    inline fun pressed(action: () -> Unit) {
        press()
        action()
    }

    inline fun selected(action: () -> Unit) {
        select()
        action()
    }

    inline fun confirmed(action: () -> Unit) {
        confirm()
        action()
    }
}

@Composable
fun rememberExpressiveHaptics(): ExpressiveHaptics {
    val feedback = LocalHapticFeedback.current
    return remember(feedback) { ExpressiveHaptics(feedback) }
}

/**
 * Adds restrained scroll texture across nested scroll containers. Feedback is emitted only for
 * direct finger movement—not flings—and is spaced far enough apart to avoid a continuous buzz.
 */
@Composable
fun Modifier.expressiveScrollFeedback(): Modifier {
    val feedback = LocalHapticFeedback.current
    val tickDistancePx = with(LocalDensity.current) { 96.dp.toPx() }
    val connection = remember(feedback, tickDistancePx) {
        object : NestedScrollConnection {
            var accumulated = 0f

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (source == NestedScrollSource.UserInput) {
                    val delta = if (abs(consumed.y) >= abs(consumed.x)) consumed.y else consumed.x
                    if (delta != 0f) {
                        if (accumulated != 0f && accumulated * delta < 0f) accumulated = 0f
                        accumulated += delta
                        if (abs(accumulated) >= tickDistancePx) {
                            feedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                            accumulated %= tickDistancePx
                        }
                    }
                }
                return Offset.Zero
            }
        }
    }
    return nestedScroll(connection)
}
