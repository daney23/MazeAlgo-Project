package io.github.daney23.mazealgo.examples;

import io.github.daney23.mazealgo.model.algorithms.mazeGenerators.Maze;
import io.github.daney23.mazealgo.model.algorithms.search.Solution;
import io.github.daney23.mazealgo.model.io.MyDecompressorInputStream;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Round-trip demo against the two servers started by {@link RunMazeServer}:
 * asks for a maze on port 5400, then asks port 5401 to solve it, and
 * prints the maze + the solution length + a few node positions.
 */
public class RunMazeClient {

    public static void main(String[] args) throws Exception {
        Maze maze = requestMaze(20, 20);
        System.out.println("Got " + maze.getRows() + "x" + maze.getColumns() + " maze from server:");
        maze.print();

        Solution solution = requestSolution(maze);
        System.out.println("Solution path length: " + solution.getSolutionPath().size());

        // print first/last few states of the path
        int n = solution.getSolutionPath().size();
        int show = Math.min(5, n);
        for (int i = 0; i < show; i++) {
            System.out.println("  " + i + ": " + solution.getSolutionPath().get(i));
        }
        if (n > 2 * show) {
            System.out.println("  ...");
            for (int i = n - show; i < n; i++) {
                System.out.println("  " + i + ": " + solution.getSolutionPath().get(i));
            }
        }
    }

    private static Maze requestMaze(int rows, int columns) throws Exception {
        try (Socket socket = new Socket("localhost", 5400);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            out.flush();
            out.writeObject(new int[]{rows, columns});
            out.flush();
            try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
                byte[] compressed = (byte[]) in.readObject();
                try (MyDecompressorInputStream decompressor =
                             new MyDecompressorInputStream(new ByteArrayInputStream(compressed))) {
                    return new Maze(decompressor.readAllBytes());
                }
            }
        }
    }

    private static Solution requestSolution(Maze maze) throws Exception {
        try (Socket socket = new Socket("localhost", 5401);
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
