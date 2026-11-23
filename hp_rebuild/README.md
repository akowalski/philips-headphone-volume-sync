# Philips Headphone Volume Sync

Synchronize the independent **3.5 mm headphone output** with the main volume level on compatible **Philips Android TVs**. The TV remote's **Volume Up**, **Volume Down**, and **Mute** controls affect the headphone output as well, without opening the Philips Quick Settings volume slider.

> **Compatibility:** Developed and tested on a **Philips 58PUS7304 / TPM191E running Android 12 (API 31)**. Other models and firmware versions are **not verified**. This application uses a private Philips API and may not work on other TVs, even if they run Android TV.

## What it does

- Mirrors the current Android `STREAM_MUSIC` level to the Philips headphone volume property **1:1** (for example, main volume `20` → headphone volume `20`).
- Sets headphone volume to **0** when the main volume is **0** or the main stream is muted.
- Restores the headphone level to the *current main volume* on unmute.
- Reads the target volume directly rather than simulating a sequence of headphone-volume key presses, avoiding missed intermediate steps.
- Uses an Android **AccessibilityService**, volume-related broadcast events, and a **one-second synchronization check**.
- Provides an on-TV diagnostics screen with **Read status** and **Synchronize now** actions.
- Does not require root, modifications to system files, or an internet connection.

The app **observes** the main volume; it does **not** set the TV's main speaker volume. It only writes the vendor-specific headphone volume property.

## How this version differs from the original project

This project is **based on and inspired by** [Philips Headphone Volume Keys](https://github.com/dulek/philips-headphone-volume-keys), originally developed by **Michał Dulko**. It retains the AccessibilityService-based approach and other project structure, but significantly changes the headphone-control implementation.

| Original project | Philips Headphone Volume Sync |
| --- | --- |
| Opens Philips Quick Settings and manipulates the headphone slider | Calls Philips' internal `TvAudioManagerImpl` API directly |
| Changes headphone volume in individual steps | Sets it to match the absolute main volume level |
| Headphone volume can differ from the main volume | Continuously synchronizes the two levels |
| Shows the Quick Settings slider while adjusting | Does not need to open the slider |
| Original test platform: Philips 49PUS6412 / TPM171E, Android TV 8.0 | This version tested on Philips 58PUS7304 / TPM191E, Android 12 |

This is an **unofficial community project** and is **not affiliated with or endorsed by Philips or TP Vision**.

## How it works

1. `HeadphoneKeyService` observes the TV's main `AudioManager.STREAM_MUSIC` volume and mute state.
2. `PhilipsAudioController` accesses `org.droidtv.tv.audio.TvAudioManagerImpl` through the Philips Settings package class loader.
3. It reads headphone volume through `getProperty(1)` and updates it with `setProperty(1, target)` when needed.
4. The synchronization check repeats every second to help recover from missed volume-change events.

The application limits requested headphone levels to **0–60**, matching the range observed in the TV settings menu. The vendor API reports a maximum of **100**; values above 60 have **not** been validated and are deliberately not used.

Mute currently works by setting headphone volume to zero rather than invoking the separate, unverified Philips headphone-mute property.

### Limitations

- The Philips API is **private and undocumented** and may vary by model, firmware, or Android version.
- Synchronization is **not atomic**: a brief delay between changing main and headphone volume is possible.
- The AccessibilityService must be enabled in the TV's accessibility settings for background operation.
- The app does not guarantee support for Bluetooth, HDMI ARC/eARC, optical audio, or other audio routes. It targets the analog headphone output.
- `targetSdk` is currently **28** for compatibility with the tested Philips implementation; this is not a general Android compatibility guarantee.

## Build and install

**Requirements:** Android Studio, JDK 17, Android SDK platform 34, Android Debug Bridge (ADB), and a Philips Android TV with ADB debugging enabled.

1. Open the project directory in **Android Studio**.
2. Wait for Gradle synchronization to finish.
3. Select **Build → Build APK(s)**.
4. Connect to the TV using ADB and verify the connection:

   ```sh
   adb devices
   ```

5. Install or update the application:

   ```sh
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

6. On the TV, open **Settings → Accessibility**, find **Philips Headphone Volume Keys** and enable it.
7. Launch **Philips Headphone Helper** from the Android TV applications menu, then use **Read status** or **Synchronize now** to verify that both volume levels match.
8. Test **Volume Up**, **Volume Down**, **volume 0**, **Mute**, and **Unmute** with the physical remote at a safe initial listening level.

**Note about signing:** If Android rejects the update because the APK is signed with a different key, do not uninstall the existing working application until you have backed up what you need and decided how to handle the signing key.

The original project's Makefile and Docker scripts are retained, but their release artifact names and some documentation may still refer to `philips-headphone-volume-keys`. Check and update these scripts before publishing a release build. The Android Studio debug build is the documented build method for this version.

## Troubleshooting

To capture logs after testing, run:

```sh
adb logcat -d -s HeadphoneKeyService:V PhilipsAudioController:V HeadphoneDiagUI:V '*:S' > headphone_debug.txt
```

Useful diagnostics:

- **Read status** shows the current main volume, mute state, headphone volume, and whether the AccessibilityService is listed as enabled.
- **Synchronize now** explicitly writes the headphone level to the current main volume (or zero while muted).
- If manual synchronization works but remote-triggered synchronization does not, check whether the AccessibilityService is actually active.

## Credits and attribution

- **Original project:** [Philips Headphone Volume Keys](https://github.com/dulek/philips-headphone-volume-keys) — **Michał Dulko**, 2026.
- This version builds on that codebase and replaces the original Quick Settings/DPAD-based headphone adjustment with a Philips internal-API implementation.
- Third-party Docker build attribution is maintained in [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md).

If redistributing modified versions, preserve the relevant copyright notices and comply with the license terms.

## License

Licensed under the **Apache License, Version 2.0**; see [`LICENSE`](LICENSE). Existing source-file copyright and SPDX notices are retained from the original project. Changes in this version do not remove the original author's attribution.
