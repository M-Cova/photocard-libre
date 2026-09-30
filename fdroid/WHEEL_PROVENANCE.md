# Approved Python wheel provenance

The five files below are the complete wheel input set for the F-Droid-specific
Python installation. `fetch-wheels.sh` downloads these exact URLs and verifies
the listed SHA-256 values before a file enters `fdroid/wheels/`.

| Distribution and version | Exact filename | Origin | Upstream source | SHA-256 |
| --- | --- | --- | --- | --- |
| Pillow 11.0.0 | `pillow-11.0.0-0-cp312-cp312-android_24_arm64_v8a.whl` | Chaquopy Python package repository 13.1 | https://github.com/python-pillow/Pillow/tree/11.0.0 | `5695b83b7e46b68b099c82368196a5a891655f6f56908ccc6c57fb6ea2520416` |
| ReportLab 5.0.1 | `reportlab-5.0.1-py3-none-any.whl` | official PyPI file service | https://files.pythonhosted.org/packages/4a/51/dbe28534ae12c852f61be91f039f343305fd1f34f1c66b8de75afae7a525/reportlab-5.0.1.tar.gz | `1c36e6bb0e71780c72331eba60da7f602e8d4389a8723825af71342e49d791e8` |
| charset-normalizer 3.5.1 | `charset_normalizer-3.5.1-py3-none-any.whl` | official PyPI file service | https://github.com/jawah/charset_normalizer/tree/3.5.1 | `6df0ec430f9a831772c23ca5a224cba36517a58a84bb32c32bb59a9fa67c47f6` |
| chaquopy-freetype 2.9.1, wheel build 2 | `chaquopy_freetype-2.9.1-2-py3-none-android_21_arm64_v8a.whl` | Chaquopy Python package repository 13.1 | https://download.savannah.gnu.org/releases/freetype/freetype-2.9.1.tar.gz | `7a8262dc69d1bbf7209b3b7576744743670bd5a8cba48c7d8f08c151943ccbc7` |
| chaquopy-libjpeg 1.5.3, wheel build 1 | `chaquopy_libjpeg-1.5.3-1-py3-none-android_21_arm64_v8a.whl` | Chaquopy Python package repository 13.1 | https://downloads.sourceforge.net/project/libjpeg-turbo/1.5.3/libjpeg-turbo-1.5.3.tar.gz | `5555be57a2633fb5a3c0c810e89c8b705c471e3dabd95aa596fc7fdd48322728` |

The wheel build number is part of the filename, not the Python distribution
version. The installed versions reported by the wheel metadata are therefore
`chaquopy-freetype==2.9.1` and `chaquopy-libjpeg==1.5.3`.

The resulting APK keeps the Python package license files inside
`assets/chaquopy/requirements-common.imy`, including Pillow, ReportLab,
charset-normalizer, FreeType, and libjpeg-turbo notices.
