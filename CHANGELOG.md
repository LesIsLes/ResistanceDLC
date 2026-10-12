# Changelog

All notable changes to ResistanceDLC will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [3.0.0] - 2026-10-12

First major release after the alpha series. 48+ features across 6 sections.

### ✨ Added
- **Voice Commands** - control the mod with voice
    - 4 built-in commands: open GUI, close GUI, change language, open HUD editor
    - MFCC + DTW recognition pipeline (SAMPLE_RATE=16000, FFT_SIZE=512, MEL_FILTERS=26, NUM_CEPS=13+13 delta)
    - VAD with calibration (CALIBRATION_FRAMES=100, MIN_VOICE_RMS=30)
    - Works even when GUI is open (except chat)
    - Record your own commands via `/voicerecord`
- **Freelook** - third-person camera detached from body
    - Bind: LCTRL by default
    - Sensitivity and distance settings
    - Auto-disables when a screen is open
- **Time Change** - client-side time of day (0..24000 ticks)
    - 6 presets: Sunrise, Day, Noon, Sunset, Night, Midnight
- **Weather Change** - 5 modes: Clear, Rain, Thunder, Snow, Overcast
    - Fully client-side. Other players see real weather
- **Predictions** - projectile trajectory
    - Block raycasting (stops at walls)
    - Landing point indicator (cross / dot)
    - Originates from camera (accounts for bob / shake / F5 / freelook)
- **Lyrics** - LRC file parsing + in-world 3D text
    - Files: `config/resistancedlc/lyrics/<track>.lrc`
    - Karaoke style: current line raised, next line lowered
    - Fade 300ms in, 500ms out
    - Support for LRC offset tags
- **Death Recap** - last 5 deaths with details
    - Reason, killer, coords, dimension, time
    - Full armor snapshot (5 slots)
    - Steve preview with equipped armor
- **Inventory Manager** - save and preview inventory snapshots
    - 5 slots, files in `config/resistancedlc/inventories/inventory_N.json`
    - 41 slots (4 armor + offhand + 9 hotbar + 27 main)
    - Save / Rename / Delete / Open folder via mini-screens
- **Smart Chat** - groups repeated chat messages
    - Same sender + text within 60 sec window
    - Badge `xN` + tooltip with timestamps
    - Clickable coords (5 formats): LMB copies to clipboard, `[wp]` inserts `/wp add x y z`
    - New `/wp add <x> <y> <z>` command
- **Jump Circles** - rings/hexagons/stars under feet on jump
    - Line-based rendering (no textures)
    - 3 styles, full color control, size, lifetime, spin speed
- **Custom Main Menu** - custom logo + adaptive splash text
    - Custom loading screen
    - Adaptive to GUI Scale: 1x-3x centered, 4x between logo and buttons
- **Ping Indicator** - player ping with progress bar in tab-list
- **Gamma Util** - brightness above vanilla limit via reflection
- **Frame-rate independent Zoom** - smooth on any FPS (30/60/144/240)
    - Update from `FOVMixin.getFov()`, not from tick
    - Formula: `speed = 1 - exp(-k * dt)`

### 🔧 Changed
- **AccordionScreen** - polished GUI, fixed double indent in FriendList, fixed `addPosEditorRow` in KillStreak
- **Splash text** - adaptive to GUI Scale
- **Logo** - moves up on 4x without touching top edge
- **Zoom smoothness** - range 0.05..1.00
- **ModConfig.currentZoom** - removed, use `ZoomManager.getCurrentZoom()`
- **AutoGGManager** - deduplication by victim name
- **TotemLog** - inject into `ClientPacketListener.handleEntityEvent` (event ID 35) instead of `LivingEntity.checkTotemDeathProtection` (private, server-only)

### 🐛 Fixed
- **AutoGG** - now works via `ClientboundEntityEventPacket` event 3 (ENTITY_DEATH) + `KillTracker`, even if server doesn't write kills to chat
- **TotemLog** - now shows correct attacker for mobs and players
    - `HitSoundMixin`: `target instanceof LivingEntity` (was `Player`)
