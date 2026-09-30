# Third-party component inventory

This is an inventory and review aid, not a replacement for the upstream
license texts. License files already present in wheels or source packages must
be preserved. Versions below are those resolved by the tested version-code 3
release build.

## Python and native runtime

| Component | Version | License information verified from package/source | Origin and upstream source | APK status and native code | Notice handling |
| --- | --- | --- | --- | --- | --- |
| Chaquopy Gradle plugin and runtime | 17.0.0 | MIT | Maven Central; https://github.com/chaquo/chaquopy | Plugin is build-time; Java/Python bridge and native runtime are included | Preserve the Chaquopy MIT license and copyright notice |
| CPython | 3.12.12, Chaquopy target build 0 | Python Software Foundation License Version 2 and the additional notices in CPython `LICENSE` | Chaquopy target artifact built from https://www.python.org/ftp/python/3.12.12/Python-3.12.12.tgz; build scripts at https://github.com/chaquo/chaquopy/tree/master/target | Interpreter, standard library, and native extension modules are included | Preserve CPython `LICENSE`, including its incorporated-software notices |
| Pillow | 11.0.0 | MIT-CMU, as declared in wheel metadata and `pillow-11.0.0.dist-info/LICENSE` | Approved Chaquopy wheel; https://github.com/python-pillow/Pillow/tree/11.0.0 | Included; contains native `_imaging` modules | License is embedded in `requirements-common.imy`; preserve it |
| ReportLab | 5.0.1 | 3-clause BSD text in `reportlab-5.0.1.dist-info/licenses/LICENSE`; bundled fonts carry their own texts | Approved PyPI wheel and the exact PyPI source archive listed in `WHEEL_PROVENANCE.md` | Included, pure Python in this build | Preserve ReportLab and bundled-font license files, including Bitstream Vera |
| charset-normalizer | 3.5.1 | MIT, from wheel metadata and its `licenses/LICENSE` | Approved PyPI wheel; https://github.com/jawah/charset_normalizer/tree/3.5.1 | Included, pure Python | License is embedded in `requirements-common.imy`; preserve it |
| FreeType | 2.9.1, Chaquopy wheel build 2 | FreeType License (FTL); the source also offers GNU GPL version 2 | Approved Chaquopy wheel; Savannah source archive listed in `WHEEL_PROVENANCE.md` | Included as `libfreetype.so`; native | Embedded `FTL.TXT` must be preserved |
| libjpeg-turbo | 1.5.3, Chaquopy wheel build 1 | IJG, modified 3-clause BSD, and zlib license terms, as enumerated in the embedded `LICENSE.md` | Approved Chaquopy wheel; SourceForge source archive listed in `WHEEL_PROVENANCE.md` | Included as `libjpeg_chaquopy.so`; native | Preserve `LICENSE.md`; binary documentation must include the required IJG acknowledgment and applicable BSD text |
| OpenSSL | 3.0.18, Chaquopy source-dependency build 0 | Apache-2.0 | BeeWare Android source-dependency archive used by Chaquopy; upstream https://github.com/openssl/openssl/tree/openssl-3.0.18 | `libcrypto_python.so` and `libssl_python.so` are included; native | Preserve the OpenSSL license and notices from the corresponding source package |
| SQLite | 3.50.4, Chaquopy source-dependency build 0 | Public domain dedication; SQLite also offers a blessing text | BeeWare Android source-dependency archive used by Chaquopy; upstream https://sqlite.org/src/ | `libsqlite3_python.so` is included; native | No attribution condition in the public-domain dedication; do not replace its wording with an invented notice |
| bzip2 | 1.0.8, Chaquopy source-dependency build 3 | bzip2 1.0.6-style permissive license in the source distribution | BeeWare Android source-dependency archive used by Chaquopy; upstream https://sourceware.org/bzip2/ | Included through CPython `_bz2`; native | Preserve the bzip2 license and disclaimer |
| libffi | 3.4.4, Chaquopy source-dependency build 3 | MIT-style libffi license in the source distribution | BeeWare Android source-dependency archive used by Chaquopy; upstream https://github.com/libffi/libffi/tree/v3.4.4 | Included through CPython `_ctypes`; native | Preserve the libffi copyright and permission notice |
| XZ Utils / liblzma | 5.4.6, Chaquopy source-dependency build 1 | The source distribution contains public-domain, 0BSD, GPL, and LGPL material; the exact files used by liblzma carry their own notices | BeeWare Android source-dependency archive used by Chaquopy; upstream https://github.com/tukaani-project/xz/tree/v5.4.6 | Included through CPython `_lzma`; native | Preserve the source package's `COPYING` and component notices; reviewer should confirm the subset shipped by the target artifact |
| Expat | 2.7.3 (vendored by CPython 3.12.12) | MIT, stated in CPython's vendored `Modules/expat/expat.h` | CPython 3.12.12 source; upstream https://github.com/libexpat/libexpat | Included through `pyexpat`; native | Preserve the Expat copyright and MIT permission notice |
| zlib | Android platform library used by the CPython extension | zlib license for upstream zlib | Android NDK/system; upstream https://zlib.net/ | `_zlib` is included, but the separate zlib shared library is supplied by Android | No additional bundled zlib binary was found in the APK |
| pyelftools | 0.26 | Public domain dedication in its embedded `LICENSE.txt` | Vendored by Chaquopy bootstrap; https://github.com/eliben/pyelftools/tree/v0.26 | Included as Python bytecode | Embedded license must be preserved |
| setuptools / pkg_resources | 68.2.2 | MIT | Vendored by Chaquopy bootstrap; https://github.com/pypa/setuptools/tree/v68.2.2 | Included as Python bytecode | Embedded setuptools license must be preserved; pkg_resources also contains vendored Python packages with their upstream notices |
| CA certificate bundle | certifi snapshot shipped by Chaquopy 17.0.0 | Mozilla Public License 2.0 applies to certifi's curated bundle distribution | Chaquopy runtime asset `cacert.pem`; upstream https://github.com/certifi/python-certifi | Included as `assets/chaquopy/cacert.pem` | Exact certifi release is not encoded in the APK; reviewer should confirm it against the Chaquopy 17.0.0 source release before publication |

