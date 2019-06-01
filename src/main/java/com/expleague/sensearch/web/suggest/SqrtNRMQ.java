package com.expleague.sensearch.web.suggest;

import java.util.Arrays;
import java.util.stream.DoubleStream;

public class SqrtNRMQ {
  private final int numberMaximums;

  private final int sqrt;
  private final int[][] sqrtTable; //idx, maximums
  private final double[] values;

  private int greaterSqrtIdx(int lowerBound) {
    return lowerBound / sqrt + 1;
  }

  private int lowerSqrtIdx(int upperBound) {
    return upperBound / sqrt;
  }

  private final void insert(int[] idxs, int newIdx) {
    if (newIdx < 0)
      return;

    int i;
    for (i = 0; i < idxs.length; i++) {
      if (idxs[i] == newIdx) {
        return;
      }

      if (idxs[i] == -1 || values[idxs[i]] < values[newIdx]) {
        break;
      }
    }

    for (; i < idxs.length; i++) {
      int t = idxs[i];
      idxs[i] = newIdx;
      newIdx = t;
    }
  }

  private final int[] getAnswerForRangeLinearly(int lowerBound, int upperBound) {
    int[] res = new int[numberMaximums];
    Arrays.fill(res, -1);

    for (int i = lowerBound; i < upperBound; i++) {
      insert(res, i);
    }

    return res;
  }


  private final void updateMaximumsIdxs(int[] updated, int[] idxs2) {
    for (int i : idxs2) {
      insert(updated, i);
    }
  }

  private void initSqrtTable() {
    for (int sqNum = 0; sqNum < sqrtTable.length; sqNum++) {
      sqrtTable[sqNum] = getAnswerForRangeLinearly(sqNum * sqrt, (sqNum + 1) * sqrt);
    }
  }

  public SqrtNRMQ(int numberMaximums, DoubleStream values) {
    this.numberMaximums = numberMaximums;
    this.values = values.toArray();

    sqrt = Math.toIntExact(Math.round(Math.floor(Math.sqrt(this.values.length))));

    sqrtTable = new int[this.values.length / sqrt][];
    initSqrtTable();
  }
  
  private int[] trimResult(int[] res) {
    int i = 0;
    for (i = 0; i < res.length; i++) {
      if (res[i] == -1) {
        break;
      }
    }
    
    return Arrays.copyOf(res, i);
  }

  /**
   * 
   * @param lowerBound нижняя граница, включительно
   * @param upperBound верхняя граница, не включая
   * @return
   */
  public int[] getMaximumIdxs(int lowerBound, int upperBound) {
    int lSqBound = greaterSqrtIdx(lowerBound);
    int uSqBound = lowerSqrtIdx(upperBound);

    int[] res = null;
    if (uSqBound > lSqBound) {
      res = getAnswerForRangeLinearly(lowerBound, lSqBound * sqrt);
      for (int i = lSqBound; i < uSqBound; i++) {
        updateMaximumsIdxs(res, sqrtTable[i]);
      }

      int[] upperAns = getAnswerForRangeLinearly(uSqBound * sqrt, upperBound);
      updateMaximumsIdxs(res, upperAns);
    } else {
      res = getAnswerForRangeLinearly(lowerBound, upperBound);
    }
    
    return trimResult(res);
  }
}