- **FriendList** - online friends now detected via `mc.level.players()` instead of tab-list. Nicknames no longer disappear
- **FriendList chat** - matching only by prefix-nick (`Notch:`, `<Notch>`, `[Notch]`, `Notch »`). False positives removed
- **Enchantment Highlight** - registered `ItemTooltipCallback`. Highlight now works
- **AutoTPAccept** - sending via `sendCommand`. 16 patterns for RU and EN
- **StatsTracker** - extracts victim name from regex, saves to `autoGgLastVictim`
- **KillStreak** - `addPosEditorRow` correctly returns Y
- **Music Player** - stable OGG playback via `VorbisAudioFileReader`

### 🗑 Removed
- **Optimization** - was breaking `options.txt`
- **Bundled KillStreak sounds** - now user-provided, copyright-safe
- **`TotemLogMixin.java`** - replaced by `ClientPacketListenerMixin`

### ⚠️ Known Issues
- AutoGG may not work on some servers if they don't send event 3 AND don't write kills to chat
- Custom hit sounds require manual installation to `config/resistancedlc/sounds/`
- Voice Commands are experimental. Require microphone calibration
- MusicPlayer duration may show ~10-15% less than actual (OGG compression approximation)

### 📦 File
`resistancedlc-3.0.0.jar` (~1.4-1.5 MB)

---

## [2.9.0] - 2026-10-01

### ✨ Added
- **GammaUtil fix** - clamp [0,1] + `GammaFullbrightMixin` (fullbright works)
- **AutoReconnect fix** - reconnect only on kick/ban/crash, not on manual disconnect
- **HUD Editor** - new screen `HudEditScreen`, opens with **F6** or from GUI (HUD section)
    - Checkbox panel on the right, drag-n-drop, RMB - reset, save via `ConfigManager`
    - `HudWidget` / `HudWidgetRegistry`
- **Crosshair Heatmap** - hit/miss visualization in the world
    - Hit via `AABB.clip`, miss at 3.1 blocks forward
    - GUI panel in Visual, statistics (hit%, miss%, winrate%, total)
- **Ping-Based Lag Indicator** - `PingIndicatorMixin` on `PlayerTabOverlay.renderPingIcon`
    - Progress bar + `42ms` text under nickname
    - Filters invisibility and players not in world
- **Death Recap** - last 5 deaths with details:
    - Reason, killer, coordinates, dimension, time, armor (5 slots)
    - Armor snapshot every tick (`DeathRecapManager.ArmorSnapshot`), skip when `health <= 0`
    - LIFO buffer of 5 entries
    - `DeathScreenAccessor` - `@Accessor("causeOfDeath")`
    - Armor icons via `ArmorIconWidget`, text via `ArmorTextWidget`
- **InventoryManager** - save and preview inventory snapshots
    - 5 slots, stored as `config/resistancedlc/inventories/inventory_N.json`
    - Snapshot: 4 armor + offhand + 9 hotbar + 27 main = **41 slots** (no craft)
    - Preview: Steve with equipped armor, hotbar, main inventory
    - Buttons: Save / Rename / Delete / Open folder
    - Mini-screens for Save (`InventorySaveScreen`) and Rename (`InventoryRenameScreen`)
    - Empty slots shown as empty frames (not hidden!)
    - ItemStack serialization via `ItemStack.CODEC` + `NbtOps` -> SNBT string
    - `TagParser.parseCompoundFully(String)` for parsing (1.21.11)

### 🐛 Fixed
- **AutoReconnect** - manual disconnect via PauseScreen now sets `manualDisconnect = true`
- **Global search** - no longer passes clicks to the background

### 🔧 Changed
- **ChatFilter** panel height is now dynamic (`calcChatFilterHeight()`)
    - Empty -> ~54px instead of 186px
    - Grows when stop-words are added

### 🗑 Removed
- Nothing in this version yet

---

## [2.7.1] - 2026-09-27

### ✨ Added
- **Jump Circles** - now rendered as **lines** (no `.png` textures), same technique as TargetESP
    - New style **Star** (replaces Portal)
    - New settings: **Color** (HEX + R/G/B/W presets) and **Line Width** (1.0-5.0)
    - Radius grows from center up to 0.5 blocks
    - Removed Brightness setting (replaced by Color)
    - Deleted textures `assets/resistancedlc/textures/jump_circles/*.png`
