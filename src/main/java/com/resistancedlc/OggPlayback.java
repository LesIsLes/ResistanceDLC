package com.resistancedlc;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;
import java.io.File;

/**
 * OggPlayback — обёртка над Java Sound для проигрывания OGG.
 *
 * ВАЖНО: Fabric ClassLoader не видит META-INF/services,
 * поэтому используем VorbisAudioFileReader НАПРЯМУЮ (без AudioSystem/SPI).
 *
 * Особенности:
 *   - Пауза через пересоздание потока + ручной skip байтов.
 *   - Громкость через MASTER_GAIN (в децибелах).
 *   - Потокобезопасность: чтение в отдельном потоке, стоп через volatile-флаг.
 */
public class OggPlayback {

    private static final int BUFFER_SIZE = 4096;

    private SourceDataLine line;
    private AudioInputStream audioStream;
    private Thread playbackThread;

    private volatile boolean running = false;
    private volatile boolean paused = false;

    private File currentFile;
    private long positionBytes = 0;
    private long totalBytes = 0;

    private float volume = 0.5f;

    private Runnable onTrackFinished;

    // ===================== УПРАВЛЕНИЕ =====================

    /**
     * Начать воспроизведение файла с нуля.
     */
    public boolean play(File file, Runnable onFinished) {
        stop();
        if (file == null || !file.exists()) return false;

        currentFile = file;
        this.onTrackFinished = onFinished;
        positionBytes = 0;
        paused = false;

        return startThread(positionBytes);
    }

    /**
     * Пауза — останавливаем поток, запоминаем позицию.
     */
    public void pause() {
        if (!running) return;
        paused = true;
        running = false;

        if (line != null) {
            line.stop();
            line.flush();
        }
    }

    /**
     * Возобновление с запомненной позиции.
     */
    public boolean resume() {
        if (currentFile == null) return false;
        if (!paused) return false;
        paused = false;
        return startThread(positionBytes);
    }

    /**
     * Полный стоп и сброс.
     */
    public void stop() {
        running = false;
        paused = false;

        if (line != null) {
            try { line.stop(); line.flush(); line.close(); } catch (Exception ignored) {}
            line = null;
        }
        if (audioStream != null) {
            try { audioStream.close(); } catch (Exception ignored) {}
            audioStream = null;
        }
        if (playbackThread != null) {
            try { playbackThread.join(500); } catch (InterruptedException ignored) {}
            playbackThread = null;
        }
        positionBytes = 0;
    }

    // ===================== ВНУТРЕННЯЯ ЛОГИКА =====================

    /**
     * Открывает OGG-поток через VorbisAudioFileReader НАПРЯМУЮ.
     * Fallback: если не сработал — пробует AudioSystem.
     */
    private AudioInputStream openVorbisStream(File file) throws Exception {
        // ✅ ПРЯМОЙ ВЫЗОВ VorbisAudioFileReader (без AudioSystem/SPI)
        try {
            Class<?> readerClass = Class.forName(
                    "com.github.trilarion.sound.vorbis.sampled.spi.VorbisAudioFileReader"
            );
            Object reader = readerClass.getDeclaredConstructor().newInstance();
            java.lang.reflect.Method getStreamMethod = readerClass.getMethod(
                    "getAudioInputStream", java.io.File.class
            );
            AudioInputStream stream = (AudioInputStream) getStreamMethod.invoke(reader, file);
            ResistanceDLC.LOGGER.info("[OggPlayback] Opened via VorbisAudioFileReader: " + file.getName());
            return stream;
        } catch (Exception e) {
            ResistanceDLC.LOGGER.warn("[OggPlayback] Direct reader failed, trying AudioSystem: " + e.getMessage());
        }

        // Fallback: AudioSystem
        try {
            AudioInputStream stream = AudioSystem.getAudioInputStream(file);
            ResistanceDLC.LOGGER.info("[OggPlayback] Opened via AudioSystem: " + file.getName());
            return stream;
        } catch (Exception e) {
            ResistanceDLC.LOGGER.error("[OggPlayback] AudioSystem also failed: " + e.getMessage());
            throw e;
        }
    }

