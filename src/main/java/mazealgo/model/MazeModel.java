package mazealgo.model;

import mazealgo.model.algorithms.mazeGenerators.Maze;
import mazealgo.model.algorithms.mazeGenerators.MyMazeGenerator;
import mazealgo.model.algorithms.search.BestFirstSearch;
import mazealgo.model.algorithms.search.SearchableMaze;
import mazealgo.model.algorithms.search.Solution;

/**
 * High-level model facade for the UI / ViewModel. In Phase 2 this will
 * delegate generation and solving to the server (MyServer) over a socket;
 * for now it runs the algorithms in-process so the rest of the stack
 * has a real object to bind to.
 */
public class MazeModel {

    public Maze generate(int rows, int columns) {
        return new MyMazeGenerator().generate(rows, columns);
    }

    public Solution solve(Maze maze) {
        return new BestFirstSearch().solve(new SearchableMaze(maze));
    }
}
