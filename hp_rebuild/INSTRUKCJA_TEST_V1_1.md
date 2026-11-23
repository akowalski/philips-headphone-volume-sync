# Philips Headphone Helper v1.1 — instrukcja testu

To zmodyfikowana wersja oryginalnego projektu `philips-headphone-volume-keys`.
Aplikacja ma ten sam `applicationId`: `local.philips.headphone`.

## Co robi
- Dostępność obserwuje VOL+/VOL−/MUTE i jednocześnie co sekundę sprawdza poziom MUSIC.
- Ustawia poziom słuchawek na wartość MUSIC, albo zero gdy MUSIC jest wyciszony.
- Nie zmienia głośności MUSIC i nie otwiera menu Philipsa.
- Ma teraz uruchamiany z ekranu aplikacji panel diagnostyczny z przyciskami **ODCZYTAJ STAN** i **SYNCHRONIZUJ TERAZ**.
- Loguje zarówno odczyt MUSIC, jak i inicjalizację/zapis do API Philipsa.

## Instalacja
Otwórz katalog projektu w Android Studio, zbuduj APK i wykonaj:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Jeśli pojawi się `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, nie odinstalowuj starej wersji, zanim nie zachowasz jej APK i ustawień. Konieczne jest zbudowanie nowej wersji tym samym kluczem podpisu.

## Test krok po kroku
1. Ustaw bezpieczną głośność TV, np. 10.
2. Uruchom z ekranu aplikacji **Philips Headphone Helper** i wybierz **ODCZYTAJ STAN**.
3. Sprawdź `MUSIC`, `MUTE` oraz `Philips headphone BEFORE`.
4. Jeśli są różne, naciśnij **SYNCHRONIZUJ TERAZ**, a następnie sprawdź w menu TV, czy zmienił się poziom słuchawek.
5. W Ułatwieniach dostępu włącz usługę **Philips headphone volume keys**. Jeśli stan na liście jest sprzeczny ze stanem na szczegółach, wykonaj komendy ADB poniżej.
6. Zmień poziom pilotem, przetestuj dojście do 0 oraz MUTE/UNMUTE.

## Zapis logów

```bash
adb logcat -c
# Teraz wykonaj test pilotem.
adb logcat -d -s HeadphoneKeyService:V PhilipsAudioController:V HeadphoneDiagUI:V '*:S' > headphone_debug_v1_1.txt
```

Aby sprawdzić rejestrację usługi:

```bash
adb shell settings get secure enabled_accessibility_services
adb shell settings get secure accessibility_enabled
adb shell dumpsys accessibility > accessibility_dump.txt
```

## Uwaga o SDK
Diagnostyka, na której potwierdzono działanie zapisu, używała targetSdk 28.
Na potrzeby rozróżnienia przyczyn awarii wersja testowa również używa targetSdk 28 (zamiast 34).
To ustawienie przejściowe do weryfikacji kompatybilności z wewnętrznym API Philipsa.
Po znalezieniu przyczyny należy rozważyć przywrócenie nowszego targetSdk.

Nie obiecujemy niezawodności w sytuacji, gdy Android ubije lub wyłączy AccessibilityService.
