package io.github.daney23.mazealgo.examples;

import io.github.daney23.mazealgo.model.algorithms.maze3D.IMaze3DGenerator;
import io.github.daney23.mazealgo.model.algorithms.maze3D.Maze3D;
import io.github.daney23.mazealgo.model.algorithms.maze3D.MyMaze3DGenerator;

public class RunMaze3DGenerator {

    public static void main(String[] args) {
        IMaze3DGenerator generator = new MyMaze3DGenerator();

        // small maze: print the layers so you can eyeball it
        Maze3D small = generator.generate(3, 5, 5);
        System.out.println("3D maze (depth=3, rows=5, columns=5):");
        small.print();
        System.out.println(String.format("Start Position: %s", small.getStartPosition()));
        System.out.println(String.format("Goal Position: %s", small.getGoalPosition()));

        // mid-size timing sanity check
        long ms = generator.measureAlgorithmTimeMillis(50, 50, 50);
        System.out.println(String.format("50x50x50 generation time(ms): %s", ms));

        // spec budget: 500*500*500 should finish in under a minute
        long t0 = System.currentTimeMillis();
        new MyMaze3DGenerator().generate(500, 500, 500);
        long elapsed = System.currentTimeMillis() - t0;
        String verdict = elapsed <= 60_000 ? "PASS" : "FAIL";
        System.out.println(String.format("500x500x500 generation: %d ms  %s", elapsed, verdict));
    }
}
