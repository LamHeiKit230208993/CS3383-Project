param(
    [string]$Chrome = 'C:/Program Files/Google/Chrome/Application/chrome.exe',
    [ValidateSet('use-case', 'class', 'sequence')]
    [string[]]$Names = @('use-case', 'class', 'sequence')
)

# The CLI uses a local loopback server. No public diagram service is used.
# Its diagram PNG is transparent; flatten onto the Print theme's white ground.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
foreach ($name in $Names) {
    $source = Join-Path $PSScriptRoot "$name.puml"
    foreach ($format in @('svg', 'png')) {
        $output = Join-Path $PSScriptRoot "$name.$format"
        npx.cmd --yes '@markdown-viewer/documd@5.3.2' $source $output --chrome $Chrome
        if ($LASTEXITCODE -ne 0) { throw "UML export failed: $name.$format" }
    }
    $svgPath = Join-Path $PSScriptRoot "$name.svg"
    $svg = [IO.File]::ReadAllText($svgPath)
    $svg = [regex]::Replace($svg, '(<svg\b[^>]*>)', '$1<rect width="100%" height="100%" fill="#ffffff"/>', 1)
    [IO.File]::WriteAllText($svgPath, $svg)

    $pngPath = Join-Path $PSScriptRoot "$name.png"
    $original = [Drawing.Image]::FromFile($pngPath)
    $white = New-Object Drawing.Bitmap($original.Width, $original.Height)
    $graphics = [Drawing.Graphics]::FromImage($white)
    try {
        $graphics.Clear([Drawing.Color]::White)
        $graphics.DrawImage($original, 0, 0, $original.Width, $original.Height)
        $original.Dispose()
        $white.Save($pngPath, [Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $original.Dispose()
        $graphics.Dispose()
        $white.Dispose()
    }
}
