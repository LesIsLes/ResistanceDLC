# Resistance DLC

Визуальный клиентский мод для Minecraft 1.21.11 на Fabric.

![Resistance DLC](src/main/resources/assets/resistancedlc/icon.png)

## ✨ Возможности

### HUD
- Координаты, биом, время, FPS, Ping, TPS, BPS, направление
- Счётчик ударов до смерти
- Эффекты зелий + Effect Warnings
- Экипировка с прочностью
- Combo Counter
- Кастомный прицел (5 форм)
- Фон HUD с настройкой прозрачности

### PvP
- **Custom Hit Sounds** — 7 кастомных звуков удара
- **AutoSwap** — свап offhand ↔ инвентарь (4 режима)
- **FastExp** — быстрое использование бутылочек опыта
- **ShiftTap** — авто-крит через отпускание Shift
- **AutoSprint** — авто-бег
- **Totem Log** — лог тотемов в радиусе 5–20 блоков

### Visual
- **Crosshair** — кастомный прицел (Крест / Точка / Круг / Стрелки / Крест+Точка)
- **FOV / Aspect Ratio** — множитель FOV
- **Low Fire / Low Shield** — низкий огонь и щит
- **Zoom** — плавный зум
- **Waypoints** — метки с рендером через `TrackedWaypoint.Projector`
- **Темы GUI** — 5 пресетов

### Misc
- Привязка клавиш GUI
- Конфигурации (JSON, save/load/list/remove)

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

## 📦 Установка

1. Установи [Fabric Loader](https://fabricmc.net/use/) для Minecraft 1.21.11
2. Скачай [Fabric API](https://modrinth.com/mod/fabric-api)
3. Положи `.jar` мода в `.minecraft/mods/`

## 🛠️ Сборка из исходников

```bash
git clone https://github.com/LesIsLes/ResistanceDLC.git
cd ResistanceDLC
./gradlew build