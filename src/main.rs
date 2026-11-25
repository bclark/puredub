#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]
mod audio;
mod metadata;

use audio::{Audio, Project, Track, parse_time, time};
use eframe::egui::{self, Color32, RichText, Vec2};
use std::{
    path::PathBuf,
    sync::{Arc, mpsc},
    time::{Duration, Instant},
};

const CYAN: Color32 = Color32::from_rgb(97, 217, 221);
const AMBER: Color32 = Color32::from_rgb(243, 182, 108);
enum Event {
    Audio(Result<(String, Audio), String>),
    Search(Result<Vec<metadata::Release>, String>),
    Release(String, Result<(String, Vec<metadata::Medium>), String>),
    Export(Result<usize, String>),
}

struct PureDub {
    project: Project,
    audio: Option<Arc<Audio>>,
    releases: Vec<metadata::Release>,
    media: Vec<metadata::Medium>,
    tx: mpsc::Sender<Event>,
    rx: mpsc::Receiver<Event>,
    busy: bool,
    status: String,
    error: bool,
    request_after: Instant,
    selected: usize,
    cursor: f64,
    quiet_snap: bool,
    output: Option<(rodio::OutputStream, rodio::OutputStreamHandle)>,
    sink: Option<rodio::Sink>,
    playback_start: f64,
    started: Option<Instant>,
    icon: Option<egui::TextureHandle>,
}

