package com.expleague.sensearch.miner.impl;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.impl.vectors.ArrayVec;
import com.expleague.commons.math.vectors.impl.vectors.VecBuilder;
import com.expleague.ml.data.tools.FeatureSet;
import com.expleague.ml.meta.FeatureMeta;
import com.expleague.ml.meta.FeatureMeta.ValueType;
import com.expleague.ml.meta.TargetMeta;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.Page.SegmentType;
import com.expleague.sensearch.SenSeArch.ResultItem;
import com.expleague.sensearch.core.impl.ResultItemImpl;
import com.expleague.sensearch.query.Query;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class TargetFeatureSet extends FeatureSet.Stub<QURLItem> {

  public final static TargetMeta TARGET_META = TargetMeta
      .create("googleDCG", "1 / position at Google", ValueType.VEC);

  private Query query;
  private Page page;

  @Override
  public void accept(QURLItem item) {
    this.page = item.pageCache();
    final Query query = item.queryCache();
    if (query.equals(this.query))
      return;
    this.query = query;
  }


  @Override
  public Vec advance() {
    try (BufferedReader reader = Files.newBufferedReader(Paths.get("./wordstat").resolve("query_" + query.text()))) {
      ObjectMapper objectMapper = new ObjectMapper();
      double vec = 0.0;
      List<ResultItem> res = Arrays.asList(objectMapper.readValue(reader, ResultItemImpl[].class));
      ResultItem resultItem = res
          .stream()
          .filter(item -> item.title().equals(page.content(SegmentType.SUB_TITLE)))
          .findFirst().orElse(null);
      if (resultItem != null) {
        vec = res.indexOf(resultItem) + 1;
        vec = 1 / vec;
      }
      return new ArrayVec(vec);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return new ArrayVec(0.0);
  }
}
