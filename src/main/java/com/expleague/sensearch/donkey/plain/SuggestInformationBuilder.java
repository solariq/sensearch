package com.expleague.sensearch.donkey.plain;

import com.expleague.sensearch.protobuf.index.IndexUnits;
import com.google.common.primitives.Longs;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.WriteBatch;
import org.iq80.leveldb.WriteOptions;

public class SuggestInformationBuilder {

  private final int maxNgramsOrder = 3;

  private int ndocs;

  private final Map<long[], Integer> multigramFreq = new HashMap<>();
  private final Map<Long, Integer> unigramFreq = new HashMap<>();
  private final Map<Long, Integer> unigramDF = new HashMap<>();
  private final Map<Long, Double> sumFreqNorm = new HashMap<>();

  private final double[] avgOrderFreq = new double[maxNgramsOrder];

  private static final WriteOptions DEFAULT_WRITE_OPTIONS =
      new WriteOptions().sync(true).snapshot(false);

  // Maps, that used in suggestor
  private final Map<Long, Double> unigramCoeff = new HashMap<>();
  private final Map<long[], Double> multigramFreqNorm = new HashMap<>();
  private final Map<Long, List<Integer>> invertedIndex = new HashMap<>();

  private final DB unigramCoeffDB;
  private final DB multigramFreqNormDB;
  private final DB invertedIndexDB;

  private <K> void addToMap(Map<K, Integer> m, K key, int inc) {
    Integer oldVal = m.get(key);
    int oVal = oldVal == null ? 0 : oldVal;
    m.put(key, oVal + inc);
  }

  public void build() {
    // computeUnigrams(titles);
    // computeMultigrams(titles);
    computeAvgOrderFreq();
    computeFreqNorm();
    computeTargetMaps();
    saveTargets();
  }

  public void accept(long[] wordIds) {
    computeUnigrams(wordIds);
    computeMultigrams(wordIds);
  }

  private void saveTargets() {

    {
      WriteBatch batch = unigramCoeffDB.createWriteBatch();
      unigramCoeff.forEach(
          (key, value) ->
              batch.put(Longs.toByteArray(key), Longs.toByteArray(Double.doubleToLongBits(value))));
      unigramCoeffDB.write(batch, DEFAULT_WRITE_OPTIONS);
    }

    {
      WriteBatch batch = multigramFreqNormDB.createWriteBatch();
      multigramFreqNorm.forEach(
          (key, value) -> {
            List<Long> l = Arrays.stream(key).boxed().collect(Collectors.toList());

            batch.put(
                IndexUnits.TermList.newBuilder().addAllTermList(l).build().toByteArray(),
                Longs.toByteArray(Double.doubleToLongBits(value)));
          });
      multigramFreqNormDB.write(batch, DEFAULT_WRITE_OPTIONS);
    }

    {
      WriteBatch batch = invertedIndexDB.createWriteBatch();
      invertedIndex.forEach(
          (key, value) ->
              batch.put(
                  Longs.toByteArray(key),
                  IndexUnits.IntegerList.newBuilder().addAllIntList(value).build().toByteArray()));
      invertedIndexDB.write(batch, DEFAULT_WRITE_OPTIONS);
    }
  }

  @Inject
  public SuggestInformationBuilder(DB unigramCoeffDB, DB multigramFreqNormDB, DB invertedIndexDB) {
    this.unigramCoeffDB = unigramCoeffDB;
    this.multigramFreqNormDB = multigramFreqNormDB;
    this.invertedIndexDB = invertedIndexDB;
  }

  private void computeUnigrams(long[] wordIds) {
    int docNum = ndocs++;
    Arrays.stream(wordIds)
        .peek(
            s -> {
              addToMap(unigramFreq, s, 1);
              sumFreqNorm.put(s, 0.0);
              if (!invertedIndex.containsKey(s)) {
                invertedIndex.put(s, new ArrayList<>());
              }
              invertedIndex.get(s).add(docNum);
            })
        .distinct()
        .forEach(s -> addToMap(unigramDF, s, 1));
  }

  private List<long[]> getNgrams(long[] wordsIds, int order) {

    List<long[]> result = new ArrayList<>();

    for (int i = 0; i < wordsIds.length - order + 1; i++) {
      result.add(Arrays.copyOfRange(wordsIds, i, i + order));
    }

    return result;
  }

  private void computeMultigrams(long[] wordIds) {
    for (int i = 1; i <= maxNgramsOrder; i++) {
      getNgrams(wordIds, i).forEach(l -> addToMap(multigramFreq, l, 1));
    }
  }

  private void computeAvgOrderFreq() {
    double[] countOfOrder = new double[maxNgramsOrder];

    multigramFreq
        .forEach((key, value) -> {
          int idx = key.length - 1;
          countOfOrder[idx]++;
          avgOrderFreq[idx] += value;
        });

    for (int i = 1; i < maxNgramsOrder; i++) {
      avgOrderFreq[i] /= countOfOrder[i];
    }
  }

  private double freqNorm(long[] l) {
    return multigramFreq.get(l) / Math.log(avgOrderFreq[l.length - 1]);
  }

  private void computeFreqNorm() {
    for (long[] l : multigramFreq.keySet()) {
      double fNorm = freqNorm(l);
      for (long s : l) {
        sumFreqNorm.put(s, sumFreqNorm.get(s) + fNorm);
      }
    }
  }

  private void computeTargetMaps() {
    multigramFreq.keySet().forEach(mtgr -> multigramFreqNorm.put(mtgr, freqNorm(mtgr)));

    unigramFreq
        .keySet()
        .forEach(
            ung ->
                unigramCoeff.put(
                    ung,
                    unigramFreq.get(ung)
                        * Math.log(1.0 * ndocs / unigramDF.get(ung))
                        / sumFreqNorm.get(ung)));
  }
}
