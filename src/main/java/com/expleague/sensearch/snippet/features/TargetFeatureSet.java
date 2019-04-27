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

  private double weight;

  public void withWeight(double weight) {
    this.weight = weight;
  }

  @Override
  public Vec advance() {
    set(TARGET_META, weight);
    return super.advance();
  }
}
