# Philips headphone volume keys — wersja z bezpośrednim API Philipsa

## Co się zmieniło

Aplikacja zachowuje **ten sam identyfikator pakietu** (`local.philips.headphone`)
i tę samą usługę Dostępności `HeadphoneKeyService`. Nie otwiera już menu,
nie naciska DPAD i nie steruje główną głośnością TV.

Po każdej wykrytej zmianie (i kontrolnie raz na sekundę) odczytuje:
- `AudioManager.STREAM_MUSIC` — poziom główny 0–60,
- `AudioManager.isStreamMute(STREAM_MUSIC)` — stan MUTE.

Następnie używa bezpośrednio `TvAudioManagerImpl` z pakietu `org.droidtv.settings`:
- `getProperty(1)` — odczyt poziomu słuchawek,
- `setProperty(1, target)` — ustawienie poziomu słuchawek.

Cel to **słuchawki = główna głośność TV**, a podczas MUTE **słuchawki = 0**.
Limit zapisu wynosi 60 (zgodnie z zakresem obserwowanym w menu), pomimo że
API Philipsa zwróciło maksimum 100. Po UNMUTE synchronizuje do aktualnej
wartości głównej. Jeśli zmienisz głośność słuchawek ręcznie, kolejna kontrola
przywróci synchronizację.

## Instalacja

1. Zachowaj kopię starego projektu.
2. Otwórz **ten rozpakowany folder** w Android Studio (**File → Open**).
3. **Build → Build APK(s)**.
4. Podłącz TV przez ADB, sprawdź `adb devices`.
5. Z terminala w katalogu projektu:

   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

   Jeśli pojawi się `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, stara wersja jest
   podpisana innym kluczem. **Nie odinstalowuj od razu**: najpierw zapisz
   ustawienia usługi i wykonaj kopię; odinstalowanie usuwa dane aplikacji
   i może wymagać ponownego włączenia Dostępności.

6. Sprawdź w telewizorze, czy usługa **Philips headphone volume keys** jest
   aktywna w ustawieniach Dostępności. W razie potrzeby wyłącz i włącz ją.
7. Ustaw głośność główną na bezpieczny poziom (np. 10–15), sprawdź czy
   słuchawki wyrównały się; sprawdź VOL+, VOL−, poziom 0 oraz MUTE/UNMUTE.

## Logi

```bash
adb logcat -c
# Użyj klawiszy VOL+/VOL-/MUTE i poczekaj kilka sekund
adb logcat -d -s HeadphoneKeyService:I PhilipsAudioController:I '*:S' > headphone_sync_log.txt
```

Prześlij `headphone_sync_log.txt` wraz z informacją, czy wyjście słuchawkowe
zachowywało się zgodnie z oczekiwaniem.

## Uwagi

- Zapis `setProperty(1,...)` został potwierdzony w aplikacji diagnostycznej
  na Philips TPM191E; **ta kompletna wersja nie została jeszcze przetestowana na TV**.
- Aplikacja reaguje z niewielkim opóźnieniem; synchronizacja dwóch torów
  dźwięku nie jest atomowa.
- `VOLUME_CHANGED_ACTION` i `STREAM_MUTE_CHANGED_ACTION` są wewnętrznymi
  akcjami Androida. Kontrola raz na sekundę zabezpiecza przed ich brakiem.
- Aplikacja nie wywołuje `setProperty(2,...)` — MUTE realizuje przez ustawienie
  wyjścia słuchawkowego na 0, bez zmiany głównego poziomu TV.
- Projekt wykorzystuje wewnętrzną implementację Philipsa, więc może wymagać
  zmian po aktualizacji firmware'u.
