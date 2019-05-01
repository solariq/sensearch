package com.expleague.sensearch.web.suggest.pool;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import com.expleague.commons.math.vectors.Vec;
import com.expleague.ml.data.tools.FeatureSet;
import com.expleague.ml.meta.FeatureMeta.ValueType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.expleague.ml.meta.TargetMeta;

public class TargetFeatureSet extends FeatureSet.Stub<QSUGItem>{
  private final static TargetMeta TARGET_META = TargetMeta.create("weight",
      "target weight for formula", ValueType.VEC);
/*
  private final ObjectMapper mapper = new ObjectMapper();

  public final Map<String, List<String>>
  map = mapper.readValue(
      Paths.get("sugg_dataset/map").toFile(),
      new TypeReference<Map<String, List<String>>>() {});
*/
  public TargetFeatureSet() throws IOException {

  }

  private QSUGItem item;

  @Override
  public void accept(QSUGItem item) {
    this.item = item;
    super.accept(item);
  }
  
  @Override
  public Vec advance() {

    if (item.isPositive) {
      set(TARGET_META, 1.0);
    } else {
      set(TARGET_META, 0.0);
    }
    
    return super.advance();
  }
}
