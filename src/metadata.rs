//! Desktop-only conversion from provider metadata into editable PureDub tracks.
use puredub_core::Track;
use release_metadata::{MetadataProvider, ReleaseId, SearchQuery, musicbrainz::MusicBrainz};
use std::sync::OnceLock;

pub struct Release {
    pub id: String,
    pub title: String,
    pub detail: String,
}

pub struct Medium {
    pub label: String,
    pub tracks: Vec<Track>,
    pub inferred_sides: bool,
}

fn client() -> Result<&'static MusicBrainz, String> {
    static CLIENT: OnceLock<Result<MusicBrainz, String>> = OnceLock::new();
    CLIENT
        .get_or_init(|| MusicBrainz::new("PureDub/0.1.0 (https://github.com/bclark/puredub)"))
        .as_ref()
        .map_err(Clone::clone)
}

pub fn search(artist: &str, album: &str) -> Result<Vec<Release>, String> {
    Ok(client()?
        .search(&SearchQuery {
            artist: artist.into(),
            title: album.into(),
            format: Some("Cassette".into()),
        })?
        .into_iter()
        .map(|r| Release {
            id: r.id.value,
            title: r.title,
            detail: r.detail,
        })
        .collect())
}

pub fn load(id: &str) -> Result<(String, Vec<Medium>), String> {
    let release = client()?.load(&ReleaseId {
        provider: "musicbrainz".into(),
        value: id.into(),
    })?;
    let media = release
        .media
        .into_iter()
        .filter(|m| m.format.as_deref() == Some("Cassette"))
        .map(|m| {
            let inferred_sides = m
                .tracks
                .iter()
                .any(|t| !matches!(t.side.as_deref(), Some("A" | "B")));
            Medium {
                label: format!("Cassette {} {}", m.position, m.title),
                inferred_sides,
                tracks: m
                    .tracks
                    .into_iter()
                    .map(|t| Track {
                        title: t.title,
                        side: t
                            .side
                            .filter(|s| s == "A" || s == "B")
                            .unwrap_or_else(|| "?".into()),
                        length_ms: t.length_ms,
                        start: String::new(),
                    })
                    .collect(),
            }
        })
        .collect();
    Ok((release.date.unwrap_or_default(), media))
}
