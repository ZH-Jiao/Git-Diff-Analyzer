package edu.cmu.cs.cs214.hw6;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * For making frequency map and calculate cosine similarity
 */
public class Similarity {

    private Repo repo1;
    private Repo repo2;

    private boolean parallel = false;
    private static final double COMPLETELY_DIFFERENT = 0;
    private static final double IDENTICAL = 1;

    /**
     * Number of threads in pool.
     */
    private static final int N_THREADS = 12;

    private PriorityQueue<Diff> pq;

    /**
     * Constructor.
     * @param repo1 first repo
     * @param repo2 second repo
     */
    public Similarity(Repo repo1, Repo repo2) {
        this.repo1 = repo1;
        this.repo2 = repo2;
        pq = new PriorityQueue<>(
                (a, b) -> {
                    double res = (a.getCosineSim() - b.getCosineSim());
                    if (res > 0) return 1;
                    else if (res < 0) return -1;
                    else return 0;
                });

    }

    /**
     * Set the instance into parallel mode.
     * @return Similarity
     */
    public Similarity parallel() {
        parallel = true;
        return this;
    }

    /**
     * Set the instance into sequential mode.
     * @return Similarity
     */
    public Similarity sequential() {
        parallel = false;
        return this;
    }

    /**
     * Get the n best similarity results.
     * @param n top n result
     * @return List of difference.
     * @throws IOException
     * @throws InterruptedException
     */
    public List<Diff> getNMostSimilarDiff(int n) throws IOException, InterruptedException {
        if (!parallel) {
            return getNMostSimilarDiffSequential(n);
        } else {
            return getNMostSimilarDiffParallel(n);
        }
    }

    private List<Diff> getNMostSimilarDiffSequential(int n) throws IOException {
        for (Revision revision1 : repo1.getRevisionList()) {
            for (Revision revision2 : repo2.getRevisionList()) {
                double sim = calculateSimilarity(revision1.getWordFreqMap(), revision2.getWordFreqMap());
                pq.offer(new Diff(revision1, revision2, sim));
                if (pq.size() > n) {
                    pq.poll();
                }
            }
        }
        List<Diff> res = new LinkedList<>();
        while (!pq.isEmpty()) {
            res.add(0, pq.poll());
        }
        return res;
    }

    private double calculateSimilarity(Map<String, Integer> freq1, Map<String, Integer> freq2) {
        // distance
        double dist1 = 0;
        double dist2 = 0;

        if (freq1 == null || freq1.isEmpty() || freq2 == null || freq2.isEmpty()) {
            return COMPLETELY_DIFFERENT;
        }

        // dot product
        double dotProduct = 0;
        for (Map.Entry<String, Integer> curr : freq1.entrySet()) {
            dotProduct += curr.getValue() * freq2.getOrDefault(curr.getKey(), 0);
            dist1 += Math.pow(curr.getValue(), 2);
        }
        // could choose the shorter doc

        dist1 = Math.sqrt(dist1);
        for (Integer num : freq2.values()) {
            dist2 += Math.pow(num, 2);
        }
        dist2 = Math.sqrt(dist2);

        double res = dotProduct / (dist1 * dist2);

        if (res > 1) {
            return IDENTICAL;
        } else if (res < 0) {
            return COMPLETELY_DIFFERENT;
        } else {
            return res;
        }
    }

    private List<Diff> getNMostSimilarDiffParallel(int n) throws InterruptedException {
        PriorityBlockingQueue<Diff> queue = new PriorityBlockingQueue<>();

        ExecutorService pool = Executors.newFixedThreadPool(N_THREADS);
        List<SimCalculation> list = new ArrayList<>();

        for (Revision revision1 : repo1.getRevisionList()) {
            for (Revision revision2 : repo2.getRevisionList()) {
                list.add(new SimCalculation(revision1, revision2, queue));
            }
        }

        pool.invokeAll(list);
        pool.shutdown();

        List<Diff> res = new LinkedList<>();
//        System.out.println(queue);
        while (res.size() < n && !queue.isEmpty()) {
            res.add(queue.poll());
        }
        return res;
    }

    /**
     * Class for parallel calculation of similarity.
     */
    class SimCalculation implements Callable<Object> {
        private Revision r1;
        private Revision r2;
        private BlockingQueue<Diff> queue;

        SimCalculation(Revision r1, Revision r2, BlockingQueue<Diff> queue) {
            this.r1 = r1;
            this.r2 = r2;
            this.queue = queue;
        }

        @Override
        public Object call() throws Exception {
            double sim = calculateSimilarity(r1.getWordFreqMap(), r2.getWordFreqMap());
            Diff diff = new Diff(r1, r2, sim);
            queue.add(diff);
            return null;
        }
    }
}
