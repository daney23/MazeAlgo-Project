package mazealgo.examples;

import mazealgo.model.algorithms.mazeGenerators.IMazeGenerator;
import mazealgo.model.algorithms.mazeGenerators.Maze;
import mazealgo.model.algorithms.mazeGenerators.MyMazeGenerator;
import mazealgo.model.algorithms.search.AState;
import mazealgo.model.algorithms.search.BestFirstSearch;
import mazealgo.model.algorithms.search.BreadthFirstSearch;
import mazealgo.model.algorithms.search.DepthFirstSearch;
import mazealgo.model.algorithms.search.ISearchable;
import mazealgo.model.algorithms.search.ISearchingAlgorithm;
import mazealgo.model.algorithms.search.SearchableMaze;
import mazealgo.model.algorithms.search.Solution;

import java.util.ArrayList;

public class RunSearchOnMaze {
    public static void main(String[] args) {
        IMazeGenerator mg = new MyMazeGenerator();
        Maze maze = mg.generate(30, 30);
        SearchableMaze searchableMaze = new SearchableMaze(maze);

        solveProblem(searchableMaze, new BreadthFirstSearch());
        solveProblem(searchableMaze, new DepthFirstSearch());
        solveProblem(searchableMaze, new BestFirstSearch());
    }

    private static void solveProblem(ISearchable domain, ISearchingAlgorithm searcher) {
        //Solve a searching problem with a searcher
        Solution solution = searcher.solve(domain);
        System.out.println(String.format("'%s' algorithm - nodes evaluated: %s", searcher.getName(), searcher.getNumberOfNodesEvaluated()));
        //Printing Solution Path
        System.out.println("Solution path:");
        ArrayList<AState> solutionPath = solution.getSolutionPath();
        for (int i = 0; i < solutionPath.size(); i++) {
            System.out.println(String.format("%s.%s", i, solutionPath.get(i)));
        }
    }
}
