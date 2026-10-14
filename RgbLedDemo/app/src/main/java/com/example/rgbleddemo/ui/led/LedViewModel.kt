package com.example.rgbleddemo.ui.led

import com.example.rgbleddemo.LedController
import com.example.rgbleddemo.ui.led.LedContract.Effect
import com.example.rgbleddemo.ui.led.LedContract.Event
import com.example.rgbleddemo.ui.led.LedContract.Mode
import com.example.rgbleddemo.ui.led.LedContract.State
import com.example.rgbleddemo.ui.mvi.BaseViewModel

class LedViewModel(
    led: Result<LedController>,
) : BaseViewModel<State, Event, Effect>(
    initialState = State(unavailableReason = led.exceptionOrNull()?.message),
),
    LedContract.ViewModel {

    private val led: LedController? = led.getOrNull()

    override fun event(event: Event) {
        // Every control is disabled without the LED API; ignore anything that slips through.
        val led = led ?: return
        when (event) {
            Event.LoadState -> handleOneTimeEvent(event) { load(led) }
            is Event.PresetClicked -> {
                updateState { it.copy(color = event.rgb) }
                applyColor(led)
            }
            is Event.BrightnessChanged -> updateState { it.copy(brightness = event.brightness) }
            Event.BrightnessChangeFinished -> reapply(led)
            is Event.ChannelToggled -> toggleChannel(led, event.channel, event.on)
            is Event.OnMsChanged -> updateState { it.copy(onMs = event.onMs) }
            is Event.OffMsChanged -> updateState { it.copy(offMs = event.offMs) }
            Event.BlinkClicked -> applyBlink(led)
            is Event.TriggerTargetSelected -> updateState { it.copy(triggerTarget = event.target) }
            is Event.TriggerSelected -> updateState { it.copy(trigger = event.trigger) }
            Event.ApplyTriggerClicked -> applyTrigger(led)
            Event.TurnOffClicked -> {
                updateState { it.copy(mode = Mode.OFF) }
                report(led, "Turn off", led.turnOff())
            }
            Event.RefreshClicked -> refresh(led)
        }
    }

    private fun load(led: LedController) {
        val triggers = led.supportedTriggers()
        updateState {
            it.copy(
                status = "Connected to RgbLedService.",
                triggers = triggers,
                trigger = triggers.firstOrNull { t -> t == "heartbeat" } ?: triggers.firstOrNull(),
            )
        }
        refresh(led)
    }

    private fun applyColor(led: LedController) {
        updateState { it.copy(mode = Mode.COLOR) }
        val s = state.value
        report(led, "Color #%06X".format(s.color), led.setColor(s.color, s.brightness))
    }

    private fun applyBlink(led: LedController) {
        updateState { it.copy(mode = Mode.BLINK) }
        val s = state.value
        report(
            led,
            "Blink #%06X %d/%d ms".format(s.color, s.onMs, s.offMs),
            led.setBlink(s.color, s.brightness, s.onMs, s.offMs),
        )
    }

    private fun toggleChannel(led: LedController, channel: Int, on: Boolean) {
        updateState { it.copy(mode = Mode.CHANNELS) }
        report(
            led,
            "Channel ${LedController.CHANNEL_NAMES[channel]} ${if (on) "on" else "off"}",
            led.setChannel(channel, if (on) state.value.brightness else 0),
        )
    }

    private fun applyTrigger(led: LedController) {
        val s = state.value
        val trigger = s.trigger ?: return
        // Apply to every target channel even if an earlier one fails.
        val ok = s.triggerTarget.channels.map { led.setTrigger(it, trigger) }.all { it }
        report(led, "Trigger '$trigger' on ${s.triggerTarget.label}", ok)
    }

    /** Brightness changed: re-run whatever the LED is currently showing. */
    private fun reapply(led: LedController) {
        val s = state.value
        when (s.mode) {
            Mode.COLOR -> applyColor(led)
            Mode.BLINK -> applyBlink(led)
            Mode.CHANNELS -> {
                s.channels.forEachIndexed { c, on -> if (on) led.setChannel(c, s.brightness) }
                refresh(led)
            }
            Mode.OFF -> Unit
        }
    }

    private fun report(led: LedController, action: String, ok: Boolean) {
        updateState { it.copy(status = action + if (ok) ": OK" else ": FAILED") }
        if (!ok) emitEffect(Effect.ShowFailure(action))
        refresh(led)
    }

    private fun refresh(led: LedController) {
        updateState {
            it.copy(
                ledState = led.describeState(),
                channels = List(3) { c -> led.brightness(c) > 0 },
            )
        }
    }
}
