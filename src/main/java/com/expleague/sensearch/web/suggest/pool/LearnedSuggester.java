package com.expleague.sensearch.web.suggest.pool;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.suggest.Lookup.LookupResult;
import org.apache.lucene.search.suggest.analyzing.AnalyzingSuggester;
import org.apache.lucene.store.FSDirectory;
import com.expleague.commons.math.Trans;
import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.VecTools;
import com.expleague.commons.math.vectors.impl.vectors.ArrayVec;
import com.expleague.ml.data.tools.DataTools;
import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.index.plain.IndexTerm;
import com.expleague.sensearch.index.plain.PlainIndex;
import com.expleague.sensearch.miner.pool.builders.FilterPoolBuilder;
import com.expleague.sensearch.web.suggest.Suggestor;
import com.google.common.primitives.Longs;

public class LearnedSuggester implements Suggestor {
  public final int RETURN_LIMIT = 10;

  private static final Logger LOG = Logger.getLogger(FilterPoolBuilder.class.getName());

  public final static String filePrefix = "prefix_sugg";

  protected final AnalyzingSuggester suggester;

  private final Trans model;

  private final AccumulatorFeatureSet featureSet = new AccumulatorFeatureSet();

  protected final Index index;

  public final static Path storePath = Paths.get("luceneSuggestPrefix/store");

  public static class StringDoublePair  implements Comparable<StringDoublePair> {

    public final String phrase;
    public final double coeff;

    public StringDoublePair(String phrase, double coeff) {
      this.phrase = phrase;
      this.coeff = coeff;
    }

    @Override
    public int compareTo(StringDoublePair o) {
      int res = Double.compare(coeff, o.coeff);
      if (res != 0)
        return res;

      return phrase.compareTo(o.phrase);
    }

  }

  public LearnedSuggester(Index index, Path suggestIndexRoot) throws IOException {
    this.index = index;

    suggester = new AnalyzingSuggester(
        //FSDirectory.open(suggestIndexRoot.resolve(storePath).getParent()),
        //filePrefix,
        new StandardAnalyzer());

    suggester.load(new FileInputStream(suggestIndexRoot.resolve(storePath).toFile()));

    if (Files.exists(suggestIndexRoot.resolve("suggest_ranker.model"))) {
      model = (Trans) DataTools.readModel(
          new InputStreamReader(
              Files.newInputStream(suggestIndexRoot.resolve("suggest_ranker.model")), StandardCharsets.UTF_8)).getFirst();

    } else {
      model = null;
      LOG.warn(
          "Rank model can not be found at path ["
              + SuggestRankingPoolBuilder.dataPath
              + "], using empty model instead");
    }

  }

  @Override
  public List<String> getSuggestions(String searchString) {
    List<String> res = null;
    try {
      res = getSuggestions(index.parse(searchString.toLowerCase())
          .collect(Collectors.toList()));

    } catch (Exception e) {
      e.printStackTrace();
    }

    //System.out.println("returning number suggestions: " + res.size());
    return res;
  }

  @Override
  public String getName() {
    return "Learned Suggester";
  }

  public static String termsToString(Term[] terms) {
    return Arrays.stream(terms).map(t -> t.text().toString())
        .collect(Collectors.joining(" "));
  }

  public List<QSUGItem> getUnsortedEndings(String query) throws IOException {
    return getUnsortedSuggestions(
        index.parse(query.toLowerCase())
        .collect(Collectors.toList())
        );
  }
  
  public List<QSUGItem> getUnsortedSuggestions(List<Term> terms) throws IOException {

    if (terms.isEmpty()) {
      return Collections.emptyList();
    }


    List<QSUGItem> res = new ArrayList<>();

    l:  for (int intersectLength = 3; intersectLength >= 1; intersectLength--) {

      if (terms.size() < intersectLength) {
        continue l;
      }

      List<Term> qc = terms.subList(0, terms.size() - intersectLength);
      String qcText = qc.stream().map(Term::text).collect(Collectors.joining(" "));
      Term[] qt = terms.subList(terms.size() - intersectLength, terms.size()).toArray(new Term[0]);

      Vec queryVec = index.vecByTerms(qc);
      Vec queryVecTfidf = ((PlainIndex) index).weightedVecByTerms(qc);

      List<LookupResult> endingPhrases = suggester.lookup(termsToString(qt), false, 1000000);

      //System.out.println("number of selected phrases: " + endingPhrases.size());
      for (LookupResult p : endingPhrases) {
        Term[] phrase = index.parse(p.key.toString().toLowerCase()).toArray(Term[]::new);
/*
        if (phrase.length > intersectLength) {
          continue;
        }
*/
        Vec phraseVec = index.vecByTerms(Arrays.asList(phrase));
        Vec phraseVecTfidf = ((PlainIndex) index).weightedVecByTerms(Arrays.asList(phrase));
        
        QSUGItem item = new QSUGItem(
            (qc.size() > 0 ? qcText + " " : "") + p.key.toString().toLowerCase(),
            intersectLength,
            phrase.length,
            Math.toIntExact(p.value),
            Double.longBitsToDouble(Longs.fromByteArray(p.payload.bytes)),
            VecTools.cosine(queryVec, phraseVec),
            VecTools.cosine(phraseVecTfidf, queryVecTfidf),
            Arrays.stream(phrase).mapToDouble(t -> ((PlainIndex )index).tfidf(t)).sum(),
            0.,
            VecTools.l2(phraseVec),
            false
            );

        res.add(item);

      }

    }

    return res;
  }

  List<String> getSuggestions(List<Term> terms) throws IOException {

    if (terms.isEmpty()) {
      return Collections.emptyList();
    }


    TreeSet<StringDoublePair> phraseProb = new TreeSet<>();

    List<QSUGItem> rawResults = getUnsortedSuggestions(terms);

    //System.out.println("number of selected phrases: " + endingPhrases.size());
    for (QSUGItem p : rawResults) {

      featureSet.accept(p);
      Vec v = featureSet.advance();
      phraseProb.add(
          new StringDoublePair(
              p.suggestion,
              model.trans(v).get(0)
              //Math.abs(p.cosine) < 1e-5 ? p.incomingLinksCount : p.cosine
              ));

      phraseProb.removeIf(it -> phraseProb.size() > RETURN_LIMIT);
    }

    return phraseProb.stream()
        .sorted(Comparator.reverseOrder())
        //.sorted()
        .map(p -> p.phrase)
        .collect(Collectors.toList());
  }
}
