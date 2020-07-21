package edu.cmu.cs.cs214.hw6;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Representation of a git repository
 */
public class Repo {
    private Git git;
    private Repository repo;
    private List<Revision> revisionList;
    private boolean parallelMode;
    private volatile boolean producing;

    private static final int N_THREADS = 8;
    private static final int QUEUE_SIZE = 50;


    /**
     * Default constructor for sequential
     * @param path filepath
     */
    public Repo(String path) {
        this(path, false);
    }

    /**
     * Constructor with parallel mode flag.
     * @param path file path
     * @param parallelMode boolean
     */
    public Repo(String path, boolean parallelMode) {
        this.parallelMode = parallelMode;
        try {
            File gitFile = new File(path);
            this.git = Git.open(gitFile);
            this.repo = git.getRepository();
            if (parallelMode) {
                this.revisionList = makeRevisionListParallel();
            } else {
                this.revisionList = makeRevisionListSequential();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Create the list of Revision from the repo with sequential implementation.
     * Reference:
     * https://github.com/centic9/jgit-cookbook/blob/master/src/main/java/org/dstadler/jgit/porcelain/ShowChangedFilesBetweenCommits.java
     * https://stackoverflow.com/questions/15822544/jgit-how-to-get-all-commits-of-a-branch-without-changes-to-the-working-direct
     */
    private List<Revision> makeRevisionListSequential() {
        // The {tree} will return the underlying tree-id instead of the commit-id itself!
        // For a description of what the carets do see e.g. http://www.paulboxley.com/blog/2011/06/git-caret-and-tilde
        // This means we are selecting the parent of the parent of the parent of the parent of current HEAD and
        // take the tree-ish of it
        // ObjectId oldHead = repo.resolve("HEAD^^^^{tree}");
        // ObjectId head = repo.resolve("HEAD^{tree}");
        HashSet<Revision> set = new HashSet<>();

        // prepare the two iterators to compute the diff between
        try {
            ObjectReader reader = repo.newObjectReader();

            List<Ref> branchs = git.branchList().call();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            DiffFormatter df = new DiffFormatter(out);
            df.setRepository(repo);

            CanonicalTreeParser parentTreeIter = new CanonicalTreeParser();
            CanonicalTreeParser childTreeIter = new CanonicalTreeParser();

            // iter in each branch
            for (Ref branch : branchs) {
                // iter all commits in the branch
                // Reference #2
                for (RevCommit commit : git.log().add(repo.resolve(branch.getName())).call()) {
                    // multiple parents
                    List<RevCommit> parents = new ArrayList<>();
                    Collections.addAll(parents, commit.getParents());
                    ObjectId childTreeId = commit.getTree().getId();

                    for (RevCommit parent : parents) {
                        ObjectId parentTreeId = parent.getTree().getId();
                        parentTreeIter.reset(reader, parentTreeId);
                        childTreeIter.reset(reader, childTreeId);

                        Revision revision = new Revision(parent.getName(), commit.getName());

                        if (!set.contains(revision)) {
                            // not duplicate
                            df.format(parentTreeId, childTreeId);
//                        System.out.println(out.size());
                            String diffText = out.toString("UTF-8");
                            revision.calculateFrequencyMap(diffText);
                            set.add(revision);
                        }
                        out.reset();
                    }
                }
            }
            out.close();
            df.close();

        } catch (IOException | GitAPIException e) {
            e.printStackTrace();
        }

        System.out.println("Number of revisions: " + set.size());
        return new ArrayList<>(set);
    }

    /**
     * Create list of revision list with parallel implementation.
     * @return List
     */
    private List<Revision> makeRevisionListParallel() {
        ConcurrentMap<Revision, Revision> set = new ConcurrentHashMap<>();
        ExecutorService pool = Executors.newFixedThreadPool(N_THREADS);
        BlockingQueue<Future<Revision>> blockingQueue = new ArrayBlockingQueue<>(QUEUE_SIZE);
        producing = true;
        // prepare the two iterators to compute the diff between
        try {

            List<Ref> branchs = git.branchList().call();
            Thread producerThread = new Thread(() -> {
                // iter in each branch
                for (Ref branch : branchs) {
                    // iter all commits in the branch
                    // Reference #2
                    try {
                        for (RevCommit commit : git.log().add(repo.resolve(branch.getName())).call()) {
                            // multiple parents
                            List<RevCommit> parents = new ArrayList<>();
                            Collections.addAll(parents, commit.getParents());
                            ObjectId childTreeId = commit.getTree().getId();
                            ObjectReader reader = repo.newObjectReader();

                            CanonicalTreeParser parentTreeIter = new CanonicalTreeParser();
                            CanonicalTreeParser childTreeIter = new CanonicalTreeParser();

                            for (RevCommit parent : parents) {
                                ObjectId parentTreeId = parent.getTree().getId();

                                parentTreeIter.reset(reader, parentTreeId);
                                childTreeIter.reset(reader, childTreeId);

                                Revision revision = new Revision(parent.getName(), commit.getName());

                                if (!set.containsKey(revision)) {
                                    // not duplicate
                                    revision.setChildTreeId(childTreeId);
                                    revision.setParentTreeId(parentTreeId);
                                    set.put(revision, revision);
                                    blockingQueue.offer(pool.submit(() -> revision));
                                }

                            }
                        }
                    } catch (GitAPIException | IOException e) {
                        e.printStackTrace();
                    }
                }
            });


            Thread consumerThread = new Thread(() -> {

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                DiffFormatter df = new DiffFormatter(out);
                df.setRepository(repo);

                while (producing || !blockingQueue.isEmpty()) {
                    if (producing && blockingQueue.isEmpty()) {
                        continue;
                    }
                    Revision revision = null;
                    try {
                        revision = blockingQueue.take().get();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                    try {
                        df.format(revision.getParentTreeId(), revision.getChildTreeId());
                        String diffText = out.toString("UTF-8");

                        revision.calculateFrequencyMap(diffText);
                        out.reset();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                df.close();


            });

            // Thread start
            producerThread.start();
            consumerThread.start();
            producerThread.join();
            producing = false;
            consumerThread.join();


        } catch (GitAPIException | InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Number of revisions: " + set.size());
        return new ArrayList<>(set.keySet());
    }


    // Accessors
    public Git getGit() {
        return git;
    }
    public Repository getRepo() {
        return repo;
    }
    public List<Revision> getRevisionList() {
        return revisionList;
    }
}
