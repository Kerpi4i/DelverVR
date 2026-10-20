# Delver VR

Unofficial Windows PC VR mod for the Steam version of **Delver**.
Tracked headset/controllers, physical interaction and combat, left-arm hotbar,
right-arm map, and the original inventory adapted for VR.

**[Download the installer](https://github.com/Kerpi4i/DelverVR/releases)** ·
[Українська](README.uk.md) · [Report a bug](https://github.com/Kerpi4i/DelverVR/issues)

**Public beta**, primarily tested with Meta Quest 2 Touch controllers through
a PC and SteamVR. This is not a standalone Quest APK. Other headsets,
controllers and third-party Delver mods are not fully tested. Shadows can
cause substantial frame drops.

## Requirements

- Windows 10/11 x64, SteamVR and a working PC VR connection.
- Your own Steam copy of Delver, with its bundled Windows x64 Java runtime.
- Quest 2: connect through Quest Link/Air Link or another SteamVR-capable PC connection.
- No separate Java/JDK, Python or PowerShell setup is needed to play.

## Install

1. Download `DelverVR-Setup-0.2.0-beta.4.exe` from Releases and run it.
2. Select the **game folder containing `delver.jar`**, for example
   `C:\Program Files (x86)\Steam\steamapps\common\Delver`.
3. Install. The installer creates a `DelverVR` subfolder and generates patches
   locally. Original game files are not overwritten. Windows may request
   administrator permission for Program Files.
4. Start SteamVR and run `<Delver folder>\DelverVR\DelverVR.exe`.
   The game starts directly: no launcher menu or console window.

The beta installer is not code-signed; Windows may show an unknown publisher.
Use this repository's Releases and verify the file against `SHA256SUMS.txt`.

## Add to SteamVR

1. Steam → **Games → Add a Non-Steam Game to My Library → Browse**.
2. Select `<Delver folder>\DelverVR\DelverVR.exe`; name the shortcut **Delver VR**.
3. In its Steam properties enable **Include in VR Library**, if shown.
4. Launch from SteamVR. No launch arguments are needed.

The VR shortcut option can vary by Steam client version. The same EXE also
works directly with SteamVR running. This non-Steam shortcut does not enable
Steam achievements or Workshop API integration.

To change floors, stand near the stairs, point a controller at them and press the index trigger.

## Touch controls

| Action | Control |
|---|---|
| Walk in head-facing direction | Left stick |
| Snap turn | Right stick left/right |
| Open inventory | BAG beside left-arm hotbar; right grip/trigger |
| Menu/inventory pointer | Right ray; trigger to click, hold and move to drag |
| Grab nearby item / hotbar slot | Grip |
| Collect a ground item into inventory | Trigger with an empty hand |
| Store / throw | Release grip near body / during a throwing swing |
| Use item / weapon | Trigger; tracked swings for melee |
| Bomb in hand | Trigger lights fuse; a physical throw also lights it |
| Bow | Hold bow, draw with other hand, release trigger |
| Ladder | Left stick forward/back while touching ladder; sideways to dismount |
| Pause / close inventory | Left X/menu action |
| Recenter | Right A |

## Saves, updates and uninstall

- Saves/settings: `%LOCALAPPDATA%\DelverVR\save`.
- Game log: `%LOCALAPPDATA%\DelverVR\delver-vr.log`.
- Install log: `%LOCALAPPDATA%\DelverVR\installer.log`.

VR saves are separate from flat-game saves. To migrate development saves,
copy the old `dist\vr-save\save` contents to the new save folder with the
game closed. Back up both folders first.

Run `DelverVR\DelverVR-Uninstall.exe` to remove managed mod files. Original
game files, VR saves and untracked files are preserved. Remove the Steam
shortcut through Steam separately. An update keeps the previous managed
installation in a `DelverVR.backup-*` folder beside the game; those backups
are not automatically deleted.

## Compatibility and limitations

Supported `delver.jar` SHA256:
`A2D58E87B09F588FF8389508E43ACCF7D3C6CE949B5AA4D31D6380574EC095AE`.
Unsupported builds are rejected. A Steam game update may require a new mod
release. Please report problems with headset/GPU details, reproduction steps
and a log reviewed for private information.

The PC release uses original shadows by default. Experimental batching is
present in source, but is not a guaranteed performance fix. Motion comfort
and performance still need broader hardware testing.

## Build from source

With your own Steam installation, on Windows:

```powershell
.\bootstrap.ps1
.\test.ps1
.\packaging\build-installer.ps1 -GamePath 'D:\SteamLibrary\steamapps\common\Delver'
```

For a custom game path, use `build.ps1 -GamePath ...` and `test.ps1 -GamePath ...`.
Development bootstrap downloads a JDK/dependencies;
these tools are not needed by players. Installer output is in `release/`.
The EXEs use the .NET Framework compiler supplied with Windows. Hosted CI
cannot build against proprietary game data without a user-supplied copy.

See [CONTRIBUTING.md](CONTRIBUTING.md) and [third-party notices](packaging/THIRD-PARTY.md).
Original mod code is MIT licensed; Delver and its assets belong to Priority
Interrupt. This mod is not endorsed by Priority Interrupt, Valve or Meta.
