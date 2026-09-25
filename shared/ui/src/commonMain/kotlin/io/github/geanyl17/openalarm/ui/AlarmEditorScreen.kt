package io.github.geanyl17.openalarm.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import io.github.geanyl17.openalarm.core.Alarm
import io.github.geanyl17.openalarm.core.Difficulty
import io.github.geanyl17.openalarm.core.Mission
import io.github.geanyl17.openalarm.core.MissionType
import io.github.geanyl17.openalarm.core.RepeatDays
import io.github.geanyl17.openalarm.core.nextTrigger
import io.github.geanyl17.openalarm.missions.LocalMissionSensors
import io.github.geanyl17.openalarm.missions.canRun
import io.github.geanyl17.openalarm.missions.difficultyName
import io.github.geanyl17.openalarm.missions.missionName
import io.github.geanyl17.openalarm.ui.resources.Res
import io.github.geanyl17.openalarm.ui.resources.alertness_measure
import io.github.geanyl17.openalarm.ui.resources.alertness_not_measured
import io.github.geanyl17.openalarm.ui.resources.alertness_speed
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
import io.github.geanyl17.openalarm.ui.resources.nfc_scan_again
import io.github.geanyl17.openalarm.ui.resources.nfc_tag_saved
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
import kotlin.time.Duration
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.seconds

private val RoundChoices = listOf(1, 2, 3, 5)

/** Keeps the missions being edited across configuration changes, as type, difficulty, rounds and tag for each. */
private val MissionsSaver = listSaver<List<Mission>, Any?>(
    save = { missions -> missions.flatMap { listOf(it.type.ordinal, it.difficulty.ordinal, it.rounds, it.tag) } },
    restore = { values ->
        values.chunked(4).map { (type, difficulty, rounds, tag) ->
            Mission(MissionType.entries[type as Int], Difficulty.entries[difficulty as Int], rounds as Int, tag as String?)
        }
    },
)

/** No limit, then fewer and fewer snoozes, down to none. */
private val SnoozeLimits = listOf(null, 3, 2, 1, 0)
private const val MAX_LABEL_LENGTH = 60

/** How far a row's press highlight reaches past its text on each side. */
private val RowHighlightBleed = 12.dp

/** Creates a new alarm when [initial] is null, otherwise edits it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AlarmEditorScreen(
    initial: Alarm?,
    use24Hour: Boolean,
    sounds: AlarmSounds,
    photos: AlarmPhotos,
    alertnessBaseline: Duration?,
    onAlertnessBaseline: (Duration) -> Unit,
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
                                        // A tag scanned for a mission that's since become another kind isn't needed.
                                        missions = missions.map { if (it.type == MissionType.NfcTag) it else it.copy(tag = null) },
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
                val sensors = LocalMissionSensors.current
                missions.forEachIndexed { index, mission ->
                    MissionCard(
                        number = index + 1,
                        mission = mission,
                        alertnessBaseline = alertnessBaseline,
                        onAlertnessBaseline = onAlertnessBaseline,
                        onChange = { changed -> missions = missions.toMutableList().also { it[index] = changed } },
                        onRemove = { missions = missions.toMutableList().also { it.removeAt(index) } },
                    )
                }
                if (missions.size < Alarm.MAX_MISSIONS) {
                    OutlinedButton(
                        onClick = {
                            // A chain is more fun with different missions, so start with one that isn't in it yet.
                            val type = MissionType.entries.firstOrNull { type -> missions.none { it.type == type } && sensors.canRun(type) }
                                ?: MissionType.Math
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
                SwitchRow(stringResource(Res.string.check_ins), checkIns) {
                    // Counting steps lets check-ins be skipped for someone who's clearly up and walking around.
                    if (it) sensors.requestSteps()
                    checkIns = it
                }

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
private fun MissionCard(
    number: Int,
    mission: Mission,
    alertnessBaseline: Duration?,
    onAlertnessBaseline: (Duration) -> Unit,
    onChange: (Mission) -> Unit,
    onRemove: () -> Unit,
) {
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
            // Missions this phone can't run aren't offered.
            val sensors = LocalMissionSensors.current
            val types = MissionType.entries.filter { it == mission.type || sensors.canRun(it) }
            var scanningTag by rememberSaveable { mutableStateOf(false) }
            ChoiceChips(
                types,
                mission.type,
                label = { missionName(it) },
                onSelect = {
                    when (it) {
                        // An NFC mission needs its tag, so it's only picked once a tag is scanned.
                        MissionType.NfcTag -> if (mission.tag == null) scanningTag = true else onChange(mission.copy(type = it))
                        MissionType.Steps -> {
                            sensors.requestSteps()
                            onChange(mission.copy(type = it))
                        }
                        else -> onChange(mission.copy(type = it))
                    }
                },
            )
            if (mission.type == MissionType.NfcTag) {
                // The tag is the whole mission: there's no difficulty or rounds.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(Res.string.nfc_tag_saved), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = { scanningTag = true }) { Text(stringResource(Res.string.nfc_scan_again)) }
                }
            } else {
                Text(stringResource(Res.string.mission_difficulty), style = MaterialTheme.typography.labelLarge)
                ChoiceChips(Difficulty.entries, mission.difficulty, label = { difficultyName(it) }, onSelect = { onChange(mission.copy(difficulty = it)) })
                if (mission.type == MissionType.Alertness) {
                    // One run is the whole mission, compared to the user's own daytime speed.
                    var measuring by rememberSaveable { mutableStateOf(false) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = alertnessBaseline?.let { stringResource(Res.string.alertness_speed, it.inWholeMilliseconds) }
                                ?: stringResource(Res.string.alertness_not_measured),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { measuring = true }) { Text(stringResource(Res.string.alertness_measure)) }
                    }
                    if (measuring) {
                        AlertnessMeasureDialog(
                            onMeasured = {
                                measuring = false
                                onAlertnessBaseline(it)
                            },
                            onDismiss = { measuring = false },
                        )
                    }
                } else {
                    Text(stringResource(Res.string.mission_rounds), style = MaterialTheme.typography.labelLarge)
                    ChoiceChips(RoundChoices, mission.rounds, label = { it.toString() }, onSelect = { onChange(mission.copy(rounds = it)) })
                }
            }
            if (scanningTag) {
                NfcTagScanDialog(
                    onScanned = { tag ->
                        scanningTag = false
                        onChange(mission.copy(type = MissionType.NfcTag, tag = tag))
                    },
                    onDismiss = { scanningTag = false },
                )
            }
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
            .padding(top = 8.dp)
            .bleed(RowHighlightBleed)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable { sounds.choose(sound, onChosen) }
            .padding(horizontal = RowHighlightBleed, vertical = 12.dp),
    ) {
        Text(stringResource(Res.string.sound), style = MaterialTheme.typography.bodyLarge)
        Text(name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Widens an element by [amount] on each side without moving its content, so a rounded press highlight
 * has some room around the text instead of ending right at its edge.
 */
private fun Modifier.bleed(amount: Dp): Modifier = layout { measurable, constraints ->
    val extra = (amount * 2).roundToPx()
    val placeable = measurable.measure(constraints.offset(horizontal = extra))
    layout(placeable.width - extra, placeable.height) { placeable.place(-extra / 2, 0) }
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
    // The whole row toggles, but only the switch lights up when pressed or hovered, not a box around the row.
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null, interactionSource = interactionSource)
    }
}
