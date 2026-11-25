# PureDub

## Platform
Windows and macOS desktop.

## Stack
Rust throughout, egui/eframe GUI. MIT-licensed source and original artwork.

## Users and purpose
Collectors digitizing commercially released cassette albums. Select the exact cassette edition, record or import each side, review suggested boundaries, export named and tagged lossless tracks.

## Principles
- Preserve untouched source recordings. No automatic destructive processing.
- Metadata assists a human; it does not prove a boundary or an edition.
- Analog capture is real-time and cannot claim EAC/AccurateRip bit accuracy.
- Missing metadata must remain editable. No inferred side assignment presented as fact.

## First milestone
Import integer PCM WAV, find cassette editions on MusicBrainz, select a medium, assign sides, propose boundaries from durations with quiet-region adjustment, audition, save a project, export tagged FLAC into a new folder.

## Open work
Live stereo capture pending hardware verification; artwork, fingerprinting, float/FLAC import, recording device selection and packaged signed releases are subsequent work. macOS must be tested on a Mac.

## Evidence
WE-001 headphone output is analog. Karai's stereo recording input has not been verified. No actual cassette recordings were supplied for this milestone; synthetic audio must be explicitly labeled.

