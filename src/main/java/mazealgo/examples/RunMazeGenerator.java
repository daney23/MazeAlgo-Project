package mazealgo.examples;

import mazealgo.model.algorithms.mazeGenerators.EmptyMazeGenerator;
import mazealgo.model.algorithms.mazeGenerators.IMazeGenerator;
import mazealgo.model.algorithms.mazeGenerators.Maze;
import mazealgo.model.algorithms.mazeGenerators.MyMazeGenerator;
import mazealgo.model.algorithms.mazeGenerators.Position;
import mazealgo.model.algorithms.mazeGenerators.SimpleMazeGenerator;

public class RunMazeGenerator {

    public static void main(String[] args) {
        testMazeGenerator(new EmptyMazeGenerator());
        testMazeGenerator(new SimpleMazeGenerator());
        testMazeGenerator(new MyMazeGenerator());
    }

    private static void testMazeGenerator(IMazeGenerator mazeGenerator) {
        // prints the time it takes the algorithm to run
        System.out.println(String.format("Maze generation time(ms): %s", mazeGenerator.measureAlgorithmTimeMillis(100/*rows*/,100/*columns*/)));
        // generate another maze
        Maze maze = mazeGenerator.generate(100/*rows*/, 100/*columns*/);

        // prints the maze
        maze.print();

        // get the maze entrance
        Position startPosition = maze.getStartPosition();

        // print the start position
        System.out.println(String.format("Start Position: %s", startPosition)); // format "{row,column}"

        // prints the maze exit position
        System.out.println(String.format("Goal Position: %s", maze.getGoalPosition()));
    }
}