- **AutoGG** - now works via **StatsTracker** (chat parsing)
    - No longer depends on `ClientboundPlayerCombatKillPacket`
    - Triggers whenever StatsTracker records `+1 kill`
    - `AutoGGMixin` removed

### 🐛 Fixed
- **AccordionScreen scroll** - no longer resets to top when:
    - Clicking on an accordion item (LMB)
    - Opening a panel after another expanded panel (Extra HUD, FOV, etc.)
    - Rebuilding a panel with new `contentHeight`
- **`updateContentMetrics()`** - now called **before** `buildPanelWidgets()`, not after. Widgets get correct Y coordinates.

### 🔧 Changed
- `ModConfig.jumpCirclesStyle`: `portal` -> `star`
- `ModConfig.jumpCirclesBrightness` - removed
- `ModConfig.jumpCirclesColor` - added (ARGB)
- `ModConfig.jumpCirclesLineWidth` - added (1.0-5.0)
- `StatsTrackerManager` - removed debug log `[StatsTracker] +1 kill ...`, added call to `AutoGGManager.onPlayerKilled()`
- `AutoGGManager` - simplified (removed `onChatMessage()`, `extractVictim()`, `onEntityKilled()`)
- `AutoGGManager.onPlayerKilled()` - removed `autoGgOnlyPlayers` check (StatsTracker already filters players)

### 🗑 Removed
- `AutoGGMixin.java` - deleted
- `resistancedlc.client.mixins.json` - removed `AutoGGMixin`
- `textures/jump_circles/circle.png`, `hexagon.png`, `portal.png` - deleted
- Fallback `AutoGGManager.onChatMessage(...)` registrations in `ResistanceDLCClient`

---

## [2.6.1] - 2026-09-20

### 🐛 Fixed
- **ConcurrentModificationException** when binding keys in GUI - all `rebuildWidgets` calls are now wrapped in `Minecraft.getInstance().execute(...)`
- Removed `[Macro]` debug logs
- **FriendList HUD widget** now works correctly

---

## [2.6.0] - 2026-09-18

### ✨ Added
- **AutoTPAccept** - auto-accepts `/tpa` requests from friends or all players
- **EnchantHighlight** - highlights selected enchants in item tooltips with custom color + bold
- **StatsTracker** - K/D counter parsed from chat (displayed in GUI)
- **KillStreak** - consecutive kill counter + custom sounds at 2, 3, 4, 5, 8, 10 kills

### 🔧 Changed
- Removed "Enable X" checkboxes - features toggle via right-click
- AutoRespawn now uses `ServerboundClientCommandPacket(PERFORM_RESPAWN)`
- `LocalizationManager.get(key, args)` - args must be Float/Integer/Double, not String

### 🗑 Removed
- Built-in KillStreak sounds - copyright-safe, now user-provided via `config/resistancedlc/sounds/`

---

## [2.5.0] - 2026-09-15

### ✨ Added
- **LowHPAlert** - warning + sound when HP drops below threshold
- **AutoRespawn** - automatic respawn after death
- **ArmorAlert** - warning when armor durability is low
- **FriendList** - friends list + HUD widget + chat highlight

---

## [2.4.0] - 2026-09-12

### ✨ Added
- **AutoTool** - auto-switch to best tool
- **Macros** - 5 command/message slots (F1-F5 by default)
- **Predictions** - projectile trajectory (bow, crossbow, multishot, snowball, potion, trident)

### 🗑 Removed
- **Optimization** - broke `options.txt`

---

## [2.3.2] - 2026-09-11

### ✨ Added
- **TargetESP** - ported from anomalith (4 variants: Crystals, Cubes, Ring, Ghosts)
- **GammaUtil** - brightness above vanilla limit

---

## [2.3.1] - 2026-09-10

### ✨ Added
- **Mod Logo & Name** - custom logo with position editor
- **oggLibs fix** - `configurations.oggLibs` + `libs/java-vorbis-support-1.2.1.jar`

---

## [2.3.0] - 2026-09-09

