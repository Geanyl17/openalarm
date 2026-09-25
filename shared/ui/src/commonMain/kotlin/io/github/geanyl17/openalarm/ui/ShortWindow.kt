package io.github.geanyl17.openalarm.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp

/** Whether the window is short, like a phone in landscape, so tall content has to shrink or move aside. */
@Composable
internal fun isShortWindow(): Boolean = LocalWindowInfo.current.containerDpSize.height < 600.dp
