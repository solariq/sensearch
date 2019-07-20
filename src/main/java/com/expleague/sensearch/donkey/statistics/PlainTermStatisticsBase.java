package com.expleague.sensearch.donkey.statistics;

import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.donkey.randomaccess.ProtoTermStatisticsIndex;
import com.expleague.sensearch.donkey.randomaccess.RandomAccess;
import com.expleague.sensearch.index.TermStatisticsBase;
import com.expleague.sensearch.index.plain.IndexTerm;
import com.expleague.sensearch.protobuf.index.IndexUnits.TermStatistics;
import com.google.common.annotations.VisibleForTesting;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntLongMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntLongHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;

public class PlainTermStatisticsBase implements TermStatisticsBase {

  private final TIntLongMap termFrequencyMap = new TIntLongHashMap();
  private final TIntIntMap documentFrequencyMap = new TIntIntHashMap();
  private final TIntObjectMap<TIntLongMap> bigramFrequenciesMap = new TIntObjectHashMap<>();

  @VisibleForTesting
  PlainTermStatisticsBase(RandomAccess<Integer, TermStatistics> termBase) {
    termBase.forEach(ts -> {
      int termId = ts.getTermId();
      termFrequencyMap.put(termId, ts.getTermFrequency());
      documentFrequencyMap.put(termId, ts.getDocumentFrequency());
      TIntLongMap neighboursMap = new TIntLongHashMap();
      ts.getBigramFrequencyList().forEach(
          bf -> neighboursMap.put(bf.getTermId(), bf.getTermFrequency())
      );
      bigramFrequenciesMap.put(termId, neighboursMap);
    });
  }

  @VisibleForTesting
  PlainTermStatisticsBase(TIntLongMap termFrequencyMap,
      TIntIntMap documentFrequencyMap, TIntObjectMap<TIntLongMap> bigramFrequenciesMap) {
    this.termFrequencyMap.putAll(termFrequencyMap);
    this.documentFrequencyMap.putAll(documentFrequencyMap);
    bigramFrequenciesMap.forEachEntry(
        (id, map) -> {
          this.bigramFrequenciesMap.putIfAbsent(id, new TIntLongHashMap());
          this.bigramFrequenciesMap.get(id).putAll(map);
          return true;
        }
    );
  }

  public static TermStatisticsBase readFrom(Path from) {
    RandomAccess<Integer, TermStatistics> tsBaseRaw = new ProtoTermStatisticsIndex(from);
    return new PlainTermStatisticsBase(tsBaseRaw);
  }

  @Override
  public long termFrequency(Term term) {
    // FIXME method should not receive 'Term'
    if (!(term instanceof IndexTerm)) {
      throw new IllegalArgumentException(
          "Wrong term type: for terms not found in the index statistics are not determined");
    }
    // FIXME probably term id should be always long or int?
    return termFrequencyMap.get((int) ((IndexTerm) term).id());
  }

  @Override
  public int documentFrequency(Term term) {
    // FIXME method should not receive 'Term'
    if (!(term instanceof IndexTerm)) {
      throw new IllegalArgumentException(
          "Wrong term type: for terms not found in the index statistics are not determined");
    }
    // FIXME probably term id should be always long?
    return documentFrequencyMap.get((int) ((IndexTerm) term).id());
  }

  @Override
  public Stream<Pair<Term, Integer>> mostFrequentNeighbours(Term term, int neighCount) {
    // TODO: implement
    throw new UnsupportedOperationException();
  }

  @Override
  public void saveTo(Path path) {

  }
}