    private boolean startThread(long skipBytes) {
        final long skip = skipBytes;

        try {
            // ✅ Открываем OGG-поток через VorbisAudioFileReader
            AudioInputStream raw = openVorbisStream(currentFile);
            AudioFormat baseFormat = raw.getFormat();

            // Конвертируем в PCM_SIGNED 16-bit — стандарт для SourceDataLine
            AudioFormat targetFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    baseFormat.getSampleRate(),
                    16,
                    baseFormat.getChannels(),
                    baseFormat.getChannels() * 2,
                    baseFormat.getSampleRate(),
                    false
            );

            audioStream = AudioSystem.getAudioInputStream(targetFormat, raw);
            totalBytes = audioStream.available();

            // Ручной skip: читаем и выбрасываем байты в буфер
            if (skip > 0) {
                long remaining = skip;
                byte[] skipBuf = new byte[BUFFER_SIZE];
                while (remaining > 0) {
                    int toRead = (int) Math.min(remaining, BUFFER_SIZE);
                    int read = audioStream.read(skipBuf, 0, toRead);
                    if (read < 0) break;
                    remaining -= read;
                }
            }

            // Открываем линию
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, targetFormat);
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(targetFormat, BUFFER_SIZE * 4);
            applyVolume();
            line.start();

        } catch (Exception e) {
            ResistanceDLC.LOGGER.error("OggPlayback: failed to open stream: " + e.getMessage());
            cleanup();
            return false;
        }

        running = true;
        final AudioInputStream stream = audioStream;
        final SourceDataLine localLine = line;

        playbackThread = new Thread(() -> {
            byte[] buffer = new byte[BUFFER_SIZE];
            long bytesReadTotal = skip;
            try {
                while (running) {
                    int read = stream.read(buffer, 0, buffer.length);
                    if (read < 0) break;
                    if (read == 0) continue;

                    localLine.write(buffer, 0, read);
                    bytesReadTotal += read;
                    positionBytes = bytesReadTotal;
                }

                if (running) {
                    localLine.drain();
                    running = false;
                    if (onTrackFinished != null) {
                        onTrackFinished.run();
                    }
                }
            } catch (Exception e) {
                if (running) {
                    ResistanceDLC.LOGGER.error("OggPlayback: playback error: " + e.getMessage());
                }
            } finally {
                try { localLine.stop(); } catch (Exception ignored) {}
            }
        }, "MusicPlayer-Playback");

        playbackThread.setDaemon(true);
        playbackThread.start();
        return true;
    }

    private void cleanup() {
        running = false;
        if (line != null) {
            try { line.close(); } catch (Exception ignored) {}
            line = null;
        }
        if (audioStream != null) {
            try { audioStream.close(); } catch (Exception ignored) {}
            audioStream = null;
        }
    }

    // ===================== ГРОМКОСТЬ =====================

    public void setVolume(float v) {
        this.volume = Math.max(0.0f, Math.min(1.0f, v));
        applyVolume();
    }

    private void applyVolume() {
        if (line == null) return;
        try {
            if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
                float dB;
                if (volume <= 0.0001f) {
                    dB = gain.getMinimum();
                } else {
                    dB = (float) (20.0 * Math.log10(volume));
                    if (dB < gain.getMinimum()) dB = gain.getMinimum();
                    if (dB > gain.getMaximum()) dB = gain.getMaximum();
                }
                gain.setValue(dB);
            }
        } catch (Exception ignored) {}
    }

    // ===================== ГЕТТЕРЫ =====================

    public boolean isRunning() { return running; }
    public boolean isPaused() { return paused; }
    public long getPositionBytes() { return positionBytes; }
    public long getTotalBytes() { return totalBytes; }

    public float getProgress() {
        if (totalBytes <= 0) return 0.0f;
        return Math.max(0.0f, Math.min(1.0f, (float) positionBytes / totalBytes));
    }
}