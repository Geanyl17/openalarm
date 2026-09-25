package io.github.geanyl17.openalarm.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.geanyl17.openalarm.core.Alarm
import io.github.geanyl17.openalarm.core.Difficulty
import io.github.geanyl17.openalarm.core.Mission
import io.github.geanyl17.openalarm.core.MissionType
import io.github.geanyl17.openalarm.core.RepeatDays
import io.github.geanyl17.openalarm.core.nextTrigger
import io.github.geanyl17.openalarm.missions.difficultyName
import io.github.geanyl17.openalarm.missions.missionName
import io.github.geanyl17.openalarm.ui.resources.Res
import io.github.geanyl17.openalarm.ui.resources.cancel
import io.github.geanyl17.openalarm.ui.resources.check_ins
import io.github.geanyl17.openalarm.ui.resources.color
import io.github.geanyl17.openalarm.ui.resources.delete_alarm
import io.github.geanyl17.openalarm.ui.resources.edit_alarm
import io.github.geanyl17.openalarm.ui.resources.fade_in
import io.github.geanyl17.openalarm.ui.resources.ic_add
import io.github.geanyl17.openalarm.ui.resources.ic_check
import io.github.geanyl17.openalarm.ui.resources.ic_close
import io.github.geanyl17.openalarm.ui.resources.ic_delete
import io.github.geanyl17.openalarm.ui.resources.label
import io.github.geanyl17.openalarm.ui.resources.mission_add
import io.github.geanyl17.openalarm.ui.resources.mission_difficulty
import io.github.geanyl17.openalarm.ui.resources.mission_number
import io.github.geanyl17.openalarm.ui.resources.mission_remove
import io.github.geanyl17.openalarm.ui.resources.mission_rounds
import io.github.geanyl17.openalarm.ui.resources.missions
import io.github.geanyl17.openalarm.ui.resources.new_alarm
import io.github.geanyl17.openalarm.ui.resources.photo
import io.github.geanyl17.openalarm.ui.resources.photo_add
import io.github.geanyl17.openalarm.ui.resources.photo_change
import io.github.geanyl17.openalarm.ui.resources.photo_remove
import io.github.geanyl17.openalarm.ui.resources.repeat
import io.github.geanyl17.openalarm.ui.resources.rings_in
import io.github.geanyl17.openalarm.ui.resources.save
import io.github.geanyl17.openalarm.ui.resources.snooze_length
import io.github.geanyl17.openalarm.ui.resources.snooze_limit
import io.github.geanyl17.openalarm.ui.resources.snooze_no_limit
import io.github.geanyl17.openalarm.ui.resources.snooze_off
import io.github.geanyl17.openalarm.ui.resources.sound
import io.github.geanyl17.openalarm.ui.resources.sound_custom
import io.github.geanyl17.openalarm.ui.resources.sound_default
import io.github.geanyl17.openalarm.ui.resources.vibrate
import io.github.geanyl17.openalarm.ui.theme.OpenAlarmTheme
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.seconds

private val RoundChoices = listOf(1, 2, 3, 5)

/** Keeps the missions being edited across configuration changes, as type, difficulty and rounds for each. */
private val MissionsSaver = listSaver<List<Mission>, Int>(
    save = { missions -> missions.flatMap { listOf(it.type.ordinal, it.difficulty.ordinal, it.rounds) } },
    restore = { values ->
        values.chunked(3).map { (type, difficulty, rounds) -> Mission(MissionType.entries[type], Difficulty.entries[difficulty], rounds) }
    },
)

/** No limit, then fewer and fewer snoozes, down to none. */
private val SnoozeLimits = listOf(null, 3, 2, 1, 0)
private const val MAX_LABEL_LENGTH = 60

