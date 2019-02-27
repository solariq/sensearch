package com.expleague.sensearch.features.sets.filter;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.seq.CharSeq;
import com.expleague.ml.data.tools.FeatureSet;
import com.expleague.ml.meta.FeatureMeta.ValueType;
import com.expleague.ml.meta.TargetMeta;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.Page.SegmentType;
import com.expleague.sensearch.core.impl.ResultItemImpl;
import com.expleague.sensearch.features.QURLItem;
import com.expleague.sensearch.query.Query;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class TargetFeatureSet extends FeatureSet.Stub<QURLItem> {

  private final static TargetMeta TARGET_META = TargetMeta
      .create("googleExists", "1 if Page exist at Google or 0 if not", ValueType.VEC);

  private Query query;
  private Page page;
  private Set<CharSeq> validTitles;

  @Override
  public void accept(QURLItem item) {
    this.page = item.pageCache();
    assert page.isRoot();
    final Query query = item.queryCache();
    if (query.equals(this.query))
      return;

    try (BufferedReader reader = Files.newBufferedReader(Paths.get("./wordstat").resolve("query_" + query.text()))) {
      ObjectMapper objectMapper = new ObjectMapper();
      validTitles = Arrays.stream(objectMapper.readValue(reader, ResultItemImpl[].class))
          .map(ResultItemImpl::title)
          .map(CharSeq::create)
          .collect(Collectors.toSet());
    } catch (IOException ignored) {
    }

    this.query = query;
  }


  @Override
  public Vec advance() {
    set(TARGET_META, validTitles.contains(CharSeq.create(page.content(SegmentType.SECTION_TITLE))) ? 1 : 0);
    return super.advance();
  }
}
