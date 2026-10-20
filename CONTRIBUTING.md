# Contributing

This repository publishes the Windows PC mod. Standalone Quest experiments,
device telemetry, extracted game code, paid game data and local tools are not
part of this public source release.

Build/test against your own Steam copy. `bootstrap.ps1` downloads LWJGL 3.3.3,
libGDX backend 1.9.9, Javassist 3.30.2-GA and a development JDK. Use
`build.ps1 -GamePath ...` for a custom game path. `test.ps1` currently uses the
standard Steam path; adjust its local game path if needed.

Regression tests need desktop OpenGL. They run a simulated VR window against
real game physics and cover interaction, stereo lighting, locomotion,
inventory cursor state, ladder physics and shadow geometry equivalence.
They do not replace headset testing.

`packaging/build-installer.ps1` packages only original mod classes.
`packaging/verify-release.py` rejects game classes/assets in that payload.
Patched classes are generated locally on the player's PC. Never publish
`delver.jar`, extracted game code/data, saves, local logs, research APKs,
credentials, signing keys or device serial numbers.

The direct EXE stays alive until Java exits, allowing Steam to track the
session. Test clean install, updates, invalid paths/hashes, direct launch and
uninstall before release. Saves must stay outside the game directory.
Keep all third-party notices and corresponding OpenAL Soft source with binary
distributions. Game compatibility remains tied to the supported JAR hash.