/** Creates a new alarm when [initial] is null, otherwise edits it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AlarmEditorScreen(
    initial: Alarm?,
    use24Hour: Boolean,
    sounds: AlarmSounds,
    photos: AlarmPhotos,
    onSave: (Alarm) -> Unit,
    onDelete: (Alarm) -> Unit,
    onClose: () -> Unit,
) {
    val base = initial ?: Alarm(hour = 7, minute = 0)
    var hour by rememberSaveable { mutableIntStateOf(base.hour) }
    var minute by rememberSaveable { mutableIntStateOf(base.minute) }
    var repeat by rememberSaveable { mutableIntStateOf(base.repeat.mask) }
    var label by rememberSaveable { mutableStateOf(base.label) }
    var missions by rememberSaveable(stateSaver = MissionsSaver) { mutableStateOf(base.missions) }
    var sound by rememberSaveable { mutableStateOf(base.sound) }
    var photo by rememberSaveable { mutableStateOf(base.photo) }
    var colorArgb by rememberSaveable { mutableStateOf(base.colorArgb) }
    var vibrate by rememberSaveable { mutableStateOf(base.vibrate) }
    var fadeIn by rememberSaveable { mutableStateOf(base.fadeIn) }
    var checkIns by rememberSaveable { mutableStateOf(base.checkIns) }
    var snoozeMinutes by rememberSaveable { mutableIntStateOf(base.snoozeMinutes) }
    var snoozeLimit by rememberSaveable { mutableStateOf(base.snoozeLimit) }

    // The editor takes on the alarm's color, as a live preview of how it will look when it rings.
    OpenAlarmTheme(seedColor = colorArgb?.let { Color(it) }) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(if (initial == null) Res.string.new_alarm else Res.string.edit_alarm)) },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(painterResource(Res.drawable.ic_close), contentDescription = stringResource(Res.string.cancel))
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                onSave(
                                    base.copy(
                                        hour = hour,
                                        minute = minute,
                                        repeat = RepeatDays(repeat),
                                        label = label.trim(),
                                        enabled = true,
                                        vibrate = vibrate,
                                        fadeIn = fadeIn,
                                        checkIns = checkIns,
                                        snoozeMinutes = snoozeMinutes,
                                        snoozeLimit = snoozeLimit,
                                        colorArgb = colorArgb,
                                        sound = sound,
                                        photo = photo,
                                        missions = missions,
                                    ),
                                )
                            },
                        ) {
                            Text(stringResource(Res.string.save))
                        }
                    },
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TimeWheels(
                    hour = hour,
                    minute = minute,
                    use24Hour = use24Hour,
                    onChange = { newHour, newMinute ->
                        hour = newHour
                        minute = newMinute
                    },
                )
                RingsIn(base.copy(hour = hour, minute = minute, repeat = RepeatDays(repeat), enabled = true, snoozedUntil = null))

                SectionTitle(stringResource(Res.string.repeat))
                DayToggles(RepeatDays(repeat), onChange = { repeat = it.mask })

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it.take(MAX_LABEL_LENGTH) },
                    label = { Text(stringResource(Res.string.label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )

                SectionTitle(stringResource(Res.string.missions))
                missions.forEachIndexed { index, mission ->
                    MissionCard(
                        number = index + 1,
                        mission = mission,
                        onChange = { changed -> missions = missions.toMutableList().also { it[index] = changed } },
                        onRemove = { missions = missions.toMutableList().also { it.removeAt(index) } },
                    )
                }
                if (missions.size < Alarm.MAX_MISSIONS) {
                    OutlinedButton(
                        onClick = {
                            // A chain is more fun with different missions, so start with one that isn't in it yet.
                            val type = MissionType.entries.firstOrNull { type -> missions.none { it.type == type } } ?: MissionType.Math
                            missions = missions + Mission(type)
                        },
                    ) {
                        Icon(painterResource(Res.drawable.ic_add), contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(Res.string.mission_add))
                    }
                }

                SoundRow(sound, sounds, onChosen = { sound = it })

                SectionTitle(stringResource(Res.string.color))
                AlarmColorPicker(selected = colorArgb, onSelect = { colorArgb = it })

                SectionTitle(stringResource(Res.string.photo))
                PhotoPicker(photo, photos, onChange = { photo = it })

                SwitchRow(stringResource(Res.string.vibrate), vibrate) { vibrate = it }
                SwitchRow(stringResource(Res.string.fade_in), fadeIn) { fadeIn = it }
                SwitchRow(stringResource(Res.string.check_ins), checkIns) { checkIns = it }

                SectionTitle(stringResource(Res.string.snooze_length))
                SnoozeLengthPicker(snoozeMinutes, onSelect = { snoozeMinutes = it })
                Text(stringResource(Res.string.snooze_limit), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
                ChoiceChips(
                    options = SnoozeLimits,
                    selected = snoozeLimit,
                    label = { limit ->
                        when (limit) {
                            null -> stringResource(Res.string.snooze_no_limit)
                            0 -> stringResource(Res.string.snooze_off)
                            else -> limit.toString()
                        }
                    },
                    onSelect = { snoozeLimit = it },
                )

                if (initial != null) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { onDelete(initial) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(painterResource(Res.drawable.ic_delete), contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(Res.string.delete_alarm))
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

/** "Rings in 7 h 32 min", so a wrong hour or AM/PM is obvious before saving. */
@Composable
private fun ColumnScope.RingsIn(alarm: Alarm) {
    val now = rememberNow(tick = 10.seconds)
    val next = alarm.nextTrigger(now, TimeZone.currentSystemDefault()) ?: return
    Text(
        text = stringResource(Res.string.rings_in, formatDuration(next - now)),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.align(Alignment.CenterHorizontally),
    )
}

