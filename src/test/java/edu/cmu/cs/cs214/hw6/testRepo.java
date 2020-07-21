package edu.cmu.cs.cs214.hw6;

import edu.cmu.cs.cs214.hw6.Repo;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class testRepo {

    private String path = "src/test/resources/.gitTeam31";
    private Repo repo;
    int revisionCount = 145;

    @Test
    public void testRepoSequential() {
        assertNotNull(repo = new Repo(path));
//        System.out.println(repo.getRevisionList());
        revisionCount = repo.getRevisionList().size();
    }

    @Test
    public void testRepoParallel() {
        assertNotNull(repo = new Repo(path, true));
        Assert.assertEquals(revisionCount, repo.getRevisionList().size());
    }

    @Test
    public void benchmarkRepo() {
        // warm up
        repo = new Repo(path);

        // benchmark
        long startTime, totalTime;

        startTime = System.nanoTime();
        Repo sequential = new Repo(path);
        totalTime = System.nanoTime() - startTime;
        System.out.println("Sequential Repo Operation work: " + totalTime / 1_000_000 + " core milliseconds");

        startTime = System.nanoTime();
        Repo parallel = new Repo(path, true);
        totalTime = System.nanoTime() - startTime;
        System.out.println("Parallel Repo Operation work: " + totalTime / 1_000_000 + " core milliseconds");

    }





}
