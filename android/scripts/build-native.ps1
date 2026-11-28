param([string]$Abis = 'arm64-v8a,x86_64', [switch]$Debug)
$ErrorActionPreference = 'Stop'
$androidRoot = Split-Path $PSScriptRoot -Parent
$sdkPath = $env:ANDROID_HOME
if (-not $sdkPath) { $sdkPath = Join-Path $env:LOCALAPPDATA 'Android/Sdk' }
$ndkRoot = Join-Path $sdkPath 'ndk/29.0.14206865'
if (-not (Test-Path -LiteralPath $ndkRoot)) { throw 'Install the NDK: android sdk install "ndk;29.0.14206865"' }
$toolchainPath = Join-Path $ndkRoot 'toolchains/llvm/prebuilt/windows-x86_64/bin'
$profileName = if ($Debug) { 'debug' } else { 'release' }
foreach ($abi in $Abis.Split(',')) {
    $targetName = switch ($abi) { 'arm64-v8a' { 'aarch64-linux-android' } 'x86_64' { 'x86_64-linux-android' } default { throw "Unsupported ABI: $abi" } }
    $linkerPath = Join-Path $toolchainPath "$($targetName)26-clang.cmd"
    $cargoTargetKey = $targetName.ToUpperInvariant().Replace('-', '_')
    $compilerTargetKey = $targetName.Replace('-', '_')
    [Environment]::SetEnvironmentVariable("CARGO_TARGET_$($cargoTargetKey)_LINKER", $linkerPath, 'Process')
    [Environment]::SetEnvironmentVariable("CARGO_TARGET_$($cargoTargetKey)_RUSTFLAGS", '-C link-arg=-Wl,-z,max-page-size=16384', 'Process')
    [Environment]::SetEnvironmentVariable("CC_$compilerTargetKey", $linkerPath, 'Process')
    [Environment]::SetEnvironmentVariable("AR_$compilerTargetKey", (Join-Path $toolchainPath 'llvm-ar.exe'), 'Process')
    $cargoArgs = @('build', '--locked', '--manifest-path', (Join-Path $androidRoot 'native/Cargo.toml'), '--target', $targetName)
    if (-not $Debug) { $cargoArgs += '--release' }
    & cargo @cargoArgs
    if ($LASTEXITCODE -ne 0) { throw "Rust build failed for $abi" }
    $outputPath = Join-Path $androidRoot "app/src/main/jniLibs/$abi"
    New-Item -ItemType Directory -Path $outputPath -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $androidRoot "native/target/$targetName/$profileName/libpuredub_android.so") -Destination $outputPath
}
