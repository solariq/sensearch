package com.expleague.sensearch.experiments.joom;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.VecTools;
import com.expleague.commons.seq.LongSeq;
import com.expleague.commons.util.ArrayTools;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.procedure.TLongProcedure;
import gnu.trove.set.hash.TIntHashSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class QtCluster {

  private final double qualityThreshold;

  public QtCluster(double qualityThreshold) {
    this.qualityThreshold = qualityThreshold;
  }

  public List<TIntList> cluster(List<Vec> vecs) {

    //          for (int u = 0; u < cooc.length(); u++) {
    //            Vec vecA = symDecomp.get(u);
    //            qualityThreshold += VecTools.cosine(sym_i, vecA);
    //          }
    //          qualityThreshold /= cooc.length();
    //          qualityThreshold = Math.max(0, qualityThreshold);
    double[] counters = new double[vecs.size()];
    final TLongArrayList validPairs = new TLongArrayList();
    Arrays.fill(counters, Double.NaN);

    for (int u = 0; u < vecs.size(); u++) {
      final Vec vecU = vecs.get(u);
      final double norm_u = VecTools.norm(vecU);
      counters[u] = 1;
      for (int v = u + 1; v < vecs.size(); v++) {
        final Vec vecV = vecs.get(v);
        final double norm_v = VecTools.norm(vecV);
        if (VecTools.multiply(vecU, vecV) / norm_u / norm_v > qualityThreshold) {
          validPairs.add(((long) (u + 1) << 32) | (v + 1));
          validPairs.add(((long) (v + 1) << 32) | (u + 1));
        }
      }
    }
    validPairs.sort();
    validPairs.forEach(
        p -> {
          final int u = (int) (p >>> 32) - 1;
          final int v = (int) (p & 0xFFFFFFFFL) - 1;
          counters[u] += 1;
          return true;
        });
    List<TIntHashSet> clusters = new ArrayList<>();
    List<TIntList> wordClusters = new ArrayList<>();
    while (true) {
      int max = ArrayTools.max(counters);
      if (max < 0) {
        break;
      }
      counters[max] = Double.NaN;
      final TIntHashSet cluster = new TIntHashSet();
      final TIntList wordsCluster = new TIntArrayList();
      cluster.add(max);
      wordsCluster.add(max);
      { // form cluster
        int index = -validPairs.binarySearch((long) (max + 1) << 32) - 1;
        long limit = ((long) (max + 2) << 32);
        long p;
        while (index < validPairs.size() && (p = validPairs.getQuick(index)) < limit) {
          int v = (int) (p & 0xFFFFFFFFL) - 1;
          if (!Double.isNaN(counters[v])) {
            counters[v] = Double.NaN;
            cluster.add(v);
            wordsCluster.add(v);
          }
          index++;
        }
      }
      validPairs.forEach(
          new TLongProcedure() {
            int current = 0;
            float currentWeight;

            @Override
            public boolean execute(long p) { // update counters
              int u = (int) (p >>> 32) - 1;
              int v = (int) (p & 0xFFFFFFFFL) - 1;
              if (u != current) {
                current = u;
                currentWeight = cluster.contains(u) ? 1 : 0.f;
              }
              if (currentWeight != 0f) {
                counters[v] -= currentWeight;
              }
              return true;
            }
          });
      clusters.add(cluster);
      if (cluster.size() == 1) {
        continue;
      }

      wordClusters.add(wordsCluster);
    }

    return wordClusters;
  }

  private float unpackWeight(LongSeq cooc, int v) {
    return Float.intBitsToFloat((int) (cooc.longAt(v) & 0xFFFFFFFFL));
  }

  private int unpackB(LongSeq cooc, int v) {
    return (int) (cooc.longAt(v) >>> 32);
  }
}
