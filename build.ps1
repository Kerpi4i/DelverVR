param([string]$GamePath='C:\Program Files (x86)\Steam\steamapps\common\Delver')
$ErrorActionPreference='Stop'
Set-Location $PSScriptRoot
$jdk=(Get-ChildItem "$PSScriptRoot/tools" -Directory -Filter 'jdk*' | Select-Object -First 1).FullName
if(!$jdk){throw 'A JDK is required in tools/jdk-*.'}
$gameJar=Join-Path $GamePath 'delver.jar'
if(!(Test-Path -LiteralPath $gameJar)){throw "Game archive not found: $gameJar"}
New-Item -ItemType Directory -Force build/classes,build/patch,dist/lib | Out-Null
$classpath="$PSScriptRoot/lib/*;$gameJar"
& "$jdk/bin/javac.exe" --release 8 -encoding UTF-8 -cp $classpath -d build/classes (Get-ChildItem src/main/java/delver/vr/*.java).FullName
if($LASTEXITCODE){throw 'Java compilation failed'}
& "$jdk/bin/java.exe" -cp "build/classes;$classpath" delver.vr.PatchGame $gameJar "$PSScriptRoot/build/classes" "$PSScriptRoot/build/patch"
if($LASTEXITCODE){throw 'Game compatibility patch failed'}
& "$jdk/bin/jar.exe" cf dist/delver-vr.jar -C build/classes . -C build/patch .
if($LASTEXITCODE){throw 'Packaging failed'}
Copy-Item lib/*.jar dist/lib -Force
Copy-Item Launch-VR.ps1,Launch-VR.cmd dist -Force
(Get-FileHash -LiteralPath $gameJar -Algorithm SHA256).Hash | Set-Content dist/build-game.sha256
if(Test-Path README.uk.md){Copy-Item README.uk.md dist -Force}
Write-Host 'Built dist/delver-vr.jar'
