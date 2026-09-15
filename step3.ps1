$ErrorActionPreference = "Stop"
$root = "C:\Users\lesis\ResistanceDLC"
$srcDir = Join-Path $root "src\main\java\com\resistancedlc"

$targetFiles = @(
    "AutoReconnectManager.java",
    "ChatFilterManager.java",
    "PvPSafeManager.java",
    "PickUpLogger.java",
    "ParticleBlockerManager.java"
)

$fields = @(
    "showHud", "hudColor", "showModLogo", "modLogoX", "modLogoY", "modLogoRussian",
    "hudBackgroundEnabled", "hudBackgroundAlpha", "hudBackgroundColor", "hudBackgroundHeight",
    "guiColor", "guiTextColor", "hudAlpha", "showPet",
    "coordsX", "coordsY", "biomeX", "biomeY", "timeX", "timeY",
    "showCoords", "showBiome", "showTime",
    "showFps", "showPing", "showTps", "showBps", "showDirection",
    "fpsX", "fpsY", "pingX", "pingY", "tpsX", "tpsY", "bpsX", "bpsY", "directionX", "directionY",
    "showHitCounter", "hitCounterX", "hitCounterY", "hitCounterRussian",
    "showPotionEffects", "potionEffectsX", "potionEffectsY", "potionEffectsRussian", "potionEffectsIcons",
    "showEquipmentHud", "equipmentHudX", "equipmentHudY", "equipmentHudRussian", "equipmentShowDurability",
    "comboEnabled", "comboX", "comboY", "comboColor", "comboResetTime", "comboFontSize", "comboRussian",
    "currentCombo", "lastComboTime",
    "effectWarningsEnabled", "effectWarningsX", "effectWarningsY", "effectWarningsColor",
    "effectWarningsThreshold", "effectWarningsAlpha", "effectWarningsShowName", "effectWarningsShowIcon",
    "effectWarningsRussian",
    "fpsRussian", "pingRussian", "tpsRussian", "bpsRussian", "directionRussian",
    "customHitSoundsEnabled", "customHitSoundVolume", "customHitSoundPitch",
    "customHitSoundsRussian", "customHitSoundPreset",
    "autoSwapEnabled", "autoSwapMode", "autoSwapOpenDelay", "autoSwapCooldown", "autoSwapRussian",
    "autoSwapInProgress", "autoSwapStage", "autoSwapNextActionTime", "autoSwapLastTime", "autoSwapSlotToSwap",
    "fastExpEnabled", "fastExpRussian",
    "autoSprintEnabled", "autoSprintRussian",
    "shiftTapEnabled", "shiftTapRussian", "shiftTapReleaseTime", "shiftTapActive",
    "totemLogEnabled", "totemLogRadius", "totemLogSound", "totemLogRussian",
    "pvpSafeEnabled", "pvpSafeTimer", "pvpSafeBlockQuit", "pvpSafeBlockCommands",
    "pvpSafeShowHud", "pvpSafeHudX", "pvpSafeHudY", "pvpSafeHudColor", "pvpSafeRussian",
    "pickupLogEnabled", "pickupLogMode", "pickupLogWeapon", "pickupLogArmor", "pickupLogPotions",
    "pickupLogTotems", "pickupLogHeads", "pickupLogSpawners", "pickupLogStructureBlocks", "pickupLogRussian",
    "tapeMouseEnabled", "tapeMouseTarget", "tapeMouseDelay", "tapeMouseRussian",
    "tapeMouseRequireTarget", "tapeMouseRequireFullAttack", "tapeMouseButton", "tapeMouseHoldRight",
    "crosshairEnabled", "crosshairColor", "crosshairSize", "crosshairThickness", "crosshairGap",
    "crosshairAlpha", "crosshairRussian", "crosshairShape",
    "aspectRatioEnabled", "aspectRatio", "aspectRatioRussian",
    "lowFireEnabled", "lowFireOffset", "lowShieldEnabled", "lowShieldOffset", "lowFireShieldRussian",
    "zoomEnabled", "zoomFactor", "zoomSmoothness", "zoomRussian", "currentZoom",
    "waypointsRaw", "waypointsMax", "waypointsRussian", "waypointsEnabled",
    "noHurtCamEnabled", "noBobbingEnabled", "cameraRussian",
    "itemPhysicsEnabled", "itemPhysicsRussian",
    "particleBlockerEnabled", "particleBlockerFire", "particleBlockerSmoke", "particleBlockerExplosion",
    "particleBlockerPotions", "particleBlockerWater", "particleBlockerRedstone",
    "particleBlockerPortal", "particleBlockerCrit", "particleBlockerRussian",
    "chatFilterEnabled", "chatFilterWordsRaw", "chatFilterRussian",
    "autoReconnectEnabled", "autoReconnectDelay", "autoReconnectShowHud", "autoReconnectRussian",
    "searchHistoryRaw", "SEARCH_HISTORY_MAX",
    "isBindingKey", "bindingTarget",
    "lastPlayerX", "lastPlayerY", "lastPlayerZ", "currentBps"
)

$utf8NoBom = New-Object System.Text.UTF8Encoding $false
$importLine = "import com.resistancedlc.config.ModConfig;"
$totalAll = 0

Write-Host ""
Write-Host "=== Step 3: replacing in 5 managers ===" -ForegroundColor Cyan

foreach ($fileName in $targetFiles) {
    $targetFile = Join-Path $srcDir $fileName
    if (-not (Test-Path $targetFile)) {
        Write-Host "  [!] Not found: $fileName" -ForegroundColor Yellow
        continue
    }

    $text = [System.IO.File]::ReadAllText($targetFile, [System.Text.Encoding]::UTF8)
    $original = $text
    $replCount = 0

    foreach ($field in $fields) {
        $pattern = "MyCustomScreen\." + [regex]::Escape($field) + "\b"
        $matches = [regex]::Matches($text, $pattern)
        if ($matches.Count -gt 0) {
            $text = [regex]::Replace($text, $pattern, "ModConfig.$field")
            $replCount += $matches.Count
        }
    }

    if (($replCount -gt 0) -and ($text -notmatch [regex]::Escape($importLine))) {
        $lines = $text -split "`r?`n"
        $out = New-Object System.Collections.Generic.List[string]
        $inserted = $false
        foreach ($line in $lines) {
            $out.Add($line)
            if ((-not $inserted) -and ($line -match '^\s*package\s+')) {
                $out.Add("")
                $out.Add($importLine)
                $inserted = $true
            }
        }
        $text = ($out -join "`r`n")
    }

    if ($text -ne $original) {
        [System.IO.File]::WriteAllText($targetFile, $text, $utf8NoBom)
        Write-Host "  [OK] $fileName : $replCount replacements" -ForegroundColor Green
        $totalAll += $replCount
    } else {
        Write-Host "  [-] $fileName : no changes" -ForegroundColor Gray
    }
}

Write-Host ""
Write-Host "Done. Total replacements: $totalAll" -ForegroundColor Cyan
Write-Host ""