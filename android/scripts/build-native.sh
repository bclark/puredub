#!/usr/bin/env bash
set -euo pipefail
android_root="$(cd "$(dirname "$0")/.." && pwd)"
sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [[ -z "$sdk" ]]; then echo 'Set ANDROID_HOME to your Android SDK.' >&2; exit 1; fi
ndk="${ANDROID_NDK_HOME:-$sdk/ndk/29.0.14206865}"
case "$(uname -s)" in
  Darwin) host=darwin-x86_64 ;;
  Linux) host=linux-x86_64 ;;
  *) echo 'On Windows, use scripts/build-native.ps1.' >&2; exit 1 ;;
esac
toolchain="$ndk/toolchains/llvm/prebuilt/$host/bin"
for abi in arm64-v8a x86_64; do
  case "$abi" in arm64-v8a) target=aarch64-linux-android ;; x86_64) target=x86_64-linux-android ;; esac
  key="$(printf '%s' "$target" | tr '[:lower:]-' '[:upper:]_')"
  compiler_key="${target//-/_}"
  env "CARGO_TARGET_${key}_LINKER=$toolchain/${target}26-clang" \
      "CARGO_TARGET_${key}_RUSTFLAGS=-C link-arg=-Wl,-z,max-page-size=16384" \
      "CC_${compiler_key}=$toolchain/${target}26-clang" \
      "AR_${compiler_key}=$toolchain/llvm-ar" \
      cargo build --locked --release --manifest-path "$android_root/native/Cargo.toml" --target "$target"
  mkdir -p "$android_root/app/src/main/jniLibs/$abi"
  cp "$android_root/native/target/$target/release/libpuredub_android.so" "$android_root/app/src/main/jniLibs/$abi/"
done
