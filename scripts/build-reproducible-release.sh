#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repository_root="$(cd "${script_dir}/.." && pwd)"

if [[ "$(git -C "${repository_root}" rev-parse --show-toplevel 2>/dev/null)" != "${repository_root}" ]]; then
    printf 'error: script must belong to the PhotoCard Libre Git repository\n' >&2
    exit 1
fi

if [[ ! -x "${repository_root}/gradlew" || ! -f "${repository_root}/app/build.gradle.kts" ]]; then
    printf 'error: incomplete repository: gradlew or app/build.gradle.kts is missing\n' >&2
    exit 1
fi

if [[ -n "$(git -C "${repository_root}" status --porcelain --untracked-files=normal)" ]]; then
    printf 'error: reproducible releases require a clean working tree\n' >&2
    exit 1
fi

source_date_epoch="$(git -C "${repository_root}" log -1 --format=%ct 2>/dev/null)"
if [[ ! "${source_date_epoch}" =~ ^[0-9]+$ ]]; then
    printf 'error: unable to derive SOURCE_DATE_EPOCH from commit HEAD\n' >&2
    exit 1
fi

export SOURCE_DATE_EPOCH="${source_date_epoch}"
printf 'Building reproducible release with SOURCE_DATE_EPOCH=%s from HEAD %s\n' \
    "${SOURCE_DATE_EPOCH}" \
    "$(git -C "${repository_root}" rev-parse --short=12 HEAD)"

cd "${repository_root}"
exec ./gradlew --no-daemon clean assembleRelease "$@"
