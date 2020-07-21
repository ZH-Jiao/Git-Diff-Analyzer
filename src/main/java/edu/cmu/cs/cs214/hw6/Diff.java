package edu.cmu.cs.cs214.hw6;

/**
 * Representation of difference of two Revision.
 */
public class Diff implements Comparable<Diff> {

    private Revision rev1;
    private Revision rev2;
    private double cosineSim;

    Diff(Revision rev1, Revision rev2, double cosineSim) {
        this.rev1 = rev1;
        this.rev2 = rev2;
        this.cosineSim = cosineSim;
    }

    public double getCosineSim() {
        return cosineSim;
    }

    public Revision getRev1() {
        return rev1;
    }

    public Revision getRev2() {
        return rev2;
    }

    @Override
    public String toString() {
        return "\nResult:\n" + "child_hash_1: " + rev1.getChildRev() + ", " + "parent_hash_1: " + rev1.getParentRev() + "; " +
                "child_hash_2: " + rev2.getChildRev() + ", " + "parent_hash_2: " + rev2.getParentRev() + "; " +
                "similarity: " + cosineSim;
    }


    @Override
    public int compareTo(Diff o) {
        double res = (this.getCosineSim() - o.getCosineSim());
        if (res > 0) return -1;
        else if (res < 0) return 1;
        else return 0;
    }
}
