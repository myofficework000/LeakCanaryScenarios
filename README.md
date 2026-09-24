# LeakCanary Scenarios

An interactive Android + Jetpack Compose project for demonstrating common memory leaks, reading
their retain paths, and applying the matching lifecycle-safe fix.

> This is a teaching project. The **Leak Mode** switch deliberately retains an Activity. Do not
> copy the leaking code into a production app.

## What you can demonstrate

| Scenario | Intentional leak | Lifecycle-safe fix |
| --- | --- | --- |
| Static reference | A companion-object field keeps an `Activity` context alive | Clear the reference in `onDestroy()` or do not store an Activity in static state |
| Singleton listener | A process-wide listener list retains the screen | Unregister the listener in the matching lifecycle callback |
| Handler callback | A delayed `Runnable` captures an Activity in the main Looper queue | Remove pending callbacks in `onDestroy()` |
| GlobalScope coroutine | A long-running coroutine outlives the Activity | Use `lifecycleScope` / structured concurrency |

## Two ways to inspect the same leak

The project intentionally contains two debug variants. Pick one in Android Studio's **Build
Variants** tool window.

| Variant | Use it for | Where the result appears |
| --- | --- | --- |
| `nativeDebug` | The standard LeakCanary experience | System notification, **Leaks** launcher icon, and Logcat |
| `studioDebug` | Android Studio integration | **View → Tool Windows → Profiler → LeakCanary** |

This separation is important: the Android Studio bridge takes ownership of heap dumping, so it
intentionally disables LeakCanary's normal in-app heap-dump flow. Use `nativeDebug` when you want
the notification and leak trace UI; use `studioDebug` when you want to present the Profiler.

## Quick demo: static Activity leak

1. Select `nativeDebug` and run the app on an emulator or device.
2. Open **Static Reference Leak** and leave **Leak Mode** enabled.
3. Tap **Finish Activity & Trigger Leak** (or navigate back).
4. Wait about five seconds, then open the LeakCanary notification or the **Leaks** icon.
5. The leak trace identifies the root cause:

   ```text
   static StaticLeakActivity.leakedActivity
   → destroyed StaticLeakActivity
   ```

6. Repeat with **Leak Mode** disabled. The Activity reference is cleared and there should be no
   new application leak.

The demo configures the `nativeDebug` build with `retainedVisibleThreshold = 1`, so one destroyed
Activity is enough to trigger analysis while the dashboard remains visible. Standard LeakCanary
defaults are more conservative for regular apps.

## Android Studio Profiler demo

1. Switch to the `studioDebug` variant and run it.
2. Open **View → Tool Windows → Profiler** and choose the running `studioDebug` process.
3. Start the Profiler's **LeakCanary** task.
4. Trigger the static leak as above.
5. Review the retained-object count and analysis in the Profiler.

## Troubleshooting

- **No native notification:** On Android 13+, allow notifications for the app. LeakCanary still
  writes its complete result to Logcat if notifications are disabled.
- **No analysis after one screen:** Confirm that `nativeDebug`, not `studioDebug`, is selected.
- **Need evidence in Android Studio:** Filter Logcat with `tag:LeakCanary`. Look for `Watching`,
  `Found ... retained`, and `HEAP ANALYSIS RESULT`.
- **Presenter tip:** Use a fresh app install or clear old LeakCanary entries before recording so
  the first result is clearly marked as new.

## Build from the command line

```bash
./gradlew :app:assembleNativeDebug
./gradlew :app:assembleStudioDebug
```

## CI artifacts

Every push to `main`, pull request, and manual run triggers the GitHub Actions workflow. Open the
run's **Artifacts** section to download:

- `LeakCanaryScenarios-debug-APKs-<run>` — installable `nativeDebug` and `studioDebug` APKs.
- `LeakCanaryScenarios-native-release-<run>` — the native release APK and native release AAB.

The release APK is intentionally named `*-unsigned.apk` until a production signing keystore is
configured. Use the debug APK for local demos; sign release outputs with your protected CI secrets
before publishing to users or Play Console.

See [LEAKCANARY_DEMO.md](LEAKCANARY_DEMO.md) for the complete testing runbook and the expected
behavior of every viewer.

## References

- [LeakCanary documentation](https://square.github.io/leakcanary/)
- [How LeakCanary works](https://square.github.io/leakcanary/fundamentals-how-leakcanary-works/)
- [LeakCanary configuration recipes](https://square.github.io/leakcanary/recipes/)
