package mazealgo.model.server;

import mazealgo.model.algorithms.mazeGenerators.IMazeGenerator;
import mazealgo.model.algorithms.mazeGenerators.Maze;
import mazealgo.model.algorithms.mazeGenerators.MyMazeGenerator;
import mazealgo.model.io.MyCompressorOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

/**
 * Server strategy: receives requested maze dimensions as {@code int[]
 * {rows, columns}}, generates a maze with {@link MyMazeGenerator}, and
 * returns it as a compressed byte array.
 *
 * <p>Wire protocol:
 * <pre>
 *   client ─[ObjectOutputStream]─ int[] dims          ─→ server
 *   client ←─────── byte[] (RLE-compressed maze) ─── server
 * </pre>
 * Compression uses {@link MyCompressorOutputStream}; the client decodes
 * via {@code MyDecompressorInputStream} and feeds the bytes into the
 * {@link Maze#Maze(byte[])} constructor.
 */
public class GenerateMazeStrategy implements IServerStrategy {
    private final IMazeGenerator generator;

    public GenerateMazeStrategy() {
        this(new MyMazeGenerator());
    }

    public GenerateMazeStrategy(IMazeGenerator generator) {
        this.generator = generator;
    }

    @Override
    public void serverStrategy(InputStream inFromClient, OutputStream outToClient) throws Exception {
        ObjectInputStream in = new ObjectInputStream(inFromClient);
        int[] dims = (int[]) in.readObject();
        Maze maze = generator.generate(dims[0], dims[1]);

        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        try (MyCompressorOutputStream compressor = new MyCompressorOutputStream(compressed)) {
            compressor.write(maze.toByteArray());
        }

        ObjectOutputStream out = new ObjectOutputStream(outToClient);
        out.writeObject(compressed.toByteArray());
        out.flush();
    }
}
