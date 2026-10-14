package com.example.rgbleddemo.ui.led

import com.example.rgbleddemo.ui.mvi.UnidirectionalViewModel

interface LedContract {

    interface ViewModel : UnidirectionalViewModel<State, Event, Effect>

    data class State(
        /** Null while the LED API is reachable; otherwise why every control is disabled. */
        val unavailableReason: String? = null,
        val status: String = "",
        val mode: Mode = Mode.OFF,
        val color: Int = PRESETS.first().rgb,
        val brightness: Int = 255,
        val onMs: Int = 500,
        val offMs: Int = 500,
        val channels: List<Boolean> = listOf(false, false, false),
        val triggers: List<String> = emptyList(),
        val triggerTarget: TriggerTarget = TriggerTarget.ALL,
        val trigger: String? = null,
        val ledState: String = "",
    ) {
        val enabled: Boolean get() = unavailableReason == null
    }

    sealed interface Event {
        data object LoadState : Event
        data class PresetClicked(val rgb: Int) : Event
        data class BrightnessChanged(val brightness: Int) : Event
        data object BrightnessChangeFinished : Event
        data class ChannelToggled(val channel: Int, val on: Boolean) : Event
        data class OnMsChanged(val onMs: Int) : Event
        data class OffMsChanged(val offMs: Int) : Event
        data object BlinkClicked : Event
        data class TriggerTargetSelected(val target: TriggerTarget) : Event
        data class TriggerSelected(val trigger: String) : Event
        data object ApplyTriggerClicked : Event
        data object TurnOffClicked : Event
        data object RefreshClicked : Event
    }

    sealed interface Effect {
        data class ShowFailure(val action: String) : Effect
    }

    /** Which LED call the brightness slider re-applies to. */
    enum class Mode { OFF, COLOR, CHANNELS, BLINK }

    enum class TriggerTarget(val label: String, val channels: List<Int>) {
        ALL("All", listOf(0, 1, 2)),
        RED("Red", listOf(0)),
        GREEN("Green", listOf(1)),
        BLUE("Blue", listOf(2)),
    }

    data class Preset(val name: String, val rgb: Int)

    companion object {
        const val MIN_ON_MS = 50
        const val MAX_BLINK_MS = 2000

        val PRESETS = listOf(
            Preset("Red", 0xFF0000), Preset("Green", 0x00FF00),
            Preset("Blue", 0x0000FF), Preset("Yellow", 0xFFFF00),
            Preset("Cyan", 0x00FFFF), Preset("Magenta", 0xFF00FF),
            Preset("White", 0xFFFFFF), Preset("Orange", 0xFF6000),
        )
    }
}
