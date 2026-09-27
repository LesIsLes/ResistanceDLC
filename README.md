# Resistance DLC

A lightweight visual client mod for Minecraft **1.21.11** (Fabric) with **45+ features** across 6 sections: HUD, PvP, PvE, Visual, Misc, and Music.

---

## ✨ Features

### 🎵 Music Player
- Play your own `.ogg` files from `config/resistancedlc/music/`
- Beautiful HUD widget with rotating vinyl
- Drag-to-adjust volume slider
- Repeat (off / one / all), shuffle, auto-skip broken files
- Full control from chat or GUI

### 🎯 PvP Tools
- **PvPSafe** — blocks `/hub`, `/logout`, `/suicide` during combat
- **AutoGG** — auto-sends "GG" after kills (via StatsTracker chat parsing)
- **AutoSwap** — instant offhand ↔ inventory swap
- **FastExp** — fast XP bottle usage
- **ShiftTap** — crits via shift
- **Custom Hit Sounds** — 7 built-in presets
- **Totem Log** — logs broken totems
- **PickUpLogger** — tracks valuable pickups
- **AutoTPAccept** — auto-accepts `/tpa` from friends or all players
- **StatsTracker** — K/D counter parsed from chat (displayed in GUI)
- **KillStreak** — consecutive kill counter + custom sounds at 1, 2, 3, 4, 5, 8, 10 kills

### 🎨 Visual
- **Zoom** — smooth zoom with keybind
- **Custom Crosshair** — 5 shapes, full color control
- **Strike Range** — shows attack distance in real time
- **Custom Hitbox** — colored debug hitboxes
- **Aspect Ratio** — stretch FOV
- **Low Fire / Shield** — lower flames and shield
- **Particle Blocker** — hide particles by category
- **Waypoints** — on-screen markers with distance
- **Item Physics** — items lie flat
- **Predictions** — projectile trajectory (bow, crossbow, multishot, snowball, potion, trident)
- **TargetESP** — 4 variants (Crystals, Cubes, Ring, Ghosts)
- **Jump Circles** — rings/hexagons/stars under feet on jump (line-based, no textures)
- **EnchantHighlight** — highlight selected enchants in item tooltips with custom color + bold
- **GammaUtil** — brightness above vanilla limit

### 🖥️ HUD
- Coordinates, biome, time, FPS, ping, TPS, BPS
- Potion effects with icons and timers
- Equipment HUD with durability
- Combo Counter
- Cooldowns
- Effect Warnings
- **Low HP Alert** — warning + sound when HP is below threshold
- **Armor Alert** — warning when armor durability is low
- **Mod Logo & Name** — custom logo with position editor
- **KillStreak HUD** — on-screen kill streak counter with timer

### ⚙️ Misc
- **ChatFilter** — regex-based chat filtering
- **AutoReconnect** — reconnects after kicks
- **DeathCoords** — saves death location
- **GUI Themes** — 5 themes (Vanilla, Dark, Neon, Candy, Blood)
- **Config Manager** — save/load configs
- **Macros** — 5 command/message slots (F1–F5 by default)
- **AutoRespawn** — automatically respawns after death
- **Friend List** — add friends by nickname, highlights them in chat

---

## 🌍 Languages
- 🇷🇺 Russian
- 🇬🇧 English

---

## 🎮 How to use
- Press **G** to open the GUI
- All features work **client-side only**
- No server modifications required
- **Left-click** an accordion item to expand it
- **Right-click** an accordion item to toggle the feature
- Click the **⚙ gear icon** inside panels to access settings

---

## 🛠️ Requirements
- Minecraft **1.21.11**
- Fabric Loader **0.19.5+**
- Fabric API

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

**No built-in sounds included** — the mod does not ship with any audio files. If a file is missing, the sound is simply skipped. You can use any OGG Vorbis files you have the rights to.

### Music Player

Place your `.ogg` files in `config/resistancedlc/music/`

---

## 📜 License
MIT License — free to use, modify, and distribute.

---

## 💬 Feedback
Found a bug or want a new feature? Open an issue on GitHub!