import edu.cmu.cs.cs214.hw6.Diff;
import edu.cmu.cs.cs214.hw6.Repo;
import edu.cmu.cs.cs214.hw6.Similarity;

import java.io.IOException;
import java.util.List;

public class ParallelProgram {

    public static void main(String[] args) throws IOException, InterruptedException {
        long startTime, totalTime;
        Repo repo1;
        Repo repo2;
        String path1 = "src/test/resources/.gitTeam31";
        String path2 = "src/test/resources/.gitTeam13";
        int n = 3;

        // Parallel
        startTime = System.nanoTime();
        repo1 = new Repo(path1, true);
        repo2 = new Repo(path2, true);
        Similarity sim2 = new Similarity(repo1, repo2).parallel();
        List<Diff> list2 = sim2.getNMostSimilarDiff(n);
        totalTime = System.nanoTime() - startTime;
        System.out.println("Parallel Similarity Operation work: " + totalTime / 1_000_000 + " core milliseconds");
        System.out.println(list2);

    }
}
