param([string]$GamePath='C:\Program Files (x86)\Steam\steamapps\common\Delver')
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot
Push-Location $root
try {
    & ./build.ps1 -GamePath $GamePath
    if($LASTEXITCODE){throw 'Mod compilation failed'}
    $jdk=(Get-ChildItem tools -Directory -Filter 'jdk*' | Select-Object -First 1).FullName
    $csc="$env:WINDIR/Microsoft.NET/Framework64/v4.0.30319/csc.exe"
    if(!(Test-Path $csc)){throw 'Windows .NET Framework C# compiler is required.'}
    $releaseRoot=Join-Path $root 'build/release'
    if(Test-Path $releaseRoot){
        $resolved=(Resolve-Path $releaseRoot).Path
        if($resolved -ne (Join-Path $root 'build\release')){throw 'Unexpected release staging directory'}
        Remove-Item -LiteralPath $resolved -Recurse -Force
    }
    $payload=Join-Path $releaseRoot 'payload'
    New-Item -ItemType Directory -Force "$payload/lib","$payload/licenses",release | Out-Null
    # Only our own classes. Patched game/framework classes are generated on install.
    & "$jdk/bin/jar.exe" cf "$payload/mod.jar" -C build/classes .
    if($LASTEXITCODE){throw 'Mod packaging failed'}
    Copy-Item lib/*.jar "$payload/lib" -Force
    Copy-Item dist/build-game.sha256 "$payload/supported-game.sha256"
    Copy-Item LICENSE,README.md,README.uk.md "$payload" -Force
    Copy-Item packaging/THIRD-PARTY.md "$payload" -Force
    Copy-Item packaging/licenses/* "$payload/licenses" -Force
    $alSource=Join-Path $payload 'licenses/openal-soft-1.23.1-source.zip'
    $cachedSource=Join-Path $root 'tools/openal-soft-1.23.1-source.zip'
    if(!(Test-Path $cachedSource)){Invoke-WebRequest https://codeload.github.com/kcat/openal-soft/zip/refs/tags/1.23.1 -OutFile $cachedSource}
    Copy-Item $cachedSource $alSource
    $common=@('/nologo','/target:winexe','/platform:x64','/optimize+',('/win32manifest:'+(Join-Path $PSScriptRoot 'app.manifest')),'/r:System.Windows.Forms.dll','/r:System.Drawing.dll')
    & $csc @common "/out:$payload\DelverVR.exe" (Join-Path $PSScriptRoot 'Start.cs') (Join-Path $PSScriptRoot 'AssemblyInfo.cs')
    if($LASTEXITCODE){throw 'Direct VR executable compilation failed'}
    $compression=@('/r:System.IO.Compression.dll','/r:System.IO.Compression.FileSystem.dll')
    & $csc @common @compression /define:UNINSTALL "/out:$payload\DelverVR-Uninstall.exe" (Join-Path $PSScriptRoot 'Setup.cs') (Join-Path $PSScriptRoot 'AssemblyInfo.cs')
    if($LASTEXITCODE){throw 'Uninstaller compilation failed'}
    # Use .NET ZIP so dotfiles/managed-install markers are included on Windows.
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip=Join-Path $releaseRoot 'payload.zip'
    [System.IO.Compression.ZipFile]::CreateFromDirectory($payload,$zip,[System.IO.Compression.CompressionLevel]::Optimal,$false)
    $output=Join-Path $root 'release/DelverVR-Setup-0.2.0.exe'
    & $csc @common @compression "/resource:$zip,payload.zip" "/out:$output" (Join-Path $PSScriptRoot 'Setup.cs') (Join-Path $PSScriptRoot 'AssemblyInfo.cs')
    if($LASTEXITCODE){throw 'Installer compilation failed'}
    & python packaging/verify-release.py $zip
    if($LASTEXITCODE){throw 'Installer payload verification failed'}
    $hash=(Get-FileHash $output -Algorithm SHA256).Hash.ToLowerInvariant()
    "$hash  DelverVR-Setup-0.2.0.exe" | Set-Content -Encoding ascii release/SHA256SUMS.txt
    Write-Host "Built $output"
}finally{Pop-Location}