## Android runtime dependencies

The tested Gradle release graph resolves these principal APK components:

| Component family | Resolved version | License | Source | APK status |
| --- | --- | --- | --- | --- |
| AndroidX Activity / Activity Compose | 1.10.0 | Apache-2.0 | https://android.googlesource.com/platform/frameworks/support/ | Included |
| AndroidX Core | 1.15.0 | Apache-2.0 | AndroidX source repository above | Included |
| AndroidX ExifInterface | 1.4.2 | Apache-2.0 | AndroidX source repository above | Included |
| AndroidX Lifecycle | 2.8.7 | Apache-2.0 | AndroidX source repository above | Included |
| AndroidX DataStore | 1.2.1 | Apache-2.0 | AndroidX source repository above | Included; contains native `libdatastore_shared_counter.so` |
| Jetpack Compose UI, Foundation, Runtime, and Animation | 1.7.7 | Apache-2.0 | AndroidX source repository above | Included; UI graphics contributes native `libandroidx.graphics.path.so` |
| Jetpack Compose Material 3 | 1.3.1 | Apache-2.0 | AndroidX source repository above | Included |
| Kotlin standard library | 2.1.0 | Apache-2.0 | https://github.com/JetBrains/kotlin/tree/v2.1.0 | Included |
| kotlinx.coroutines | 1.9.0 | Apache-2.0 | https://github.com/Kotlin/kotlinx.coroutines/tree/1.9.0 | Included |
| kotlinx.serialization | 1.7.3 | Apache-2.0 | https://github.com/Kotlin/kotlinx.serialization/tree/v1.7.3 | Included through DataStore |
| Okio | 3.9.1 | Apache-2.0 | https://github.com/square/okio/tree/3.9.1 | Included through DataStore |

Build tools and their dependency graph (Android Gradle Plugin 8.8.2, Kotlin
Gradle plugin 2.1.0, Gradle 8.10.2, and `org.json:json:20231013`) are
build-time only. The APK defines no `org.json` classes; application references
use Android's platform `org.json` implementation.

## App-bundled font

`DejaVuSans.ttf` is DejaVu Sans 2.37 and is included in the Python app assets.
Its Bitstream Vera terms and DejaVu public-domain changes are documented by
the already tracked `app/src/main/python/photo_album/assets/DEJAVU-LICENSE.txt`.
That file must remain with binary distributions.

## Reviewer checks still required

- Confirm the exact certifi snapshot and ship its applicable notice.
- Confirm that the CPython target artifact contains the license/notice files
  required for OpenSSL, bzip2, libffi, XZ/liblzma, SQLite, and Expat.
- Decide whether prebuilt Chaquopy Maven artifacts and the native wheels are
  acceptable for the target F-Droid repository or must be rebuilt from source.
- Generate or verify a complete Android dependency notice set from the final
  resolved release graph before publication.
