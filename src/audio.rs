use flacenc::{component::BitRepr, error::Verify};
use lofty::{
    config::WriteOptions,
    tag::{Accessor, Tag, TagExt, TagType},
};
use serde::{Deserialize, Serialize};
use std::{fs, path::Path, sync::Arc};

#[derive(Clone, Serialize, Deserialize, Default)]
pub struct Track {
    pub title: String,
    pub side: String,
    pub length_ms: Option<u64>,
    pub start: String,
}

#[derive(Clone, Serialize, Deserialize)]
pub struct Project {
    pub version: u32,
    pub source: String,
    pub artist: String,
    pub album: String,
    pub year: String,
    pub release_id: String,
    pub side: String,
    pub offset: String,
    pub end: String,
    pub tracks: Vec<Track>,
}

impl Default for Project {
    fn default() -> Self {
        Self {
            version: 1,
            source: String::new(),
            artist: String::new(),
            album: String::new(),
            year: String::new(),
            release_id: String::new(),
            side: "A".into(),
            offset: "0:00.000".into(),
            end: String::new(),
            tracks: vec![],
        }
    }
}

pub struct Audio {
    pub spec: hound::WavSpec,
    pub samples: Arc<Vec<i32>>,
    pub peaks: Vec<f32>,
    pub quiet: Vec<f32>,
    pub clipped: usize,
}

impl Audio {
    pub fn load(path: &Path) -> Result<Self, String> {
        let bytes = fs::metadata(path).map_err(|e| e.to_string())?.len();
        if bytes > 2_000_000_000 {
            return Err(
                "This preview supports WAV files up to 2 GB. Split the recording by side first."
                    .into(),
            );
        }
        let mut reader = hound::WavReader::open(path).map_err(|e| e.to_string())?;
        let spec = reader.spec();
        if spec.sample_format != hound::SampleFormat::Int
            || ![16, 24].contains(&spec.bits_per_sample)
            || ![1, 2].contains(&spec.channels)
        {
            return Err("Import a mono or stereo 16-bit/24-bit integer PCM WAV. Float WAV and other formats are not supported yet.".into());
        }
        if !(8000..=192000).contains(&spec.sample_rate) {
            return Err("Unsupported sample rate (8–192 kHz supported).".into());
        }
        let samples: Vec<i32> = reader
            .samples::<i32>()
            .collect::<Result<_, _>>()
            .map_err(|e| e.to_string())?;
        if samples.is_empty() || samples.len() % spec.channels as usize != 0 {
            return Err("WAV has no complete audio frames.".into());
        }
        let scale = (1u32 << (spec.bits_per_sample - 1)) as f32;
        let clipped = samples
            .iter()
            .filter(|s| (**s as f32).abs() >= scale - 1.0)
            .count();
        let peaks = samples
            .chunks((samples.len() / 1800).max(1))
            .map(|chunk| {
                chunk
                    .iter()
                    .map(|s| (*s as f32).abs() / scale)
                    .fold(0.0, f32::max)
            })
            .collect();
        let window = (spec.sample_rate as usize / 10).max(1) * spec.channels as usize;
        let quiet = samples
            .chunks(window)
            .map(|chunk| {
                (chunk
                    .iter()
                    .map(|s| (*s as f64 / scale as f64).powi(2))
                    .sum::<f64>()
                    / chunk.len() as f64)
                    .sqrt() as f32
            })
            .collect();
        Ok(Self {
            spec,
            samples: Arc::new(samples),
            peaks,
            quiet,
            clipped,
        })
    }

    pub fn duration(&self) -> f64 {
        self.samples.len() as f64 / self.spec.channels as f64 / self.spec.sample_rate as f64
    }

    pub fn frame(&self, seconds: f64) -> usize {
        ((seconds * self.spec.sample_rate as f64).round() as usize)
            .min(self.samples.len() / self.spec.channels as usize)
    }

    pub fn nearby_quiet(&self, seconds: f64) -> f64 {
        let center = (seconds * 10.0).round() as usize;
        let low = center.saturating_sub(80).max(2);
        let high = (center + 80).min(self.quiet.len().saturating_sub(2));
        // Quiet is only a suggestion: sustained low energy plus a distance penalty.
        (low..high)
            .min_by(|a, b| {
                let score = |i: usize| {
                    self.quiet[i - 1..=i + 1]
                        .iter()
                        .copied()
                        .fold(0.0, f32::max)
                        + i.abs_diff(center) as f32 * 0.00015
                };
                score(*a).total_cmp(&score(*b))
            })
            .map(|i| i as f64 / 10.0)
            .unwrap_or(seconds)
    }
}

