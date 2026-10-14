# 4. RgbLedDemo app

Source: [`RgbLedDemo/`](../RgbLedDemo/)

A single-screen Jetpack Compose app that exercises every `RgbLedManager` call.

<!-- TODO: screenshot -->

## Features

- **Color presets** (red, green, blue, yellow, cyan, magenta, white, orange) → `setColor()`
- **Brightness slider**, re-applied to whatever mode is active (color / channels / blink)
- **Per-channel toggles** for R, G, B → `setChannel()`
- **Blink** with on/off sliders → `setBlink()` (the PMIC hardware pattern, so no CPU wakeups)
- **Kernel triggers**: lists `getSupportedTriggers()` and applies one to all channels or a single
  one → `setTrigger()` (`heartbeat`, `timer`, `none`, ...)
- **Live state**: per-channel brightness and active trigger read back from the HAL
- **Turn off** → `turnOff()`

If the API is unavailable, every control is disabled and the reason is shown (see
[docs/03](03-sdk-stubs.md#guarding-against-missing-classes-at-runtime)).

## Structure

```
LedController.kt        the only class that imports android.hardware.rgbled.*
MainActivity.kt         builds LedViewModel(LedController.create(context))
ui/mvi/                 small MVI base: UnidirectionalViewModel<State, Event, Effect>
ui/led/LedContract.kt   State, Event, Effect for the screen
ui/led/LedViewModel.kt  turns Events into LedController calls, updates State
ui/led/LedScreen.kt     collects state/effects
ui/led/LedContent.kt    stateless composables
```

Keeping the SDK in `LedController` means the rest of the app never loads a class that may not
exist on the device.

## Build notes

| Setting | Value | Why |
|---|---|---|
| `compileSdk` | 35 | Current AndroidX/Compose libraries require it |
| `minSdk` / `targetSdk` | 27 | The device runs Android 8.1 (API 27) |
| `compileOnly files('libs/rgbled-sdk.jar')` | | See [docs/03](03-sdk-stubs.md) |
| `<uses-permission CONTROL_RGB_LED>` | | Required by the mutating calls; `normal` level, granted at install |
| lint `ExpiredTargetSdkVersion` disabled | | targetSdk 27 is deliberate |

```bash
cd RgbLedDemo
echo "sdk.dir=$HOME/Android/Sdk" > local.properties   # if Android Studio hasn't created it
./gradlew installDebug
```
