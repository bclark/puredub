# Android preview validation

Validated on 2026-11-28. All audio used was explicitly synthetic. No real cassette
or Scarlett capture is claimed.

## Build and automated checks

- Stable Rust 1.99.0; Android NDK r29 (29.0.14206865).
- ARM64 (`aarch64-linux-android`) and emulator (`x86_64-linux-android`) release
  libraries compiled successfully using the pinned public library revisions.
- Native bridge end-to-end test passed: demo generation, format inspection, cut
  suggestions, preview PCM, memory rejection and verified export/overwrite refusal.
- Native Clippy with warnings denied passed.
- Gradle debug app and instrumentation APKs built successfully.
- Kotlin time parsing/formatting unit test passed.
- Android lint: zero errors; remaining warnings concern dependency/template
  versions, unused generated launcher resources and optional KTX style.
- Native ARM64 ELF LOAD segments have `0x4000` (16 KB) alignment. Physical device
  validation used a 4 KB-page device, not a 16 KB-page device.
- APK verification with `zipalign -c -P 16 -v 4` passed.

## Device evidence

Samsung SM-S938U (S25 Ultra), Android 17, ARM64, screen 1440 × 3120.

- Instrumented test passed through JNI and the real Compose interface: load
  synthetic stereo, set a cut at 1.250 seconds, suggest starts, recreate the
  activity, save a project, start a new project, reopen it with its display name,
  and export two sample-verified FLACs.
- The quiet-region Switch is found by its accessible name and toggled off/on by
  the instrumented test. The final interface review's two material fixes (that
  label and product-platform documentation) were both scored resolved.
- Force-stop/relaunch restored the autosaved project and cached WAV.
- AudioTrack preview started/stopped successfully and advanced the playback cursor.
- Android file picker imported a 24-bit, 8 kHz stereo WAV. Its original SHA-256
  remained `2a8304f400fe5aa402943c3370576193418ac874b8f0376fa7dd2a71ff15734c`.
- Android folder picker created a task-owned `PureDub-preview-validation` folder.
  Export produced two tagged FLACs and project JSON under
  `Synthetic test - Stereo demo - Side A`; both PCM verification and destination
  copy hashing completed successfully.
- Create-document save and open-document project reopening succeeded.
- Dark UI and light UI with 1.3 font scale were visually checked. Phone settings
  were restored to the original font scale 1.0 and automatic night mode.

The physical test files in the phone's `PureDub-preview-validation` directory are
synthetic and may be removed independently of original recordings.

## Limits of this evidence

The installed API-37 emulator failed to boot within its timeout; no emulator UI,
tablet layout or older Android version is claimed tested. x86-64 compilation is
verified. Live MusicBrainz networking, long real recordings, cloud document
providers, 16 KB-page devices, background export/process-death recovery during an
export, and Scarlett stereo capture still need validation.

This is a development-signed preview. Different builders' debug signing keys can
differ; installing one over another may require uninstalling, which removes
private working copies. Keep source recordings and exported project documents.
