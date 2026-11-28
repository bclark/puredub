//! App-specific JSON/JNI bridge. Domain logic remains in the public Rust libraries.
#![cfg_attr(not(target_os = "android"), allow(dead_code))]
use puredub_core::{Audio, Project, export, regions, safe_name};
use release_metadata::{MetadataProvider, ReleaseId, SearchQuery, musicbrainz::MusicBrainz};
use serde_json::{Value, json};
use std::{fs, io::Write, path::Path, sync::OnceLock};

#[derive(Default)]
struct Session {
    audio: Option<Audio>,
}

fn text<'a>(v: &'a Value, key: &str) -> Result<&'a str, String> {
    v[key].as_str().ok_or_else(|| format!("Missing {key}."))
}

fn project(v: &Value) -> Result<Project, String> {
    Project::from_json(&serde_json::to_vec(&v["project"]).map_err(|e| e.to_string())?)
}

fn provider() -> Result<&'static MusicBrainz, String> {
    static CLIENT: OnceLock<Result<MusicBrainz, String>> = OnceLock::new();
    CLIENT
        .get_or_init(|| {
            MusicBrainz::new("PureDub-Android/0.1.0 (https://github.com/bclark/puredub)")
        })
        .as_ref()
        .map_err(Clone::clone)
}

fn load(session: &mut Session, path: &Path, budget: u64) -> Result<Value, String> {
    let reader = hound::WavReader::open(path).map_err(|e| e.to_string())?;
    let required = reader.len() as u64 * 4;
    if required > budget {
        return Err(format!(
            "This recording needs {} MB of sample memory; the current safe budget is {} MB. Import a shorter WAV. Long recordings need chunked processing in a future version.",
            required / 1_048_576,
            budget / 1_048_576
        ));
    }
    drop(reader);
    let audio = Audio::load(path)?;
    let info = json!({
        "duration": audio.duration(), "sample_rate": audio.spec().sample_rate,
        "bits": audio.spec().bits_per_sample, "channels": audio.spec().channels,
        "peaks": audio.peaks(), "full_scale_samples": audio.full_scale_samples()
    });
    session.audio = Some(audio);
    Ok(info)
}

fn dispatch(session: &mut Session, request: &Value) -> Result<Value, String> {
    match text(request, "op")? {
        "clear" => {
            session.audio = None;
            Ok(json!({}))
        }
        "load" => load(
            session,
            Path::new(text(request, "path")?),
            request["budget"].as_u64().unwrap_or(64 * 1_048_576),
        ),
        "demo" => {
            let path = Path::new(text(request, "path")?);
            let file = fs::OpenOptions::new()
                .write(true)
                .create_new(true)
                .open(path)
                .map_err(|e| e.to_string())?;
            let spec = hound::WavSpec {
                channels: 2,
                sample_rate: 48000,
                bits_per_sample: 24,
                sample_format: hound::SampleFormat::Int,
            };
            let mut writer = hound::WavWriter::new(file, spec).map_err(|e| e.to_string())?;
            for frame in 0..384000 {
                let second = frame as f64 / 48000.0;
                let envelope = if (3.8..4.2).contains(&second) {
                    0.0
                } else {
                    0.25
                };
                for frequency in [440.0, 660.0] {
                    let sample = ((second * frequency * std::f64::consts::TAU).sin()
                        * envelope
                        * 8_388_607.0) as i32;
                    writer.write_sample(sample).map_err(|e| e.to_string())?;
                }
            }
            writer.finalize().map_err(|e| e.to_string())?;
            load(session, path, 64 * 1_048_576)
        }
        "suggest" => {
            let audio = session.audio.as_ref().ok_or("Import a WAV first.")?;
            let mut p = project(request)?;
            p.suggest_starts(audio, request["quiet"].as_bool().unwrap_or(true))?;
            serde_json::to_value(p).map_err(|e| e.to_string())
        }
        "validate" => {
            let audio = session.audio.as_ref().ok_or("Import a WAV first.")?;
            let p = project(request)?;
            let spans = regions(&p, audio)?;
            Ok(json!({"tracks": spans.len()}))
        }
        "export" => {
            let audio = session.audio.as_ref().ok_or("Import a WAV first.")?;
            let p = project(request)?;
            let parent = Path::new(text(request, "parent")?);
            let count = export(&p, audio, parent)?;
            let folder = parent.join(format!(
                "{} - {} - Side {}",
                safe_name(&p.artist),
                safe_name(&p.album),
                safe_name(&p.side)
            ));
            let mut files: Vec<_> = fs::read_dir(&folder)
                .map_err(|e| e.to_string())?
                .map(|entry| entry.map(|e| e.path().to_string_lossy().into_owned()))
                .collect::<Result<_, _>>()
                .map_err(|e| e.to_string())?;
            files.sort();
            Ok(json!({"tracks": count, "folder": folder.to_string_lossy(), "files": files}))
        }
        "preview" => {
            let audio = session.audio.as_ref().ok_or("Import a WAV first.")?;
            let start = request["start"].as_f64().ok_or("Invalid preview start.")?;
            if !start.is_finite() || start < 0.0 || start >= audio.duration() {
                return Err("Choose a preview position inside the recording.".into());
            }
            let seconds = request["seconds"].as_f64().unwrap_or(10.0).clamp(0.1, 10.0);
            let channels = audio.spec().channels as usize;
            let first = audio.frame(start) * channels;
            let last = audio.frame((start + seconds).min(audio.duration())) * channels;
            let scale = (1u32 << (audio.spec().bits_per_sample - 1)) as f32;
            let mut output = std::io::BufWriter::new(
                fs::File::create(text(request, "path")?).map_err(|e| e.to_string())?,
            );
            for &sample in &audio.samples()[first..last] {
                output
                    .write_all(&(sample as f32 / scale).to_le_bytes())
                    .map_err(|e| e.to_string())?;
            }
            output.flush().map_err(|e| e.to_string())?;
            Ok(
                json!({"sample_rate": audio.spec().sample_rate, "channels": channels, "duration": (last - first) as f64 / channels as f64 / audio.spec().sample_rate as f64}),
            )
        }
        "search" => serde_json::to_value(provider()?.search(&SearchQuery {
            artist: text(request, "artist")?.into(),
            title: text(request, "album")?.into(),
            format: Some("Cassette".into()),
        })?)
        .map_err(|e| e.to_string()),
        "release" => serde_json::to_value(provider()?.load(&ReleaseId {
            provider: "musicbrainz".into(),
            value: text(request, "id")?.into(),
        })?)
        .map_err(|e| e.to_string()),
        "project" => serde_json::to_value(project(request)?).map_err(|e| e.to_string()),
        _ => Err("Unknown native operation.".into()),
    }
}

