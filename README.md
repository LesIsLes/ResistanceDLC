# ResistanceDLC

[![GitHub release](https://img.shields.io/github/v/release/LesIsLes/ResistanceDLC?include_prereleases&sort=semver)](https://github.com/LesIsLes/ResistanceDLC/releases)
[![GitHub issues](https://img.shields.io/github/issues/LesIsLes/ResistanceDLC)](https://github.com/LesIsLes/ResistanceDLC/issues)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11%20%7C%2026.1-blue)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Mod%20Loader-Fabric-dbd0b4)](https://fabricmc.net/)

A lightweight client-side visual mod for **Minecraft 1.21.11 and 26.1 (Fabric)** with **48+ features** across 6 sections: **HUD, PvP, PvE, Visual, Misc, and Music**.

Everything is **100% client-side** - no server modifications, no packet manipulation, no anti-cheat bypass. Just visual quality-of-life improvements for your game.

---

## 📥 Installation

1. Install **Fabric Loader** from [fabricmc.net](https://fabricmc.net/use/installer/).
2. Download **Fabric API** from [Modrinth](https://modrinth.com/mod/fabric-api) or [CurseForge](https://www.curseforge.com/minecraft/mc-mods/fabric-api).
3. Download the latest **ResistanceDLC** release from [Releases](https://github.com/LesIsLes/ResistanceDLC/releases).
4. Drop both `.jar` files into your `.minecraft/mods/` folder.
5. Launch Minecraft with the **Fabric** profile.

### Requirements

**For Minecraft 26.1:**

- Minecraft 26.1
- Fabric Loader 0.18.4+
- Fabric API 0.144.0+26.1
- Java 25+

**For Minecraft 1.21.11:**

- Minecraft 1.21.11
- Fabric Loader 0.19.5+
- Fabric API (latest for 1.21.11)
- Java 21+

---

## ✨ Features

### 🎯 PvP Tools

- **PvPSafe** - blocks `/hub`, `/logout`, `/suicide` during combat
- **AutoGG** - auto-sends "GG" after kills. Works via chat parsing OR via entity death event (no chat required)
- **AutoSwap** - legitimate offhand item swapping without packet manipulation. Offhand <-> inventory swap
- **FastExp** - fast XP bottle usage
- **ShiftTap** - crits via shift
- **Custom Hit Sounds** - 7 built-in presets (user-provided)
- **Totem Log** - logs broken totems with attacker name
- **PickUpLogger** - tracks valuable pickups
- **AutoTPAccept** - auto-accepts `/tpa` from friends or all players
- **StatsTracker** - K/D counter parsed from chat (displayed in GUI)
- **KillStreak** - consecutive kill counter + custom sounds (user-provided)
- **TargetESP** - 4 variants (Crystals, Cubes, Ring, Ghosts) - 3D target highlight

### 🎨 Visual

- **Zoom** - smooth zoom with keybind. Frame-rate independent (works the same on 30 or 240 FPS)
- **Custom Crosshair** - 5 shapes, full color control
- **Strike Range** - shows attack distance in real time
- **Custom Hitbox** - colored debug hitboxes
- **Aspect Ratio** - stretch FOV
- **Low Fire / Shield** - lower flames and shield
- **Particle Blocker** - hide particles by category
- **Waypoints** - on-screen markers with distance
- **Item Physics** - items lie flat
- **Predictions** - projectile trajectory (bow, crossbow, multishot, snowball, potion, trident)
- **EnchantHighlight** - highlight selected enchants in item tooltips with custom color + bold
- **GammaUtil** - brightness above vanilla limit
- **Jump Circles** - rings/hexagons/stars under feet on jump (line-based, no textures)
- **Crosshair Heatmap** - visualize your hit/miss pattern around the crosshair
  - Hit detection via AABB.clip, miss at 3.1 blocks forward
  - Statistics: hit %, miss %, winrate %, total shots

### 🖥️ HUD

- Coordinates, biome, time, FPS, ping, TPS, BPS
- Potion effects with icons and timers
- Equipment HUD with durability
- Combo Counter
- Cooldowns
- Effect Warnings
- Low HP Alert - warning + sound when HP is below threshold
- Armor Alert - warning when armor durability is low
- Mod Logo & Name - custom logo with position editor
- KillStreak HUD - on-screen kill streak counter with timer
- HUD Editor - drag-n-drop layout editor (F6)
- Ping Indicator - player ping with progress bar in tab-list (below nickname)

### ⚙️ Misc

- **ChatFilter** - regex-based chat filtering (dynamic panel height)
- **AutoReconnect** - reconnects after kicks/bans/crashes
- **DeathCoords** - saves death location
- **Death Recap** - last 5 deaths with reason, killer, coords, dimension, time, and full armor loadout
- **GUI Themes** - 5 themes (Vanilla, Dark, Neon, Candy, Blood)
- **Config Manager** - save/load configs
- **Macros** - 5 command/message slots (F1-F5 by default)
- **AutoRespawn** - automatically respawns after death
- **Friend List** - add friends by nickname, highlights them in chat + HUD widget
- **SmartChat** - groups repeated chat messages + clickable coordinates
  - Groups same sender + text within 60 sec window
  - Badge xN + tooltip with timestamps
  - Clickable coords (5 formats): LMB copies to clipboard, [wp] inserts `/wp add x y z`
  - New `/wp add <x> <y> <z>` command
- **InventoryManager** - save and preview inventory snapshots
  - 5 slots, files in `config/resistancedlc/inventories/`
  - 41 slots (4 armor + offhand + 9 hotbar + 27 main)
  - Steve preview with equipped armor, hotbar, and main inventory
  - Save / Rename / Delete / Open folder via mini-screens

### 🎤 Voice Commands (New in 3.0.0)

Control the mod with your voice. No keyboard needed.

- 4 built-in commands: open GUI, close GUI, change language, open HUD editor
- MFCC + DTW recognition pipeline (same tech as Shazam, but simpler)
- Voice Activity Detection with calibration
- Works even when GUI is open (except chat)
- Record your own commands via `/voicerecord`

### 🎥 Freelook (New in 3.0.0)

Third-person camera detached from body.

- Bind: LCTRL by default
- Sensitivity and distance settings
- Auto-disables when a screen is open
- Does NOT disable in combat (by design)

### 🌦️ Time & Weather Change (New in 3.0.0)

- **Time Change:** client-side time of day (0-24000 ticks). 6 presets: Sunrise, Day, Noon, Sunset, Night, Midnight
- **Weather Change:** 5 modes: Clear, Rain, Thunder, Snow, Overcast
- Fully client-side. Other players see the real time/weather

### 🎵 Music Player

- Play your own `.ogg` files from `config/resistancedlc/music/`
- Beautiful HUD widget with rotating vinyl
- Drag-to-adjust volume slider
- Repeat (off / one / all), shuffle, auto-skip broken files
- **Lyrics support:** `.lrc` files in `config/resistancedlc/lyrics/<track>.lrc`
  - 3D karaoke-style text rendering with fade in/out
  - LRCLIB integration - automatic lyrics download
  - Holographic, Shake, Entrance, Pulse, Outline, Gradient, Offset X

---

## 🎮 How to Use

- Press **G** to open the GUI
- **Left-click** an accordion item to expand it
- **Right-click** an accordion item to toggle the feature
- Click the **⚙ gear** icon inside panels to access settings
- Press **F6** to open the HUD Editor
- All features are **client-side only** - no server modifications

---

## ⚠️ About the "KillAura" feature

The "KillAura" button in the PvP section is an **easter egg / joke only**.

- It does **NOT** provide any gameplay advantage
- It does **NOT** send attack packets to the server
- It does **NOT** bypass anti-cheat
- It does **NOT** work as an actual cheat

It simply plays a "canceled" sound effect and displays a humorous message. It's a lighthearted nod to the absurdity of cheat clients - we're not cheaters, we're modders.

---

## 📁 Optional Files

### KillStreak sounds

To hear a sound when you reach a kill streak, place your own `.ogg` files in:
config/resistancedlc/sounds/

text

Supported filenames (by streak level):
killstreak_1.ogg
killstreak_2.ogg
killstreak_3.ogg
killstreak_4.ogg
killstreak_5.ogg
killstreak_8.ogg
killstreak_10.ogg

text

No built-in sounds included - the mod does not ship with any audio files. If a file is missing, the sound is simply skipped. You can use any OGG Vorbis files you have the rights to.

### Music Player

Place your `.ogg` files in `config/resistancedlc/music/`.

### Lyrics

Place your `.lrc` files in `config/resistancedlc/lyrics/` (filename must match the track name).

Or let the mod download lyrics automatically from **LRCLIB**.

### Voice Commands

Place your recorded `.wav` files in `config/resistancedlc/voice/`.

### InventoryManager

Snapshots are saved automatically to `config/resistancedlc/inventories/inventory_N.json`.

---

## 🌍 Languages

- 🇷🇺 Russian (Русский)
- 🇬🇧 English

Language is auto-detected from the game settings. Switch anytime in the mod's GUI (top-right button).

---

## 🛠️ Building from Source

### Requirements for development

- **JDK 25** (for 26.1) or **JDK 21** (for 1.21.11)
- **IntelliJ IDEA 2025.3+** (recommended)
- **Git**

git clone https://github.com/LesIsLes/ResistanceDLC.git
cd ResistanceDLC
./gradlew clean build --no-daemon
The output .jar will be in build/libs/.

Running in dev environment
bash
./gradlew runClient --no-daemon

🐛 Known Issues
MusicPlayer duration may show ~10-15% less than the actual track length (OGG Vorbis compression approximation)

AutoGG may not work on some servers if they don't send entity death events AND don't write kill messages to chat

Voice Commands are experimental and require microphone calibration

LRCLIB requires internet connection

🤝 Contributing
Contributions are welcome! To contribute:

Fork the repository.

Create a branch: git checkout -b feature/amazing-feature

Commit your changes: git commit -m "Add amazing feature"

Push to the branch: git push origin feature/amazing-feature

Open a Pull Request.

Please follow the existing code style and test your changes on both supported Minecraft versions where applicable.

📜 License
MIT License - free to use, modify, and distribute.

See LICENSE for details.

💬 Feedback & Support
Found a bug or want a new feature?

GitHub Issues: github.com/LesIsLes/ResistanceDLC/issues

Modrinth: modrinth.com/mod/resistancedlc

🙏 Credits
Author: lesis (@LesIsLes)

Special thanks:

Fabric team for the mod loader

Mojang for Minecraft

LRCLIB for the lyrics database

All contributors and testers

Made with ❤ by lesis