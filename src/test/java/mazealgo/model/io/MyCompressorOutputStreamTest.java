package mazealgo.model.io;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MyCompressorOutputStreamTest {

    @Test
    void roundtrip_empty() throws IOException {
        assertRoundtrip(new byte[0]);
    }

    @Test
    void roundtrip_singleByte() throws IOException {
        assertRoundtrip(new byte[]{0x2A});
    }

    @Test
    void roundtrip_allZeros() throws IOException {
        byte[] data = new byte[1000];
        assertRoundtrip(data);
    }

    @Test
    void roundtrip_allOnes() throws IOException {
        byte[] data = new byte[300]; // > MAX_RUN, forces a run split
        for (int i = 0; i < data.length; i++) data[i] = 1;
        assertRoundtrip(data);
    }

    @Test
    void roundtrip_alternating() throws IOException {
        byte[] data = new byte[256];
        for (int i = 0; i < data.length; i++) data[i] = (byte) (i & 1);
        assertRoundtrip(data);
    }

    @Test
    void roundtrip_randomBytes() throws IOException {
        byte[] data = new byte[4096];
        new Random(42).nextBytes(data);
        assertRoundtrip(data);
    }

    @Test
    void compression_isLossless_acrossLongRuns() throws IOException {
        // 600 zeros, then 600 ones — each requires a run split (>255).
        byte[] data = new byte[1200];
        for (int i = 600; i < 1200; i++) data[i] = 1;
        assertRoundtrip(data);
    }

    @Test
    void compression_shrinksBinaryGrids() throws IOException {
        // 10_000 byte block of mostly-zeros — what a real maze grid looks like.
        byte[] data = new byte[10_000];
        // sprinkle a few 1s
        data[123] = 1;
        data[4567] = 1;
        data[9999] = 1;
        byte[] compressed = compress(data);
        assertTrue(compressed.length < data.length / 10,
                "expected >10x compression on a sparse grid, got " + compressed.length + " bytes from " + data.length);
        assertArrayEquals(data, decompress(compressed));
    }

    private static void assertRoundtrip(byte[] data) throws IOException {
        byte[] decoded = decompress(compress(data));
        assertArrayEquals(data, decoded);
    }

    private static byte[] compress(byte[] data) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (MyCompressorOutputStream compressor = new MyCompressorOutputStream(out)) {
            compressor.write(data);
        }
        return out.toByteArray();
    }

    private static byte[] decompress(byte[] compressed) throws IOException {
        try (MyDecompressorInputStream in = new MyDecompressorInputStream(new ByteArrayInputStream(compressed))) {
            return in.readAllBytes();
        }
    }
}
