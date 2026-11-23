# Experimental TV speaker switch — Philips Headphone Volume Sync 1.2.0

This is an experimental extension of the last functioning 1.1.1 release.

## What's new

- Read Philips `AUDMGR_PROPERTY_TVSPEAKER_ONOFF` (property ID `32`).
- Two explicit, confirmation-protected buttons **TURN TV SPEAKERS ON** and **TURN TV SPEAKERS OFF**.
- Readback verification and log entries; **REFRESH TV SPEAKER STATUS**.
- No changes to `HeadphoneKeyService` or background headphone synchronization.
- No support for changing Bluetooth or HDMI routing.

## Test

1. Back up your working project/commit (or create a Git branch).
2. Build and install using the same debug signing key as your previous build (Android Studio: Build > Build APK(s)).
3. Manually set Philips Audio Output to **TV Speakers Off**.
4. Launch **Philips Headphone Volume Sync**, tap **REFRESH TV SPEAKER STATUS** (expected OFF).
5. Tap **TURN TV SPEAKERS ON** and confirm. Listen, verify via Philips Audio Output menu.
6. Tap **TURN TV SPEAKERS OFF** and confirm; verify again in TV menu.
7. Test VOL+, VOL-, MUTE afterwards to ensure unchanged behavior.

Save diagnostic log:

```powershell
adb logcat -d -s HeadphoneDiagUI:I PhilipsAudioController:I '*:S' > tv_speakers_test.txt
```

The Philips vendor `setProperty(32, ...)` call has **not** been tested on the TV yet. If the speakers unexpectedly change output or you lose sound, restore Audio Output manually in Philips TV Settings. This app does not automatically toggle speakers during startup, volume changes, or background processing.
