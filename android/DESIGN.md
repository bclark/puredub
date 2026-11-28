---
name: PureDub Android
description: Native Material 3 extension of the PureDub audio workbench
colors:
  dark-primary: "#F3B66C"
  dark-on-primary: "#35200A"
  dark-primary-container: "#52391E"
  dark-on-primary-container: "#FFDDB8"
  dark-secondary: "#61D9DD"
  dark-on-secondary: "#00373A"
  dark-secondary-container: "#164C51"
  dark-on-secondary-container: "#AEF3F4"
  dark-background: "#101820"
  dark-on-background: "#E6EEF2"
  dark-surface: "#101820"
  dark-on-surface: "#E6EEF2"
  dark-surface-variant: "#23303D"
  dark-on-surface-variant: "#BDCBD4"
  dark-surface-container: "#19232D"
  dark-surface-container-high: "#23303D"
  dark-outline: "#7E929F"
  dark-outline-variant: "#3A4B58"
  light-primary: "#795019"
  light-on-primary: "#FFFFFF"
  light-primary-container: "#FFDDB8"
  light-on-primary-container: "#2B1700"
  light-secondary: "#00666D"
  light-on-secondary: "#FFFFFF"
  light-secondary-container: "#A5EFF2"
  light-on-secondary-container: "#002023"
  light-background: "#F5F8FA"
  light-on-background: "#14212A"
  light-surface: "#F5F8FA"
  light-on-surface: "#14212A"
  light-surface-variant: "#DDE6EC"
  light-on-surface-variant: "#3A4B58"
  light-surface-container: "#E9EFF3"
  light-surface-container-high: "#E0E8EE"
  light-outline: "#627580"
  light-outline-variant: "#B9C8D1"
typography:
  body-large:
    fontFamily: "Android system default"
    fontSize: "16sp"
    fontWeight: 400
    lineHeight: "24sp"
    letterSpacing: "0.5sp"
spacing:
  detail: "4dp"
  inline: "8dp"
  group: "12dp"
  section: "16dp"
  page: "20dp"
---

# Design System: PureDub Android

## Overview

**Creative North Star: "PureDub workbench"**

This app extends the established desktop identity through native Material 3 controls. Ink navy, cool text, cyan audio data and amber actions become semantic color roles with a separate light scheme. The task is importing an existing WAV, reviewing cuts and exporting FLAC; recording remains deferred.

**Key Characteristics:**
- Source identity stays near audio review.
- Text labels explain actions and selected states.
- Real waveform data supplies the signature visual.

This document describes `ui/theme/Theme.kt`, `ui/theme/Type.kt`, `PureDubScreen.kt` and `MainActivity.kt`. The root `DESIGN.md` remains the desktop identity authority; `SURFACE.md` holds this surface's direction and quality bar.

## Colors

### Primary

Amber primary roles identify filled actions and waveform cut markers. Each scheme has its own on-primary and container pairing; use the active Material role rather than choosing a raw color in a component.

### Secondary

Cyan secondary draws waveform peaks and identifies synthetic audio. Secondary-container highlights the selected track. The tonal playback button uses Material's secondary-container treatment.

### Neutral

Background and surface are the workbench ground. Surface-container backs the waveform; on-surface draws the cursor, outline-variant the center line. Supporting text uses on-surface-variant. All explicit light and dark assignments appear above; unoverridden roles, including error and inverse snackbar roles, retain Material 3 defaults. System dark mode selects the static scheme; wallpaper-derived dynamic color is not enabled.

**The Semantic Role Rule.** Resolve component colors through `MaterialTheme.colorScheme` so both schemes retain their role pairings.

## Typography

Use Android system typography through Material 3. Only body-large is explicitly customized in `Type.kt`; all other roles inherit Material defaults. Text uses scalable sp units.

- Headline-large: the empty Project introduction.
- Headline-small: Tracks and Export section introductions.
- Title-large: app title, release/album sections and export completion.
- Title-medium: source name, track titles and smaller sections.
- Body-large: introductory copy, cursor time and export summary.
- Body-medium/small: source properties, guidance and verification detail.
- Label-large: explicit synthetic-audio label; controls otherwise use their native defaults.