pub fn parse_time(text: &str) -> Result<f64, String> {
    let values: Vec<&str> = text.trim().split(':').collect();
    if values.is_empty() || values.len() > 3 {
        return Err(format!("Invalid time: {text}"));
    }
    let mut result = 0.0;
    for (i, v) in values.iter().enumerate() {
        let n: f64 = v.parse().map_err(|_| format!("Invalid time: {text}"))?;
        if !n.is_finite()
            || n < 0.0
            || (i > 0 && n >= 60.0)
            || (i + 1 < values.len() && n.fract() != 0.0)
        {
            return Err(format!("Invalid time: {text}"));
        }
        result = result * 60.0 + n;
    }
    Ok(result)
}

pub fn time(seconds: f64) -> String {
    let millis = (seconds.max(0.0) * 1000.0).round() as u64;
    format!(
        "{}:{:02}.{:03}",
        millis / 60000,
        (millis / 1000) % 60,
        millis % 1000
    )
}

pub fn safe_name(text: &str) -> String {
    let s: String = text
        .chars()
        .map(|c| {
            if c.is_control() || "<>:\"/\\|?*".contains(c) {
                '_'
            } else {
                c
            }
        })
        .take(100)
        .collect();
    let s = s.trim().trim_end_matches('.');
    if s.is_empty() {
        "Untitled".into()
    } else {
        s.into()
    }
}

pub fn regions(project: &Project, audio: &Audio) -> Result<Vec<(usize, usize, usize)>, String> {
    let tracks: Vec<_> = project
        .tracks
        .iter()
        .enumerate()
        .filter(|(_, t)| t.side == project.side)
        .collect();
    if tracks.is_empty() {
        return Err("Assign at least one track to this side.".into());
    }
    let end = parse_time(&project.end)?;
    if end > audio.duration() + 0.0005 {
        return Err("Side end exceeds the recording length.".into());
    }
    let mut result = vec![];
    for (pos, (index, track)) in tracks.iter().enumerate() {
        let start = parse_time(&track.start)?;
        let stop = if pos + 1 < tracks.len() {
            parse_time(&tracks[pos + 1].1.start)?
        } else {
            end
        };
        if start >= stop || audio.frame(start) >= audio.frame(stop) {
            return Err(format!(
                "Track {} has an empty or reversed range. Correct the start times and side end.",
                index + 1
            ));
        }
        result.push((*index, audio.frame(start), audio.frame(stop)));
    }
    Ok(result)
}

pub fn export(project: &Project, audio: &Audio, parent: &Path) -> Result<usize, String> {
    let spans = regions(project, audio)?;
    // create_dir is deliberately exclusive: an existing export is never overwritten.
    let folder = parent.join(format!(
        "{} - {} - Side {}",
        safe_name(&project.artist),
        safe_name(&project.album),
        safe_name(&project.side)
    ));
    fs::create_dir(&folder).map_err(|e|format!("Cannot create a NEW export folder: {e}. Choose another location or rename the previous export."))?;
    let outcome = (|| {
        let mut settings = flacenc::config::Encoder::default();
        // flacenc 0.5.1's fixed predictor can overflow its cost estimate on
        // full-scale 24-bit transients; LPC still falls back to verbatim blocks.
        if audio.spec.bits_per_sample == 24 {
            settings.subframe_coding.use_fixed = false;
        }
        let config = settings
            .into_verified()
            .map_err(|e| format!("FLAC config: {e:?}"))?;
        for (index, start, end) in &spans {
            let track = &project.tracks[*index];
            let channels = audio.spec.channels as usize;
            let source = flacenc::source::MemSource::from_samples(
                &audio.samples[start * channels..end * channels],
                channels,
                audio.spec.bits_per_sample as usize,
                audio.spec.sample_rate as usize,
            );
            let stream = flacenc::encode_with_fixed_block_size(&config, source, config.block_size)
                .map_err(|e| e.to_string())?;
            let mut sink = flacenc::bitsink::ByteSink::new();
            stream.write(&mut sink).map_err(|e| e.to_string())?;
            let file = folder.join(format!(
                "{:02} - {}.flac",
                index + 1,
                safe_name(&track.title)
            ));
            fs::write(&file, sink.as_slice()).map_err(|e| e.to_string())?;
            let mut tag = Tag::new(TagType::VorbisComments);
            tag.set_title(track.title.clone());
            tag.set_artist(project.artist.clone());
            tag.set_album(project.album.clone());
            tag.set_track((*index + 1) as u32);
            tag.set_track_total(project.tracks.len() as u32);
            if let Ok(year) = project.year.get(..4).unwrap_or("").parse::<u32>() {
                tag.insert_text(lofty::tag::ItemKey::RecordingDate, year.to_string());
            }
            tag.save_to_path(&file, WriteOptions::default())
                .map_err(|e| e.to_string())?;
            let mut check =
                claxon::FlacReader::open(&file).map_err(|e| format!("FLAC verification: {e}"))?;
            let mut decoded = check.samples();
            for original in &audio.samples[start * channels..end * channels] {
                let sample = decoded
                    .next()
                    .ok_or("FLAC verification: output ended early")?
                    .map_err(|e| format!("FLAC verification: {e}"))?;
                if sample != *original {
                    return Err("FLAC verification: decoded samples differ from source".into());
                }
            }
            if decoded.next().is_some() {
                return Err("FLAC verification: output has extra samples".into());
            }
        }
        fs::write(
            folder.join("puredub-project.json"),
            serde_json::to_vec_pretty(project).map_err(|e| e.to_string())?,
        )
        .map_err(|e| e.to_string())?;
        Ok(spans.len())
    })();
    outcome.map_err(|e: String| {
        format!(
            "Export incomplete: {e}. Partial files are retained in {}. Source audio is unchanged.",
            folder.display()
        )
    })
}

