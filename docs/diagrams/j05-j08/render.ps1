param(
    [string]$PlantUmlJar = (Join-Path $env:USERPROFILE '.vscode/extensions/jebbs.plantuml-2.18.1/plantuml.jar'),
    [string]$Java = 'java',
    [ValidateSet('use-case', 'class', 'application-class', 'sequence')]
    [string[]]$Names = @('use-case', 'class', 'application-class', 'sequence')
)

$ErrorActionPreference = 'Stop'
if (-not (Test-Path -LiteralPath $PlantUmlJar -PathType Leaf)) {
    throw "PlantUML JAR not found. Pass -PlantUmlJar with the official JAR path."
}
$version = (& $Java -jar $PlantUmlJar --version | Out-String)
Write-Output ($version.Trim() | Select-Object -First 1)
if ($LASTEXITCODE -ne 0 -or $version -notmatch 'PlantUML version 1\.\d{4}\.\d+') {
    throw 'A working official PlantUML JAR is required for these exports.'
}
$major = [int]($Matches[0] -replace '.*1\.(\d{4}).*', '$1')
if ($major -ne 2026) {
    Write-Warning "Rendering with PlantUML $($Matches[0]) (project exports historically used 1.2026.9). Record the version in Test/history.md."
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
    & $Java '-Djava.awt.headless=true' -jar $PlantUmlJar --charset UTF-8 --no-error-image -o $PSScriptRoot "-t$format" @sourceFiles
    if ($LASTEXITCODE -ne 0) { throw "PlantUML $format export failed." }
    foreach ($name in $Names) {
        $output = Join-Path $PSScriptRoot "$name.$format"
        if (-not (Test-Path -LiteralPath $output -PathType Leaf) -or (Get-Item -LiteralPath $output).Length -eq 0) {
            throw "PlantUML produced no usable output: $output"
        }
        $versionLine = ($version -split "\r?\n" | Where-Object { $_ -match 'PlantUML version' } | Select-Object -First 1).Trim()
        Write-Output "Exported $name.$format ($versionLine)"
    }
}
