package io.github.geanyl17.openalarm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.geanyl17.openalarm.core.Alarm
import io.github.geanyl17.openalarm.core.Difficulty
import io.github.geanyl17.openalarm.core.Mission
import io.github.geanyl17.openalarm.core.MissionType
import io.github.geanyl17.openalarm.core.RepeatDays
import io.github.geanyl17.openalarm.missions.difficultyName
import io.github.geanyl17.openalarm.missions.missionDescription
import io.github.geanyl17.openalarm.missions.missionName
import io.github.geanyl17.openalarm.ui.resources.Res
import io.github.geanyl17.openalarm.ui.resources.cancel
import io.github.geanyl17.openalarm.ui.resources.color
import io.github.geanyl17.openalarm.ui.resources.delete_alarm
import io.github.geanyl17.openalarm.ui.resources.edit_alarm
import io.github.geanyl17.openalarm.ui.resources.fade_in
import io.github.geanyl17.openalarm.ui.resources.fade_in_description
import io.github.geanyl17.openalarm.ui.resources.ic_close
import io.github.geanyl17.openalarm.ui.resources.ic_delete
import io.github.geanyl17.openalarm.ui.resources.ic_keyboard
import io.github.geanyl17.openalarm.ui.resources.ic_schedule
import io.github.geanyl17.openalarm.ui.resources.label
import io.github.geanyl17.openalarm.ui.resources.minutes_short
import io.github.geanyl17.openalarm.ui.resources.mission
import io.github.geanyl17.openalarm.ui.resources.mission_difficulty
import io.github.geanyl17.openalarm.ui.resources.mission_none
import io.github.geanyl17.openalarm.ui.resources.mission_rounds
import io.github.geanyl17.openalarm.ui.resources.new_alarm
import io.github.geanyl17.openalarm.ui.resources.repeat
import io.github.geanyl17.openalarm.ui.resources.save
import io.github.geanyl17.openalarm.ui.resources.snooze_length
import io.github.geanyl17.openalarm.ui.resources.sound
import io.github.geanyl17.openalarm.ui.resources.sound_custom
import io.github.geanyl17.openalarm.ui.resources.sound_default
import io.github.geanyl17.openalarm.ui.resources.type_time
import io.github.geanyl17.openalarm.ui.resources.use_dial
import io.github.geanyl17.openalarm.ui.resources.vibrate
import kotlinx.datetime.DayOfWeek
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val SnoozeChoices = listOf(5, 9, 10, 15, 20)
private val RoundChoices = listOf(1, 2, 3, 5)
private const val MAX_LABEL_LENGTH = 60

/** Creates a new alarm when [initial] is null, otherwise edits it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AlarmEditorScreen(
    initial: Alarm?,
    use24Hour: Boolean,
    sounds: AlarmSounds,
    onSave: (Alarm) -> Unit,
    onDelete: (Alarm) -> Unit,
    onClose: () -> Unit,
) {
    val base = initial ?: Alarm(hour = 7, minute = 0)
    val baseMission = base.missions.firstOrNull()
    val timeState = rememberTimePickerState(initialHour = base.hour, initialMinute = base.minute, is24Hour = use24Hour)
    var typing by rememberSaveable { mutableStateOf(false) }
    var repeat by rememberSaveable { mutableIntStateOf(base.repeat.mask) }
    var label by rememberSaveable { mutableStateOf(base.label) }
    var missionType by rememberSaveable { mutableStateOf(baseMission?.type) }
    var difficulty by rememberSaveable { mutableStateOf(baseMission?.difficulty ?: Difficulty.Normal) }
    var rounds by rememberSaveable { mutableIntStateOf(baseMission?.rounds ?: 3) }
    var sound by rememberSaveable { mutableStateOf(base.sound) }
    var colorArgb by rememberSaveable { mutableStateOf(base.colorArgb) }
    var vibrate by rememberSaveable { mutableStateOf(base.vibrate) }
    var fadeIn by rememberSaveable { mutableStateOf(base.fadeIn) }
    var snoozeMinutes by rememberSaveable { mutableIntStateOf(base.snoozeMinutes) }

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
                                    hour = timeState.hour,
                                    minute = timeState.minute,
                                    repeat = RepeatDays(repeat),
                                    label = label.trim(),
                                    enabled = true,
                                    vibrate = vibrate,
                                    fadeIn = fadeIn,
                                    snoozeMinutes = snoozeMinutes,
                                    colorArgb = colorArgb,
                                    sound = sound,
                                    missions = listOfNotNull(missionType?.let { Mission(it, difficulty, rounds) }),
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
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (typing) TimeInput(state = timeState) else TimePicker(state = timeState)
            }
            TextButton(onClick = { typing = !typing }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Icon(painterResource(if (typing) Res.drawable.ic_schedule else Res.drawable.ic_keyboard), contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(if (typing) Res.string.use_dial else Res.string.type_time))
            }

            SectionTitle(stringResource(Res.string.repeat))
            DayToggles(RepeatDays(repeat), onChange = { repeat = it.mask })

            OutlinedTextField(
                value = label,
                onValueChange = { label = it.take(MAX_LABEL_LENGTH) },
                label = { Text(stringResource(Res.string.label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )

            SectionTitle(stringResource(Res.string.mission))
            ChoiceChips(
                options = listOf(null) + MissionType.entries,
                selected = missionType,
                label = { type -> if (type == null) stringResource(Res.string.mission_none) else missionName(type) },
                onSelect = { missionType = it },
            )
            missionType?.let { type ->
                Text(
                    text = missionDescription(Mission(type, difficulty, rounds)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(stringResource(Res.string.mission_difficulty), style = MaterialTheme.typography.labelLarge)
                ChoiceChips(Difficulty.entries, difficulty, label = { difficultyName(it) }, onSelect = { difficulty = it })
                Text(stringResource(Res.string.mission_rounds), style = MaterialTheme.typography.labelLarge)
                ChoiceChips(RoundChoices, rounds, label = { it.toString() }, onSelect = { rounds = it })
            }

            SoundRow(sound, sounds, onChosen = { sound = it })

            SectionTitle(stringResource(Res.string.color))
            AlarmColorPicker(selected = colorArgb, onSelect = { colorArgb = it })

            SwitchRow(stringResource(Res.string.vibrate), null, vibrate) { vibrate = it }
            SwitchRow(stringResource(Res.string.fade_in), stringResource(Res.string.fade_in_description), fadeIn) { fadeIn = it }

            SectionTitle(stringResource(Res.string.snooze_length))
            ChoiceChips(SnoozeChoices, snoozeMinutes, label = { stringResource(Res.string.minutes_short, it) }, onSelect = { snoozeMinutes = it })

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

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun <T> ChoiceChips(options: List<T>, selected: T, label: @Composable (T) -> String, onSelect: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(label(option)) },
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
private fun DayToggles(repeat: RepeatDays, onChange: (RepeatDays) -> Unit) {
    Row(Modifier.fillMaxWidth()) {
        DayOfWeek.entries.forEach { day ->
            val selected = day in repeat
            val name = stringResource(day.fullName)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onChange(repeat.toggle(day)) })
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
private fun SwitchRow(title: String, description: String?, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (description != null) {
                Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}
