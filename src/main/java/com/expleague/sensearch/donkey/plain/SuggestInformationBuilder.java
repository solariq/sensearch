package com.expleague.sensearch.donkey.plain;

import com.expleague.sensearch.Page;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.index.plain.IndexTerm;
import com.expleague.sensearch.protobuf.index.IndexUnits;
import com.google.common.primitives.Longs;
import com.google.inject.Inject;
import gnu.trove.map.TLongDoubleMap;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TLongDoubleHashMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.WriteBatch;
import org.iq80.leveldb.WriteOptions;

public class SuggestInformationBuilder {

  private final int maxNgramsOrder = 3;

  private int ndocs;

  private final TObjectIntMap<long[]> multigramFreq = new TObjectIntHashMap<>();
  private final TLongIntMap unigramFreq = new TLongIntHashMap();

  private final TLongIntMap unigramDF = new TLongIntHashMap();
  private final TLongDoubleMap sumFreqNorm = new TLongDoubleHashMap();

  private final double[] avgOrderFreq = new double[maxNgramsOrder];

  private static final WriteOptions DEFAULT_WRITE_OPTIONS =
      new WriteOptions().sync(true).snapshot(false);

  // Maps, that used in suggestor
  private final TLongDoubleMap unigramCoeff = new TLongDoubleHashMap();
  private final TObjectDoubleMap<long[]> multigramFreqNorm = new TObjectDoubleHashMap<>();

  private final DB unigramCoeffDB;
  private final DB multigramFreqNormDB;

  private final Index index;
  
  public void build() throws IOException {
    useIndex();
    computeAvgOrderFreq();
    computeFreqNorm();
    computeTargetMaps();
    saveTargets();
  }
  
  private void useIndex() {
    index
    .allDocuments()
    .forEach(d -> {
      accept(toTermIds(d.content(Page.SegmentType.FULL_TITLE)), d.incomingLinksCount(Page.LinkType.ALL_LINKS));
    });
  }
  
  private long[] toTermIds(CharSequence text) {
    return index.parse(text)
        .map(t -> ((IndexTerm) t).id())
        .mapToLong(i -> i)
        .toArray();
  }
  
  private void accept(long[] wordIds, int docIncomingLinks) {
    computeUnigrams(wordIds);
    computeMultigrams(wordIds, docIncomingLinks);
  }

  private void saveTargets() throws IOException {

    {
      WriteBatch batch = unigramCoeffDB.createWriteBatch();
      unigramCoeff.forEachEntry(
          (key, value) -> {
            batch.put(Longs.toByteArray(key), Longs.toByteArray(Double.doubleToLongBits(value)));
            return true;
          });

      unigramCoeffDB.write(batch, DEFAULT_WRITE_OPTIONS);
      batch.close();
    }

    {
      WriteBatch batch = multigramFreqNormDB.createWriteBatch();
      multigramFreqNorm.forEachEntry(
          (key, value) -> {
            List<Long> l = Arrays.stream(key).boxed().collect(Collectors.toList());

            batch.put(
                IndexUnits.TermList.newBuilder().addAllTermList(l).build().toByteArray(),
                Longs.toByteArray(Double.doubleToLongBits(value)));

            return true;
          });
      multigramFreqNormDB.write(batch, DEFAULT_WRITE_OPTIONS);
      batch.close();
    }

  }

  @Inject
  public SuggestInformationBuilder(Index index, DB unigramCoeffDB, DB multigramFreqNormDB) {
    this.index = index;
    this.unigramCoeffDB = unigramCoeffDB;
    this.multigramFreqNormDB = multigramFreqNormDB;
  }

  private void computeUnigrams(long[] wordIds) {
    Arrays.stream(wordIds)
    .peek(s -> {
      unigramFreq.putIfAbsent(s, 0);
      unigramFreq.increment(s);
    })
    .distinct()
    .forEach(s -> {
      unigramDF.putIfAbsent(s, 0);
      unigramDF.increment(s);
    });
  }

  private List<long[]> getNgrams(long[] wordsIds, int order) {

    List<long[]> result = new ArrayList<>();

    for (int i = 0; i < wordsIds.length - order + 1; i++) {
      result.add(Arrays.copyOfRange(wordsIds, i, i + order));
    }

    return result;
  }

  private void computeMultigrams(long[] wordIds, int docIncomingLinks) {
    for (int i = 1; i <= maxNgramsOrder; i++) {
      getNgrams(wordIds, i).forEach(l -> {
        multigramFreq.adjustOrPutValue(l, docIncomingLinks + 1, docIncomingLinks + 1);
      });
    }
  }

  private void computeAvgOrderFreq() {
    double[] countOfOrder = new double[maxNgramsOrder];

    multigramFreq
    .forEachEntry((key, value) -> {
      int idx = key.length - 1;
      countOfOrder[idx]++;
      avgOrderFreq[idx] += value;
      return true;
    });

    for (int i = 0; i < maxNgramsOrder; i++) {
      if (countOfOrder[i] > 0)
        avgOrderFreq[i] /= countOfOrder[i];
    }
  }

  private double freqNorm(long[] l) {
    return multigramFreq.get(l) / Math.log(1 + avgOrderFreq[l.length - 1]);
  }

  private void computeFreqNorm() {
    for (long[] l : multigramFreq.keySet()) {
      double fNorm = freqNorm(l);
      for (long s : l) {
        sumFreqNorm.putIfAbsent(s, 0.0);
        sumFreqNorm.put(s, sumFreqNorm.get(s) + fNorm);
      }
    }
  }

  private void computeTargetMaps() {
    multigramFreq.keySet().forEach(mtgr -> multigramFreqNorm.put(mtgr, freqNorm(mtgr)));

    unigramFreq
    .keySet()
    .forEach(
        ung -> {
          unigramCoeff.put(
              ung,
              unigramFreq.get(ung)
              * Math.log(1.0 * ndocs / unigramDF.get(ung))
              / sumFreqNorm.get(ung));
          return true;
        });
  }
}
