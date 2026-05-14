package mazealgo.model.server;

import mazealgo.model.algorithms.mazeGenerators.Maze;
import mazealgo.model.algorithms.mazeGenerators.MyMazeGenerator;
import mazealgo.model.algorithms.search.BestFirstSearch;
import mazealgo.model.algorithms.search.Solution;
import mazealgo.model.io.MyDecompressorInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end: bind {@link MyServer} on an OS-assigned port, connect a
 * real socket client, verify the strategy answers correctly.
 */
class MyServerIntegrationTest {

    @Test
    @Timeout(10)
    void generateMazeStrategy_returnsCompressedMaze_clientDecodesIt() throws Exception {
        MyServer server = new MyServer(0, 200, new GenerateMazeStrategy());
        server.start();
        try {
            int port = server.getPort();
            try (Socket socket = new Socket("localhost", port);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
                out.flush();
                out.writeObject(new int[]{15, 17});
                out.flush();
                try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
                    byte[] compressed = (byte[]) in.readObject();
                    byte[] mazeBytes;
                    try (MyDecompressorInputStream dec =
                                 new MyDecompressorInputStream(new ByteArrayInputStream(compressed))) {
                        mazeBytes = dec.readAllBytes();
                    }
                    Maze maze = new Maze(mazeBytes);
                    assertEquals(15, maze.getRows());
                    assertEquals(17, maze.getColumns());
                    assertNotNull(maze.getStartPosition());
                    assertNotNull(maze.getGoalPosition());
                }
            }
        } finally {
            server.stop();
        }
    }

    @Test
    @Timeout(10)
    void solveMazeStrategy_returnsSolution_andCachesIt(@TempDir Path cacheDir) throws Exception {
        AtomicInteger solveCount = new AtomicInteger();
        SolutionCache cache = new SolutionCache(cacheDir);
        SolveMazeStrategy strategy = new SolveMazeStrategy(cache, () -> {
            solveCount.incrementAndGet();
            return new BestFirstSearch();
        });

        MyServer server = new MyServer(0, 200, strategy);
        server.start();
        try {
            int port = server.getPort();
            Maze maze = new MyMazeGenerator().generate(10, 10);

            Solution first = roundtripSolve(port, maze);
            Solution second = roundtripSolve(port, maze);

            assertTrue(first.getSolutionPath().size() > 0);
            assertEquals(first.getSolutionPath().size(), second.getSolutionPath().size());
            // first request runs the algorithm; second is served from the cache.
            assertEquals(1, solveCount.get(), "second identical request should hit the cache");
        } finally {
            server.stop();
        }
    }

    private static Solution roundtripSolve(int port, Maze maze) throws Exception {
        try (Socket socket = new Socket("localhost", port);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            out.flush();
            out.writeObject(maze);
            out.flush();
            try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
                return (Solution) in.readObject();
            }
        }
    }
}
