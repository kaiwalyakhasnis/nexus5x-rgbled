# 1. Kernel: the `leds-qpnp` driver

Patches: [`patches/kernel_lge_bullhead/`](../patches/kernel_lge_bullhead/)

## How the notification LED is wired

On the Nexus 5X the notification LED is a three-channel RGB LED driven by the **LPG** (Light Pulse
Generator) of the PMI8994 PMIC. The driver is `drivers/leds/leds-qpnp.c`. It registers one LED class
device per channel:

```
/sys/class/leds/red/
/sys/class/leds/green/
/sys/class/leds/blue/
    brightness   0-255
    on_off_ms    "<on> <off>" blink timing, programmed into the LPG lookup table
    rgb_start    1 = (re)start the pattern on ALL three channels, 0 = stop
    trigger      kernel LED trigger: "none [timer] heartbeat ..."
```

The important detail is `rgb_start`. The LPG does not apply a new level or pattern just because
`brightness` changed. `rgb_start_store()` walks all three RGB channels and calls `qpnp_rgb_set()` on
each one, and that is the point where the duty cycle / lookup table is written to the hardware. The
stock `liblight` follows this protocol: `rgb_start=0`, write every channel, `rgb_start=1`.

You can try it from a root shell:

```bash
echo 0   > /sys/class/leds/red/rgb_start
echo 255 > /sys/class/leds/red/brightness
echo "500 500" > /sys/class/leds/red/on_off_ms
echo 1   > /sys/class/leds/red/rgb_start      # red blinks
```

## Patch 1: dtc build fix (host toolchain only)

`scripts/dtc` defines `YYLTYPE yylloc;` in both the lexer and the parser. GCC 10+ defaults to
`-fno-common`, so the link fails with a *multiple definition of `yylloc`* error on any current
distro. Both definitions become `extern`. This has nothing to do with LEDs. The 2015-era kernel
simply won't build on a modern host without it.

## Patch 2: enable `CONFIG_LEDS_TRIGGER_HEARTBEAT`

LED *triggers* are kernel-side drivers that change an LED's brightness on their own. `timer` was
already enabled. This patch adds `heartbeat`, which pulses the LED in a double-beat rhythm that
speeds up with the system load average. After this patch, `heartbeat` appears in
`/sys/class/leds/*/trigger`.

## Patch 3: restart the RGB pattern from `brightness_set`

Enabling the trigger is not enough. A trigger changes the LED by calling the class device's
`brightness_set` callback, `qpnp_led_set()`, directly. That function only stores the new value and
schedules the work item. Nothing cycles `rgb_start`, so on the LPG-driven RGB channels the trigger
had no visible effect.

The patch adds `rgb_ledclassdev_start_store(led, bool)`, which is the body of the `rgb_start` sysfs
handler without the string parsing, and wraps `qpnp_led_set()` for the three RGB IDs:

```c
if (isRgb)
    rgb_ledclassdev_start_store(led, false);   // stop pattern on R, G, B

led->cdev.brightness = value;
queue_work(...) / schedule_work(...);          // stock path

if (isRgb)
    rgb_ledclassdev_start_store(led, true);    // restart -> qpnp_rgb_set() programs new level
```

After this change every brightness change, whether it comes from a trigger, from sysfs, or from the
HAL, reprograms the hardware, and `heartbeat` shows up on the LED.

### Caveats

- This is experiment-grade code. The restart touches all three channels on every brightness change
  of any one of them, and the work item queued in between runs asynchronously. It worked reliably on
  the device, but a cleaner upstream-style fix would do the restart inside the work handler.
- Formatting does not follow kernel style (spaces vs. tabs) and has not been run through
  `checkpatch.pl`.
