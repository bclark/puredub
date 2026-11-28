# Third-party dependencies

Original application code is covered by the root MIT LICENSE. The PureDub icon
is reused unchanged from assets/puredub.png; see ../assets/ICON.md for provenance.

Android/Jetpack libraries, Kotlin, the Android CLI template and the Gradle wrapper
retain their upstream licenses. AndroidX and Kotlin use Apache-2.0; this inventory
does not replace the license notices shipped with upstream packages. No source
recordings, service credentials or production signing keys are distributed.

The native dependency declarations below come from the committed native/Cargo.lock
using `cargo metadata --locked --format-version 1`, including target-specific packages.

| Rust package | Version | Declared license |
|---|---|---|
| [adler2](https://github.com/oyvindln/adler2) | 2.0.1 | 0BSD OR MIT OR Apache-2.0 |
| [atomic-waker](https://github.com/smol-rs/atomic-waker) | 1.1.2 | Apache-2.0 OR MIT |
| [autocfg](https://github.com/cuviper/autocfg) | 1.5.1 | Apache-2.0 OR MIT |
| [base64](https://github.com/marshallpierce/rust-base64) | 0.22.1 | MIT OR Apache-2.0 |
| [base64](https://github.com/marshallpierce/rust-base64) | 0.23.1 | MIT OR Apache-2.0 |
| [bitflags](https://github.com/bitflags/bitflags) | 2.13.2 | MIT OR Apache-2.0 |
| [block-buffer](https://github.com/RustCrypto/utils) | 0.10.4 | MIT OR Apache-2.0 |
| [built](https://github.com/lukaslueg/built) | 0.7.7 | MIT |
| [bumpalo](https://github.com/fitzgen/bumpalo) | 3.20.4 | MIT OR Apache-2.0 |
| [byteorder](https://github.com/BurntSushi/byteorder) | 1.5.0 | Unlicense OR MIT |
| [bytes](https://github.com/tokio-rs/bytes) | 1.12.2 | MIT |
| [cc](https://github.com/rust-lang/cc-rs) | 1.12.0 | MIT OR Apache-2.0 |
| [cesu8](https://github.com/emk/cesu8-rs) | 1.1.0 | Apache-2.0/MIT |
| [cfg_aliases](https://github.com/katharostech/cfg_aliases) | 0.2.2 | MIT |
| [cfg-if](https://github.com/rust-lang/cfg-if) | 1.0.5 | MIT OR Apache-2.0 |
| [chacha20](https://github.com/RustCrypto/stream-ciphers) | 0.10.2 | MIT OR Apache-2.0 |
| [claxon](https://github.com/ruuda/claxon) | 0.4.3 | Apache-2.0 |
| [combine](https://github.com/Marwes/combine) | 4.6.9 | MIT |
| [cpufeatures](https://github.com/RustCrypto/utils) | 0.3.1 | MIT OR Apache-2.0 |
| [crc](https://github.com/mrhooray/crc-rs.git) | 3.4.0 | MIT OR Apache-2.0 |
| [crc32fast](https://github.com/srijs/rust-crc32fast) | 1.5.6 | MIT OR Apache-2.0 |
| [crc-catalog](https://github.com/akhilles/crc-catalog.git) | 2.5.0 | MIT OR Apache-2.0 |
| [crossbeam-channel](https://github.com/crossbeam-rs/crossbeam) | 0.5.17 | MIT OR Apache-2.0 |
| [crossbeam-utils](https://github.com/crossbeam-rs/crossbeam) | 0.8.23 | MIT OR Apache-2.0 |
| [crypto-common](https://github.com/RustCrypto/traits) | 0.1.7 | MIT OR Apache-2.0 |
| [data-encoding](https://github.com/ia0/data-encoding) | 2.11.1 | MIT |
| [digest](https://github.com/RustCrypto/traits) | 0.10.7 | MIT OR Apache-2.0 |
| [displaydoc](https://github.com/yaahc/displaydoc) | 0.2.7 | MIT OR Apache-2.0 |
| [find-msvc-tools](https://github.com/rust-lang/cc-rs) | 0.1.16 | MIT OR Apache-2.0 |
| [flacenc](https://github.com/yotarok/flacenc-rs/) | 0.5.1 | Apache-2.0 |
| [flate2](https://github.com/rust-lang/flate2-rs) | 1.1.11 | MIT OR Apache-2.0 |
| [form_urlencoded](https://github.com/servo/rust-url) | 1.2.3 | MIT OR Apache-2.0 |
| [futures-channel](https://github.com/rust-lang/futures-rs) | 0.3.34 | MIT OR Apache-2.0 |
| [futures-core](https://github.com/rust-lang/futures-rs) | 0.3.34 | MIT OR Apache-2.0 |
| [futures-io](https://github.com/rust-lang/futures-rs) | 0.3.34 | MIT OR Apache-2.0 |
| [futures-sink](https://github.com/rust-lang/futures-rs) | 0.3.34 | MIT OR Apache-2.0 |
| [futures-task](https://github.com/rust-lang/futures-rs) | 0.3.34 | MIT OR Apache-2.0 |
| [futures-util](https://github.com/rust-lang/futures-rs) | 0.3.34 | MIT OR Apache-2.0 |
| [generic-array](https://github.com/fizyk20/generic-array.git) | 0.14.7 | MIT |
| [getrandom](https://github.com/rust-random/getrandom) | 0.2.17 | MIT OR Apache-2.0 |
| [getrandom](https://github.com/rust-random/getrandom) | 0.4.3 | MIT OR Apache-2.0 |
| [hash32](https://github.com/japaric/hash32) | 0.3.1 | MIT OR Apache-2.0 |
| [heapless](https://github.com/rust-embedded/heapless) | 0.8.0 | MIT OR Apache-2.0 |
| [hound](https://github.com/ruuda/hound) | 3.5.1 | Apache-2.0 |
| [http](https://github.com/hyperium/http) | 1.5.0 | MIT OR Apache-2.0 |
| [httparse](https://github.com/seanmonstar/httparse) | 1.10.1 | MIT OR Apache-2.0 |
| [http-body](https://github.com/hyperium/http-body) | 1.1.0 | MIT |
| [http-body-util](https://github.com/hyperium/http-body) | 0.1.5 | MIT |
| [hyper](https://github.com/hyperium/hyper) | 1.13.0 | MIT |
| [hyper-rustls](https://github.com/rustls/hyper-rustls) | 0.27.10 | Apache-2.0 OR ISC OR MIT |
| [hyper-util](https://github.com/hyperium/hyper-util) | 0.1.21 | MIT |
| [icu_collections](https://github.com/unicode-org/icu4x) | 2.4.0 | Unicode-3.0 |
| [icu_locale_core](https://github.com/unicode-org/icu4x) | 2.4.0 | Unicode-3.0 |
| [icu_normalizer](https://github.com/unicode-org/icu4x) | 2.4.0 | Unicode-3.0 |
| [icu_normalizer_data](https://github.com/unicode-org/icu4x) | 2.4.0 | Unicode-3.0 |
| [icu_properties](https://github.com/unicode-org/icu4x) | 2.4.0 | Unicode-3.0 |
| [icu_properties_data](https://github.com/unicode-org/icu4x) | 2.4.0 | Unicode-3.0 |
| [icu_provider](https://github.com/unicode-org/icu4x) | 2.4.0 | Unicode-3.0 |
| [idna](https://github.com/servo/rust-url/) | 1.1.0 | MIT OR Apache-2.0 |
| [idna_adapter](https://github.com/hsivonen/idna_adapter) | 1.2.2 | Apache-2.0 OR MIT |
| [ipnet](https://github.com/krisprice/ipnet) | 2.12.2 | MIT OR Apache-2.0 |
| [itoa](https://github.com/dtolnay/itoa) | 1.0.19 | MIT OR Apache-2.0 |
| [jni](https://github.com/jni-rs/jni-rs) | 0.21.1 | MIT/Apache-2.0 |
| [jni-sys](https://github.com/jni-rs/jni-sys) | 0.3.1 | MIT OR Apache-2.0 |
| [jni-sys](https://github.com/jni-rs/jni-sys) | 0.4.1 | MIT OR Apache-2.0 |
| [jni-sys-macros](https://github.com/jni-rs/jni-sys) | 0.4.1 | MIT OR Apache-2.0 |
| [js-sys](https://github.com/wasm-bindgen/wasm-bindgen/tree/master/crates/js-sys) | 0.3.107 | MIT OR Apache-2.0 |
| [libc](https://github.com/rust-lang/libc) | 0.2.191 | MIT OR Apache-2.0 |
| [litemap](https://github.com/unicode-org/icu4x) | 0.8.3 | Unicode-3.0 |
| [lofty](https://github.com/Serial-ATA/lofty-rs) | 0.25.5 | MIT OR Apache-2.0 |
| [lofty_attr](https://github.com/Serial-ATA/lofty-rs) | 0.13.0 | MIT OR Apache-2.0 |
| [log](https://github.com/rust-lang/log) | 0.4.35 | MIT OR Apache-2.0 |
| [lru-slab](https://github.com/Ralith/lru-slab) | 0.1.3 | MIT OR Apache-2.0 OR Zlib |
| [md-5](https://github.com/RustCrypto/hashes) | 0.10.6 | MIT OR Apache-2.0 |
| [memchr](https://github.com/BurntSushi/memchr) | 2.8.3 | Unlicense OR MIT |
| [miniz_oxide](https://github.com/Frommi/miniz_oxide/tree/master/miniz_oxide) | 0.9.1 | MIT OR Zlib OR Apache-2.0 |
| [mio](https://github.com/tokio-rs/mio) | 1.2.5 | MIT |
| [num-traits](https://github.com/rust-num/num-traits) | 0.2.19 | MIT OR Apache-2.0 |
| [ogg_pager](https://github.com/Serial-ATA/lofty-rs) | 0.8.0 | MIT OR Apache-2.0 |
| [once_cell](https://github.com/matklad/once_cell) | 1.21.4 | MIT OR Apache-2.0 |
| [paste](https://github.com/dtolnay/paste) | 1.0.15 | MIT OR Apache-2.0 |
| [percent-encoding](https://github.com/servo/rust-url/) | 2.3.2 | MIT OR Apache-2.0 |
| [pin-project-lite](https://github.com/taiki-e/pin-project-lite) | 0.2.18 | Apache-2.0 OR MIT |
| [potential_utf](https://github.com/unicode-org/icu4x) | 0.1.6 | Unicode-3.0 |
| [proc-macro2](https://github.com/dtolnay/proc-macro2) | 1.0.107 | MIT OR Apache-2.0 |
| [puredub-core](https://github.com/bclark/puredub-core) | 0.1.0 | MIT |
| [quinn](https://github.com/quinn-rs/quinn) | 0.11.12 | MIT OR Apache-2.0 |
| [quinn-proto](https://github.com/quinn-rs/quinn) | 0.11.19 | MIT OR Apache-2.0 |
| [quinn-udp](https://github.com/quinn-rs/quinn) | 0.5.16 | MIT OR Apache-2.0 |
| [quote](https://github.com/dtolnay/quote) | 1.0.47 | MIT OR Apache-2.0 |
| [rand](https://github.com/rust-random/rand) | 0.10.4 | MIT OR Apache-2.0 |
| [rand_core](https://github.com/rust-random/rand_core) | 0.10.1 | MIT OR Apache-2.0 |
| [rand_pcg](https://github.com/rust-random/rngs) | 0.10.3 | MIT OR Apache-2.0 |
| [r-efi](https://github.com/r-efi/r-efi) | 6.0.0 | MIT OR Apache-2.0 OR LGPL-2.1-or-later |
| [release-metadata](https://github.com/bclark/release-metadata) | 0.1.0 | MIT |
| [reqwest](https://github.com/seanmonstar/reqwest) | 0.12.28 | MIT OR Apache-2.0 |
| [ring](https://github.com/briansmith/ring) | 0.17.14 | Apache-2.0 AND ISC |
| [rustc-hash](https://github.com/rust-lang/rustc-hash) | 2.1.3 | Apache-2.0 OR MIT |
| [rustls](https://github.com/rustls/rustls) | 0.23.46 | Apache-2.0 OR ISC OR MIT |
| [rustls-pki-types](https://github.com/rustls/pki-types) | 1.16.0 | MIT OR Apache-2.0 |
| [rustls-webpki](https://github.com/rustls/webpki) | 0.103.17 | ISC |
| [rustversion](https://github.com/dtolnay/rustversion) | 1.0.23 | MIT OR Apache-2.0 |
| [ryu](https://github.com/dtolnay/ryu) | 1.0.23 | Apache-2.0 OR BSL-1.0 |
| [same-file](https://github.com/BurntSushi/same-file) | 1.0.6 | Unlicense/MIT |
| [seq-macro](https://github.com/dtolnay/seq-macro) | 0.3.6 | MIT OR Apache-2.0 |
| [serde](https://github.com/serde-rs/serde) | 1.0.229 | MIT OR Apache-2.0 |
| [serde_core](https://github.com/serde-rs/serde) | 1.0.229 | MIT OR Apache-2.0 |
| [serde_derive](https://github.com/serde-rs/serde) | 1.0.229 | MIT OR Apache-2.0 |
| [serde_json](https://github.com/serde-rs/json) | 1.0.154 | MIT OR Apache-2.0 |
| [serde_urlencoded](https://github.com/nox/serde_urlencoded) | 0.7.1 | MIT/Apache-2.0 |
| [shlex](https://github.com/comex/rust-shlex) | 2.0.1 | MIT OR Apache-2.0 |
| [simd-adler32](https://github.com/mcountryman/simd-adler32) | 0.3.10 | MIT |
| [slab](https://github.com/tokio-rs/slab) | 0.4.12 | MIT |
| [smallvec](https://github.com/smallvec/smallvec) | 1.16.6 | MIT OR Apache-2.0 |
| [socket2](https://github.com/rust-lang/socket2) | 0.6.5 | MIT OR Apache-2.0 |
| [stable_deref_trait](https://github.com/storyyeller/stable_deref_trait) | 1.2.1 | MIT OR Apache-2.0 |
| [subtle](https://github.com/dalek-cryptography/subtle) | 2.6.1 | BSD-3-Clause |
| [syn](https://github.com/dtolnay/syn) | 2.0.119 | MIT OR Apache-2.0 |
| [syn](https://github.com/dtolnay/syn) | 3.0.10 | MIT OR Apache-2.0 |
| [sync_wrapper](https://github.com/Actyx/sync_wrapper) | 1.0.2 | Apache-2.0 |
| [synstructure](https://github.com/mystor/synstructure) | 0.14.0 | MIT |
| [thiserror](https://github.com/dtolnay/thiserror) | 1.0.69 | MIT OR Apache-2.0 |
| [thiserror](https://github.com/dtolnay/thiserror) | 2.0.21 | MIT OR Apache-2.0 |
| [thiserror-impl](https://github.com/dtolnay/thiserror) | 1.0.69 | MIT OR Apache-2.0 |
| [thiserror-impl](https://github.com/dtolnay/thiserror) | 2.0.21 | MIT OR Apache-2.0 |
| [tinystr](https://github.com/unicode-org/icu4x) | 0.8.5 | Unicode-3.0 |
| [tinyvec](https://github.com/Lokathor/tinyvec) | 1.13.3 | Zlib OR Apache-2.0 OR MIT |
| [tokio](https://github.com/tokio-rs/tokio) | 1.54.1 | MIT |
| [tokio-rustls](https://github.com/rustls/tokio-rustls) | 0.26.6 | MIT OR Apache-2.0 |
| [tower](https://github.com/tower-rs/tower) | 0.5.3 | MIT |
| [tower-http](https://github.com/tower-rs/tower-http) | 0.6.11 | MIT |
| [tower-layer](https://github.com/tower-rs/tower) | 0.3.3 | MIT |
| [tower-service](https://github.com/tower-rs/tower) | 0.3.3 | MIT |
| [tracing](https://github.com/tokio-rs/tracing) | 0.1.44 | MIT |
| [tracing-core](https://github.com/tokio-rs/tracing) | 0.1.36 | MIT |
| [try-lock](https://github.com/seanmonstar/try-lock) | 0.2.5 | MIT |
| [typenum](https://github.com/paholg/typenum) | 1.20.1 | MIT OR Apache-2.0 |
| [unicode-ident](https://github.com/dtolnay/unicode-ident) | 1.0.27 | (MIT OR Apache-2.0) AND Unicode-3.0 |
| [untrusted](https://github.com/briansmith/untrusted) | 0.9.0 | ISC |
| [url](https://github.com/servo/rust-url) | 2.5.9 | MIT OR Apache-2.0 |
| [utf_types](https://github.com/hsivonen/utf_types) | 1.0.0 | Apache-2.0 OR MIT |
| [utf8_iter](https://github.com/hsivonen/utf8_iter) | 1.1.0 | Apache-2.0 OR MIT |
| [version_check](https://github.com/SergioBenitez/version_check) | 0.9.5 | MIT/Apache-2.0 |
| [walkdir](https://github.com/BurntSushi/walkdir) | 2.5.1 | Unlicense/MIT |
| [want](https://github.com/seanmonstar/want) | 0.3.2 | MIT |
| [wasi](https://github.com/bytecodealliance/wasi) | 0.11.1+wasi-snapshot-preview1 | Apache-2.0 WITH LLVM-exception OR Apache-2.0 OR MIT |
| [wasm-bindgen](https://github.com/wasm-bindgen/wasm-bindgen) | 0.2.130 | MIT OR Apache-2.0 |
| [wasm-bindgen-futures](https://github.com/wasm-bindgen/wasm-bindgen/tree/master/crates/futures) | 0.4.80 | MIT OR Apache-2.0 |
| [wasm-bindgen-macro](https://github.com/wasm-bindgen/wasm-bindgen/tree/master/crates/macro) | 0.2.130 | MIT OR Apache-2.0 |
| [wasm-bindgen-macro-support](https://github.com/wasm-bindgen/wasm-bindgen/tree/main/crates/macro-support) | 0.2.130 | MIT OR Apache-2.0 |
| [wasm-bindgen-shared](https://github.com/wasm-bindgen/wasm-bindgen/tree/master/crates/shared) | 0.2.130 | MIT OR Apache-2.0 |
| [webpki-roots](https://github.com/rustls/webpki-roots) | 1.0.12 | CDLA-Permissive-2.0 |
| [web-sys](https://github.com/wasm-bindgen/wasm-bindgen/tree/master/crates/web-sys) | 0.3.107 | MIT OR Apache-2.0 |
| [web-time](https://github.com/daxpedda/web-time) | 1.1.0 | MIT OR Apache-2.0 |
| [winapi-util](https://github.com/BurntSushi/winapi-util) | 0.1.11 | Unlicense OR MIT |
| [windows_aarch64_gnullvm](https://github.com/microsoft/windows-rs) | 0.42.2 | MIT OR Apache-2.0 |
| [windows_aarch64_gnullvm](https://github.com/microsoft/windows-rs) | 0.52.6 | MIT OR Apache-2.0 |
| [windows_aarch64_msvc](https://github.com/microsoft/windows-rs) | 0.42.2 | MIT OR Apache-2.0 |
| [windows_aarch64_msvc](https://github.com/microsoft/windows-rs) | 0.52.6 | MIT OR Apache-2.0 |
| [windows_i686_gnu](https://github.com/microsoft/windows-rs) | 0.42.2 | MIT OR Apache-2.0 |
| [windows_i686_gnu](https://github.com/microsoft/windows-rs) | 0.52.6 | MIT OR Apache-2.0 |
| [windows_i686_gnullvm](https://github.com/microsoft/windows-rs) | 0.52.6 | MIT OR Apache-2.0 |
| [windows_i686_msvc](https://github.com/microsoft/windows-rs) | 0.42.2 | MIT OR Apache-2.0 |
| [windows_i686_msvc](https://github.com/microsoft/windows-rs) | 0.52.6 | MIT OR Apache-2.0 |
| [windows_x86_64_gnu](https://github.com/microsoft/windows-rs) | 0.42.2 | MIT OR Apache-2.0 |
| [windows_x86_64_gnu](https://github.com/microsoft/windows-rs) | 0.52.6 | MIT OR Apache-2.0 |
| [windows_x86_64_gnullvm](https://github.com/microsoft/windows-rs) | 0.42.2 | MIT OR Apache-2.0 |
| [windows_x86_64_gnullvm](https://github.com/microsoft/windows-rs) | 0.52.6 | MIT OR Apache-2.0 |
| [windows_x86_64_msvc](https://github.com/microsoft/windows-rs) | 0.42.2 | MIT OR Apache-2.0 |
| [windows_x86_64_msvc](https://github.com/microsoft/windows-rs) | 0.52.6 | MIT OR Apache-2.0 |
| [windows-link](https://github.com/microsoft/windows-rs) | 0.2.1 | MIT OR Apache-2.0 |
| [windows-sys](https://github.com/microsoft/windows-rs) | 0.45.0 | MIT OR Apache-2.0 |
| [windows-sys](https://github.com/microsoft/windows-rs) | 0.52.0 | MIT OR Apache-2.0 |
| [windows-sys](https://github.com/microsoft/windows-rs) | 0.61.2 | MIT OR Apache-2.0 |
| [windows-targets](https://github.com/microsoft/windows-rs) | 0.42.2 | MIT OR Apache-2.0 |
| [windows-targets](https://github.com/microsoft/windows-rs) | 0.52.6 | MIT OR Apache-2.0 |
| [writeable](https://github.com/unicode-org/icu4x) | 0.6.4 | Unicode-3.0 |
| [yoke](https://github.com/unicode-org/icu4x) | 0.8.4 | Unicode-3.0 |
| [yoke-derive](https://github.com/unicode-org/icu4x) | 0.8.4 | Unicode-3.0 |
| [zerofrom](https://github.com/unicode-org/icu4x) | 0.1.8 | Unicode-3.0 |
| [zerofrom-derive](https://github.com/unicode-org/icu4x) | 0.1.8 | Unicode-3.0 |
| [zeroize](https://github.com/RustCrypto/utils) | 1.9.2 | Apache-2.0 OR MIT |
| [zerotrie](https://github.com/unicode-org/icu4x) | 0.2.5 | Unicode-3.0 |
| [zerovec](https://github.com/unicode-org/icu4x) | 0.11.9 | Unicode-3.0 |
| [zerovec-derive](https://github.com/unicode-org/icu4x) | 0.11.7 | Unicode-3.0 |
| [zlib-rs](https://github.com/trifectatechfoundation/zlib-rs) | 0.6.9 | Zlib |
| [zmij](https://github.com/dtolnay/zmij) | 1.0.23 | MIT |
