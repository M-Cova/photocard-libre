#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
destination="${1:-${script_dir}/wheels}"
mkdir -p "${destination}"

fetch() {
    local filename="$1"
    local url="$2"
    local expected_sha256="$3"
    local target="${destination}/${filename}"
    local temporary="${target}.part"

    if [[ -f "${target}" ]] && printf '%s  %s\n' "${expected_sha256}" "${target}" | sha256sum --check --status; then
        printf 'verified cached %s\n' "${filename}"
        return
    fi

    rm -f "${temporary}"
    curl --fail --location --proto '=https' --tlsv1.2 --output "${temporary}" "${url}"
    printf '%s  %s\n' "${expected_sha256}" "${temporary}" | sha256sum --check --status
    mv "${temporary}" "${target}"
    printf 'downloaded and verified %s\n' "${filename}"
}

fetch \
    'pillow-11.0.0-0-cp313-cp313-android_24_arm64_v8a.whl' \
    'https://chaquo.com/pypi-13.1/pillow/pillow-11.0.0-0-cp313-cp313-android_24_arm64_v8a.whl' \
    '0d380685ae11d55ab6ac8152b1eb79999913c458f412fa50d7cc9157558bd174'
fetch \
    'reportlab-5.0.1-py3-none-any.whl' \
    'https://files.pythonhosted.org/packages/db/cb/dacbc268cb68d0428ea2cbd85266195a9ab3e677449589ddae59bd7542ac/reportlab-5.0.1-py3-none-any.whl' \
    '1c36e6bb0e71780c72331eba60da7f602e8d4389a8723825af71342e49d791e8'
fetch \
    'charset_normalizer-3.5.1-py3-none-any.whl' \
    'https://files.pythonhosted.org/packages/cc/61/d01fc49b8dea277640b55a9e15960dbca9fdc8c9fde18e572d39c59f4019/charset_normalizer-3.5.1-py3-none-any.whl' \
    '6df0ec430f9a831772c23ca5a224cba36517a58a84bb32c32bb59a9fa67c47f6'
fetch \
    'chaquopy_freetype-2.9.1-2-py3-none-android_21_arm64_v8a.whl' \
    'https://chaquo.com/pypi-13.1/chaquopy-freetype/chaquopy_freetype-2.9.1-2-py3-none-android_21_arm64_v8a.whl' \
    '7a8262dc69d1bbf7209b3b7576744743670bd5a8cba48c7d8f08c151943ccbc7'
fetch \
    'chaquopy_libjpeg-1.5.3-1-py3-none-android_21_arm64_v8a.whl' \
    'https://chaquo.com/pypi-13.1/chaquopy-libjpeg/chaquopy_libjpeg-1.5.3-1-py3-none-android_21_arm64_v8a.whl' \
    '5555be57a2633fb5a3c0c810e89c8b705c471e3dabd95aa596fc7fdd48322728'

(
    cd "${destination}"
    sha256sum --check "${script_dir}/wheel-hashes.sha256"
)
