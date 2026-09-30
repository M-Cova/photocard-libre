# Beta 6 — checklist di test

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

I cinque pacchetti Python della release sono dipendenze dirette e hanno versioni
fissate nel blocco Chaquopy di `app/build.gradle.kts`. Non modificare manualmente
queste versioni o i timestamp durante la preparazione di una release.

La release riproducibile deve essere costruita dalla radice del repository con:

```bash
./scripts/build-reproducible-release.sh
```

Lo script imposta `SOURCE_DATE_EPOCH` al timestamp Unix del commit `HEAD`, ottenuto
con `git log -1 --format=%ct`, quindi esegue una build release pulita usando la
configurazione di signing già prevista dal progetto. Nessuna password o chiave è
contenuta nello script. Il checkout deve essere pulito: lo script interrompe la
build se rileva modifiche o file non tracciati.

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

Additional translations are included and have been tested for basic UI functionality, but some languages have not been reviewed by native speakers.