#[cfg(target_os = "android")]
mod android {
    use super::*;
    use jni::{
        JNIEnv,
        objects::{JObject, JString},
        sys::jstring,
    };
    use std::sync::Mutex;

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_puredub_android_NativeCore_execute(
        mut env: JNIEnv,
        _this: JObject,
        request: JString,
    ) -> jstring {
        static SESSION: OnceLock<Mutex<Session>> = OnceLock::new();
        let outcome =
            std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| -> Result<Value, String> {
                let input: String = env.get_string(&request).map_err(|e| e.to_string())?.into();
                let value: Value = serde_json::from_str(&input).map_err(|e| e.to_string())?;
                let mut session = SESSION
                    .get_or_init(|| Mutex::new(Session::default()))
                    .lock()
                    .map_err(|_| "Native session failed. Restart PureDub.")?;
                dispatch(&mut session, &value)
            }));
        let response = match outcome {
            Ok(Ok(data)) => json!({"ok": true, "data": data}),
            Ok(Err(error)) => json!({"ok": false, "error": error}),
            Err(_) => {
                json!({"ok": false, "error": "Audio processing failed. Restart PureDub and try a shorter recording."})
            }
        };
        match env.new_string(response.to_string()) {
            Ok(result) => result.into_raw(),
            Err(_) => std::ptr::null_mut(),
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use puredub_core::Track;

    #[test]
    fn bridge_runs_manual_demo_suggest_preview_and_verified_export() {
        let root = std::env::temp_dir().join(format!(
            "puredub-bridge-{}-{}",
            std::process::id(),
            std::time::SystemTime::now()
                .duration_since(std::time::UNIX_EPOCH)
                .unwrap()
                .as_nanos()
        ));
        fs::create_dir(&root).unwrap();
        let source = root.join("synthetic.wav");
        let mut session = Session::default();
        let info = dispatch(&mut session, &json!({"op": "demo", "path": source})).unwrap();
        assert_eq!(info["channels"], 2);
        assert_eq!(info["bits"], 24);
        assert_eq!(info["duration"], 8.0);
        let p = Project {
            source: source.to_string_lossy().into(),
            end: "0:08".into(),
            artist: "Synthetic".into(),
            album: "Bridge test".into(),
            tracks: vec![
                Track {
                    title: "One".into(),
                    side: "A".into(),
                    length_ms: Some(4000),
                    ..Default::default()
                },
                Track {
                    title: "Two".into(),
                    side: "A".into(),
                    ..Default::default()
                },
            ],
            ..Default::default()
        };
        let suggested = dispatch(
            &mut session,
            &json!({"op": "suggest", "project": p, "quiet": false}),
        )
        .unwrap();
        assert_eq!(suggested["tracks"][1]["start"], "0:04.000");
        assert_eq!(
            dispatch(
                &mut session,
                &json!({"op": "validate", "project": suggested})
            )
            .unwrap()["tracks"],
            2
        );
        let pcm = root.join("preview.pcm");
        let preview = dispatch(
            &mut session,
            &json!({"op": "preview", "path": pcm, "start": 0.0, "seconds": 0.5}),
        )
        .unwrap();
        assert_eq!(preview["sample_rate"], 48000);
        assert_eq!(fs::metadata(&pcm).unwrap().len(), 48000 * 2 * 4 / 2);
        assert!(
            dispatch(
                &mut session,
                &json!({"op": "load", "path": source, "budget": 1})
            )
            .is_err()
        );
        assert!(session.audio.is_some());
        let exported = dispatch(
            &mut session,
            &json!({"op": "export", "project": suggested, "parent": root}),
        )
        .unwrap();
        assert_eq!(exported["tracks"], 2);
        assert_eq!(exported["files"].as_array().unwrap().len(), 3);
        assert!(
            dispatch(
                &mut session,
                &json!({"op": "export", "project": suggested, "parent": root})
            )
            .is_err()
        );
        fs::remove_dir_all(root).unwrap();
    }
}
