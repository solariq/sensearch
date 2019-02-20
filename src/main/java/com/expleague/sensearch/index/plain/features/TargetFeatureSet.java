package com.expleague.sensearch.index.plain.features;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.ml.data.tools.FeatureSet;
import com.expleague.ml.meta.FeatureMeta.ValueType;
import com.expleague.ml.meta.TargetMeta;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.Page.SegmentType;
import com.expleague.sensearch.SenSeArch.ResultItem;
import com.expleague.sensearch.core.impl.ResultItemImpl;
import com.expleague.sensearch.miner.features.QURLItem;
import com.expleague.sensearch.query.Query;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

public class TargetFeatureSet extends FeatureSet.Stub<QURLItem> {

  private final static TargetMeta TARGET_META = TargetMeta
      .create("googleExists", "1 if Page exist at Google or 0 if not", ValueType.VEC);

  private Query query;
  private Page page;

  @Override
  public void accept(QURLItem item) {
    this.page = item.pageCache();
    assert page.isRoot();
    final Query query = item.queryCache();
    if (query.equals(this.query))
      return;
    this.query = query;
  }


  @Override
  public Vec advance() {
    double vec = 0.0;
    try (BufferedReader reader = Files.newBufferedReader(Paths.get("./wordstat").resolve("query_" + query.text()))) {
      ObjectMapper objectMapper = new ObjectMapper();
      ResultItem resultItem = Arrays.stream(objectMapper.readValue(reader, ResultItemImpl[].class))
          .filter(item -> item.title().equals(page.content(SegmentType.SECTION_TITLE)))
          .findFirst().orElse(null);
      if (resultItem != null) {
        vec = 1.0;
      }
    } catch (IOException ignored) {
    }
    set(TARGET_META, vec);
    return super.advance();
  }
}
