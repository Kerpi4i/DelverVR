param([string]$GamePath='C:\Program Files (x86)\Steam\steamapps\common\Delver')
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot
$fixture=Join-Path $root ('build/installer-test-'+[Guid]::NewGuid().ToString('N'))
$game=Join-Path $fixture 'Steam library/Delver Україна'
if(Test-Path $fixture){throw 'Installer test directory already exists. Use a fresh build/installer-test directory.'}
New-Item -ItemType Directory -Force $game | Out-Null
Copy-Item (Join-Path $GamePath 'delver.jar') $game
# Test runtime is a read-only reference; installer only writes the sibling DelverVR directory.
New-Item -ItemType Junction -Path (Join-Path $game 'jre-x64-windows') -Target (Join-Path $GamePath 'jre-x64-windows') | Out-Null
$installer=Join-Path $root 'release/DelverVR-Setup-0.2.0-beta.5.exe'
$original=(Get-FileHash (Join-Path $game 'delver.jar')).Hash
function Run-TestExe([string]$Exe,[string]$Arguments,[int]$Expected=0){
    $process=Start-Process -FilePath $Exe -ArgumentList $Arguments -WindowStyle Hidden -PassThru
    if(!$process.WaitForExit(120000)){$process.Kill();throw "Timed out: $Exe"}
    if($process.ExitCode -ne $Expected){throw "Unexpected exit code $($process.ExitCode), expected $Expected : $Exe"}
}
Run-TestExe $installer ('/install "'+$game+'" /quiet')
$installed=Join-Path $game 'DelverVR'
foreach($name in @('DelverVR.exe','DelverVR-Uninstall.exe','patch.jar','mod.jar','.delver-vr-install','installed-files.txt')){
    if(!(Test-Path (Join-Path $installed $name))){throw "Missing installed file: $name"}
}
Run-TestExe (Join-Path $installed 'DelverVR.exe') '--check'
Run-TestExe (Join-Path $installed 'DelverVR.exe') '--simulate-smoke'
$smoke=Join-Path $env:TEMP 'DelverVR-smoke/delver-vr.log'
if(!(Select-String -Path $smoke -Pattern 'SMOKE PASS: 180 frames, GL error=0')){throw 'Installed direct EXE failed its real-game smoke test'}
Copy-Item $smoke (Join-Path $fixture 'installed-smoke.log')
# Reinstall retains a rollback copy instead of deleting user-added installation files.
Set-Content -LiteralPath (Join-Path $installed 'user-note.txt') 'untracked sentinel'
Run-TestExe $installer ('/install "'+$game+'" /quiet')
$backup=Get-ChildItem -LiteralPath $game -Directory -Filter 'DelverVR.backup-*' | Select-Object -First 1
if(!$backup -or !(Test-Path (Join-Path $backup.FullName 'user-note.txt'))){throw 'Previous installation was not preserved'}
Run-TestExe (Join-Path $installed 'DelverVR.exe') '--check'
# Unknown game versions must fail without changing the installed version.
$installedHash=(Get-FileHash (Join-Path $installed 'DelverVR.exe')).Hash
$stream=[System.IO.File]::Open((Join-Path $game 'delver.jar'),[System.IO.FileMode]::Append)
try{$stream.WriteByte(0)}finally{$stream.Dispose()}
Run-TestExe $installer ('/install "'+$game+'" /quiet') 1
Run-TestExe (Join-Path $installed 'DelverVR.exe') '--check' 1
if((Get-FileHash (Join-Path $installed 'DelverVR.exe')).Hash -ne $installedHash){throw 'Failed installation changed existing files'}
Copy-Item (Join-Path $GamePath 'delver.jar') (Join-Path $game 'delver.jar') -Force
$invalid=Join-Path $fixture 'not-a-game';New-Item -ItemType Directory -Force $invalid | Out-Null
Run-TestExe $installer ('/install "'+$invalid+'" /quiet') 1
if(Test-Path (Join-Path $invalid 'DelverVR')){throw 'Invalid directory was changed'}
$sentinel=Join-Path $env:LOCALAPPDATA 'DelverVR/installer-test-save-sentinel.txt'
Set-Content -LiteralPath $sentinel 'keep this profile'
Set-Content -LiteralPath (Join-Path $installed 'keep-user-file.txt') 'keep this file'
Run-TestExe (Join-Path $installed 'DelverVR-Uninstall.exe') '/quiet'
$limit=(Get-Date).AddSeconds(20)
while((Test-Path (Join-Path $installed 'DelverVR.exe')) -and (Get-Date) -lt $limit){Start-Sleep -Milliseconds 200}
if(Test-Path (Join-Path $installed 'DelverVR.exe')){throw 'Uninstall did not finish'}
if(!(Test-Path $sentinel) -or !(Test-Path (Join-Path $installed 'keep-user-file.txt'))){throw 'Uninstall removed untracked data'}
Run-TestExe $installer ('/install "'+$game+'" /quiet')
Run-TestExe (Join-Path $installed 'DelverVR.exe') '--check'
Run-TestExe (Join-Path $installed 'DelverVR-Uninstall.exe') '/quiet'
$limit=(Get-Date).AddSeconds(20)
while((Test-Path $installed) -and (Get-Date) -lt $limit){Start-Sleep -Milliseconds 200}
if(Test-Path $installed){throw 'Clean uninstall left a managed installation behind'}
Remove-Item -LiteralPath $sentinel
if((Get-FileHash (Join-Path $game 'delver.jar')).Hash -ne $original){throw 'Original game archive changed'}
if((Get-ChildItem -LiteralPath $game -Directory -Filter 'DelverVR.installing-*').Count){throw 'Staging directory was left behind'}
'INSTALLER PASS: clean install, Unicode/spaces path, bundled-JRE local patching, direct EXE 180 frames GL=0, update backup, unsupported hash rollback, invalid path, managed uninstall, saves/untracked files retained, original game unchanged' | Tee-Object -FilePath (Join-Path $root 'release/VALIDATION.txt')
