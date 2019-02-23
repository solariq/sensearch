package com.expleague.sensearch.snippet;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.impl.vectors.ArrayVec;
import com.expleague.commons.seq.CharSeqTools;
import com.expleague.commons.util.Pair;
import com.expleague.ml.meta.FeatureMeta;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.Page.SegmentType;
import com.expleague.sensearch.core.PartOfSpeech;
import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.miner.Features;
import com.expleague.sensearch.query.Query;
import com.expleague.sensearch.snippet.docbased_snippet.DocBasedSnippet;
import com.expleague.sensearch.snippet.docbased_snippet.KeyWord;
import com.expleague.sensearch.snippet.features.AccumulatorFeatureSet;
import com.expleague.sensearch.snippet.features.QPASItem;
import com.expleague.sensearch.snippet.passage.Passage;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by Maxim on 06.10.2018. Email: alvinmax@mail.ru
 */
public class SnippetsCreator {

  private static final long NUMBER_OF_KEYWORDS = 6;

  private final Index index;

  @Inject
  public SnippetsCreator(Index index) {
    this.index = index;
  }

  private boolean containsWithLemma(Passage passage, Term term) {
    return passage.words().anyMatch(x -> x.lemma() == term.lemma());
  }

  private boolean contains(Passage passage, Term term) {
    return passage.words().anyMatch(x -> x == term);
  }

  public Snippet getSnippet(Page page, Query query) {
    CharSequence title = page.content(SegmentType.SECTION_TITLE);

    List<Passage> passages =
        page
            .sentences(SegmentType.SUB_BODY)
            .map(x -> new Passage(page, x, index.parse(x).collect(Collectors.toList())))
            .collect(Collectors.toList());

    for (int i = 0; i < passages.size(); i++) {
      passages.get(i).setId(i);
    }

    Set<KeyWord> uniqueWords =
        passages
            .stream()
            .flatMap(passage -> passage.words().map(KeyWord::new))
            .collect(Collectors.toCollection(HashSet::new));

    Predicate<Passage> queryRelevant =
        passage -> query.terms().stream().anyMatch(term -> containsWithLemma(passage, term));

    Predicate<Passage> notQueryRelevant =
        passage -> query.terms().stream().noneMatch(term -> containsWithLemma(passage, term));

    List<Passage> passagesWithQueryWords =
        passages.stream().filter(queryRelevant).collect(Collectors.toList());

    List<Passage> passagesWithoutQueryWords =
        passages.stream().filter(notQueryRelevant).collect(Collectors.toList());

    List<KeyWord> keyWords =
        uniqueWords
            .stream()
            .filter(x -> x.word().partOfSpeech() == PartOfSpeech.S)
            .peek(
                x -> {
                  long r =
                      passagesWithQueryWords
                          .stream()
                          .filter(passage -> containsWithLemma(passage, x.word()))
                          .count();
                  long R = passagesWithQueryWords.size();
                  long s =
                      passagesWithoutQueryWords
                          .stream()
                          .filter(passage -> containsWithLemma(passage, x.word()))
                          .count();
                  long S = passagesWithoutQueryWords.size();
                  // System.out.println(x.word().text() + " " + r + " " + R + " " + s + " " + S);
                  double w = Math.log((r + 0.5) * (S - s + 0.5) / ((R - r + 0.5) * (s + 0.5)));
                  x.setRank(w);
                })
            .sorted(Comparator.comparingDouble(KeyWord::rank).reversed())
            .limit(NUMBER_OF_KEYWORDS)
            .collect(Collectors.toList());

    final AccumulatorFeatureSet features = new AccumulatorFeatureSet(index);
    features.withKeyWords(keyWords);

    final Map<Passage, Features> passagesFeatures = new HashMap<>();

    passages.forEach(passage -> {
      features.accept(new QPASItem(query, passage));
      Vec all = features.advance();

      passagesFeatures.put(
          passage,
          new Features() {
            @Override
            public Vec features() {
              return all;
            }

            @Override
            public Vec features(FeatureMeta... metas) {
              return new ArrayVec(
                  Stream.of(metas)
                      .mapToInt(features::index)
                      .mapToDouble(all::get)
                      .toArray());
            }

            @Override
            public FeatureMeta meta(int index) {
              return features.meta(index);
            }

            @Override
            public int dim() {
              return features.dim();
            }
          }
      );
    });

    Map<Passage, Double> mp = passagesFeatures
        .entrySet()
        .stream()
        .collect(
            Collectors.toMap(Entry::getKey, p -> rank(p.getValue().features())));

    List<Passage> bestPassages = mp
        .entrySet()
        .stream()
        .map(p -> Pair.create(p.getKey(), p.getValue()))
        .sorted(Comparator.<Pair<Passage, Double>>comparingDouble(Pair::getSecond).reversed())
        .map(Pair::getFirst)
        .limit(4)
        .collect(Collectors.toList());

    return new DocBasedSnippet(title, bestPassages, query);
  }

  private double rank(Vec features) {
    return features.get(3);
  }

}
