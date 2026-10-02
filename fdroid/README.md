# F-Droid build inputs

PhotoCard Libre uses Chaquopy 17.0.0 with Python 3.13. The build host must provide
the explicit `python3.13` command: Chaquopy requires the build interpreter's major
and minor version to match the Python runtime packaged in the app.

Fetch and verify the five pinned wheels before starting an offline F-Droid build:

```bash
fdroid/fetch-wheels.sh fdroid/wheels
```

Then enable the deterministic wheel mode when invoking Gradle:

```bash
./gradlew -PphotocardFdroidOfflineWheels=true assembleRelease
```

This mode uses only `fdroid/wheels`, disables package indexes with `--no-index`,
and requires every artifact to match the hashes in `fdroid/requirements.txt`.
Configuration fails with an explicit message if the wheel directory, hash
manifest, requirements file, or any expected wheel is missing.

The generated `fdroid/wheels/` directory is ignored by Git. The scripts,
requirements, hashes, and provenance documents in `fdroid/` are tracked.
