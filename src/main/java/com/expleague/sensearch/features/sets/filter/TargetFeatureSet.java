package com.expleague.sensearch.features.sets.filter;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.ml.data.tools.FeatureSet;
import com.expleague.ml.meta.FeatureMeta.ValueType;
import com.expleague.ml.meta.TargetMeta;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.core.Term;
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
import java.util.stream.Stream;

public class TargetFeatureSet extends FeatureSet.Stub<QURLItem> {

  private final static TargetMeta TARGET_META = TargetMeta
      .create("googleExists", "1 if Page exist at Google or 0 if not", ValueType.VEC);

  private Query query;
  private Page page;
  private Set<CharSequence> validTitles;

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
          .map(ch -> ch.toString().toLowerCase())
          .collect(Collectors.toSet());
    } catch (IOException ignored) {
    }

    this.query = query;
  }

  CharSequence toCS(Stream<Term> texts) {
    StringBuilder builder = new StringBuilder();
    texts.forEach(builder::append);
    return builder.toString();
  }

  @Override
  public Vec advance() {
//    set(TARGET_META, validTitles.contains(toCS(page.content(true, SegmentType.SECTION_TITLE))) ? 1 : 0);
    return super.advance();
  }
}
