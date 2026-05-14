package mazealgo.model.server;

import mazealgo.model.algorithms.search.Solution;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

/**
 * Caches solved {@link Solution}s on disk keyed by SHA-256 of the
 * maze's byte representation. A second {@code solve} request for the
 * same maze short-circuits the search and reads the cached solution
 * instead — orders of magnitude faster than re-running BestFirstSearch.
 *
 * <p>Storage layout: one serialized {@code Solution} per file under the
 * configured directory (default {@code $TMPDIR/MazeAlgo/cache}), named
 * {@code <sha256>.sol}.
 */
public class SolutionCache {
    private static final Logger log = LogManager.getLogger(SolutionCache.class);

    private final Path cacheDir;

    public SolutionCache() {
        this(defaultCacheDir());
    }

    public SolutionCache(Path cacheDir) {
        this.cacheDir = cacheDir;
        try {
            Files.createDirectories(cacheDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create cache dir " + cacheDir, e);
        }
    }

    private static Path defaultCacheDir() {
        return Path.of(System.getProperty("java.io.tmpdir"), "MazeAlgo", "cache");
    }

    public Path getCacheDir() {
        return cacheDir;
    }

    public Optional<Solution> get(byte[] mazeBytes) {
        Path file = pathFor(mazeBytes);
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(file))) {
            return Optional.of((Solution) in.readObject());
        } catch (IOException | ClassNotFoundException e) {
            // Corrupted or stale entry — drop it so the next request rebuilds.
            log.warn("Dropping bad cache entry {}: {}", file.getFileName(), e.toString());
            try {
                Files.deleteIfExists(file);
            } catch (IOException ignored) {
                // best-effort
            }
            return Optional.empty();
        }
    }

    public void put(byte[] mazeBytes, Solution solution) {
        Path file = pathFor(mazeBytes);
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(file))) {
            out.writeObject(solution);
        } catch (IOException e) {
            // Cache failure must not fail the actual request — log and move on.
            log.warn("Failed to write cache entry {}: {}", file.getFileName(), e.toString());
        }
    }

    private Path pathFor(byte[] mazeBytes) {
        return cacheDir.resolve(sha256Hex(mazeBytes) + ".sol");
    }

    static String sha256Hex(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("SHA-256 not available in this JRE", e);
        }
    }
}
