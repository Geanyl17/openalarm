package io.github.geanyl17.openalarm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private val ItemHeight = 52.dp

/** How many copies of a looping wheel's values to lay out, so it can be scrolled practically forever. */
private const val LOOP_COPIES = 400

/** How many values a wheel shows: fewer in a short window, so what's around the wheels stays in view. */
@Composable
internal fun wheelVisibleItems(): Int = if (isShortWindow()) 3 else 5

/** Wheels side by side, over a band that marks the selected row. */
@Composable
internal fun WheelRow(visibleItems: Int, modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Box(modifier.fillMaxWidth().height(ItemHeight * visibleItems), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .height(ItemHeight)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
        )
        Row(verticalAlignment = Alignment.CenterVertically, content = content)
    }
}

/**
 * One wheel of [values]. Flick, drag or tap a value to move it into the band. Screen readers see it
 * as a single adjustable control (like a slider) rather than a list of numbers.
 */
@Composable
internal fun <T> Wheel(
    values: List<T>,
    initial: T,
    label: (T) -> String,
    description: String,
    onSelect: (T) -> Unit,
    visibleItems: Int,
    looping: Boolean = true,
    width: Dp = 80.dp,
) {
    val size = values.size
    val count = if (looping) size * LOOP_COPIES else size
    val initialIndex = values.indexOf(initial).coerceAtLeast(0) + if (looping) size * (LOOP_COPIES / 2) else 0
    val state = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val currentOnSelect by rememberUpdatedState(onSelect)
    val centered by remember { derivedStateOf { state.centeredItemIndex() ?: initialIndex } }
    val centeredIndex = centered

    LaunchedEffect(state) {
        snapshotFlow { centered }.drop(1).collect { index ->
            haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
            currentOnSelect(values[index % size])
        }
    }

    LazyColumn(
        state = state,
        flingBehavior = rememberSnapFlingBehavior(state),
        // Scroll positions count from after this padding, so changing visibleItems keeps the same value selected.
        contentPadding = PaddingValues(vertical = ItemHeight * (visibleItems / 2)),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(width)
            .height(ItemHeight * visibleItems)
            .clearAndSetSemantics {
                contentDescription = description
                stateDescription = label(values[centeredIndex % size])
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = (centeredIndex % size).toFloat(),
                    range = 0f..(size - 1).toFloat(),
                    steps = (size - 2).coerceAtLeast(0),
                )
                setProgress { target ->
                    val delta = target.roundToInt().coerceIn(0, size - 1) - centeredIndex % size
                    scope.launch { state.animateScrollToItem(centeredIndex + delta) }
                    true
                }
            },
    ) {
        items(count) { index ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(ItemHeight)
                    .clickable(interactionSource = null, indication = null) {
                        scope.launch { state.animateScrollToItem(index) }
                    }
                    .graphicsLayer {
                        // Values shrink and fade the further they are from the band.
                        val distance = state.distanceFromCenter(index)
                        alpha = (1f - 0.3f * distance).coerceAtLeast(0.2f)
                        scaleX = (1f - 0.12f * distance).coerceAtLeast(0.7f)
                        scaleY = scaleX
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label(values[index % size]),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

private fun LazyListState.centeredItemIndex(): Int? {
    val info = layoutInfo
    val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
    return info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - center) }?.index
}

/** How far item [index] is from the middle of the wheel, in items. */
private fun LazyListState.distanceFromCenter(index: Int): Float {
    val info = layoutInfo
    val item = info.visibleItemsInfo.firstOrNull { it.index == index } ?: return Float.MAX_VALUE
    val center = (info.viewportStartOffset + info.viewportEndOffset) / 2f
    return abs(item.offset + item.size / 2f - center) / item.size
}
