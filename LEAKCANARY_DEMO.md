# LeakCanary demo runbook

This project intentionally provides two debug variants because LeakCanary's own UI and Android
Studio's Profiler use different heap-dump owners.

| Variant | What it demonstrates | Where to see the result |
| --- | --- | --- |
| `nativeDebug` | LeakCanary's notification, heap analysis, and leak trace UI | Device notification, then the **Leaks** launcher icon and Logcat (`LeakCanary`) |
| `studioDebug` | Android Studio Profiler's LeakCanary integration | Android Studio **View > Tool Windows > Profiler**, select the running app, then use its LeakCanary task |

`studioDebug` includes Android Studio's bridge dependency. That bridge intentionally disables
LeakCanary's normal heap dumping so Studio can request and display the analysis. For that reason,
the native LeakCanary notification is expected only from `nativeDebug`.

## Native LeakCanary walkthrough

1. In **Build Variants**, select `nativeDebug`, then uninstall any older demo package from the
   emulator and run it.
2. Open **1. Static Reference Leak**. Leave **Leak Mode** enabled and tap **Finish Activity &
   Trigger Leak**.
3. Keep the app open on the dashboard. This demo sets `retainedVisibleThreshold = 1`, so the
   single destroyed Activity is enough to trigger a heap dump after LeakCanary's five-second
   retained-object check.
4. Tap the LeakCanary notification, or open **Leaks** in the launcher. In Android Studio Logcat,
   filter with `tag:LeakCanary` to see `Watching`, `retained`, `HEAP ANALYSIS RESULT`, and the
   reference chain.
5. Repeat with Leak Mode disabled. There should be no new application leak for the clean path.

On Android 13 and later, allow the notification permission when LeakCanary asks. If it was denied,
enable **Notifications** for the `LeakCanary Demo` app in system settings; heap analysis is still
printed to Logcat even when the banner is blocked.

The expected static leak chain contains a `static StaticLeakActivity.leakedActivity` reference.
The other screens demonstrate an unregistered singleton listener, a delayed main-thread Handler
callback, and a coroutine that outlives the Activity.

## Android Studio Profiler walkthrough

1. Select and run `studioDebug`.
2. Open **View > Tool Windows > Profiler** and select the running `studioDebug` process.
3. Use the Profiler's **LeakCanary** task. Trigger the same static leak by opening the screen with
   Leak Mode enabled and finishing it.
4. The Studio bridge receives the retained-object event and lets Profiler request the heap
   analysis. Use this build for the IDE view, not the separate **Leaks** app.

If nothing appears, verify the active variant first. Then use Logcat filters `LeakCanary` and
`StudioLeakCanary`, wait at least five seconds after the screen is destroyed, and ensure the app
was installed as a debug build.
