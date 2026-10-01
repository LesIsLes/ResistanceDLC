# Resistance DLC

A lightweight **visual client mod** for Minecraft **1.21.11** (Fabric) with **48+ features** across 6 sections: HUD, PvP, PvE, Visual, Misc, and Music.

Everything is **100% client-side** - no server modifications, no packet manipulation, no anti-cheat bypass. Just visual quality-of-life improvements for your game.

---

## ✨ What's inside

### 🎯 PvP Tools
- **PvPSafe** - blocks `/hub`, `/logout`, `/suicide` during combat
- **AutoGG** - auto-sends "GG" after kills
- **AutoSwap** - instant offhand ↔ inventory swap
- **FastExp** - fast XP bottle usage
- **ShiftTap** - crits via shift
- **Custom Hit Sounds** - 7 built-in presets
- **Totem Log** - logs broken totems
- **PickUpLogger** - tracks valuable pickups
- **AutoTPAccept** - auto-accepts `/tpa` from friends or all players
- **StatsTracker** - K/D counter parsed from chat (displayed in GUI)
- **KillStreak** - consecutive kill counter + custom sounds (user-provided)
- **TargetESP** - 4 variants (Crystals, Cubes, Ring, Ghosts) - 3D target highlight

### 🎨 Visual
- **Zoom** - smooth zoom with keybind
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
  - Hit detection via `AABB.clip`, miss at 3.1 blocks forward
  - Statistics: hit %, miss %, winrate %, total shots

### 🖥️ HUD
- Coordinates, biome, time, FPS, ping, TPS, BPS
- Potion effects with icons and timers
- Equipment HUD with durability
- Combo Counter
- Cooldowns
- Effect Warnings
- **Low HP Alert** - warning + sound when HP is below threshold
- **Armor Alert** - warning when armor durability is low
- **Mod Logo & Name** - custom logo with position editor
- **KillStreak HUD** - on-screen kill streak counter with timer
- **HUD Editor** - drag-n-drop layout editor (F6)
- **Ping Indicator** - player ping with progress bar in tab-list (below nickname)

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
  - Badge `×N` + tooltip with timestamps
  - Clickable coords (5 formats): **LMB** copies to clipboard, `[wp]` inserts `/wp add x y z`
  - New `/wp add <x> <y> <z>` command
- **InventoryManager** - save and preview inventory snapshots
  - 5 slots, files in `config/resistancedlc/inventories/`
  - 41 slots (4 armor + offhand + 9 hotbar + 27 main)
  - Steve preview with equipped armor, hotbar, and main inventory
  - Save / Rename / Delete / Open folder via mini-screens

### 🎵 Music Player
- Play your own `.ogg` files from `config/resistancedlc/music/`
- Beautiful HUD widget with rotating vinyl
- Drag-to-adjust volume slider
- Repeat (off / one / all), shuffle, auto-skip broken files
- Now properly stops when the feature is disabled

---

## 🎮 How to use

- Press **G** to open the GUI
- **Left-click** an accordion item to expand it
- **Right-click** an accordion item to toggle the feature
- Click the **⚙ gear icon** inside panels to access settings
- Press **F6** to open the HUD Editor
- All features are **client-side only** - no server modifications

---

## ⚠️ About the "KillAura" feature

The **"KillAura"** button in the PvP section is an **easter egg / joke only**.

- ❌ It does **NOT** provide any gameplay advantage
- ❌ It does **NOT** send attack packets to the server
- ❌ It does **NOT** bypass anti-cheat
- ❌ It does **NOT** work as an actual cheat

It simply plays a "denied" sound effect and displays a humorous message. It's a lighthearted nod to the absurdity of cheat clients - **we're not cheaters, we're modders**. 😄

---

## 📁 Optional files

### KillStreak sounds

To hear a sound when you reach a kill streak, place your own `.ogg` files in:

`config/resistancedlc/sounds/`

Supported filenames (by streak level):
- `killstreak_1.ogg`
- `killstreak_2.ogg`
- `killstreak_3.ogg`
- `killstreak_4.ogg`
- `killstreak_5.ogg`
- `killstreak_8.ogg`
- `killstreak_10.ogg`

**No built-in sounds included** - the mod does not ship with any audio files. If a file is missing, the sound is simply skipped. You can use any OGG Vorbis files you have the rights to.

### Music Player

Place your `.ogg` files in `config/resistancedlc/music/`

### InventoryManager

Snapshots are saved automatically to `config/resistancedlc/inventories/inventory_N.json`.

---

## 🌍 Languages
- 🇷🇺 Russian (Русский)
- 🇬🇧 English

Language is auto-detected from the game settings. Switch anytime in the mod's GUI (top-right button).

---

## 🛠️ Requirements
- Minecraft **1.21.11**
- Fabric Loader **0.19.5+**
- [Fabric API](https://modrinth.com/mod/fabric-api)

---

## ⚠️ Known issues
- **MusicPlayer duration** may show ~10-15% less than the actual track length (OGG Vorbis compression approximation)

---

## 📜 License
MIT License - free to use, modify, and distribute.

---

## 💬 Feedback & Support
Found a bug or want a new feature? Open an issue on [GitHub](https://github.com/LesIsLes/ResistanceDLC)!

---

**Made with ❤️ by lesis**
