```markdown
# Resistance DLC

![Resistance DLC](src/main/resources/assets/resistancedlc/icon.png)

Визуальный клиентский мод для Minecraft **1.21.11** на **Fabric**.

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-green.svg)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric-0.19.5+-blue.svg)](https://fabricmc.net/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Version](https://img.shields.io/badge/Version-2.0.0--dev-orange.svg)](https://github.com/LesIsLes/ResistanceDLC/releases)

## ✨ Возможности

### 🎯 HUD (14+ элементов)
- Координаты · Биом · Время · FPS · Ping · TPS · BPS · Направление
- Счётчик ударов до смерти
- Эффекты зелий с иконками
- Экипировка с прочностью
- Combo Counter
- Effect Warnings (предупреждение о конце эффектов)
- Кастомный прицел (5 форм)
- Фон HUD с настройкой прозрачности
- **DeathCoords** — сохранение координат смерти с командой `/dc`

### ⚔️ PVP
- **Custom Hit Sounds** — 7 кастомных звуков удара
- **AutoSwap** — свап offhand ↔ инвентарь (4 режима)
- **FastExp** — быстрое использование бутылочек опыта
- **ShiftTap** — авто-крит через отпускание Shift
- **AutoSprint** — авто-бег
- **Totem Log** — лог тотемов в радиусе 5–20 блоков
- **PvPSafe** — блокировка выхода и опасных команд в бою
- **PickUpLogger** — лог подобранных предметов (3 режима)

### 🖱️ PVE
- **TapeMouse** — автокликер (ЛКМ/ПКМ, задержка, фильтр цели)
- **ItemScroller** — быстрый перенос предметов скроллом в GUI контейнера
  - Скролл → вся стопка
  - Shift + скролл → 1 предмет
  - Ctrl + скролл → все стопки такого типа

### 🎨 Visual
- **Crosshair** — кастомный прицел (Крест / Точка / Круг / Стрелки / Крест+Точка)
- **FOV / Aspect Ratio** — множитель FOV
- **Low Fire / Low Shield** — низкий огонь и щит
- **Zoom** — плавный зум (клавиша C)
- **Waypoints** — метки с рендером + стрелками на краю экрана
- **Camera** — NoHurtCam + NoBobbing
- **ItemPhysics** — предметы лежат плашмя на земле
- **Particle Blocker** — отключение частиц по 8 категориям
- **Custom Hitbox** — изменение цвета debug-хитбоксов (F3+B)
- **Темы GUI** — 5 пресетов

### 🔧 Misc
- **ChatFilter** — фильтр чата по стоп-словам
- **AutoReconnect** — авто-переподключение к серверу
- Привязка клавиш GUI
- Конфигурации (JSON, save/load/list/remove)
- **DeathCoords** — команда `/dc` (last / clear / toggle / help)

### 🔍 Поиск
- **~250 записей** — находит **ВСЕ** настройки мода
- История поиска — 8 последних запросов
- Клик по результату → переход в раздел/страницу

## 🔑 Кейбинды

| Клавиша | Действие |
|---------|----------|
| `G` | Открыть GUI |
| `C` | Zoom |
| `R` | TapeMouse |
| `H` | AutoSwap |
| `J` | Custom Hit Sounds |
| `K` | FastExp |
| `L` | ShiftTap |
| `M` | Combo Counter |
| `N` | Effect Warnings |
| `B` | Добавить метку |
| `O` | Totem Log |
| `P` | PickUpLogger |

## 📦 Установка

1. Установи [Fabric Loader](https://fabricmc.net/use/) для **Minecraft 1.21.11**
2. Скачай [Fabric API](https://modrinth.com/mod/fabric-api) версии **0.141.6+**
3. Положи `.jar` мода в `.minecraft/mods/`
4. Запусти игру

**Требования:**
- Minecraft **1.21.11**
- Fabric Loader **0.19.5+**
- Fabric API **0.141.6+**
- Java **21**

## 🛠️ Сборка из исходников

```bash
git clone https://github.com/LesIsLes/ResistanceDLC.git
cd ResistanceDLC
./gradlew build

📋 Планы на v2.0.0
✅ DeathCoords — сохранение координат смерти

✅ Custom Hitbox — цвет debug-хитбоксов

✅ ItemScroller — быстрый перенос предметов скроллом

⏳ CoolDowns — кулдауны предметов в HUD (в работе)

⏳ HUD-аккордеон — переработка GUI (следующая большая цель)

Лицензия: MIT, Copyright (c) 2026 lesis