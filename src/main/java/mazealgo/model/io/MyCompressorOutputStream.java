package mazealgo.model.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

/**
 * Decorator that wraps another {@link OutputStream} and applies a
 * streaming run-length encoding. Output format: pairs of {@code (count,
 * value)} where count is 1..255. Runs longer than 255 split across
 * multiple pairs with the same value.
 *
 * <p>Designed for the maze grid, whose cells are almost all 0 or 1 with
 * long runs — typical compression ratios of 50-100x. On varied data
 * (the maze header, for instance) it costs 2 bytes per input byte, so
 * the wrapper is net positive only when long runs dominate.
 *
 * <p>Pair this with {@link MyDecompressorInputStream} on the read side.
 */
public class MyCompressorOutputStream extends OutputStream {
    private static final int MAX_RUN = 255;

    private final OutputStream out;
    private int currentValue = -1;
    private int currentCount = 0;

    public MyCompressorOutputStream(OutputStream out) {
        this.out = Objects.requireNonNull(out, "out");
    }

    @Override
    public void write(int b) throws IOException {
        int v = b & 0xFF;
        if (currentCount == 0) {
            currentValue = v;
            currentCount = 1;
            return;
        }
        if (v == currentValue) {
            currentCount++;
            if (currentCount == MAX_RUN) {
                emitRun();
            }
        } else {
            emitRun();
            currentValue = v;
            currentCount = 1;
        }
    }

    @Override
    public void flush() throws IOException {
        emitRun();
        out.flush();
    }

    @Override
    public void close() throws IOException {
        try {
            emitRun();
        } finally {
            out.close();
        }
    }

    private void emitRun() throws IOException {
        if (currentCount > 0) {
            out.write(currentCount);
            out.write(currentValue);
            currentCount = 0;
        }
    }
}
