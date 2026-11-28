# Shared library architecture

PureDub uses three independent repositories. The existing desktop client and the
Android preview in `android/` share the published Rust libraries.

```mermaid
flowchart TB
    App["PureDub desktop / Android app"]
    Core["puredub-core: offline projects and audio"]
    Metadata["release-metadata: optional providers"]
    MB["MusicBrainz adapter: opt-in feature"]
    Other["External provider implementations"]
    Manual["Manual track information"]
    App --> Core
    App --> Metadata
    Metadata --> MB
    Metadata --> Other
    Manual --> App
```

## Ownership

- **Core** owns validated PCM, waveform analysis, quiet-region suggestions,
  project serialization, cut ranges, FLAC encoding/tagging and post-tagging
  sample comparison. It never calls a metadata provider.
- **Metadata** owns provider-qualified release IDs, release/media/track data and
  the blocking provider trait. The MusicBrainz feature adds request pacing,
  HTTP/JSON parsing and source attribution. The default build has no HTTP client.
- **Applications** own file selection/permissions, background execution, playback,
  manual entry and conversion between provider tracks and editable Core tracks.
  A source duration is assistance, never an authoritative audio boundary.

The desktop conversion is in `src/metadata.rs`; service HTTP code is in the
release-metadata repository. It selects cassette media and maps missing/unsupported
A/B labels to `?` for review. Core treats the old project `release_id` string as
opaque; a future project-format revision can preserve richer provider provenance
without forcing Core to depend on the metadata crate.

## Compatibility

Version-1 project JSON and the desktop command-line/file workflows are retained.
Both libraries are 0.1 APIs. Each is released separately, and this app pins the
exact tested Git revisions. Neither has been published to crates.io yet.

Windows synthetic tests and desktop compilation are checked locally. Both library
repositories have active GitHub Actions checks for Windows, Linux and macOS.
Android now has an app-specific JNI/JSON bridge, native Material 3 UI, Android
document access and AudioTrack previews. Swift/iOS bindings remain future work.
See [android/README.md](android/README.md) for workflows and limitations.

## Next evidence

With the Scarlett 2i2 available, create a short known stereo capture using existing
recording software. Verify that left and right remain independent, confirm the
actual WAV sample rate/bit depth, and test reviewed cuts and FLAC reassembly.
Report real hardware evidence separately from the current synthetic fixtures.
Live recording integration is still separate work.

The Android consumer uses the existing public Rust APIs without requiring library
changes. Its app-specific bridge keeps native audio resident while returning only
waveform/format metadata to Kotlin. Preview samples are bounded temporary float
PCM; exported samples remain unchanged integers. Android document access stages
WAVs privately, and verified FLACs are copied to a new destination folder with a
second byte-level hash check.

Long high-resolution sides still expand to `i32` in RAM. Android checks a safe
memory budget before decoding; chunked processing and background export remain
follow-up library/client work. There is no live recorder or iOS client yet.
