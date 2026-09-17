package com.resistancedlc;

import java.nio.file.Path;

/**
 * MusicTrack — запись одного музыкального трека.
 *
 * @param name      Отображаемое название (например, "Blinding Lights")
 * @param artist    Артист (например, "The Weeknd"), может быть пустым
 * @param fileName  Имя файла (например, "The Weeknd - Blinding Lights.ogg")
 * @param path      Полный путь к файлу
 */
public record MusicTrack(String name, String artist, String fileName, Path path) {

    /**
     * Парсит имя файла в MusicTrack.
     * Формат: "Artist - Title.ogg" → artist="Artist", name="Title"
     * Если нет " - ", то name = имя файла без расширения, artist = "".
     */
    public static MusicTrack fromPath(Path filePath) {
        String fileName = filePath.getFileName().toString();
        String baseName = fileName;
        int dotIdx = baseName.lastIndexOf('.');
        if (dotIdx > 0) {
            baseName = baseName.substring(0, dotIdx);
        }

        String name;
        String artist = "";

        int dashIdx = baseName.indexOf(" - ");
        if (dashIdx > 0) {
            artist = baseName.substring(0, dashIdx).trim();
            name = baseName.substring(dashIdx + 3).trim();
        } else {
            name = baseName.trim();
        }

        return new MusicTrack(name, artist, fileName, filePath);
    }

    /**
     * Полное отображаемое имя: "Artist - Name" или просто "Name".
     */
    public String displayFull() {
        if (artist == null || artist.isEmpty()) return name;
        return artist + " - " + name;
    }

    public String displayName() {
        return name;
    }

    public String displayArtist() {
        return artist == null ? "" : artist;
    }
}