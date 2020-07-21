# Homework 6 discussion

## Overview

I create Diff, Repo, Revision, Similarity as the classes to represent this task. In Repo and Similarity, I implemented different methods to execute the program sequentially and parallely under different setting. For each of those implementations, I did test and benchmark on those modules separately.

## Run

To run the program,  run the Main class at ```/src/main/java/Main.java```. It will execute sequential implementation and parallel implementation in order with benchmark. You can also run the ```SequentialProgram``` and ```ParallelProgram``` seperately.

## Design

Diff, Repo, Revision, Similarity are the four key classes. 

- Repo represent a git repository. It has methods for travsal every commit to generate diff text and create parent-child pair as Revision, and invoke methods in Revision to calculate the frequency map. 
- The Revision represents the relation of a parent and a child commit, calculate and stores the frequency map. 
- The Similarity class is for calculating the similarity given two repository. It calculate similarity of each two Revisions and construct Diff objects to store the infomation of a Revision pair and cosine similarity. 
- Diff is the class for storing the calculation result between two Revisions. It implements Comparable for sorting.

## Strategy and Discussion

In the Repo class, I use different constructors with a boolean argument to determine whether the program runs in sequential or parallel mode. For the sequential implementation, I do the freqency map calculation within the loops of iterating commits. For the parallel implementation, I applied Producer-Consumer Pattern with ExecutorService. The producer thread travsal through commits and offer the Revision objects to BlockingQueue. The consumer thread invoke Revision objects to calculate the frequency. I use a volatile boolean flag to make sure the consumer keeping running until the producer finishes.

In the test of this part, I found the parallel version is 4 times faster than the sequential version (2000 milisecond to 8000 millisecond). I think it's because the travsal and freq calculation have large overhead and the producer and consumer split the work on a sweet spot.



In the Similarity class, I use a parallel() method and a sequential() to return different implementation of the class for code reuse. In sequential implementation, I travsal through all revision pairs and calculate the similarities and sorted them by maintaining a priority queue to get N best results. In parallel implementation, I originally apply the Producer-Consumer pattern, but it didn't work well. Then, I switched to the ForkJoin pattern, where I use ExecutorService to invoke a collection of SimCalculation to construct Diff objects. The SimCalculation implements Callable to do the calculation. It works better because those calculations are isolated in some sense. 

However, the test showed that the sequential Similarity runs the same or slightly faster than the parallel version (2500 millisecond to 2600 millisecond). It is like the demo in the lecture, where sequential runs better. I think the reason for this is that the calculation of simlarity is light compare to Repo, and the overhead of running the concurrent implementation compensate the efficiency of parallel comparing to sequential version.