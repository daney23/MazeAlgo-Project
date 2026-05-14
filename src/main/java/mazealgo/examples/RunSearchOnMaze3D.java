package mazealgo.examples;

import mazealgo.model.algorithms.maze3D.Maze3D;
import mazealgo.model.algorithms.maze3D.MyMaze3DGenerator;
import mazealgo.model.algorithms.maze3D.SearchableMaze3D;
import mazealgo.model.algorithms.search.AState;
import mazealgo.model.algorithms.search.BestFirstSearch;
import mazealgo.model.algorithms.search.BreadthFirstSearch;
import mazealgo.model.algorithms.search.DepthFirstSearch;
import mazealgo.model.algorithms.search.ISearchable;
import mazealgo.model.algorithms.search.ISearchingAlgorithm;
import mazealgo.model.algorithms.search.Solution;

import java.util.ArrayList;

public class RunSearchOnMaze3D {

    public static void main(String[] args) {
        Maze3D maze = new MyMaze3DGenerator().generate(10, 10, 10);
        SearchableMaze3D searchableMaze = new SearchableMaze3D(maze);

        solveProblem(searchableMaze, new BreadthFirstSearch());
        solveProblem(searchableMaze, new DepthFirstSearch());
        solveProblem(searchableMaze, new BestFirstSearch());
    }

    private static void solveProblem(ISearchable domain, ISearchingAlgorithm searcher) {
        Solution solution = searcher.solve(domain);
        System.out.println(String.format("'%s' algorithm - nodes evaluated: %s", searcher.getName(), searcher.getNumberOfNodesEvaluated()));
        System.out.println("Solution path:");
        ArrayList<AState> solutionPath = solution.getSolutionPath();
        for (int i = 0; i < solutionPath.size(); i++) {
            System.out.println(String.format("%s.%s", i, solutionPath.get(i)));
        }
    }
}
