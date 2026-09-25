# PhotoCard Libre Android v0.1-beta.2

PhotoCard Libre è un'app Android per comporre fotografie su pagine A4 e generare PDF pronti per la stampa. Il frontend è Kotlin + Jetpack Compose; layout MaxRects, didascalie, anteprima e PDF rimangono nel core Python eseguito da Chaquopy.

> Stato: versione Android funzionante e già testata su telefono reale.

Il progetto desktop `/home/codex/foto-album` non viene usato a runtime e non deve essere modificato.

## Flusso utente

1. `AGGIUNGI FOTO` apre il Photo Picker Android multiplo.
2. `PhotoCacheAdapter` legge ogni `content://` con `ContentResolver` e copia JPEG/PNG in `cache/photo_inputs`. Gli URI non vengono trasformati in path.
3. La schermata mostra miniature, selezione, anteprima grande, didascalia, riordino ed eliminazione.
4. `ANTEPRIMA` chiede al core Python di creare una PNG per ogni pagina A4 in `cache/photo_output`.
5. `CREA PDF` genera `cache/photo_output/foto-album.pdf` e le anteprime dallo stesso `LayoutResult`.
6. Su Android 10+ il PDF viene salvato automaticamente tramite MediaStore in `Download/PhotoCard Libre/` con nome `PhotoCard_YYYY-MM-DD_HHMM.pdf`.
7. Su Android 7–9 si apre automaticamente `ACTION_CREATE_DOCUMENT` come fallback. Dopo il salvataggio restano disponibili `APRI PDF` e `CONDIVIDI PDF`.

## Architettura

- `ui/AlbumScreen.kt`: UI Compose in italiano.
- `AlbumViewModel.kt`, `model/AlbumState.kt`: stato, operazioni e lavoro asincrono.
- `storage/PhotoCacheAdapter.kt`: confine `content://` → copia cache → path locale.
- `python/PythonAlbumBridge.kt`: payload JSON e chiamata Chaquopy.
- `export/PdfExport.kt`: Storage Access Framework e condivisione.
- `app/src/main/python/photo_album/`: core Python condiviso, senza Tkinter/XDG.
- `app/src/main/python/android_bridge.py`: costruzione `PhotoItem`, unico layout e output.

## Cache e formati

All'apertura di una nuova sessione, `cache/photo_inputs` viene cancellata. Ogni eliminazione rimuove anche la copia corrispondente. PDF e anteprime hanno nomi stabili e vengono sovrascritti; le vecchie anteprime vengono eliminate prima di ogni rendering. Non si accumulano sessioni senza limite.

La V0.1 accetta intenzionalmente solo MIME `image/jpeg` e `image/png`. HEIC e WebP non sono dichiarati supportati perché i relativi codec non sono stati validati sul wheel Pillow Android ARM64.

La barra inferiore usa `navigationBarsPadding()`: i tre comandi principali ricevono dinamicamente l'inset della barra di navigazione, senza assumere un'altezza fissa.

## Dipendenze verificate dal build

- Chaquopy 17.0.0 e Python 3.12;
- Pillow 11.0.0, wheel Android CPython 3.12 `arm64-v8a`. È la versione più
  recente pubblicata nel repository wheel Chaquopy per questa combinazione;
  le release Pillow successive non hanno un wheel Android compatibile e non
  vengono forzate da sorgente;
- ReportLab 5.0.1;
- Compose BOM 2025.01.01;
- minSdk 24, target/compileSdk 35, solo ABI `arm64-v8a`.

Il font DejaVu Sans è incluso nel package e caricato relativamente a `typography.py`; la licenza è in `assets/DEJAVU-LICENSE.txt`.

## Build e test

Con JDK 17 e Android SDK 35 configurati:

```bash
./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest
PYTHONPATH=app/src/main/python python3 -m unittest discover -s tests -v
```

Il test Compose strumentale è compilato nell'APK di test ma richiede un device/emulatore:

```bash
./gradlew connectedDebugAndroidTest
```

## Prova obbligatoria su telefono ARM64

```bash
adb install -r app/build/outputs/apk/debug/PhotoCard-Libre-v0.1-beta.2-arm64.apk
adb shell am start -n org.photocardlibre.app/.MainActivity
```

Validare manualmente Photo Picker, import di JPEG/PNG reali, miniature, rotazione/configuration change, anteprima multipagina, PDF, salvataggio MediaStore in `Download/PhotoCard Libre`, fallback SAF su API 24–28, apertura/condivisione e stampa del PDF al 100%.

## Nota tecnica interna

PhotoCard Libre deriva tecnicamente dalla precedente V0.1 FOTO ALBUM. Le build di sviluppo precedenti usavano l'identificatore storico `it.fotoalbum.spike`; l'identificatore definitivo è `org.photocardlibre.app`.