#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    fn tagged_flac_preserves_pcm_and_refuses_overwrite() {
        use lofty::{file::TaggedFileExt, probe::Probe};
        let root = std::env::temp_dir().join(format!(
            "puredub-test-{}-{}",
            std::process::id(),
            std::time::SystemTime::now()
                .duration_since(std::time::UNIX_EPOCH)
                .unwrap()
                .as_nanos()
        ));
        fs::create_dir(&root).unwrap();
        for bits in [16, 24] {
            let spec = hound::WavSpec {
                channels: 2,
                sample_rate: 8000,
                bits_per_sample: bits,
                sample_format: hound::SampleFormat::Int,
            };
            let limit = 1i32 << (bits - 1);
            let samples: Vec<i32> = (0..32000)
                .map(|i| match i % 17 {
                    0 => -limit,
                    1 => limit - 1,
                    _ => (i * 179) % limit,
                })
                .collect();
            let source = root.join(format!("{bits}.wav"));
            let mut writer = hound::WavWriter::create(&source, spec).unwrap();
            for sample in &samples {
                writer.write_sample(*sample).unwrap();
            }
            writer.finalize().unwrap();
            let original = fs::read(&source).unwrap();
            let audio = Audio::load(&source).unwrap();
            let project = Project {
                artist: "Test Artist".into(),
                album: format!("Synthetic {bits}"),
                year: "1991".into(),
                source: source.to_string_lossy().into(),
                end: "0:02".into(),
                tracks: vec![
                    Track {
                        title: "First".into(),
                        side: "A".into(),
                        start: "0:00".into(),
                        ..Default::default()
                    },
                    Track {
                        title: "Second".into(),
                        side: "A".into(),
                        start: "0:01".into(),
                        ..Default::default()
                    },
                ],
                ..Default::default()
            };
            assert_eq!(export(&project, &audio, &root).unwrap(), 2);
            let folder = root.join(format!("Test Artist - Synthetic {bits} - Side A"));
            let mut decoded = vec![];
            for (index, title) in ["First", "Second"].iter().enumerate() {
                let path = folder.join(format!("{:02} - {title}.flac", index + 1));
                let mut reader = claxon::FlacReader::open(&path).unwrap();
                assert_eq!(reader.streaminfo().bits_per_sample, u32::from(bits));
                decoded.extend(reader.samples().map(Result::unwrap));
                let tagged = Probe::open(path).unwrap().read().unwrap();
                let tag = tagged.primary_tag().unwrap();
                assert_eq!(tag.title().as_deref(), Some(*title));
                assert_eq!(tag.artist().as_deref(), Some("Test Artist"));
                assert_eq!(tag.track(), Some(index as u32 + 1));
            }
            assert_eq!(decoded, samples);
            assert!(export(&project, &audio, &root).is_err());
            assert_eq!(fs::read(source).unwrap(), original);
        }
        fs::remove_dir_all(root).unwrap();
    }
    #[test]
    fn time_and_ranges() {
        assert_eq!(parse_time("1:02.500").unwrap(), 62.5);
        for bad in ["NaN", "-1", "1:60", "1.5:20", "inf", ""] {
            assert!(parse_time(bad).is_err(), "{bad}");
        }
        assert_eq!(time(62.5), "1:02.500");
        let audio = Audio {
            spec: hound::WavSpec {
                channels: 2,
                sample_rate: 10000,
                bits_per_sample: 16,
                sample_format: hound::SampleFormat::Int,
            },
            samples: Arc::new(vec![0; 40000]),
            peaks: vec![],
            quiet: vec![],
            clipped: 0,
        };
        let mut p = Project {
            end: "0:02".into(),
            tracks: vec![
                Track {
                    side: "A".into(),
                    start: "0:00".into(),
                    ..Default::default()
                },
                Track {
                    side: "A".into(),
                    start: "0:01".into(),
                    ..Default::default()
                },
            ],
            ..Default::default()
        };
        let spans = regions(&p, &audio).unwrap();
        assert_eq!(spans[0].2, spans[1].1);
        assert_eq!(spans[1].2, 20000);
        p.tracks[1].start = "0:00".into();
        assert!(regions(&p, &audio).is_err());
        p.end = "0:03".into();
        assert!(regions(&p, &audio).is_err());
    }
}
