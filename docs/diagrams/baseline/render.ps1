param(
    [string]$PlantUmlJar = (Join-Path $env:USERPROFILE '.codex/tools/plantuml/1.2026.9/plantuml-1.2026.9.jar'),
    [string]$Java = 'java',
    [ValidateSet('use-case', 'class', 'application-class', 'sequence')]
    [string[]]$Names = @('use-case', 'class', 'application-class', 'sequence')
)

$ErrorActionPreference = 'Stop'
if (-not (Test-Path -LiteralPath $PlantUmlJar -PathType Leaf)) {
    throw "PlantUML JAR not found. Pass -PlantUmlJar with the official 1.2026.9 JAR path."
}
$version = (& $Java -jar $PlantUmlJar --version | Out-String)
if ($LASTEXITCODE -ne 0 -or $version -notmatch 'PlantUML version 1\.2026\.9\b') {
    throw 'Official PlantUML 1.2026.9 is required for these exports.'
}
$sourceFiles = @($Names | ForEach-Object {
    $source = Join-Path $PSScriptRoot "$_.puml"
    if (-not (Test-Path -LiteralPath $source -PathType Leaf)) { throw "Missing UML source: $source" }
    $source
})

# Check every selected source before replacing any existing exports.
& $Java '-Djava.awt.headless=true' -jar $PlantUmlJar --charset UTF-8 --check-syntax @sourceFiles
if ($LASTEXITCODE -ne 0) { throw 'PlantUML syntax check failed; existing exports were kept.' }
foreach ($format in @('svg', 'png')) {
    & $Java '-Djava.awt.headless=true' -jar $PlantUmlJar --charset UTF-8 --no-error-image --ignore-startuml-filename --format $format --output-dir $PSScriptRoot @sourceFiles
    if ($LASTEXITCODE -ne 0) { throw "Official PlantUML $format export failed." }
    foreach ($name in $Names) {
        $output = Join-Path $PSScriptRoot "$name.$format"
        if (-not (Test-Path -LiteralPath $output -PathType Leaf) -or (Get-Item -LiteralPath $output).Length -eq 0) {
            throw "PlantUML produced no usable output: $output"
        }
        Write-Output "Exported $name.$format (PlantUML 1.2026.9)"
    }
}
