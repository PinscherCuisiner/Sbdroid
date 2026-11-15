# SbDroid (v0.2)

Android launcher shell for Starbound: your own packed.pak, mod manager, full touch/gamepad controls.
**The game engine is NOT included** – `engine/GameEngine.kt` is a stub (see "Engine" below).

## Features
- Select your own `packed.pak` (checked for the "SBAsset6" header, copied to app storage)
- Mod manager: import .pak, enable/disable, reorder. On launch, enabled mods are staged as
  `001_name.pak, 002_name.pak ...` + a generated `sbinit.config` (assetDirectories / storageDirectory / logDirectory)
- Controls: 2 joysticks, attack/alt/jump/use, hotbar 1-6 (7-0 hidden, enable them), inventory, chat, map,
  crafting, codex, quests, beam, drop, swap, walk, zoom, screenshot, 8-emote wheel
- Customize: drag, resize, opacity, show/hide, Reset, Mirror (left-handed). Saved as screen fractions
- Gamepad: sticks = move/aim, A jump, B alt, X use, Y inventory, R1/R2 attack, L1/L2 alt,
  Start chat, Select map, D-pad L/R hotbar prev/next

## Build WITHOUT a PC (from your phone)
1. Install Termux from F-Droid. Then:
   ```
   termux-setup-storage
   pkg update && pkg install git unzip
   cd ~/storage/downloads && unzip SbDroid.zip && cd SbDroid
   git init && git add . && git commit -m "SbDroid"
   git branch -M main
   git remote add origin https://github.com/YOUR_NAME/SbDroid.git
   git push -u origin main      # password = a GitHub personal access token
   ```
   (create the empty repo first on github.com; token: Settings > Developer settings > Tokens, scope "repo" + "workflow")
2. GitHub > your repo > Actions > "Build APK" (it also runs on every push).
3. Open the finished run > Artifacts > `SbDroid-debug-apk` > download, unzip, install the .apk
   (allow "install unknown apps" for your browser/file manager).

Each change = edit file (in Termux with `nano`, or in the Code Editor app) > `git commit -am x && git push` > new APK in ~3-5 min.

## Engine
Hook an OpenStarbound port in by implementing `GameEngine`:
`start(config)` receives packed.pak, staged mods and sbinit.config; `send()` receives every touch/gamepad event.

## Engine (oSBM integrated)
oSBM is vendored in `engine/oSBM`. CI builds its arm64 `libmain.so` (CMake + vcpkg, first run is slow),
Gradle packs it with oSBM's SDL Java host, and `OsbmEngine` launches that activity from SbDroid.
oSBM brings its own in-game controls; SbDroid's overlay is only used by the stub engine.
