# Beta 4 — checklist di test

## Test automatici

Con JDK 17 e Android SDK 35 configurati:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebugAndroidTest
./gradlew lintDebug
./gradlew lintRelease
./gradlew assembleRelease
git diff --check
```

Se l’emulatore `PhotoCardTest35` o un device compatibile è disponibile:

```bash
./gradlew connectedDebugAndroidTest
```

## Controlli manuali su device

- avvio con ciascuna delle undici lingue supportate e fallback inglese per una lingua di sistema non supportata;
- persistenza della lingua dopo la ricreazione dell’Activity e il riavvio dell’app;
- Home vuota con tagline, pulsante di aggiunta e nota JPEG/PNG;
- pagina Info completa, scrollabile e con versione della build;
- arabo: direzione RTL, navigazione indietro, Settings, Lingua, Info, Album, dettaglio foto, crop, anteprima e dialogo PDF;
- import di JPEG e PNG reali e messaggi dei limiti di sicurezza;
- crop, didascalia di massimo 4 parole, riordino ed eliminazione con conferma;
- anteprima A4, creazione, nome, salvataggio, apertura e condivisione del PDF.

La sola esecuzione di `assembleDebugAndroidTest` compila l’APK dei test ma non esegue test su device o emulatore.
