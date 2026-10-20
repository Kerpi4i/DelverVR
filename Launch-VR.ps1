param([string]$GamePath='C:\Program Files (x86)\Steam\steamapps\common\Delver',[switch]$Check)
$ErrorActionPreference='Stop'
$base=$PSScriptRoot
if(!(Test-Path -LiteralPath "$base/delver-vr.jar")){$base=Join-Path $base 'dist'}
$gameJar=Join-Path $GamePath 'delver.jar'
$java=Join-Path $GamePath 'jre-x64-windows/bin/java.exe'
if(!(Test-Path -LiteralPath $gameJar)){throw "Delver not found: $gameJar"}
if(!(Test-Path -LiteralPath $java)){throw "Bundled Java not found: $java"}
# Keep all options and saves local to the VR edition.
$save=Join-Path $base 'vr-save'
New-Item -ItemType Directory -Force $save | Out-Null
$hash=(Get-FileHash -LiteralPath $gameJar -Algorithm SHA256).Hash
if(Test-Path -LiteralPath "$base/build-game.sha256"){
    $expected=(Get-Content -LiteralPath "$base/build-game.sha256" -Raw).Trim()
    if($expected -ne $hash){throw 'Delver has changed since this VR build. Run build.ps1 again before launching.'}
}
"Steam archive SHA256: $hash" | Set-Content (Join-Path $base 'game-version.txt')
if($Check){Write-Host "Game: $gameJar`nJava: $java`nVR saves: $save`nSHA256: $hash";exit 0}
Set-Location -LiteralPath $save
# Windows PowerShell 5.1 wraps native stderr in ErrorRecord objects. Delver
# writes recoverable asset lookups there, so Stop would abort the pipeline
# and close the game even when Java has not failed. Keep stderr as log text
# and decide success from the Java process exit code instead.
$savedErrorPreference=$ErrorActionPreference
$PSNativeCommandUseErrorActionPreference=$false
try {
    $ErrorActionPreference='Continue'
    & $java '-Xmx2G' -cp "$base/delver-vr.jar;$base/lib/*;$gameJar" delver.vr.Launcher 2>&1 |
        ForEach-Object { $_.ToString() } |
        Tee-Object -FilePath (Join-Path $base 'delver-vr.log')
    $javaExitCode=$LASTEXITCODE
} finally {
    $ErrorActionPreference=$savedErrorPreference
}
if($javaExitCode){Write-Host "VR launch failed (exit $javaExitCode); see $base/delver-vr.log";exit $javaExitCode}
