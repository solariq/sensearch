package com.expleague.sensearch.snippet;

import com.expleague.commons.math.Trans;
import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.util.Pair;
import com.expleague.ml.meta.FeatureMeta;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.Page.SegmentType;
import com.expleague.sensearch.core.Annotations.SnippetModel;
import com.expleague.sensearch.core.PartOfSpeech;
import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.features.Features;
import com.expleague.sensearch.features.FeaturesImpl;
import com.expleague.sensearch.query.Query;
import com.expleague.sensearch.snippet.docbased_snippet.DocBasedSnippet;
import com.expleague.sensearch.snippet.docbased_snippet.KeyWord;
import com.expleague.sensearch.snippet.features.AccumulatorFeatureSet;
import com.expleague.sensearch.snippet.features.QPASItem;
import com.expleague.sensearch.snippet.passage.Passage;
import com.google.inject.Inject;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created by Maxim on 06.10.2018. Email: alvinmax@mail.ru
 */
public class SnippetsCreator {

  private final Index index;
  private final Trans model;
  private final FeatureMeta[] featuresInModel;

  @Inject
  public SnippetsCreator(Index index, @SnippetModel Pair<Function, FeatureMeta[]> snippetModel) {
    this.index = index;
    this.model = (Trans) snippetModel.getFirst();
    this.featuresInModel = snippetModel.getSecond();
  }

  public Snippet getSnippet(Page page, Query query) {
    CharSequence title = page.content(SegmentType.SECTION_TITLE);

    List<Passage> passages = page
        .sentences(SegmentType.SUB_BODY)
        .map(x -> new Passage(x, index.parse(x).collect(Collectors.toList()), page))
        .collect(Collectors.toList());

    for (int i = 0; i < passages.size(); i++) {
      passages.get(i).setId(i);
    }

    final AccumulatorFeatureSet features = new AccumulatorFeatureSet(index);
    final Map<Passage, Features> passagesFeatures = new HashMap<>();

    passages.forEach(passage -> {
      features.accept(new QPASItem(query, passage));
      Vec all = features.advance();
      passagesFeatures.put(
          passage, new FeaturesImpl(features, all)
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
    return model.trans(features).get(0);
  }

}
