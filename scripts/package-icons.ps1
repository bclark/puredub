$ErrorActionPreference = 'Stop'
# Platform packaging only: resize the existing artwork, then wrap PNG payloads.
Add-Type -AssemblyName System.Drawing
$assetRoot = Join-Path $PSScriptRoot '..\assets'
$source = [System.Drawing.Image]::FromFile((Join-Path $assetRoot 'puredub.png'))
$sizes = @(16,24,32,48,64,128,256)
$payloads = @()
foreach ($size in $sizes) {
    $bitmap = [System.Drawing.Bitmap]::new($size,$size)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graphics.DrawImage($source,0,0,$size,$size)
    $stream = [System.IO.MemoryStream]::new()
    $bitmap.Save($stream,[System.Drawing.Imaging.ImageFormat]::Png)
    $payloads += ,$stream.ToArray()
    $graphics.Dispose(); $bitmap.Dispose(); $stream.Dispose()
}
$output = [System.IO.MemoryStream]::new()
$writer = [System.IO.BinaryWriter]::new($output)
$writer.Write([uint16]0); $writer.Write([uint16]1); $writer.Write([uint16]$sizes.Count)
$offset = 6 + 16*$sizes.Count
for ($i=0; $i -lt $sizes.Count; $i++) {
    $dimension = if ($sizes[$i] -eq 256) {0} else {$sizes[$i]}
    $writer.Write([byte]$dimension); $writer.Write([byte]$dimension)
    $writer.Write([byte]0); $writer.Write([byte]0)
    $writer.Write([uint16]1); $writer.Write([uint16]32)
    $writer.Write([uint32]$payloads[$i].Length); $writer.Write([uint32]$offset)
    $offset += $payloads[$i].Length
}
foreach ($payload in $payloads) {$writer.Write([byte[]]$payload)}
[System.IO.File]::WriteAllBytes((Join-Path $assetRoot 'puredub.ico'),$output.ToArray())
$writer.Dispose(); $output.Dispose()
# ic10 is a 1024-pixel PNG representation in an ICNS container.
$macBitmap = [System.Drawing.Bitmap]::new(1024,1024)
$macGraphics = [System.Drawing.Graphics]::FromImage($macBitmap)
$macGraphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$macGraphics.DrawImage($source,0,0,1024,1024)
$macStream = [System.IO.MemoryStream]::new()
$macBitmap.Save($macStream,[System.Drawing.Imaging.ImageFormat]::Png)
$png = $macStream.ToArray()
$macGraphics.Dispose(); $macBitmap.Dispose(); $macStream.Dispose(); $source.Dispose()
$chunkLength = [BitConverter]::GetBytes([uint32]($png.Length+8)); [Array]::Reverse($chunkLength)
$totalLength = [BitConverter]::GetBytes([uint32]($png.Length+16)); [Array]::Reverse($totalLength)
$icns = [System.Text.Encoding]::ASCII.GetBytes('icns') + $totalLength + [System.Text.Encoding]::ASCII.GetBytes('ic10') + $chunkLength + $png
[System.IO.File]::WriteAllBytes((Join-Path $assetRoot 'puredub.icns'),$icns)