impl PureDub {
    fn new(cc: &eframe::CreationContext<'_>) -> Self {
        let mut style = (*cc.egui_ctx.style()).clone();
        style.visuals = egui::Visuals::dark();
        style.visuals.panel_fill = Color32::from_rgb(16, 24, 32);
        style.visuals.window_fill = Color32::from_rgb(22, 32, 42);
        style.visuals.override_text_color = Some(Color32::from_rgb(230, 238, 242));
        style.visuals.selection.bg_fill = Color32::from_rgb(32, 84, 91);
        style.visuals.widgets.inactive.bg_fill = Color32::from_rgb(35, 48, 61);
        style.visuals.widgets.hovered.bg_fill = Color32::from_rgb(46, 66, 80);
        style.spacing.item_spacing = Vec2::new(10.0, 9.0);
        style.spacing.button_padding = Vec2::new(13.0, 8.0);
        style
            .text_styles
            .insert(egui::TextStyle::Body, egui::FontId::proportional(16.0));
        cc.egui_ctx.set_style(style);
        let (tx, rx) = mpsc::channel();
        let icon = load_icon().map(|i| cc.egui_ctx.load_texture("PureDub", i, Default::default()));
        let mut app = Self {
            project: Project::default(),
            audio: None,
            releases: vec![],
            media: vec![],
            tx,
            rx,
            busy: false,
            status: "Import a side recording to begin. Your original stays untouched.".into(),
            error: false,
            request_after: Instant::now(),
            selected: 0,
            cursor: 0.0,
            quiet_snap: true,
            output: None,
            sink: None,
            playback_start: 0.0,
            started: None,
            icon,
        };
        if let Some(path) = std::env::args_os().nth(1) {
            let path = PathBuf::from(path);
            if path
                .extension()
                .is_some_and(|s| s.eq_ignore_ascii_case("wav"))
            {
                app.import(path);
            } else {
                match std::fs::read(&path)
                    .map_err(|e| e.to_string())
                    .and_then(|bytes| {
                        serde_json::from_slice::<Project>(&bytes).map_err(|e| e.to_string())
                    }) {
                    Ok(project) if project.version == 1 => {
                        let source = project.source.clone();
                        app.project = project;
                        app.import(source.into());
                    }
                    Ok(_) => app.message(Err("Unsupported project version.".into())),
                    Err(e) => app.message(Err(e)),
                }
            }
        }
        app
    }
    fn message(&mut self, result: Result<String, String>) {
        self.error = result.is_err();
        self.status = match result {
            Ok(s) | Err(s) => s,
        };
    }
    fn stop(&mut self) {
        if let Some(s) = self.sink.take() {
            s.stop();
        }
        self.started = None;
    }
    fn import(&mut self, path: PathBuf) {
        self.stop();
        self.audio = None;
        self.busy = true;
        self.status = "Reading WAV and building waveform…".into();
        self.error = false;
        let tx = self.tx.clone();
        std::thread::spawn(move || {
            let result = Audio::load(&path).map(|a| (path.to_string_lossy().into(), a));
            let _ = tx.send(Event::Audio(result));
        });
    }
    fn events(&mut self) {
        while let Ok(event) = self.rx.try_recv() {
            self.busy = false;
            match event {
                Event::Audio(r) => {
                    match r {
                        Ok((path, a)) => {
                            self.project.source = path;
                            self.project.end = if self.project.end.is_empty() {
                                time(a.duration())
                            } else {
                                self.project.end.clone()
                            };
                            self.cursor = 0.0;
                            self.message(Ok(format!("Imported {} · {} Hz · {}-bit · {} channels · {} full-scale samples",time(a.duration()),a.spec.sample_rate,a.spec.bits_per_sample,a.spec.channels,a.clipped)));
                            self.audio = Some(Arc::new(a));
                        }
                        Err(e) => self.message(Err(e)),
                    }
                }
                Event::Search(r) => match r {
                    Ok(rs) => {
                        self.status = format!(
                            "{} cassette releases found. Confirm edition and track order.",
                            rs.len()
                        );
                        self.releases = rs;
                    }
                    Err(e) => self.message(Err(e)),
                },
                Event::Release(id, r) => match r {
                    Ok((date, media)) => {
                        self.project.release_id = id;
                        self.project.year = date;
                        self.media = media;
                        self.message(Ok("Choose the cassette below to use its track list.".into()));
                    }
                    Err(e) => self.message(Err(e)),
                },
                Event::Export(r) => self.message(r.map(|n| {
                    format!("Exported {n} tagged FLAC tracks into a new folder. Source unchanged.")
                })),
            }
        }
    }
    fn play(&mut self, start: f64, seconds: f64) -> Result<(), String> {
        self.stop();
        let a = self.audio.as_ref().ok_or("Import audio first.")?;
        if self.output.is_none() {
            self.output = Some(
                rodio::OutputStream::try_default()
                    .map_err(|e| format!("Cannot open playback device: {e}"))?,
            );
        }
        let sink =
            rodio::Sink::try_new(&self.output.as_ref().unwrap().1).map_err(|e| e.to_string())?;
        let ch = a.spec.channels as usize;
        let first = a.frame(start) * ch;
        let last = a.frame((start + seconds).min(a.duration())) * ch;
        let scale = (1u32 << (a.spec.bits_per_sample - 1)) as f32;
        let samples: Vec<f32> = a.samples[first..last]
            .iter()
            .map(|s| *s as f32 / scale)
            .collect();
        sink.append(rodio::buffer::SamplesBuffer::new(
            a.spec.channels,
            a.spec.sample_rate,
            samples,
        ));
        self.sink = Some(sink);
        self.cursor = start;
        self.playback_start = start;
        self.started = Some(Instant::now());
        Ok(())
    }
    fn suggest(&mut self) -> Result<String, String> {
        let a = self.audio.as_ref().ok_or("Import a WAV recording first.")?;
        let mut expected = parse_time(&self.project.offset)?;
        let indices: Vec<_> = self
            .project
            .tracks
            .iter()
            .enumerate()
            .filter(|(_, t)| t.side == self.project.side)
            .map(|(i, _)| i)
            .collect();
        if indices.is_empty() {
            return Err("Assign tracks to the selected side first.".into());
        }
        // Validate all metadata before altering any boundaries.
        for &i in indices.iter().take(indices.len() - 1) {
            if self.project.tracks[i].length_ms.is_none() {
                return Err(format!(
                    "Track {} has no published duration. Set starts manually, or enter durations first.",
                    i + 1
                ));
            }
        }
        let mut starts = vec![];
        for (pos, &i) in indices.iter().enumerate() {
            let candidate = if pos > 0 && self.quiet_snap {
                a.nearby_quiet(expected)
            } else {
                expected
            };
            if candidate >= a.duration()
                || starts.last().is_some_and(|previous| *previous >= candidate)
            {
                return Err("Expected track times exceed the recording or overlap. Check side assignments, durations and leading silence.".into());
            }
            starts.push(candidate);
            expected += self.project.tracks[i].length_ms.unwrap_or(0) as f64 / 1000.0;
        }
        for (i, start) in indices.iter().zip(starts) {
            self.project.tracks[*i].start = time(start);
        }
        Ok("Suggested starts are ready. Listen across each cut before exporting; the last track ends at Side end.".into())
    }
    fn files(&mut self, ui: &mut egui::Ui) {
        if ui.button("Import side WAV…").clicked() {
            if let Some(p) = rfd::FileDialog::new()
                .add_filter("PCM WAV", &["wav"])
                .pick_file()
            {
                self.project.end.clear();
                for track in &mut self.project.tracks {
                    if track.side == self.project.side {
                        track.start.clear();
                    }
                }
                self.import(p);
            }
        }
        if ui.button("Open project…").clicked() {
            if let Some(p) = rfd::FileDialog::new()
                .add_filter("PureDub JSON", &["json"])
                .pick_file()
            {
                let r = (|| {
                    let p: Project =
                        serde_json::from_slice(&std::fs::read(p).map_err(|e| e.to_string())?)
                            .map_err(|e| e.to_string())?;
                    if p.version != 1 {
                        return Err("Unsupported project version.".into());
                    }
                    Ok(p)
                })();
                match r {
                    Ok(p) => {
                        let source = p.source.clone();
                        self.project = p;
                        self.import(source.into());
                    }
                    Err(e) => self.message(Err(e)),
                }
            }
        }
        if ui.button("Save project…").clicked() {
            if let Some(p) = rfd::FileDialog::new()
                .set_file_name("side-a.puredub.json")
                .add_filter("PureDub JSON", &["json"])
                .save_file()
            {
                let r = serde_json::to_vec_pretty(&self.project)
                    .map_err(|e| e.to_string())
                    .and_then(|b| {
                        use std::io::Write;
                        let mut file=std::fs::OpenOptions::new().write(true).create_new(true).open(p).map_err(|e|format!("Choose a new project filename (existing files are protected): {e}"))?;
                        file.write_all(&b).map_err(|e|e.to_string())
                    });
                self.message(r.map(|_|"Project saved. It references the original recording; keep that WAV in place.".into()));
            }
        }
    }
    fn release_panel(&mut self, ui: &mut egui::Ui) {
        ui.heading("Find your cassette");
        ui.label("Choose the edition, not just the album.");
        ui.add_space(12.0);
        ui.label("Artist");
        ui.text_edit_singleline(&mut self.project.artist);
        ui.label("Album");
        ui.text_edit_singleline(&mut self.project.album);
        if ui
            .add_enabled(
                !self.busy
                    && Instant::now() >= self.request_after
                    && !self.project.artist.trim().is_empty()
                    && !self.project.album.trim().is_empty(),
                egui::Button::new("Search MusicBrainz"),
            )
            .clicked()
        {
            self.busy = true;
            self.error = false;
            self.status = "Searching cassette editions…".into();
            self.request_after = Instant::now() + Duration::from_millis(1100);
            let tx = self.tx.clone();
            let artist = self.project.artist.clone();
            let album = self.project.album.clone();
            std::thread::spawn(move || {
                let _ = tx.send(Event::Search(metadata::search(&artist, &album)));
            });
        }
        ui.small("Only your search terms are sent to MusicBrainz. Audio stays on this PC.");
        ui.separator();
        let mut load = None;
        egui::ScrollArea::vertical()
            .id_salt("releases")
            .max_height(250.0)
            .show(ui, |ui| {
                for release in &self.releases {
                    ui.add_space(6.0);
                    if ui
                        .add_enabled(
                            !self.busy && Instant::now() >= self.request_after,
                            egui::Button::new(&release.title).wrap(),
                        )
                        .clicked()
                    {
                        load = Some(release.id.clone());
                    }
                    ui.small(&release.detail);
                }
            });
        if let Some(id) = load {
            self.busy = true;
            self.status = "Fetching cassette track list…".into();
            self.request_after = Instant::now() + Duration::from_millis(1100);
            let tx = self.tx.clone();
            std::thread::spawn(move || {
                let result = metadata::load(&id);
                let _ = tx.send(Event::Release(id, result));
            });
        }
        let mut chosen = None;
        for (i, m) in self.media.iter().enumerate() {
            if ui
                .button(format!("Use {} ({} tracks)", m.label, m.tracks.len()))
                .clicked()
            {
                chosen = Some(i);
            }
        }
        if let Some(i) = chosen {
            self.project.tracks = self.media[i].tracks.clone();
            self.selected = 0;
            let message = if self.media[i].inferred_sides {
                "Some side labels are missing. Assign ? tracks to A/B from the cassette sleeve before suggesting cuts."
            } else {
                "Track list loaded. Confirm it against the cassette sleeve, then suggest cuts."
            };
            self.message(Ok(message.into()));
        }
        ui.add_space(12.0);
        ui.separator();
        ui.label("Edition details");
        ui.horizontal(|ui| {
            ui.label("Year");
            ui.add(egui::TextEdit::singleline(&mut self.project.year).desired_width(100.0));
        });
        if !self.project.release_id.is_empty() {
            ui.hyperlink_to(
                "View release on MusicBrainz",
                format!(
                    "https://musicbrainz.org/release/{}",
                    self.project.release_id
                ),
            );
        }
        ui.add_space(16.0);
        ui.label(RichText::new("Live recording is next").color(AMBER));
        ui.small("This preview imports existing recordings. A stereo capture input still needs to be verified. No noise reduction or normalization is applied.");
    }
    fn waveform(&mut self, ui: &mut egui::Ui) {
        let (rect, response) =
            ui.allocate_exact_size(Vec2::new(ui.available_width(), 155.0), egui::Sense::click());
        let painter = ui.painter_at(rect);
        painter.rect_filled(rect, 6.0, Color32::from_rgb(10, 18, 25));
        if let Some(a) = &self.audio {
            let mid = rect.center().y;
            painter.line_segment(
                [egui::pos2(rect.left(), mid), egui::pos2(rect.right(), mid)],
                egui::Stroke::new(1.0_f32, Color32::from_gray(65)),
            );
            for (i, peak) in a.peaks.iter().enumerate() {
                let x = rect.left() + i as f32 / a.peaks.len() as f32 * rect.width();
                let h = peak * rect.height() * 0.44;
                painter.line_segment(
                    [egui::pos2(x, mid - h), egui::pos2(x, mid + h)],
                    egui::Stroke::new(1.0_f32, CYAN),
                );
            }
            for (i, t) in self
                .project
                .tracks
                .iter()
                .enumerate()
                .filter(|(_, t)| t.side == self.project.side)
            {
                if let Ok(s) = parse_time(&t.start) {
                    if s <= a.duration() {
                        let x = rect.left() + (s / a.duration()) as f32 * rect.width();
                        painter.line_segment(
                            [egui::pos2(x, rect.top()), egui::pos2(x, rect.bottom())],
                            egui::Stroke::new(1.5_f32, AMBER),
                        );
                        painter.text(
                            egui::pos2(x + 3.0, rect.top() + 5.0),
                            egui::Align2::LEFT_TOP,
                            format!("{}", i + 1),
                            egui::FontId::monospace(12.0),
                            AMBER,
                        );
                    }
                }
            }
            let x = rect.left() + (self.cursor / a.duration()) as f32 * rect.width();
            painter.line_segment(
                [egui::pos2(x, rect.top()), egui::pos2(x, rect.bottom())],
                egui::Stroke::new(2.0_f32, Color32::WHITE),
            );
            if response.clicked() {
                if let Some(pos) = response.interact_pointer_pos() {
                    let seconds = ((pos.x - rect.left()) / rect.width()) as f64 * a.duration();
                    self.stop();
                    self.cursor = seconds;
                }
            }
        } else {
            painter.text(
                rect.center(),
                egui::Align2::CENTER_CENTER,
                "Your side recording will appear here",
                egui::FontId::proportional(19.0),
                Color32::from_rgb(157, 178, 190),
            );
        }
    }
    fn workbench(&mut self, ui: &mut egui::Ui) {
        ui.horizontal(|ui| {
            ui.heading("Side workbench");
            ui.add_space(12.0);
            ui.selectable_value(&mut self.project.side, "A".into(), "Side A");
            ui.selectable_value(&mut self.project.side, "B".into(), "Side B");
        });
        ui.label(if self.project.source.is_empty() {
            "No recording imported"
        } else {
            &self.project.source
        });
        self.waveform(ui);
        ui.add_enabled_ui(self.audio.is_some(), |ui| {
            ui.horizontal(|ui| {
                if ui
                    .button(RichText::new("Play 10 seconds").color(AMBER))
                    .clicked()
                {
                    let r = self.play(self.cursor, 10.0);
                    if let Err(e) = r {
                        self.message(Err(e));
                    }
                }
                if ui.button("Stop").clicked() {
                    self.stop();
                }
                ui.monospace(time(self.cursor));
                ui.label("Click waveform to seek");
            });
        });
        ui.add_space(6.0);
        ui.horizontal_wrapped(|ui| {
            ui.label("Leading silence");
            ui.add(egui::TextEdit::singleline(&mut self.project.offset).desired_width(85.0));
            ui.label("Side end");
            ui.add(egui::TextEdit::singleline(&mut self.project.end).desired_width(85.0));
            ui.checkbox(&mut self.quiet_snap, "Find nearby quiet (±8s)");
            if ui
                .add_enabled(self.audio.is_some(), egui::Button::new("Suggest cuts"))
                .clicked()
            {
                let r = self.suggest();
                self.message(r);
            }
        });
        ui.separator();
        ui.label("Track starts · editable m:ss.sss · select a row to audition its cut");
        let mut remove = None;
        egui::ScrollArea::vertical()
            .id_salt("tracks")
            .max_height((ui.available_height() - 105.0).max(120.0))
            .show(ui, |ui| {
                egui::Grid::new("track_table")
                    .striped(true)
                    .num_columns(6)
                    .spacing([10.0, 10.0])
                    .show(ui, |ui| {
                        ui.label("Track");
                        ui.label("Side");
                        ui.label("Title");
                        ui.label("Duration (s)");
                        ui.label("Start");
                        ui.label("");
                        ui.end_row();
                        for (i, t) in self.project.tracks.iter_mut().enumerate() {
                            if ui
                                .selectable_label(self.selected == i, format!("{:02}", i + 1))
                                .clicked()
                            {
                                self.selected = i;
                            }
                            egui::ComboBox::from_id_salt(("side", i))
                                .selected_text(&t.side)
                                .width(40.0)
                                .show_ui(ui, |ui| {
                                    for side in ["A", "B", "?"] {
                                        ui.selectable_value(&mut t.side, side.into(), side);
                                    }
                                });
                            ui.add(
                                egui::TextEdit::singleline(&mut t.title).desired_width(
                                    (ui.available_width() - 230.0).clamp(130.0, 350.0),
                                ),
                            );
                            let mut seconds = t.length_ms.unwrap_or(0) as f64 / 1000.0;
                            if ui
                                .add(
                                    egui::DragValue::new(&mut seconds)
                                        .range(0.0..=7200.0)
                                        .speed(0.1)
                                        .max_decimals(3),
                                )
                                .changed()
                            {
                                t.length_ms = if seconds > 0.0 {
                                    Some((seconds * 1000.0).round() as u64)
                                } else {
                                    None
                                };
                            }
                            ui.add(egui::TextEdit::singleline(&mut t.start).desired_width(90.0));
                            if ui.small_button("Remove").clicked() {
                                remove = Some(i);
                            }
                            ui.end_row();
                        }
                    });
            });
        if let Some(i) = remove {
            self.project.tracks.remove(i);
            self.selected = self
                .selected
                .min(self.project.tracks.len().saturating_sub(1));
        }
        ui.horizontal_wrapped(|ui| {
            if ui.button("Add track").clicked() {
                self.project.tracks.push(Track {
                    title: "Untitled".into(),
                    side: self.project.side.clone(),
                    start: time(self.cursor),
                    ..Default::default()
                });
                self.selected = self.project.tracks.len() - 1;
            }
            let selected = self.project.tracks.get(self.selected).cloned();
            ui.add_enabled_ui(
                self.audio.is_some()
                    && selected
                        .as_ref()
                        .is_some_and(|t| t.side == self.project.side),
                |ui| {
                    if ui.button("Set start at cursor").clicked() {
                        if let Some(t) = self.project.tracks.get_mut(self.selected) {
                            t.start = time(self.cursor);
                        }
                    }
                    if ui.button("Listen across cut").clicked() {
                        let r = (|| {
                            let start = parse_time(&selected.as_ref().unwrap().start)?;
                            self.play((start - 3.0).max(0.0), 6.0)
                        })();
                        if let Err(e) = r {
                            self.message(Err(e));
                        }
                    }
                },
            );
        });
        ui.separator();
        ui.horizontal_wrapped(|ui| {
            if ui
                .add_enabled(
                    self.audio.is_some(),
                    egui::Button::new(RichText::new("Export side as FLAC…").color(CYAN)),
                )
                .clicked()
            {
                if let Some(parent) = rfd::FileDialog::new()
                    .set_title("Choose parent folder; PureDub creates a new album/side folder")
                    .pick_folder()
                {
                    let a = self.audio.as_ref().unwrap().clone();
                    let p = self.project.clone();
                    let tx = self.tx.clone();
                    self.busy = true;
                    self.error = false;
                    self.stop();
                    self.status = "Encoding tagged FLAC tracks… Keep PureDub open.".into();
                    std::thread::spawn(move || {
                        let _ = tx.send(Event::Export(audio::export(&p, &a, &parent)));
                    });
                }
            }
            ui.small("No fades, resampling or normalization. Original PCM samples preserved.");
        });
    }
}

