package io.github.daney23.mazealgo.model.io;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * Decorator that reverses {@link MyCompressorOutputStream}: reads
 * {@code (count, value)} pairs from the underlying stream and re-emits
 * {@code count} copies of {@code value} on each read.
 */
public class MyDecompressorInputStream extends InputStream {
    private final InputStream in;
    private int remainingInRun = 0;
    private int currentValue = -1;

    public MyDecompressorInputStream(InputStream in) {
        this.in = Objects.requireNonNull(in, "in");
    }

    @Override
    public int read() throws IOException {
        if (remainingInRun == 0) {
            int count = in.read();
            if (count == -1) return -1;
            int value = in.read();
            if (value == -1) {
                throw new EOFException("Truncated compressed stream: count without value");
            }
            remainingInRun = count;
            currentValue = value;
        }
        remainingInRun--;
        return currentValue;
    }

    @Override
    public void close() throws IOException {
        in.close();
    }
}