Monospace is applied to the playback cursor, track numbers and the side/start supporting line. Do not turn the whole interface into an instrument display.

## Layout

The Scaffold supplies a top app bar with Save project and three destinations: Project, Tracks and Export. Below 600dp the destinations occupy a bottom navigation bar; at 600dp and above they become a navigation rail. The content column declares a 1000dp maximum width. Expanded behavior exists in code but has no tablet screenshot verification.

Scaffold padding and IME padding protect content from system bars and the keyboard; the activity enables edge-to-edge. Project and Export scroll vertically; Tracks uses a LazyColumn. Page padding is 20dp. Project uses 16dp section gaps, Tracks 12dp, Export 20dp. Track interiors use 12dp padding and 8dp detail gaps; wrapping action rows use 8dp or 12dp gaps. The waveform is 128dp high. Retain wrapping and scrolling for system font scaling.

## Elevation & Depth

Tonal surfaces and selection containers establish hierarchy. There is no custom shadow vocabulary or custom elevation override. Dialogs, app bars, navigation and controls retain Material elevation and interaction defaults; no custom motion contract has been established or tested.

## Shapes

Material default shapes govern native controls and dialogs. Track surfaces explicitly use `MaterialTheme.shapes.medium`; the theme does not override the shape scale. The rectangular Canvas waveform remains an audio plot rather than a decorative card.

## Components

- **Project:** Import WAV leads the empty state, followed by Open project and an explicitly opted-in synthetic demo. Loaded content begins with source name and audio properties. Labeled outlined fields collect release data; filter chips choose manual/offline or MusicBrainz metadata. Replacing existing work requires a native confirmation dialog.
- **Source summary:** Show the filename, bit depth, sample rate, channels and duration. Mark synthetic audio in text. A full-scale sample count includes its listening caveat.
- **Waveform and transport:** Canvas peaks are secondary, cuts primary and the cursor on-surface. Tapping seeks; the labeled Recording position slider provides an accessible alternative. The tonal Play 10s/Stop control combines a vector icon with text. The quiet-search switch has an explicit accessible label.
- **Tracks:** Side chips explain that side selection does not change the WAV. Selected track surfaces expose selected semantics. Set start at cursor and Listen across cut appear for the eligible selected track. Leading silence and side-end fields signal invalid time input.
- **Editor:** A scrollable Material AlertDialog contains labeled title, side, optional duration and start fields. Invalid duration/start disables Save; duration has supporting guidance. Remove track uses the error role. Save and Cancel remain native text actions.
- **Export:** Album, side, count and source precede validation and the full-width Export FLAC action. Detailed preservation/copy verification follows that action. A system folder picker selects the destination. Completion includes an Open export folder action and a polite live announcement.
- **Feedback:** Busy work disables relevant controls, adds progress and polite status text. Material snackbars carry transient messages; errors use error-container roles and remain dismissible. Native components retain their touch-target, focus and interaction behavior; no bespoke target-size override is defined.

## Do's and Don'ts

### Do:
- **Do** retain labeled native controls and the accessible slider alternative to waveform tapping.
- **Do** keep source identity, explicit synthetic labels and export validation visible in the task hierarchy.
- **Do** preserve the original icon: `app/src/main/res/drawable-nodpi/puredub.png` is an unchanged copy of root `assets/puredub.png`; root `assets/ICON.md` records its MIT provenance.

### Don't:
- **Don't** treat suggested cuts or metadata as proven boundaries.
- **Don't** replace the data-driven Canvas with decorative waveform imagery. No generated raster asset or Android image composition exists.
- **Don't** claim tablet, emulator or whole-platform certification from the phone review.

Phone review evidence lives in root `.impeccable/review/android-{empty,tracks,track-list,editor,export,light-large-text}.png`, physically captured on Samsung SM-S938U, Android 17, at 1440×3120. Both reported findings (switch label and stale PRODUCT platform) were resolved; the reviewer passed those fixes' scope. The track-list image is the latest light capture after automatic night mode was restored; original font scale 1.0 was restored too. Tablet/emulator UI remains unverified because the emulator did not boot. Both native ABIs compile, which is build evidence rather than UI evidence. No web detector ran for this native surface.
