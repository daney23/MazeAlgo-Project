package mazealgo.view;

import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.net.URL;

/**
 * Audio playback. Two channels:
 *
 * <ul>
 *   <li><b>Background music</b> — JavaFX {@link MediaPlayer}, loaded
 *       from {@code /mazealgo/view/sounds/background.mp3} if present;
 *       silently no-op otherwise. Drop a licensed file in and it just
 *       starts working.
 *   <li><b>Victory chime</b> — if {@code victory.wav} ships in the
 *       same directory it's used; otherwise a short C-E-G-C arpeggio
 *       is synthesized in code via {@code javax.sound.sampled} so the
 *       feature works out of the box with no third-party assets.
 * </ul>
 */
public class SoundPlayer {
    private static final Logger log = LogManager.getLogger(SoundPlayer.class);

    private MediaPlayer backgroundPlayer;
    private AudioClip victoryClip;

    public SoundPlayer() {
        URL backgroundUrl = firstAvailable(
                "/mazealgo/view/sounds/background.mp3",
                "/mazealgo/view/sounds/background.wav");
        if (backgroundUrl != null) {
            try {
                backgroundPlayer = new MediaPlayer(new Media(backgroundUrl.toExternalForm()));
                backgroundPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                backgroundPlayer.setVolume(0.3);
            } catch (Exception e) {
                log.warn("Background music failed to load: {}", e.toString());
            }
        }

        URL victoryUrl = firstAvailable(
                "/mazealgo/view/sounds/victory.mp3",
                "/mazealgo/view/sounds/victory.wav");
        if (victoryUrl != null) {
            try {
                victoryClip = new AudioClip(victoryUrl.toExternalForm());
            } catch (Exception e) {
                log.warn("Victory clip failed to load: {}", e.toString());
            }
        }
    }

    private URL firstAvailable(String... resourcePaths) {
        for (String path : resourcePaths) {
            URL url = getClass().getResource(path);
            if (url != null) return url;
        }
        return null;
    }

    public void playBackground() {
        if (backgroundPlayer != null) backgroundPlayer.play();
    }

    public void stopBackground() {
        if (backgroundPlayer != null) backgroundPlayer.stop();
    }

    /**
     * Plays the victory sound. Uses the bundled clip if one was found
     * at startup, otherwise synthesizes a short ascending arpeggio.
     * Runs on a background thread either way — never blocks the FX
     * application thread.
     */
    public void playVictory() {
        if (victoryClip != null) {
            victoryClip.play();
            return;
        }
        Thread t = new Thread(SoundPlayer::synthesizeVictoryChime, "VictoryChime");
        t.setDaemon(true);
        t.start();
    }

    /**
     * C5–E5–G5–C6 ascending arpeggio, ~120ms per note, with a short
     * linear fade at the head and tail of each note to avoid clicks.
     * Self-contained: needs nothing on the classpath.
     */
    private static void synthesizeVictoryChime() {
        try {
            int sampleRate = 44100;
            AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
            try (SourceDataLine line = AudioSystem.getSourceDataLine(format)) {
                line.open(format);
                line.start();

                double[] notes = {523.25, 659.25, 783.99, 1046.50}; // C5 E5 G5 C6
                int samplesPerNote = sampleRate * 12 / 100; // 120 ms
                int fadeSamples = 600;

                for (double freq : notes) {
                    byte[] buf = new byte[samplesPerNote * 2];
                    for (int i = 0; i < samplesPerNote; i++) {
                        double envelope = 1.0;
                        if (i < fadeSamples) envelope = (double) i / fadeSamples;
                        else if (i > samplesPerNote - fadeSamples) envelope = (double) (samplesPerNote - i) / fadeSamples;

                        double sample = envelope * 0.3 * Math.sin(2 * Math.PI * freq * i / sampleRate);
                        short s = (short) (sample * Short.MAX_VALUE);
                        buf[i * 2] = (byte) (s & 0xFF);
                        buf[i * 2 + 1] = (byte) ((s >> 8) & 0xFF);
                    }
                    line.write(buf, 0, buf.length);
                }
                line.drain();
            }
        } catch (Exception e) {
            // No audio output device, headless JVM, denied access — silently skip.
            log.debug("Synthesized chime skipped: {}", e.toString());
        }
    }
}
