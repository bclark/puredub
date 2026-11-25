use crate::audio::Track;
use serde_json::Value;
use std::time::Duration;

#[derive(Clone)]
pub struct Release {
    pub id: String,
    pub title: String,
    pub detail: String,
}
#[derive(Clone)]
pub struct Medium {
    pub label: String,
    pub tracks: Vec<Track>,
    pub inferred_sides: bool,
}

fn client() -> Result<reqwest::blocking::Client, String> {
    reqwest::blocking::Client::builder()
        .user_agent("PureDub/0.1.0 (https://github.com/bclark/puredub)")
        .timeout(Duration::from_secs(25))
        .build()
        .map_err(|e| e.to_string())
}
fn text(v: &Value, key: &str) -> String {
    v[key].as_str().unwrap_or("").into()
}
fn quote(s: &str) -> String {
    format!("\"{}\"", s.replace('\\', "\\\\").replace('"', "\\\""))
}

pub fn search(artist: &str, album: &str) -> Result<Vec<Release>, String> {
    let query = format!(
        "artist:{} AND release:{} AND format:Cassette",
        quote(artist),
        quote(album)
    );
    let v: Value = client()?
        .get("https://musicbrainz.org/ws/2/release/")
        .query(&[("query", query.as_str()), ("fmt", "json"), ("limit", "20")])
        .send()
        .and_then(|r| r.error_for_status())
        .and_then(|r| r.json())
        .map_err(|e| format!("MusicBrainz search: {e}"))?;
    Ok(v["releases"]
        .as_array()
        .into_iter()
        .flatten()
        .map(|r| Release {
            id: text(r, "id"),
            title: text(r, "title"),
            detail: format!(
                "{} · {} · {}",
                text(r, "date"),
                text(r, "country"),
                text(r, "disambiguation")
            ),
        })
        .collect())
}

pub fn load(id: &str) -> Result<(String, Vec<Medium>), String> {
    if id.len() != 36 || !id.chars().all(|c| c.is_ascii_hexdigit() || c == '-') {
        return Err("Invalid MusicBrainz release ID.".into());
    }
    let v: Value = client()?
        .get(format!("https://musicbrainz.org/ws/2/release/{id}"))
        .query(&[("inc", "recordings"), ("fmt", "json")])
        .send()
        .and_then(|r| r.error_for_status())
        .and_then(|r| r.json())
        .map_err(|e| format!("MusicBrainz release: {e}"))?;
    let media = v["media"]
        .as_array()
        .into_iter()
        .flatten()
        .filter(|m| m["format"].as_str() == Some("Cassette"))
        .map(|m| {
            let mut inferred = false;
            let tracks = m["tracks"]
                .as_array()
                .into_iter()
                .flatten()
                .map(|t| {
                    let number = text(t, "number").to_uppercase();
                    let side = if number.starts_with('A') {
                        "A"
                    } else if number.starts_with('B') {
                        "B"
                    } else {
                        inferred = true;
                        "?"
                    };
                    Track {
                        title: t["title"]
                            .as_str()
                            .or(t["recording"]["title"].as_str())
                            .unwrap_or("Untitled")
                            .into(),
                        side: side.into(),
                        length_ms: t["length"].as_u64().or(t["recording"]["length"].as_u64()),
                        start: String::new(),
                    }
                })
                .collect();
            Medium {
                label: format!("Cassette {} {}", m["position"], text(m, "title")),
                tracks,
                inferred_sides: inferred,
            }
        })
        .collect();
    Ok((text(&v, "date"), media))
}

#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    #[ignore = "Explicit online MusicBrainz smoke test; do not run during offline builds"]
    fn live_cassette_lookup() {
        let releases = search("Pink Floyd", "The Dark Side of the Moon").unwrap();
        assert!(!releases.is_empty());
        std::thread::sleep(Duration::from_millis(1100));
        let (_, media) = load(&releases[0].id).unwrap();
        assert!(media.iter().any(|m| !m.tracks.is_empty()));
    }
}
