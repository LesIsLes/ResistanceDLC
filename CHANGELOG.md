All notable changes to ResistanceDLC will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [2.3.0] — 2026-09-17

### ✨ Added
- **MusicPlayer** — full OGG player with HUD widget
  - Rotating vinyl, track title, artist, timing
  - Buttons: `[⏸/▶] [⏭] [⚙] [🔁]` + drag-to-adjust volume slider
  - Repeat: off / one / all
  - Shuffle mode
  - Auto-skip broken files
  - Command `/music` with subcommands
- **AutoGG** — auto-sends "GG" after kills
  - Mixin on `ClientboundPlayerCombatKillPacket`
  - Fallback — chat parsing
  - Configurable template and delay
- **StrikeRange** — shows attack distance in real time
  - Works on any entity (players, mobs, armor stands)
  - Configurable position, color, font size, duration
- **KillAura Easter Egg** — joke feature in PvP section
  - Plays `denied.ogg` sound
  - Random chat messages
  - Red flash animation

### 🔧 Changed
- Mixin signature updated for 1.21.11: `MouseHandler.onButton` (was `onPress`)
- Mouse coordinates now use `MouseHandler.getScaledXPos()` (fixes HUD click detection)
- HUD MusicPlayer widget now works **over chat**
- GUI section added: **Music** (6th section)

### 🐛 Fixed
- MusicPlayer `00:00 / 00:00` timing issue
- Volume slider overflow at 100%
- Click detection on HUD widget in multiplayer
- `AutoGGManager` chat parsing edge cases

### 📚 Documentation
- CONTEXT.md updated to v2.3.0
- README.md rewritten with new features
- CHANGELOG.md added

---

## [2.2.0] — 2026-09-13

### ✨ Added
- **LocalizationManager** — EN/RU manual localization system
- **en_us.json** / **ru_ru.json** — all GUI keys
- **PvPSafe** — only PvP damage triggers (AttackEntityCallback + `LivingEntity.hurtServer` + chat parsing)
- **PvPSafe warning** in GUI about perks/special items
- Hover tooltips on R/G/B/W color presets, sound presets, OK/↺ buttons
- Precise `contentHeight` for Config Manager / Waypoints panels

### 🔧 Changed
- Refactored all GUI strings to use `LocalizationManager.get()`
- Updated mixin `PvPSafeHurtMixin` to `LivingEntity.hurtServer`
- Fixed `delay_ms` for ItemScroller/AutoSwap
- Fixed `mode_head_head` etc. localization keys

---

## [2.1.0] — 2026-09-10

### ✨ Added
- **AccordionScreen** — brand new GUI with accordion-style panels
- 29 features across 5 sections (HUD, PvP, PvE, Visual, Misc)
- Content scrolling with clipping
- Local + global search
- Highlight of found items (3px yellow-white pulsing frame, 2 sec)
- Fade-in overlay + fade-out
- Fade-in list on section change
- 5 GUI themes (Vanilla, Dark, Neon, Candy, Blood)
- Keybinds for each feature
- Config Manager panel
- Waypoints panel

### 🔧 Changed
- Old `MyCustomScreen` moved to backup
- `AccordionItem` model with `contentHeight` per item

---

## [2.0.0] — 2026-09-05

### ✨ Added
- **DeathCoords** — save/load death coordinates
- **Custom Hitbox** — colored debug hitboxes
- **ItemScroller** — fast scroll transfer in containers
- **CoolDowns** — show items on cooldown
- Refactored `ModConfig` (200+ fields)

### 🔧 Changed
- Migrated to Minecraft **1.21.11**
- Updated all mixins for 1.21.11 signatures

---

## [1.0.0] — 2026-08-20

### ✨ Added
- Initial release
- Basic HUD: coords, biome, time, FPS, ping
- No Hurt Cam, No Bobbing
- Zoom, Custom Crosshair
- AutoSprint, FastExp
- Particle Blocker
- ChatFilter

---

[2.3.0]: https://github.com/LesIsLes/ResistanceDLC/compare/v2.2.0...v2.3.0
[2.2.0]: https://github.com/LesIsLes/ResistanceDLC/compare/v2.1.0...v2.2.0
[2.1.0]: https://github.com/LesIsLes/ResistanceDLC/compare/v2.0.0...v2.1.0
[2.0.0]: https://github.com/LesIsLes/ResistanceDLC/compare/v1.0.0...v2.0.0
[1.0.0]: https://github.com/LesIsLes/ResistanceDLC/releases/tag/v1.0.0