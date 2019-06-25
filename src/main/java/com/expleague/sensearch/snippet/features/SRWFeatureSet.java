package com.expleague.sensearch.snippet.features;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.ml.data.tools.FeatureSet;
import com.expleague.ml.meta.FeatureMeta;
import com.expleague.ml.meta.FeatureMeta.ValueType;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.Page.SegmentType;
import com.expleague.sensearch.core.PartOfSpeech;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.query.Query;
import com.expleague.sensearch.snippet.docbasedsnippet.KeyWord;
import com.expleague.sensearch.snippet.passage.Passage;
import com.expleague.sensearch.snippet.passage.Passages;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class SRWFeatureSet extends FeatureSet.Stub<QPASItem> {

  private static final FeatureMeta SRW = FeatureMeta
      .create("sr-weight", "Statistical Relevance Weight", ValueType.VEC);

  private static final long NUMBER_OF_KEYWORDS = 6;

  private Query query;
  private Passage passage;
  private Page owner;
  private Index index;
  private List<KeyWord> keywords;

  public void withIndex(Index index) {
    this.index = index;
  }

  @Override
  public void accept(QPASItem item) {
    super.accept(item);
    this.passage = item.passageCache();
    this.query = item.queryCache();
    if (this.owner != this.passage.owner()) {
      this.owner = this.passage.owner();
      createKeywords();
    }
  }

  private void createKeywords() {
    List<Passage> passages = owner
        .sentences(SegmentType.SUB_BODY)
            .map(x -> new Passage(x, owner))
        .collect(Collectors.toList());

    Set<KeyWord> uniqueWords = passages
        .stream()
        .flatMap(passage -> passage.words().map(KeyWord::new))
        .collect(Collectors.toCollection(HashSet::new));

    Predicate<Passage> queryRelevant =
        passage -> query.terms().stream()
            .anyMatch(term -> Passages.containsWithLemma(passage, term));

    Predicate<Passage> notQueryRelevant =
        passage -> query.terms().stream()
            .noneMatch(term -> Passages.containsWithLemma(passage, term));

    List<Passage> passagesWithQueryWords =
        passages.stream().filter(queryRelevant).collect(Collectors.toList());

    List<Passage> passagesWithoutQueryWords =
        passages.stream().filter(notQueryRelevant).collect(Collectors.toList());

    this.keywords = uniqueWords
        .stream()
        .filter(x -> x.word().partOfSpeech() == PartOfSpeech.S)
        .peek(
            x -> {
              long r =
                  passagesWithQueryWords
                      .stream()
                      .filter(passage -> Passages.containsWithLemma(passage, x.word()))
                      .count();
              long R = passagesWithQueryWords.size();
              long s =
                  passagesWithoutQueryWords
                      .stream()
                      .filter(passage -> Passages.containsWithLemma(passage, x.word()))
                      .count();
              long S = passagesWithoutQueryWords.size();
              double w = Math.log((r + 0.5) * (S - s + 0.5) / ((R - r + 0.5) * (s + 0.5)));
              x.setRank(w);
            })
        .sorted(Comparator.comparingDouble(KeyWord::rank).reversed())
        .limit(NUMBER_OF_KEYWORDS)
        .collect(Collectors.toList());
  }

  @Override
  public Vec advance() {
    double sum = keywords
        .stream()
        .filter(keyWord -> Passages.containsWithLemma(passage, keyWord.word()))
        .mapToDouble(KeyWord::rank)
        .sum();

    set(SRW, sum);

    return super.advance();
  }
}
