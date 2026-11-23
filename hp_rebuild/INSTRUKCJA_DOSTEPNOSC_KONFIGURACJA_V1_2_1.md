# Philips Headphone Volume Sync 1.2.1 — konfiguracja w Dostępności

Wersja 1.2.1 zachowuje działające VOL+/VOL−/MUTE oraz przyciski TV Speakers na ekranie aplikacji.

## Nowa funkcja

W telewizorze otwórz Ustawienia > Ułatwienia dostępu > Philips Headphone Volume Sync > Konfiguracja.
Powinien otworzyć się ekran "Philips Headphone Volume Sync — Settings" z przełącznikiem "Built-in TV speakers".

- ON = włączone wbudowane głośniki TV (właściwość Philips 32 = 1).
- OFF = wyłączone wbudowane głośniki TV (właściwość Philips 32 = 0).
- Stan jest każdorazowo odczytywany z telewizora, nie z pamięci aplikacji.
- Zmiana wymaga potwierdzenia i jest weryfikowana ponownym odczytem.
- Nie dotyczy przełączania wyjścia na Bluetooth lub HDMI.

## Instalacja

W Android Studio otwórz katalog `hp_rebuild`, wybierz Build > Build APK(s), po czym:

```powershell
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Zachowano `applicationId = local.philips.headphone` i nazwę klasy AccessibilityService. Instalacja -r powinna być aktualizacją bez usuwania danych, jeśli podpis jest zgodny.

## Test

1. Sprawdź w aplikacji głównej, czy głośniki można włączać i wyłączać (działało w 1.2.0).
2. Otwórz pozycję Konfiguracja przy usłudze w Ułatwieniach dostępu.
3. Wyłącz i włącz głośniki z nowego przełącznika, potwierdzając każdą operację.
4. Po zmianie w menu Philipsa ręcznie wejdź ponownie na ekran konfiguracji — stan powinien się odświeżyć.
5. Potwierdź, że VOL+, VOL− i MUTE nadal działają.

## Log

```powershell
adb logcat -d -s SpeakerSettings:I HeadphoneDiagUI:I PhilipsAudioController:I HeadphoneKeyService:I '*:S' > tv_speakers_settings_test.txt
```

Uwaga: zależnie od wersji Philips Android TV pozycja "Konfiguracja" może mieć inny wygląd. Jeśli nie stanie się aktywna po aktualizacji, wejdź ponownie w ekran usługi albo uruchom ponownie telewizor (bez odinstalowania aplikacji).