impl eframe::App for PureDub {
    fn update(&mut self, ctx: &egui::Context, _: &mut eframe::Frame) {
        self.events();
        if let Some(started) = self.started {
            if self.sink.as_ref().is_some_and(|s| !s.empty()) {
                self.cursor = (self.playback_start + started.elapsed().as_secs_f64())
                    .min(self.audio.as_ref().map_or(0.0, |a| a.duration()));
            } else {
                self.started = None;
            }
        }
        ctx.request_repaint_after(Duration::from_millis(80));
        egui::TopBottomPanel::top("header").show(ctx, |ui| {
            ui.add_space(10.0);
            ui.horizontal(|ui| {
                if let Some(icon) = &self.icon {
                    ui.image((icon.id(), Vec2::splat(44.0)));
                }
                ui.label(RichText::new("PureDub").size(30.0).strong());
                ui.add_space(10.0);
                ui.label("Faithful tape capture. Tracks made simple.");
            });
            ui.add_space(6.0);
            ui.add_enabled_ui(!self.busy, |ui| {
                ui.horizontal_wrapped(|ui| self.files(ui));
            });
            ui.add_space(6.0);
        });
        egui::TopBottomPanel::bottom("status").show(ctx, |ui| {
            ui.add_space(5.0);
            ui.horizontal_wrapped(|ui| {
                if self.busy {
                    ui.spinner();
                }
                ui.label(RichText::new(&self.status).color(if self.error {
                    Color32::from_rgb(255, 150, 140)
                } else {
                    CYAN
                }));
            });
            ui.add_space(5.0);
        });
        egui::SidePanel::left("release")
            .default_width(280.0)
            .min_width(240.0)
            .max_width(360.0)
            .show(ctx, |ui| {
                ui.add_enabled_ui(!self.busy, |ui| {
                    egui::ScrollArea::vertical().show(ui, |ui| self.release_panel(ui));
                });
            });
        egui::CentralPanel::default().show(ctx, |ui| {
            ui.add_enabled_ui(!self.busy, |ui| self.workbench(ui));
        });
    }
}

fn load_icon() -> Option<egui::ColorImage> {
    let image = image::load_from_memory(include_bytes!("../assets/puredub.png"))
        .ok()?
        .to_rgba8();
    Some(egui::ColorImage::from_rgba_unmultiplied(
        [image.width() as usize, image.height() as usize],
        &image,
    ))
}

fn main() -> eframe::Result {
    let mut viewport = egui::ViewportBuilder::default()
        .with_inner_size([1240.0, 850.0])
        .with_min_inner_size([980.0, 700.0]);
    if let Some(icon) = load_icon() {
        viewport = viewport.with_icon(egui::IconData {
            rgba: icon.pixels.iter().flat_map(|p| p.to_array()).collect(),
            width: icon.size[0] as u32,
            height: icon.size[1] as u32,
        });
    }
    eframe::run_native(
        "PureDub",
        eframe::NativeOptions {
            viewport,
            ..Default::default()
        },
        Box::new(|cc| Ok(Box::new(PureDub::new(cc)))),
    )
}
