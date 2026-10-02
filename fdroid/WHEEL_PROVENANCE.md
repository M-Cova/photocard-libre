# Python wheel provenance

These are the exact artifacts used by PhotoCard Libre's deterministic F-Droid
mode. `fdroid/fetch-wheels.sh` downloads each HTTPS URL and verifies its SHA-256
before making it available to Gradle.

## Pillow 11.0.0

- File: `pillow-11.0.0-0-cp313-cp313-android_24_arm64_v8a.whl`
- URL: <https://chaquo.com/pypi-13.1/pillow/pillow-11.0.0-0-cp313-cp313-android_24_arm64_v8a.whl>
- SHA-256: `0d380685ae11d55ab6ac8152b1eb79999913c458f412fa50d7cc9157558bd174`
- Provenance: Chaquopy Android package repository; recipe source is the
  Chaquopy `server/pypi/packages/pillow` directory and upstream Pillow 11.0.0.
- License: HPND (Pillow License); the wheel metadata labels it `MIT-CMU`, and
  the bundled `LICENSE` contains the Pillow HPND text.
- Native code: yes, CPython 3.13 Android extension modules for arm64-v8a.

## ReportLab 5.0.1

- File: `reportlab-5.0.1-py3-none-any.whl`
- URL: <https://files.pythonhosted.org/packages/db/cb/dacbc268cb68d0428ea2cbd85266195a9ab3e677449589ddae59bd7542ac/reportlab-5.0.1-py3-none-any.whl>
- SHA-256: `1c36e6bb0e71780c72331eba60da7f602e8d4389a8723825af71342e49d791e8`
- Provenance: official ReportLab 5.0.1 distribution on PyPI.
- License: BSD license, as declared by the upstream package metadata.
- Native code: no; this selected artifact is `py3-none-any`.

## charset-normalizer 3.5.1

- File: `charset_normalizer-3.5.1-py3-none-any.whl`
- URL: <https://files.pythonhosted.org/packages/cc/61/d01fc49b8dea277640b55a9e15960dbca9fdc8c9fde18e572d39c59f4019/charset_normalizer-3.5.1-py3-none-any.whl>
- SHA-256: `6df0ec430f9a831772c23ca5a224cba36517a58a84bb32c32bb59a9fa67c47f6`
- Provenance: official charset-normalizer 3.5.1 distribution on PyPI.
- License: MIT.
- Native code: no; this selected artifact is `py3-none-any`.

## chaquopy-freetype 2.9.1, build 2

- File: `chaquopy_freetype-2.9.1-2-py3-none-android_21_arm64_v8a.whl`
- URL: <https://chaquo.com/pypi-13.1/chaquopy-freetype/chaquopy_freetype-2.9.1-2-py3-none-android_21_arm64_v8a.whl>
- SHA-256: `7a8262dc69d1bbf7209b3b7576744743670bd5a8cba48c7d8f08c151943ccbc7`
- Provenance: Chaquopy Android package repository; built from upstream
  FreeType 2.9.1 by the Chaquopy package recipe.
- License: FreeType License (FTL), included upstream as `docs/FTL.TXT`.
- Native code: yes, Android arm64-v8a shared libraries.

## chaquopy-libjpeg 1.5.3, build 1

- File: `chaquopy_libjpeg-1.5.3-1-py3-none-android_21_arm64_v8a.whl`
- URL: <https://chaquo.com/pypi-13.1/chaquopy-libjpeg/chaquopy_libjpeg-1.5.3-1-py3-none-android_21_arm64_v8a.whl>
- SHA-256: `5555be57a2633fb5a3c0c810e89c8b705c471e3dabd95aa596fc7fdd48322728`
- Provenance: Chaquopy Android package repository; built from the libjpeg-turbo
  1.5.3 source used by the Chaquopy package recipe.
- License: IJG/BSD-style and zlib licenses included by upstream libjpeg-turbo.
- Native code: yes, Android arm64-v8a shared libraries.

## 16 KiB page-size risk

The FreeType and libjpeg Android wheels are historical builds from before
October 2024. Chaquopy warns that Android wheels built before that date may fail
to load on devices using 16 KiB memory pages. Their hashes and provenance are
pinned here, but pinning does not remove that compatibility risk. They should be
replaced by newly rebuilt, source-corresponding artifacts when such builds become
available and are accepted by F-Droid.
