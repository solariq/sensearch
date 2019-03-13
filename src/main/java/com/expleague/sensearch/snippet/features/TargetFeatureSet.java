package com.expleague.sensearch.snippet.features;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.seq.CharSeqTools;
import com.expleague.ml.data.tools.FeatureSet;
import com.expleague.ml.meta.FeatureMeta.ValueType;
import com.expleague.ml.meta.TargetMeta;
import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.query.Query;
import com.expleague.sensearch.snippet.experiments.Data;
import com.expleague.sensearch.snippet.passage.Passage;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
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
        .filter(d -> d.getTitle().equals(query.text()))
        .findFirst();
    if (data.isPresent()) {
      CharSequence shortAnswer = data.get().getShort_answer();
      List<CharSequence> longAnswer = index.parse(data.get().getLong_answer())
          .map(Term::text)
          .collect(Collectors.toList());
      CharSequence sentence = passage.sentence();

      if (CharSeqTools.indexOf(sentence, shortAnswer) != -1) {
        set(TARGET_META, 1.0);
      } else {
        if (longAnswer.stream().anyMatch(x -> x.equals(sentence))) {
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
