# 🎮 ResistanceDLC

**A lightweight visual client mod for Minecraft 1.21.11 (Fabric)** with 33 features across 6 sections: HUD, PvP, PvE, Visual, Misc, and Music.

![Version](https://img.shields.io/badge/version-2.3.0-green)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-blue)
![Loader](https://img.shields.io/badge/Loader-Fabric-orange)
![License](https://img.shields.io/badge/license-MIT-blue)
![Languages](https://img.shields.io/badge/languages-EN%20%7C%20RU-red)

---

## ✨ Features

### 🎵 Music Player
- Play your own `.ogg` files from `config/resistancedlc/music/`
- Beautiful HUD widget with rotating vinyl
- Drag-to-adjust volume slider
- Repeat (off / one / all), shuffle, auto-skip broken files
- Full control from chat or GUI
- Format: **OGG Vorbis**

### 🎯 PvP Tools
- **PvPSafe** — blocks `/hub`, `/logout`, `/suicide` during combat
- **AutoGG** — auto-sends "GG" after kills
- **AutoSwap** — instant offhand ↔ inventory swap
- **FastExp** — fast XP bottle usage
- **ShiftTap** — crits via shift
- **Custom Hit Sounds** — 7 built-in presets
- **Totem Log** — logs broken totems
- **PickUpLogger** — tracks valuable pickups
- **KillAura Easter Egg** — a harmless joke 🎭

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

### 🖥️ HUD
- Coordinates, biome, time, FPS, ping, TPS, BPS
- Potion effects with icons and timers
- Equipment HUD with durability
- Combo Counter
- Cooldowns
- Effect Warnings
- Fully customizable position and colors

### ⚙️ Misc
- **ChatFilter** — regex-based chat filtering
- **AutoReconnect** — reconnects after kicks
- **DeathCoords** — saves death location with `/dc`
- **GUI Themes** — 5 themes (Vanilla, Dark, Neon, Candy, Blood)
- **Config Manager** — save/load configs with `/cfg`

---

## 🌍 Languages

- 🇬🇧 English
- 🇷🇺 Russian

Switch language in GUI: **G → top-right `RU`/`EN` button**

---

## 🎮 How to use

### Installation
1. Install **Fabric Loader 0.19.5+** for Minecraft **1.21.11**
2. Install **Fabric API**
3. Download `resistancedlc-2.3.0.jar` from [Releases](../../releases)
4. Put it in `.minecraft/mods/`
5. Launch the game

### Opening the GUI
Press **`G`** in game to open the main mod GUI.

### Music Player setup
1. Open GUI → **Music** section → **Music Player** → **Enable**
2. Click **"Open folder"** — music folder will open
3. Drop your `.ogg` files there (e.g. `Artist - Title.ogg`)
4. Click **"Rescan"** to reload the playlist
5. Press `▶` to play

---

## 🛠️ Requirements

- **Minecraft:** 1.21.11
- **Fabric Loader:** 0.19.5 or newer
- **Fabric API:** latest for 1.21.11
- **Java:** 21+

---

## 📦 Installation from source

```bash
git clone https://github.com/LesIsLes/ResistanceDLC.git
cd ResistanceDLC
./gradlew clean build
Output .jar: build/libs/resistancedlc-2.3.0.jar

🎨 GUI Themes
Theme	Color	Description
Vanilla	#00FF00	Classic green
Dark	#808080	Dark grey
Neon	#00FFFF	Cyberpunk
Candy	#FF69B4	Pink
Blood	#FF0000	Red
📝 Commands
Command	Description
/cfg	Config manager help
/cfg dir	Open config folder
/cfg list	List saved configs
/cfg save <name>	Save current config
/cfg load <name>	Load config
/dc	Show last death coords
/dc clear	Clear saved death coords
/chatfilter add <word>	Add stop-word
/music	Toggle MusicPlayer
/music list	List playlist
/music folder	Open music folder
🤝 Contributing
Found a bug or want a new feature? Open an Issue.

Pull requests welcome!

📜 License
MIT License — Copyright (c) 2026 lesis

See LICENSE for details.

💖 Credits
Author: lesis

AI assistance: Claude (Anthropic) — for code review, refactoring, and debugging

Libraries: java-vorbis-support by Trilarion

⚠️ AI Disclosure
This mod contains AI-generated code (assisted development). All features, textures, and descriptions were reviewed and tested by the human author. Declared in compliance with Modrinth's content rules.

Made with 💚 by lesis