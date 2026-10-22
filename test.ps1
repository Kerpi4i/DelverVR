param([string]$GamePath='C:\Program Files (x86)\Steam\steamapps\common\Delver')
$ErrorActionPreference='Stop'
Set-Location $PSScriptRoot
$jdk=(Get-ChildItem tools -Directory -Filter 'jdk*' | Select-Object -First 1).FullName
$gameJar=Join-Path $GamePath 'delver.jar'
& "$jdk/bin/javac.exe" --release 8 -cp "build/classes;$gameJar;lib/*" -d build/test (Get-ChildItem src/test/java/delver/vr/*.java).FullName
if($LASTEXITCODE){throw 'Test compilation failed'}
& "$jdk/bin/java.exe" -cp "build/test;build/classes;$gameJar" delver.vr.SpatialTest
if($LASTEXITCODE){throw 'Spatial tests failed'}
& "$jdk/bin/java.exe" -cp "build/test;build/classes;$gameJar" delver.vr.RenderPreparationTest
if($LASTEXITCODE){throw 'Stereo preparation tests failed'}
& "$jdk/bin/java.exe" -cp "build/test;dist/delver-vr.jar;$gameJar" delver.vr.NativeUiInputTest
if($LASTEXITCODE){throw 'Inventory cursor state tests failed'}
$base="$PSScriptRoot/dist"
$testClasses="$PSScriptRoot/build/test"
New-Item -ItemType Directory -Force dist/smoke-save | Out-Null
Push-Location dist/smoke-save
try {
    & (Join-Path $GamePath 'jre-x64-windows\bin\java.exe') '-Ddelver.vr.simulate=true' '-Ddelver.vr.smoke=true' '-Ddelver.vr.regression=true' '-Ddelver.vr.shadowMode=parallel' -cp "$testClasses;$base/delver-vr.jar;$base/lib/*;$gameJar" delver.vr.Launcher > "$base/smoke.log" 2>&1
    if($LASTEXITCODE){throw 'Runtime smoke test failed; see dist/smoke.log'}
} finally {Pop-Location}
if(!(Select-String -Path dist/smoke.log -Pattern 'SMOKE PASS')){throw 'Smoke test did not finish'}
if(!(Select-String -Path dist/smoke.log -Pattern 'INTERACTION PASS')){throw 'Interaction regressions did not finish'}
if(!(Select-String -Path dist/smoke.log -Pattern 'SKY PASS')){throw 'Sky stereo state regression did not finish'}
if(!(Select-String -Path dist/smoke.log -Pattern 'LADDER PASS')){throw 'Ladder physics regressions did not finish'}
if(!(Select-String -Path dist/smoke.log -Pattern 'ITEM USE PASS')){throw 'Bomb/inventory regressions did not finish'}
if((Select-String -Path dist/smoke.log -Pattern 'SHADOW BATCH PASS').Count -ne 2){throw 'Shadow batch regressions did not finish'}
Get-Content dist/smoke.log -Tail 3


if(!(Select-String -Path dist/smoke.log -Pattern 'STAIRS PASS')){throw 'Stair regressions did not finish'}

if(!(Select-String -Path dist/smoke.log -Pattern 'VR SETTINGS PASS')){throw 'VR settings regressions did not finish'}

if(!(Select-String -Path dist/smoke.log -Pattern 'VR CONTROLS PASS')){throw 'VR controls regressions did not finish'}

if(!(Select-String -Path dist/smoke.log -Pattern 'WORKSHOP PASS')){throw 'Workshop regressions did not finish'}

if(!(Select-String -Path dist/smoke.log -Pattern 'NATIVE USE PASS')){throw 'Native mod interaction regressions did not finish'}

if(!(Select-String -Path dist/smoke.log -Pattern 'LIGHT SELECTION PASS')){throw 'Light selection regressions did not finish'}

if(!(Select-String -Path dist/smoke.log -Pattern 'INVENTORY EXPANSION PASS')){throw 'Inventory expansion regressions did not finish'}
