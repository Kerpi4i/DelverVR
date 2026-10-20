$ErrorActionPreference='Stop'
Set-Location $PSScriptRoot
New-Item -ItemType Directory -Force tools,lib | Out-Null
if(!(Get-ChildItem tools -Directory -Filter 'jdk*')){
    Invoke-WebRequest 'https://download.oracle.com/java/21/latest/jdk-21_windows-x64_bin.zip' -OutFile tools/jdk.zip
    Expand-Archive tools/jdk.zip tools -Force
}
$modules=@('lwjgl','lwjgl-glfw','lwjgl-jemalloc','lwjgl-openal','lwjgl-opengl','lwjgl-stb','lwjgl-openvr')
foreach($module in $modules){foreach($suffix in @('','-natives-windows')){
    $target="lib/$module$suffix.jar"
    if(!(Test-Path $target)){Invoke-WebRequest "https://repo.maven.apache.org/maven2/org/lwjgl/$module/3.3.3/$module-3.3.3$suffix.jar" -OutFile $target}
}}
if(!(Test-Path lib/gdx-backend-lwjgl3.jar)){Invoke-WebRequest https://repo.maven.apache.org/maven2/com/badlogicgames/gdx/gdx-backend-lwjgl3/1.9.9/gdx-backend-lwjgl3-1.9.9.jar -OutFile lib/gdx-backend-lwjgl3.jar}
if(!(Test-Path lib/javassist.jar)){Invoke-WebRequest https://repo.maven.apache.org/maven2/org/javassist/javassist/3.30.2-GA/javassist-3.30.2-GA.jar -OutFile lib/javassist.jar}
& ./build.ps1
