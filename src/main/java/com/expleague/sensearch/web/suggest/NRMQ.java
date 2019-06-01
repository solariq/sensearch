package com.expleague.sensearch.web.suggest;

import java.util.stream.DoubleStream;
import org.apache.commons.lang3.ArrayUtils;

public class NRMQ {
  private final int numberMaximums;


  private final int[][][] sparseTable; //pow, idx, maximums
  private final double[] values;

  private int[] get2power(int n) {
    int pow = 0;
    int v = 1;

    while (2 * v <= n) {
      pow++;
      v *= 2;
    }

    return new int[] {pow, v};
  }
  /*
  private final void sortIndices(int[] idxs) {
    for (int i = 1; i < idxs.length; i++) {
      for (int j = i - 1; j >= 0; j--) {
        if (values[idxs[j]] < values[idxs[j+1]]) {
          int t = idxs[j];
          idxs[j] = idxs[i];
          idxs[i] = t;
        }
      }
    }
  }
   */
  private final int[] getMaximumsIdxs(int[] idxs1, int[] idxs2) {
    /*return Stream.concat(
        Arrays.stream(idxs1).mapToObj(i -> i),
        Arrays.stream(idxs2).mapToObj(i -> i))
        .distinct()
        .sorted(Comparator.comparingDouble(i -> -values[i]))
        .limit(numberMaximums)
        .mapToInt(i -> i)
        .toArray();*/

    int[] res = new int[Math.min(idxs1.length + idxs2.length, numberMaximums)];

    int k = 0, i = 0, j = 0;

    while (k < res.length && i < idxs1.length && j < idxs2.length) {
      int cidx;
      if (values[idxs1[i]] > values[idxs2[j]]) {
        cidx = idxs1[i++];
      } else {
        cidx = idxs2[j++];
      }

      if (k == 0 || cidx != res[k - 1]) {
        res[k++] = cidx;
      }
    }

    while (k < res.length && i < idxs1.length) {
      int cidx = idxs1[i++];
      if (k == 0 || cidx != res[k - 1]) {
        res[k++] = cidx;
      }
    }

    while (k < res.length && j < idxs2.length) {
      int cidx = idxs2[j++];
      if (k == 0 || cidx != res[k - 1]) {
        res[k++] = cidx;
      }
    }

    if (k == res.length) {
      return res;
    } else {
      return ArrayUtils.subarray(res, 0, k);
    }
  }

  private void initSparseTable() {

    sparseTable[0] = new int[values.length][1];

    for (int i = 0; i < values.length; i++) {
      sparseTable[0][i][0] = i;
    }

    for (int pow = 1, v = 2; pow < sparseTable.length; pow++, v *= 2) {

      int numberRanges = values.length - v + 1;
      sparseTable[pow] = new int[numberRanges][Math.min(v, numberMaximums)];

      for (int i = 0; i < numberRanges; i++) {
        sparseTable[pow][i] = getMaximumsIdxs(
            sparseTable[pow - 1][i],
            sparseTable[pow - 1][i + v / 2]);
      }

      //System.err.println("Sparse table for power " + pow + " built.");
    }
  }

  public NRMQ(int numberMaximums, DoubleStream values) {
    this.numberMaximums = numberMaximums;
    this.values = values.toArray();

    int maxPower = get2power(this.values.length)[0];

    sparseTable = new int[maxPower + 1][][];
    initSparseTable();
  }

  /**
   * 
   * @param lowerBound нижняя граница, включительно
   * @param upperBound верхняя граница, не включая
   * @return
   */
  public int[] getMaximumIdxs(int lowerBound, int upperBound) {
    int[] pow = get2power(upperBound - lowerBound);

    return getMaximumsIdxs(
        sparseTable[pow[0]][lowerBound],
        sparseTable[pow[0]][upperBound - pow[1]]);
  }
}
