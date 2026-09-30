# PhotoCard Libre F-Droid build support

The canonical fdroidserver metadata is
`metadata/org.photocardlibre.app.yml`. The recipe targets fdroidserver 2.4.2
and builds version code 3 from tag `v0.1-beta.4`.

## Local prerequisites

- OpenJDK 17
- Android SDK platform 35 and build-tools 35.0.0
- Python 3.12 available as `python3.12`
- Gradle 8.10.2 (the version declared by the upstream wrapper)
- `curl` and `sha256sum`

`fdroidserver` normally uses its configured `gradlew-fdroid` executable and
removes the upstream wrapper scripts and JAR during source scanning. For the
local test, `config.yml` may point `gradle` at a trusted Gradle 8.10.2
installation. Keep that machine-specific file outside Git.

The official F-Droid category catalog must also be available to `fdroid lint`.
`Graphics` is an existing category and covers image-processing and visual
design tools; it is more specific than `Multimedia` for this app.

## Test procedure

From the repository root:

```sh
fdroid lint --format org.photocardlibre.app
fdroid build --test --no-tarball org.photocardlibre.app:3
```

The recipe fetches exactly five approved wheels into an ignored local cache,
checks every SHA-256, applies `--no-index`, `--find-links`, and
`--require-hashes`, and builds `PhotoCard-Libre-release-unsigned.apk`.

For a manual cache check:

```sh
./fdroid/fetch-wheels.sh
(cd fdroid/wheels && sha256sum --check ../wheel-hashes.sha256)
```

`fdroid/wheels/` is ignored by `fdroid/.gitignore`; no wheel belongs in Git.

## Upstream review points

The local fdroidserver build is testable, but an official F-Droid build worker
normally disables network access for `prebuild`. A reviewer must choose an
accepted acquisition mechanism for the five hash-pinned wheels (for example,
pre-populated build inputs) or require source builds of their native parts.

Chaquopy also resolves prebuilt FLOSS Maven runtime artifacts, including
CPython and native support libraries. Their source, licenses, and resolved
versions are documented in `THIRD_PARTY_NOTICES.md`, but acceptance of these
prebuilt artifacts is an F-Droid policy decision rather than a metadata or
lint issue.
