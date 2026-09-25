package io.github.geanyl17.openalarm.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import io.github.geanyl17.openalarm.core.Alarm
import io.github.geanyl17.openalarm.core.AlarmController
import io.github.geanyl17.openalarm.core.ThemeSettings
import io.github.geanyl17.openalarm.core.nextTrigger
import io.github.geanyl17.openalarm.ui.resources.Res
import io.github.geanyl17.openalarm.ui.resources.alarm_set_for
import io.github.geanyl17.openalarm.ui.theme.OpenAlarmTheme
import io.github.geanyl17.openalarm.ui.theme.ProvideAppTheme
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.getString
import kotlin.time.Clock

private sealed interface Screen {
    data object AlarmList : Screen

    /** Editing [alarm], or creating a new one when it's null. */
    data class EditAlarm(val alarm: Alarm?) : Screen

    data object Theme : Screen
}

/**
 * The main app UI. [setupIssues] lists the permissions still missing, and [onFixSetupIssue]
 * takes the user to where they can grant one. Both come from the platform, as do [sounds] and [photos].
 * [wallpaperColor] is the wallpaper's main color, or null on phones that don't offer one.
 */
@Composable
fun App(
    controller: AlarmController,
    setupIssues: List<SetupIssue>,
    onFixSetupIssue: (SetupIssue) -> Unit,
    sounds: AlarmSounds,
    photos: AlarmPhotos,
    use24Hour: Boolean,
    theme: ThemeSettings,
    wallpaperColor: Color?,
    onThemeChange: (ThemeSettings) -> Unit,
) = ProvideAppTheme(theme, wallpaperColor) {
    OpenAlarmTheme {
        val backStack = remember { mutableStateListOf<Screen>(Screen.AlarmList) }
        val scope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }

        // Confirms when an alarm will ring, so a wrong AM/PM or day is easy to spot.
        suspend fun confirmSchedule(alarm: Alarm) {
            val now = Clock.System.now()
            val at = alarm.nextTrigger(now, TimeZone.currentSystemDefault()) ?: return
            snackbarHostState.showSnackbar(getString(Res.string.alarm_set_for, durationText(at - now)))
        }

        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryProvider = entryProvider {
                entry<Screen.AlarmList> {
                    val alarms by controller.alarms.collectAsState(initial = null)
                    AlarmListScreen(
                        alarms = alarms,
                        setupIssues = setupIssues,
                        use24Hour = use24Hour,
                        photos = photos,
                        snackbarHostState = snackbarHostState,
                        onFixSetupIssue = onFixSetupIssue,
                        onAdd = { backStack.add(Screen.EditAlarm(null)) },
                        onEdit = { backStack.add(Screen.EditAlarm(it)) },
                        onToggle = { alarm, enabled ->
                            scope.launch {
                                controller.setEnabled(alarm.id, enabled)
                                if (enabled) confirmSchedule(alarm.copy(enabled = true, snoozedUntil = null))
                            }
                        },
                        onOpenTheme = { backStack.add(Screen.Theme) },
                    )
                }
                entry<Screen.Theme> {
                    ThemeScreen(
                        theme = theme,
                        wallpaperColorsAvailable = wallpaperColor != null,
                        onChange = onThemeChange,
                        onClose = { backStack.removeLastOrNull() },
                    )
                }
                entry<Screen.EditAlarm> { screen ->
                    AlarmEditorScreen(
                        initial = screen.alarm,
                        use24Hour = use24Hour,
                        sounds = sounds,
                        photos = photos,
                        onSave = { alarm ->
                            backStack.removeLastOrNull()
                            scope.launch { confirmSchedule(controller.save(alarm)) }
                        },
                        onDelete = { alarm ->
                            backStack.removeLastOrNull()
                            scope.launch { controller.delete(alarm.id) }
                        },
                        onClose = { backStack.removeLastOrNull() },
                    )
                }
            },
        )
    }
}
