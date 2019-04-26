package com.expleague.sensearch.snippet.features;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.ml.data.tools.FeatureSet;
import com.expleague.ml.meta.FeatureMeta.ValueType;
import com.expleague.ml.meta.TargetMeta;
import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.query.Query;
import com.expleague.sensearch.snippet.experiments.naturalquestions.Data;
import com.expleague.sensearch.snippet.passage.Passage;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TargetFeatureSet extends FeatureSet.Stub<QPASItem> {

  private final static TargetMeta TARGET_META = TargetMeta
      .create("natural-questions", "rank by natural-questions", ValueType.VEC);

  private final Index index;
  private Query query;
  private Passage passage;

  public TargetFeatureSet(Index index) {
    this.index = index;
  }

  @Override
  public void accept(QPASItem item) {
    super.accept(item);
    this.passage = item.passageCache();
    this.query = item.queryCache();
  }

  @Override
  public Vec advance() {
    Data[] datas = new Data[0];
    try {
      byte[] jsonData = Files.readAllBytes(
          Paths.get("./src/main/java/com/expleague/sensearch/snippet/experiments/data.json"));
      ObjectMapper objectMapper = new ObjectMapper();
      datas = objectMapper.readValue(jsonData, Data[].class);
    } catch (IOException ignored) {
    }

    Optional<Data> data = Arrays.stream(datas)
        .filter(d -> d.getQuery().equals(query.text()))
        .findFirst();
    if (data.isPresent()) {
      CharSequence shortAnswer = data.get().getShort_answer();
      List<Term> shortAnswerTerms = index.parse(shortAnswer).collect(Collectors.toList());

      List<CharSequence> longAnswer = index.sentences(data.get().getLong_answer())
          .collect(Collectors.toList());

      CharSequence sentence = passage.sentence();
      List<Term> sentenceTerms = passage.words().collect(Collectors.toList());

      if (Collections.indexOfSubList(sentenceTerms, shortAnswerTerms) != -1) {
        set(TARGET_META, 1.0);
      } else {
        if (longAnswer.stream().anyMatch(la -> {
          List<Term> longAnswerTerms = index.parse(la).collect(Collectors.toList());
          if (Collections.indexOfSubList(sentenceTerms, longAnswerTerms) != -1) {
            System.out.println("--");
            System.out.println(la);
            System.out.println(sentence);
            System.out.println("--");
          }
          return Collections.indexOfSubList(sentenceTerms, longAnswerTerms) != -1;
        })) {
          set(TARGET_META, 0.5);
        } else {
          set(TARGET_META, 0.0);
        }
      }
    } else {
      set(TARGET_META, 0.0);
    }
    return super.advance();
  }
}
