# 3. The stub-jar trick: compiling an app against a custom framework API

Files: [`sdk/`](../sdk/)

## The problem

`RgbLedManager` lives in the device's `framework.jar`. The normal `android.jar` from the Android SDK
that Android Studio compiles against has never heard of it, so this fails to compile:

```kotlin
import android.hardware.rgbled.RgbLedManager   // unresolved reference
```

Common workarounds are reflection (no type safety, ugly) or swapping in a full custom `android.jar`
(fragile, and it breaks with every SDK/AGP update).

## The solution: a tiny compile-only jar

The class only has to exist **at compile time**. At runtime the app's class loader asks the boot
class path first, and `framework.jar` on the device already contains the real `RgbLedManager`. So:

1. Build a jar that contains only `android/hardware/rgbled/*.class` **stubs**: the same
   signatures, with bodies that just `throw new RuntimeException("Stub!")`.
2. Add it to the app as **`compileOnly`**, so it is on the compile classpath but **not packaged into
   the APK**.

```groovy
dependencies {
    // RGB LED SDK stubs; the real classes come from the device framework at runtime.
    compileOnly files('libs/rgbled-sdk.jar')
}
```

```
compile time:  app.kt ──► rgbled-sdk.jar (stubs)  ✔ type-checks
APK:           classes.dex has NO android.hardware.rgbled.* classes
runtime:       BootClassLoader ──► /system/framework/framework.jar ──► real RgbLedManager
```

If the stubs were packaged with `implementation`, the APK would carry its own copy. The boot class
path still wins, so it would mostly work, but it would be wrong and would hide mistakes.

## Where the stubs come from

The platform build already generates them. `m update-api` (or any SDK build) runs metalava/doclava
over `frameworks/base` and writes stub sources for the public API to:

```
out/target/common/obj/JAVA_LIBRARIES/android_stubs_current_intermediates/src/
```

[`build-sdk.sh`](../sdk/build-sdk.sh) takes only the `android/hardware/rgbled` package from there,
compiles it with `javac -source 1.8 -target 1.8`, and produces:

| Output | Purpose |
|---|---|
| `rgbled-sdk.jar` | The compile-only stubs (about 1.5 KB) |
| `rgbled-sdk-sources.jar` | The real `RgbLedManager.java`, so the IDE shows javadoc |
| `api.txt` | The `android.hardware.rgbled` section of `api/current.txt`, as a readable API reference |

It also copies the jar into `RgbLedDemo/app/libs/` if the app is next to it.

```bash
source build/envsetup.sh && breakfast bullhead
m update-api
/path/to/nexus5x-rgbled/sdk/build-sdk.sh
```

Because the stubs come from the generated API surface instead of a hand-written copy, they can't
drift from what's actually on the device.

## Guarding against missing classes at runtime

On a stock ROM the class doesn't exist, and touching it throws `NoClassDefFoundError`. The demo
keeps every use of the API in one class, `LedController`, and checks availability first:

```kotlin
try {
    Class.forName("android.hardware.rgbled.RgbLedManager")
} catch (e: ClassNotFoundException) {
    return fail("This system image has no RGB LED API...")
}
val manager = context.getSystemService(RgbLedManager::class.java)
    ?: return fail("RgbLedService is not running in system_server.")
if (!manager.isAvailable) return fail("...HAL (vendor.lineage.rgbled@1.0) is not reachable.")
```

That covers the three ways it can be missing: the class (wrong ROM), the service (not started), or
the HAL (daemon not running or blocked by SELinux).

## Same idea elsewhere

This is the same mechanism behind `<uses-library>` + `compileOnly` for OEM SDKs (for example
`com.google.android.maps` and many vendor add-ons), and behind how platform apps compile against
`@hide` APIs. A stub jar is the lightweight version that needs no SDK add-on packaging.
