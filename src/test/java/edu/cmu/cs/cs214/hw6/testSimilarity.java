package edu.cmu.cs.cs214.hw6;

import edu.cmu.cs.cs214.hw6.Diff;
import edu.cmu.cs.cs214.hw6.Repo;
import edu.cmu.cs.cs214.hw6.Similarity;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class testSimilarity {

    String path1 = "src/test/resources/.gitTeam31";
    String path2 = "src/test/resources/.gitTeam13";
    Repo repo1;
    Repo repo2;

    @Test
    public void testSimSequential() throws IOException, InterruptedException {
        repo1 = new Repo(path1);
        repo2 = new Repo(path2);
        Similarity sim = new Similarity(repo1, repo2).sequential();
        List<Diff> list = sim.getNMostSimilarDiff(3);
        Assert.assertEquals(3, list.size());
        System.out.println(list);
    }

    @Test
    public void testSimParallel() throws IOException, InterruptedException {
        repo1 = new Repo(path1);
        repo2 = new Repo(path2);
        Similarity sim = new Similarity(repo1, repo2).parallel();
        List<Diff> list = sim.getNMostSimilarDiff(3);
        Assert.assertEquals(3, list.size());
        System.out.println(list);
    }

    @Test
    public void benchmarkSimilarity() throws IOException, InterruptedException {
        long startTime, totalTime;

        startTime = System.nanoTime();
        repo1 = new Repo(path1, true);
        repo2 = new Repo(path2, true);
        Similarity sim = new Similarity(repo1, repo2).sequential();
        List<Diff> list = sim.getNMostSimilarDiff(3);
        totalTime = System.nanoTime() - startTime;
        System.out.println(list);
        System.out.println("Sequential Similarity Operation work: " + totalTime / 1_000_000 + " core milliseconds");

        // Parallel
        startTime = System.nanoTime();
        repo1 = new Repo(path1, true);
        repo2 = new Repo(path2, true);
        Similarity sim2 = new Similarity(repo1, repo2).parallel();
        List<Diff> list2 = sim2.getNMostSimilarDiff(3);
        totalTime = System.nanoTime() - startTime;
        System.out.println(list2);
        System.out.println("Parallel Similarity Operation work: " + totalTime / 1_000_000 + " core milliseconds");

    }

}
