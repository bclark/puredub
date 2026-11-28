# PureDub for Android

Native Kotlin / Jetpack Compose workbench backed by the public PureDub Rust
libraries. This is an early preview for importing recorded cassette-side WAVs,
reviewing cuts and exporting tagged FLAC tracks. Android 8.0 (API 26) or newer;
ARM64 phones and x86-64 emulators are packaged.

## Workflow

1. Import a WAV through Android's file picker. PureDub makes a private working
   copy and never writes to your original. Or explicitly load the **synthetic
   stereo demo**: eight seconds, 24-bit / 48 kHz, left 440 Hz and right 660 Hz.
2. Enter artist, album and year manually. Optionally select MusicBrainz and search
   cassette editions. Choose the exact physical cassette and review all metadata.
3. On Tracks, select Side A or B, add/edit tracks, set durations and cut positions.
   Side selection filters export; it does not load the other side's WAV. Unknown
   side labels stay `?` until reviewed.
4. Tap the waveform or use the accessible position slider to seek. Play a short
   preview, listen across the selected cut, and set its start at the cursor. You
   can enter exact `m:ss.sss` starts in the track editor.
5. Suggest cuts from durations, optionally finding nearby quiet. These are only
   suggestions: listen across every cut and check the side end.
6. Export to a parent folder selected through Android's document picker. A new
   album/side directory is created; existing exports are refused. FLACs are
   decoded and compared against source PCM after tagging, then copied and checked
   with SHA-256 at the destination.

The current project autosaves privately. Save a separate JSON document for an
explicit project backup. It references an app-private WAV, rather than embedding
audio. Reopening on another device or after uninstall requires reimporting the
original WAV; the relink workflow retains the saved cuts. Cached source recordings
are retained because other saved projects may reference them. Clearing app data
or uninstalling removes private working copies; keep your original recordings.

## Build on Windows

Install stable Rust, Android Studio/SDK, and the Android CLI. Configure Java 17 or
newer in JAVA_HOME and the SDK in ANDROID_HOME. The standard Gradle wrapper is
committed; no system Gradle installation is needed.

```powershell
android sdk install platforms/android-36 build-tools/36.0.0 "ndk;29.0.14206865"
rustup target add aarch64-linux-android x86_64-linux-android
cd android
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

If your Java runtime isn't on PATH, Android Studio's bundled runtime normally
lives at `<Android Studio>/jbr`; set JAVA_HOME to that folder. Gradle resolves
the Java 17 compilation toolchain. Android Studio can open this `android` folder
as a normal Gradle project. Supply the same environment variables to Studio.

`preBuild` invokes `scripts/build-native.ps1`, builds both Rust targets with NDK
r29, and copies the shared libraries into generated `jniLibs` directories. Native
libraries use 16 KB page alignment. Outputs are ignored by Git.

On Linux/macOS, use `bash gradlew` after the same SDK/target setup; Gradle invokes
`scripts/build-native.sh`. ANDROID_NDK_HOME can override the NDK path on Unix.

APK: `app/build/outputs/apk/debug/app-debug.apk` (development signing only).

```powershell
android run --device=<device-serial> app/build/outputs/apk/debug/app-debug.apk
```

## Checks

```powershell
cargo test --locked --manifest-path native/Cargo.toml
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug
# Select a test device when several are connected:
$env:ANDROID_SERIAL = "<device-serial>"
.\gradlew.bat :app:connectedDebugAndroidTest
```

The native test covers synthetic WAV loading, cut suggestions, previews, memory
budget rejection and sample-verified export. The instrumented test crosses JNI,
edits cuts, recreates the activity and exports FLACs. No test makes MusicBrainz
requests. Real tape/Scarlett capture is separate evidence.

## Architecture and limits

- `app/`: Material 3 UI, project state, Android file access, autosave, AudioTrack
  playback and destination copying. System theme, edge-to-edge/IME insets,
  compact navigation bar and expanded navigation rail are supported.
- `native/`: small app-specific JNI/JSON bridge, not a new public library API.
  Domain operations call pinned `puredub-core` and `release-metadata` revisions.
- MusicBrainz is optional at runtime. Choosing manual mode never contacts it.
  Other services can implement the metadata library's provider contract; this
  client currently offers manual entry and MusicBrainz only.
- Import supports mono/stereo 16/24-bit integer PCM WAV, 8–192 kHz, up to 2 GB.
  Samples currently expand to `i32` in native RAM. Before decoding, the app checks
  estimated sample memory against a conservative device budget (one third of
  currently available memory, at most 768 MiB). Large sides may need shorter WAVs;
  chunked processing in Core remains future work.
- All audio work runs off the UI thread. Keep the app open during exports; there
  is no foreground export service, resumable copy or cancellation API yet. Failed
  output is retained and its location is reported. Native verification occurs
  before destination copying; SHA-256 verifies the copied files afterward.
- Playback uses normalized float PCM for audition only. Export retains original
  integer samples, bit depth, channels and rate. A device may not support preview
  at every source rate even when Core can import/export that rate.
- Live recording/USB input selection, waveform zoom, artwork, fingerprinting,
  FLAC/float-WAV import and store distribution are not implemented.

The original PureDub icon is reused from `../assets/puredub.png` under its existing
MIT license. Android build/template dependencies retain their own licenses. The
Gradle wrapper and initial template came from Google's Android CLI.
