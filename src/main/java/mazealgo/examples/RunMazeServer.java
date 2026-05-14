package mazealgo.examples;

import mazealgo.model.server.GenerateMazeStrategy;
import mazealgo.model.server.MyServer;
import mazealgo.model.server.SolveMazeStrategy;

import java.util.Scanner;

/**
 * Boots two {@link MyServer} instances: one hosting
 * {@link GenerateMazeStrategy} on port 5400, one hosting
 * {@link SolveMazeStrategy} on port 5401. Press Enter to stop.
 *
 * <p>Pair with {@link RunMazeClient} to see the full round-trip.
 */
public class RunMazeServer {

    public static void main(String[] args) {
        MyServer generateServer = new MyServer(5400, 1000, new GenerateMazeStrategy());
        MyServer solveServer = new MyServer(5401, 1000, new SolveMazeStrategy());

        generateServer.start();
        solveServer.start();

        System.out.println("Servers running on 5400 (generate) and 5401 (solve).");
        System.out.println("Press Enter to stop...");
        try (Scanner scanner = new Scanner(System.in)) {
            scanner.nextLine();
        }

        generateServer.stop();
        solveServer.stop();
        System.out.println("Stopped.");
    }
}
