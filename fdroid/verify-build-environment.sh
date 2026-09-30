#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

java_version="$(java -version 2>&1 | sed -n '1p')"
case "${java_version}" in
    *'17.'*) ;;
    *) printf 'Java 17 required, found: %s\n' "${java_version}" >&2; exit 1 ;;
esac

: "${ANDROID_HOME:?ANDROID_HOME must point to the Android SDK}"
test -d "${ANDROID_HOME}/platforms/android-35"
test -x "${repo_root}/gradlew"
test ! -e "${repo_root}/local.properties"
test ! -e "${repo_root}/keystore.properties"

if rg -n -g '!verify-build-environment.sh' \
        '/home/[[:alnum:]_.-]+|PHOTOCARD_RELEASE_(KEYSTORE|STORE_PASSWORD|KEY_ALIAS|KEY_PASSWORD)' \
        "${repo_root}/metadata" "${repo_root}/fdroid"; then
    printf 'Personal path or release-signing environment reference found\n' >&2
    exit 1
fi

printf 'Java: %s\n' "${java_version}"
printf 'Android SDK: %s\n' "${ANDROID_HOME}"
printf 'Android platform 35: present\n'
printf 'Personal local.properties/keystore.properties: absent\n'