### ✨ Added
- **MusicPlayer** - full OGG player with HUD widget
    - Rotating vinyl, track title, artist, timing
    - Buttons: `[⏸/▶] [⏭] [⚙] [🔁]` + drag-to-adjust volume slider
    - Repeat: off / one / all
    - Shuffle mode
    - Auto-skip broken files
    - Command `/music` with subcommands
- **AutoGG** - auto-sends "GG" after kills
    - Mixin on `ClientboundPlayerCombatKillPacket`
    - Fallback - chat parsing
    - Configurable template and delay
- **StrikeRange** - shows attack distance in real time
    - Works on any entity (players, mobs, armor stands)
    - Configurable position, color, font size, duration
- **KillAura Easter Egg** - joke feature in PvP section
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

## [2.2.0] - 2026-09-07

### ✨ Added
- **LocalizationManager** - EN/RU manual localization system
- **en_us.json** / **ru_ru.json** - all GUI keys
- **PvPSafe** - only PvP damage triggers (AttackEntityCallback + `LivingEntity.hurtServer` + chat parsing)
- **PvPSafe warning** in GUI about perks/special items
- Hover tooltips on R/G/B/W color presets, sound presets, OK/↺ buttons
- Precise `contentHeight` for Config Manager / Waypoints panels

### 🔧 Changed
- Refactored all GUI strings to use `LocalizationManager.get()`
- Updated mixin `PvPSafeHurtMixin` to `LivingEntity.hurtServer`
- Fixed `delay_ms` for ItemScroller/AutoSwap
- Fixed `mode_head_head` etc. localization keys

---

## [2.1.0] - 2026-09-05

### ✨ Added
- **AccordionScreen** - brand new GUI with accordion-style panels
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

## [2.0.0] - 2026-09-03

### ✨ Added
- **DeathCoords** - save/load death coordinates
- **Custom Hitbox** - colored debug hitboxes
- **ItemScroller** - fast scroll transfer in containers
- **CoolDowns** - show items on cooldown
- Refactored `ModConfig` (200+ fields)

### 🔧 Changed
- Migrated to Minecraft **1.21.11**
- Updated all mixins for 1.21.11 signatures

---

## [1.0.0] - 2026-08-20

### ✨ Added
- Initial release
- Basic HUD: coords, biome, time, FPS, ping
- No Hurt Cam, No Bobbing
- Zoom, Custom Crosshair
- AutoSprint, FastExp
- Particle Blocker
- ChatFilter

---

[3.0.0]: https://github.com/LesIsLes/ResistanceDLC/compare/v2.9.0...v3.0.0
[2.9.0]: https://github.com/LesIsLes/ResistanceDLC/compare/v2.7.1...v2.9.0
[2.7.1]: https://github.com/LesIsLes/ResistanceDLC/compare/v2.6.1...v2.7.1
[2.6.1]: https://github.com/LesIsLes/ResistanceDLC/compare/v2.6.0...v2.6.1
[2.6.0]: https://github.com/LesIsLes/ResistanceDLC/compare/v2.5.0...v2.6.0
[2.5.0]: https://github.com/LesIsLes/ResistanceDLC/compare/v2.4.0...v2.5.0
[2.4.0]: https://github.com/LesIsLes/ResistanceDLC/compare/v2.3.2...v2.4.0
[2.3.2]: https://github.com/LesIsLes/ResistanceDLC/compare/v2.3.1...v2.3.2
[2.3.1]: https://github.com/LesIsLes/ResistanceDLC/compare/v2.3.0...v2.3.1
[2.3.0]: https://github.com/LesIsLes/ResistanceDLC/compare/v2.2.0...v2.3.0
[2.2.0]: https://github.com/LesIsLes/ResistanceDLC/compare/v2.1.0...v2.2.0
[2.1.0]: https://github.com/LesIsLes/ResistanceDLC/compare/v2.0.0...v2.1.0
[2.0.0]: https://github.com/LesIsLes/ResistanceDLC/compare/v1.0.0...v2.0.0
[1.0.0]: https://github.com/LesIsLes/ResistanceDLC/releases/tag/v1.0.0
