package mazealgo.model;

import mazealgo.model.algorithms.mazeGenerators.Maze;
import mazealgo.model.algorithms.mazeGenerators.MyMazeGenerator;
import mazealgo.model.algorithms.search.BestFirstSearch;
import mazealgo.model.algorithms.search.SearchableMaze;
import mazealgo.model.algorithms.search.Solution;
import mazealgo.model.io.MyDecompressorInputStream;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * High-level model facade for the UI / ViewModel. Two operating modes:
 *
 * <ul>
 *   <li><b>In-process</b> (no-arg constructor) — generates and solves
 *       directly with {@code MyMazeGenerator} / {@code BestFirstSearch}.
 *       Used by tests; also a convenient fallback if running the UI
 *       without {@link mazealgo.MazeApp}'s embedded servers.</li>
 *   <li><b>Remote</b> ({@code MazeModel(int, int)} constructor) — talks
 *       to {@code MyServer} over localhost sockets. The JavaFX app
 *       embeds both servers on OS-assigned ports and injects them here,
 *       so every Generate / Solution Hint click goes through the
 *       Strategy + Decorator-compression + SHA-256 cache pipeline
 *       Phase 2 built.</li>
 * </ul>
 */
public class MazeModel {
    private final int generatePort;
    private final int solvePort;
    private final boolean remote;

    /** In-process mode: algorithms run on the calling thread, no sockets. */
    public MazeModel() {
        this.generatePort = -1;
        this.solvePort = -1;
        this.remote = false;
    }

    /**
     * Remote mode: every {@link #generate(int, int)} / {@link #solve(Maze)}
     * call opens a fresh localhost socket to the given ports.
     */
    public MazeModel(int generatePort, int solvePort) {
        this.generatePort = generatePort;
        this.solvePort = solvePort;
        this.remote = true;
    }

    public Maze generate(int rows, int columns) {
        if (!remote) {
            return new MyMazeGenerator().generate(rows, columns);
        }
        try (Socket socket = new Socket("localhost", generatePort);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            out.flush();
            out.writeObject(new int[]{rows, columns});
            out.flush();
            try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
                byte[] compressed = (byte[]) in.readObject();
                try (MyDecompressorInputStream dec =
                             new MyDecompressorInputStream(new ByteArrayInputStream(compressed))) {
                    return new Maze(dec.readAllBytes());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Generate request to localhost:" + generatePort + " failed", e);
        }
    }

    public Solution solve(Maze maze) {
        if (!remote) {
            BestFirstSearch searcher = new BestFirstSearch();
            Solution sol = searcher.solve(new SearchableMaze(maze));
            // buildSolution already stamps the count, but be explicit for
            // anyone reading this — the count is always populated here.
            return sol;
        }
        try (Socket socket = new Socket("localhost", solvePort);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            out.flush();
            out.writeObject(maze);
            out.flush();
            try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
                return (Solution) in.readObject();
            }
        } catch (Exception e) {
            throw new RuntimeException("Solve request to localhost:" + solvePort + " failed", e);
        }
    }

    public boolean isRemote() {
        return remote;
    }
}
