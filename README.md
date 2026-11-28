# PureDub

**Faithful tape capture. Tracks made simple.**

![PureDub icon](assets/puredub.png)

A Rust desktop workbench for turning commercially released cassette recordings into named, tagged lossless tracks. Open source under MIT. Original icon artwork is also MIT-licensed; dependency licenses remain their own.

## Current preview: 0.1.0

- Windows/macOS GUI built with egui/eframe, entirely in Rust.
- Search MusicBrainz cassette editions and choose the exact cassette medium.
- Import mono/stereo 16-bit or 24-bit integer PCM WAV (8–192 kHz, maximum 2 GB).
- Waveform navigation and 10-second playback; audition across a proposed cut.
- Editable titles, durations, side assignments and millisecond start positions.
- Suggest boundaries from published durations, optionally searching ±8 seconds for a sustained quiet region. Suggestions always need listening review.
- Save/reopen a side project, referencing the original recording.
- Export one side to a **new** folder of numbered FLAC files with artist, album, year and track tags. No overwrites, processing, resampling or fades. Failed exports retain partial files and explain where they are.

Live recording, album artwork retrieval, fingerprinting, float-WAV/FLAC import, zoomable waveform and signed installers are not implemented yet. Verify stereo capture hardware before live recording work. This is an early preview, not a production archival tool.

## Workflow

1. Record each cassette side as an untouched WAV using existing recording software. Use a real stereo line input/interface, not an unverified headset microphone input.
2. Import Side A. Search artist/album and confirm the cassette edition against its sleeve. Select the correct medium for multi-cassette releases.
3. Confirm A/B labels. Unknown labels are `?`, never silently split halfway. Track order follows the selected release; remove/re-add a row if the source data is out of order.
4. Set **Leading silence** to the first track's start, then suggest cuts. Missing durations can be entered as seconds; unknown values remain zero/unset. Alternatively set every start manually.
5. Select a row, listen across its cut, click the waveform and **Set start at cursor**, or type an exact `m:ss.sss` time. The last track runs to **Side end**; trim blank tail there if desired.
6. Save an A project. Export after reviewing all starts. Import the B recording, select Side B, check leading silence/cuts, and save a separate B project. A project represents one source recording and selected side; the full album track list is retained for numbering.

Changing A/B changes which track rows are exported, **not** which WAV is loaded. The imported source filename is always shown. Keep original WAVs at their saved paths. Saving a project requires a new filename; existing project files and export folders are protected against overwriting.

## Build

Install stable Rust and the platform C/C++ build tools (MSVC on Windows; Xcode command-line tools on macOS).

```sh
cargo run
cargo test --locked
cargo build --release --locked
```

Windows executable: `target/release/puredub.exe`. macOS executable: `target/release/puredub`. A prepared GitHub Actions template is in `ci/desktop-build.yml`: it builds/tests both platforms and packages a Windows ZIP and an unsigned macOS `.app` ZIP. It is **inactive** because the publishing GitHub login lacks workflow scope; move it to `.github/workflows/build.yml` with appropriately authorized credentials to enable it. macOS builds and real-hardware testing have not yet been verified. macOS signing/notarization is not provided. No Python, Node or external FFmpeg is needed to run the app. An optional WAV or PureDub project path can be supplied as the first command-line argument.

## Fidelity and privacy

An analog cassette cannot be extracted bit-perfectly like a CD. PureDub can preserve the **captured PCM samples**, without claiming they exactly reproduce the original tape. Every exported FLAC is decoded and compared sample-by-sample after tagging, before success is reported. Tests cover 16-bit and 24-bit full-scale signals, contiguous cuts, tagging and overwrite protection. The fixed predictor in flacenc 0.5.1 is disabled for 24-bit audio because its cost estimate failed on full-scale transients; LPC and verbatim fallback remain available. No noise reduction, Dolby decoding or speed correction is applied. Your player, alignment, tape condition and ADC determine capture quality.

Audio remains local. Only explicit release searches/lookups contact MusicBrainz over HTTPS with an identifying User-Agent and at most one request per 1.1 seconds per app instance. Hyperlinks open external websites. MusicBrainz edition metadata may be incomplete or inaccurate; review it. Metadata attribution: [MusicBrainz](https://musicbrainz.org/doc/About/Data_License).

Import currently loads one side into RAM (24-bit PCM expands to i32). Busy work runs off the UI thread; keep the app open until it completes. No background service, telemetry or account requirement.

## Development

The reusable libraries now live in independent MIT-licensed repositories:

| Repository | Responsibility |
|---|---|
| [puredub-core](https://github.com/bclark/puredub-core) | Offline WAV analysis, project data, cut suggestions, tagged FLAC export and sample verification |
| [release-metadata](https://github.com/bclark/release-metadata) | Common metadata types and an optional MusicBrainz adapter; other providers can implement its public trait |
| This repository | Existing desktop interface, file dialogs, playback and conversion from provider results into editable projects |

Core has no dependency on metadata services. Manual titles, durations and cut
positions work without making a release search. The desktop app currently enables
MusicBrainz; additional provider selection belongs to future client work. No
Android/iOS client or bindings are included yet. Dependencies are pinned to Git
revisions in Cargo.toml so a normal clone can build without sibling checkouts.

Run each library's documented tests in its own repository; `cargo test` here
doesn't run dependency test suites. The Core repository includes a synthetic,
hardware-free offline export demo. Real Scarlett capture and channel routing still
need hardware validation.

For coordinated local edits, clone the libraries beside this checkout and use
Cargo's local patch configuration (do not commit local absolute paths):

```toml
# .cargo/config.toml
[patch."https://github.com/bclark/puredub-core"]
puredub-core = { path = "../puredub-core" }
[patch."https://github.com/bclark/release-metadata"]
release-metadata = { path = "../release-metadata" }
```

See [ARCHITECTURE.md](ARCHITECTURE.md) for ownership and the next validation steps.

See [PRODUCT.md](PRODUCT.md) for product constraints, [DESIGN.md](DESIGN.md) for the workbench design and [assets/ICON.md](assets/ICON.md) for icon provenance. Contributions should include checks for sample preservation, range validation and source safety where relevant.