@Composable
internal fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun MissionCard(number: Int, mission: Mission, onChange: (Mission) -> Unit, onRemove: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(Res.string.mission_number, number), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                IconButton(onClick = onRemove, modifier = Modifier.offset(x = 12.dp)) {
                    Icon(painterResource(Res.drawable.ic_close), contentDescription = stringResource(Res.string.mission_remove, number))
                }
            }
            ChoiceChips(MissionType.entries, mission.type, label = { missionName(it) }, onSelect = { onChange(mission.copy(type = it)) })
            Text(stringResource(Res.string.mission_difficulty), style = MaterialTheme.typography.labelLarge)
            ChoiceChips(Difficulty.entries, mission.difficulty, label = { difficultyName(it) }, onSelect = { onChange(mission.copy(difficulty = it)) })
            Text(stringResource(Res.string.mission_rounds), style = MaterialTheme.typography.labelLarge)
            ChoiceChips(RoundChoices, mission.rounds, label = { it.toString() }, onSelect = { onChange(mission.copy(rounds = it)) })
        }
    }
}

@Composable
internal fun <T> ChoiceChips(options: List<T>, selected: T, label: @Composable (T) -> String, onSelect: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(label(option)) },
                // Shows the choice without relying on color, which matters most in Night red.
                leadingIcon = if (option == selected) {
                    { Icon(painterResource(Res.drawable.ic_check), contentDescription = null, Modifier.size(FilterChipDefaults.IconSize)) }
                } else {
                    null
                },
            )
        }
    }
}

@Composable
private fun SoundRow(sound: String?, sounds: AlarmSounds, onChosen: (String?) -> Unit) {
    val name = if (sound == null) {
        stringResource(Res.string.sound_default)
    } else {
        remember(sound) { sounds.name(sound) } ?: stringResource(Res.string.sound_custom)
    }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clickable { sounds.choose(sound, onChosen) }
            .padding(vertical = 12.dp),
    ) {
        Text(stringResource(Res.string.sound), style = MaterialTheme.typography.bodyLarge)
        Text(name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PhotoPicker(photo: String?, photos: AlarmPhotos, onChange: (String?) -> Unit) {
    if (photo == null) {
        OutlinedButton(onClick = { photos.choose(onChange) }) { Text(stringResource(Res.string.photo_add)) }
        return
    }
    val bitmap = rememberPhoto(photos, photo, maxSize = 1024)
    Column {
        Box(
            Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            bitmap?.let { Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        }
        Row(Modifier.align(Alignment.End)) {
            TextButton(onClick = { photos.choose(onChange) }) { Text(stringResource(Res.string.photo_change)) }
            TextButton(onClick = { onChange(null) }) { Text(stringResource(Res.string.photo_remove)) }
        }
    }
}

@Composable
private fun DayToggles(repeat: RepeatDays, onChange: (RepeatDays) -> Unit) {
    Row(Modifier.fillMaxWidth()) {
        DayOfWeek.entries.forEach { day ->
            val selected = day in repeat
            val name = stringResource(day.fullName)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .toggleable(
                        value = selected,
                        interactionSource = null,
                        // The whole cell is tappable, but the ripple is round to match the circle.
                        indication = ripple(bounded = false, radius = 22.dp),
                        role = Role.Checkbox,
                        onValueChange = { onChange(repeat.toggle(day)) },
                    )
                    .semantics { contentDescription = name },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainerHighest,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(day.letter),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
internal fun SwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}
