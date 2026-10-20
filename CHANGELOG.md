# Changelog

## 0.2.0-beta.3 — 2026-10-20

- Fix bomb loss after pickup: inventory items being inactive in the world are
  no longer mistaken for detonated bombs. Equipped ticking temporarily enables
  the native bomb tick without triggering automatic pickup, then restores the
  inventory state.
- Grip picks ground items into the hand. Action trigger with an empty hand
  collects ground items into inventory without auto-equipping or consuming.
- Action trigger while holding a bomb lights its fuse before other contextual
  actions. Existing throwing behavior is retained.
- Regression test now starts with native ground pickup, verifies inactive
  inventory state, then follows holding, lighting, throwing and detonation.

## 0.2.0-beta.2 — 2026-10-20

- Action trigger lights held bombs; physical throws arm unlit bombs.
- Bomb fuses tick once per game update in either tracked hand, at the hand's
  position. Expired bombs are removed from the hand and inventory. Native wet
  fuses and dud behavior are preserved; placing an unlit bomb does not arm it.
- Inventory clicks use food/potions/scrolls even when the item is already
  selected. Native drag-and-drop and weapon/armor equip behavior are retained.
- Regression tests exercise native bomb countdowns and real inventory click
  events, including selected items and dragging without consumption.
- Test script accepts `-GamePath` for isolated/custom game installations.

## 0.2.0-beta.1 — 2026-10-20

- EXE installer: game folder selection and local compatibility patch generation.
- Direct `DelverVR.exe` for non-Steam/SteamVR shortcuts; no launcher window.
- Per-user saves/logs, managed uninstall, retained previous-install backups.
- Tracked headset/controllers, physical item interaction and combat.
- Wrist hotbar/map, native inventory pointer, throws, bow poses and textured hands.
- Comfortable steps, calibrated stick speed and ladder movement.
- Shared mesh preparation; experimental shadow batching is opt-in on PC.

Known issues: frame drops with shadows, limited headset/controller coverage,
no integrated Steam achievements/Workshop API. No standalone Quest APK is
included. This is a public beta, not a performance-certified release.
