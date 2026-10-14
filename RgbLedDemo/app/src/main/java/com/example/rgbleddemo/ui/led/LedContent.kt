package com.example.rgbleddemo.ui.led

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.rgbleddemo.R
import com.example.rgbleddemo.ui.led.LedContract.Event
import com.example.rgbleddemo.ui.led.LedContract.Companion.MAX_BLINK_MS
import com.example.rgbleddemo.ui.led.LedContract.Companion.MIN_ON_MS
import com.example.rgbleddemo.ui.led.LedContract.Companion.PRESETS
import com.example.rgbleddemo.ui.led.LedContract.State
import com.example.rgbleddemo.ui.led.LedContract.TriggerTarget

/** Stateless: renders [state] and reports every user action as an [Event]. */
@Composable
fun LedContent(
    state: State,
    onEvent: (Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(
            state.unavailableReason ?: state.status,
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(12.dp),
            style = MaterialTheme.typography.bodySmall,
        )
        ColorSection(state, onEvent)
        BrightnessSection(state, onEvent)
        ChannelSection(state, onEvent)
        BlinkSection(state, onEvent)
        TriggerSection(state, onEvent)
        StateSection(state, onEvent)
    }
}

@Composable
private fun ColorSection(state: State, onEvent: (Event) -> Unit) {
    SectionTitle(stringResource(R.string.section_color))
    PRESETS.chunked(4).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            row.forEach { preset ->
                Button(
                    onClick = { onEvent(Event.PresetClicked(preset.rgb)) },
                    enabled = state.enabled,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF000000.toInt() or preset.rgb),
                        contentColor = if (isLight(preset.rgb)) Color.Black else Color.White,
                    ),
                    contentPadding = ButtonDefaults.TextButtonContentPadding,
                ) { Text(preset.name, maxLines = 1) }
            }
        }
    }
}

@Composable
private fun BrightnessSection(state: State, onEvent: (Event) -> Unit) {
    SectionTitle("${stringResource(R.string.section_brightness)}: ${state.brightness}")
    Slider(
        value = state.brightness.toFloat(),
        onValueChange = { onEvent(Event.BrightnessChanged(it.toInt())) },
        onValueChangeFinished = { onEvent(Event.BrightnessChangeFinished) },
        valueRange = 0f..255f,
        enabled = state.enabled,
    )
}

@Composable
private fun ChannelSection(state: State, onEvent: (Event) -> Unit) {
    SectionTitle(stringResource(R.string.section_channels))
    Row {
        listOf(R.string.red, R.string.green, R.string.blue).forEachIndexed { c, label ->
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(label), Modifier.padding(end = 8.dp))
                Switch(
                    checked = state.channels[c],
                    onCheckedChange = { onEvent(Event.ChannelToggled(c, it)) },
                    enabled = state.enabled,
                )
            }
        }
    }
}

@Composable
private fun BlinkSection(state: State, onEvent: (Event) -> Unit) {
    SectionTitle(stringResource(R.string.section_blink))
    Text("On time: ${state.onMs} ms")
    Slider(
        value = state.onMs.toFloat(),
        onValueChange = { onEvent(Event.OnMsChanged(it.toInt())) },
        valueRange = MIN_ON_MS.toFloat()..MAX_BLINK_MS.toFloat(),
        enabled = state.enabled,
    )
    Text("Off time: ${state.offMs} ms (0 = solid)")
    Slider(
        value = state.offMs.toFloat(),
        onValueChange = { onEvent(Event.OffMsChanged(it.toInt())) },
        valueRange = 0f..MAX_BLINK_MS.toFloat(),
        enabled = state.enabled,
    )
    Button(
        onClick = { onEvent(Event.BlinkClicked) },
        enabled = state.enabled,
        modifier = Modifier.fillMaxWidth(),
    ) { Text(stringResource(R.string.start_blink)) }
}

@Composable
private fun TriggerSection(state: State, onEvent: (Event) -> Unit) {
    SectionTitle(stringResource(R.string.section_trigger))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Dropdown(
            items = TriggerTarget.entries,
            selected = state.triggerTarget,
            label = { it.label },
            enabled = state.enabled,
            modifier = Modifier.weight(1f),
            onSelect = { onEvent(Event.TriggerTargetSelected(it)) },
        )
        Dropdown(
            items = state.triggers,
            selected = state.trigger,
            label = { it },
            enabled = state.enabled,
            modifier = Modifier.weight(2f),
            onSelect = { onEvent(Event.TriggerSelected(it)) },
        )
    }
    Button(
        onClick = { onEvent(Event.ApplyTriggerClicked) },
        enabled = state.enabled && state.trigger != null,
        modifier = Modifier.fillMaxWidth(),
    ) { Text(stringResource(R.string.apply_trigger)) }
}

@Composable
private fun StateSection(state: State, onEvent: (Event) -> Unit) {
    SectionTitle(stringResource(R.string.section_state))
    Text(state.ledState, fontFamily = FontFamily.Monospace)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = { onEvent(Event.TurnOffClicked) },
            enabled = state.enabled,
            modifier = Modifier.weight(1f),
        ) { Text(stringResource(R.string.turn_off)) }
        OutlinedButton(
            onClick = { onEvent(Event.RefreshClicked) },
            enabled = state.enabled,
            modifier = Modifier.weight(1f),
        ) { Text(stringResource(R.string.refresh)) }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        Modifier.padding(top = 20.dp, bottom = 4.dp),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun <T> Dropdown(
    items: List<T>,
    selected: T?,
    label: (T) -> String,
    enabled: Boolean,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Whether the menu is open is purely visual, so it stays local instead of in State.
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            enabled = enabled && items.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("${selected?.let(label) ?: "-"} ▾", maxLines = 1) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(label(item)) },
                    onClick = {
                        onSelect(item)
                        expanded = false
                    },
                )
            }
        }
    }
}

private fun isLight(rgb: Int): Boolean {
    val r = (rgb shr 16) and 0xff
    val g = (rgb shr 8) and 0xff
    val b = rgb and 0xff
    return (r * 299 + g * 587 + b * 114) / 1000 > 150
}

@Preview(showBackground = true)
@Composable
private fun LedContentPreview() {
    MaterialTheme {
        Surface {
            LedContent(
                state = State(
                    status = "Color #FF00FF: OK",
                    color = 0xFF00FF,
                    channels = listOf(true, false, true),
                    triggers = listOf("none", "timer", "heartbeat"),
                    trigger = "heartbeat",
                    ledState = "red   brightness=255  trigger=none\n" +
                        "green brightness=  0  trigger=none\n" +
                        "blue  brightness=255  trigger=none",
                ),
                onEvent = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LedContentUnavailablePreview() {
    MaterialTheme {
        Surface {
            LedContent(
                state = State(unavailableReason = "RgbLedService is not running in system_server."),
                onEvent = {},
            )
        }
    }
}
