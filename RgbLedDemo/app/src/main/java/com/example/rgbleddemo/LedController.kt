package com.example.rgbleddemo

import android.content.Context
import android.hardware.rgbled.RgbLedManager

/**
 * The only class that touches the RGB LED SDK. RgbLedManager exists only on builds
 * that include RgbLedService, so availability is checked before the class is used.
 */
class LedController private constructor(private val manager: RgbLedManager) {

    fun setColor(rgb: Int, brightness: Int) = manager.setColor(rgb, brightness)

    fun setChannel(channel: Int, brightness: Int) = manager.setChannel(channel, brightness)

    fun setBlink(rgb: Int, brightness: Int, onMs: Int, offMs: Int) =
        manager.setBlink(rgb, brightness, onMs, offMs)

    fun supportedTriggers(): List<String> = manager.supportedTriggers.toList()

    fun setTrigger(channel: Int, trigger: String) = manager.setTrigger(channel, trigger)

    fun brightness(channel: Int) = manager.getBrightness(channel)

    fun turnOff() = manager.turnOff()

    fun describeState(): String =
        (RgbLedManager.CHANNEL_RED..RgbLedManager.CHANNEL_BLUE).joinToString("\n") { c ->
            "%-5s brightness=%3d  trigger=%s".format(
                CHANNEL_NAMES[c], manager.getBrightness(c), manager.getTrigger(c)
            )
        }

    companion object {
        val CHANNEL_NAMES = listOf("red", "green", "blue")

        /** Returns the controller, or a reason why the LED API is unusable. */
        fun create(context: Context): Result<LedController> {
            try {
                Class.forName("android.hardware.rgbled.RgbLedManager")
            } catch (e: ClassNotFoundException) {
                return fail(
                    "This system image has no RGB LED API. Flash the build that includes " +
                        "RgbLedService, then reopen the app."
                )
            }
            val manager = context.getSystemService(RgbLedManager::class.java)
                ?: return fail("RgbLedService is not running in system_server.")
            if (!manager.isAvailable) {
                return fail(
                    "RgbLedService is running but the RGB LED HAL " +
                        "(vendor.lineage.rgbled@1.0) is not reachable."
                )
            }
            return Result.success(LedController(manager))
        }

        private fun fail(why: String) = Result.failure<LedController>(IllegalStateException(why))
    }
}
