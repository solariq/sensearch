package com.expleague.sensearch.web.suggest;

import com.expleague.sensearch.Page;
import com.expleague.sensearch.core.Term;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.suggest.analyzing.AnalyzingInfixSuggester;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
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

  private final AnalyzingInfixSuggester luceneSuggestor;

  private static final Logger LOG = Logger.getLogger(SuggestInformationBuilder.class);

  public void build() throws IOException {
    useIndex();
    computeAvgOrderFreq();
    computeFreqNorm();
    computeTargetMaps();
    saveTargets();

    luceneSuggestor.commit();
    luceneSuggestor.close();
  }

  private void useIndex() throws IOException {

    int[] cnt = {0};

    index
    .allDocuments()
    .forEach(d -> {
      CharSequence title = d.content(Page.SegmentType.FULL_TITLE);
      accept(toTerms(title), d.incomingLinksCount(Page.LinkType.ALL_LINKS));

      cnt[0]++;
      if (cnt[0] % 10000 == 0) {
        LOG.info(cnt[0] + " documents processed");
      }
    });
  }

  private long[] toTermIds(CharSequence text) {
    return index.parse(text)
        .map(t -> ((IndexTerm) t).id())
        .mapToLong(i -> i)
        .toArray();
  }

  private Term[] toTerms(CharSequence text) {
    return index.parse(text)
        .toArray(Term[]::new);
  }

  private long[] termsToIds(Term[] terms) {
    long[] res = new long[terms.length];

    for (int i = 0; i < terms.length; i++) {
      res[i] = ((IndexTerm)terms[i]).id();
    }

    return res;
  }

  private String termsToString(Term[] terms) {
    return Arrays.stream(terms).map(t -> t.text().toString())
        .collect(Collectors.joining(" "));
  }

  private void accept(Term[] wordIds, int docIncomingLinks) {
//    computeUnigrams(wordIds);
//    computeMultigrams(wordIds, docIncomingLinks);
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
  public SuggestInformationBuilder(Index index, Path indexRoot, DB unigramCoeffDB, DB multigramFreqNormDB) throws IOException {
    Properties logProperties = new Properties();
    logProperties.load(Files.newInputStream(Paths.get("log4j.properties")));
    PropertyConfigurator.configure(logProperties);

    this.index = index;
    this.unigramCoeffDB = unigramCoeffDB;
    this.multigramFreqNormDB = multigramFreqNormDB;

    luceneSuggestor =  new AnalyzingInfixSuggester(
        FSDirectory.open(Files.createDirectory(indexRoot.resolve("luceneSuggest"))),
        new StandardAnalyzer());
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

  private List<Term[]> getNgrams(Term[] wordsIds, int order) {

    List<Term[]> result = new ArrayList<>();

    for (int i = 0; i < wordsIds.length - order + 1; i++) {
      result.add(Arrays.copyOfRange(wordsIds, i, i + order));
    }

    return result;
  }

  private void computeMultigrams(Term[] wordIds, int docIncomingLinks) {
    for (int i = 1; i <= maxNgramsOrder; i++) {
      for(Term[] l : getNgrams(wordIds, i)) {
        long[] lIds = termsToIds(l);
        try {
          luceneSuggestor.add(new BytesRef(termsToString(l)), null, docIncomingLinks, null);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        multigramFreq.adjustOrPutValue(lIds, docIncomingLinks + 1, docIncomingLinks + 1);
      }
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
