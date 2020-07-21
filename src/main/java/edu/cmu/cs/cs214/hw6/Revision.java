package edu.cmu.cs.cs214.hw6;


import org.eclipse.jgit.lib.ObjectId;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Object that represents a revision between child and parent commit.
 */
public class Revision {

    private String parentRev;
    private String childRev;
    private ObjectId parentTreeId;
    private ObjectId childTreeId;
    private Map<String, Integer> wordFreqMap;

    /**
     * Constructor.
     * @param parent parent rev name
     * @param child child rev name
     */
    Revision (String parent, String child) {
        this.parentRev = parent;
        this.childRev = child;
    }


    /**
     * Given the diff text, calculate the frequency map.
     * Reference:
     * https://stackoverflow.com/questions/12493916/getting-commit-information-from-a-revcommit-object-in-jgit
     * @param diffText text of git diff
     * @throws IOException
     */
    void calculateFrequencyMap(String diffText) throws IOException {
        Map<String, Integer> wordFreqMap = new HashMap<>();

        // diff Text
        for (String line : diffText.split("\n")) {
            for (String word : line.split("\\W+")) {
                if (word.isEmpty()) {
                    continue;
                }
                wordFreqMap.put(word, wordFreqMap.getOrDefault(word, 0) + 1);
            }
        }
        this.wordFreqMap = wordFreqMap;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.parentRev, this.childRev);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Revision)) {
            throw new IllegalArgumentException();
        }
        Revision other = (Revision) o;
        return (this.parentRev.equals(other.parentRev)) && (this.childRev.equals(other.childRev));
    }

    ObjectId getChildTreeId() {
        return childTreeId;
    }

    ObjectId getParentTreeId() {
        return parentTreeId;
    }

    String getChildRev() {
        return childRev;
    }

    String getParentRev() {
        return parentRev;
    }

    Map<String, Integer> getWordFreqMap() {
        return wordFreqMap;
    }

    void setChildTreeId(ObjectId childTreeId) {
        this.childTreeId = childTreeId;
    }

    void setParentTreeId(ObjectId parentTreeId) {
        this.parentTreeId = parentTreeId;
    }
}
