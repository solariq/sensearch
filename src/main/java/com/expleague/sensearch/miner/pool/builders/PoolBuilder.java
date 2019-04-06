package com.expleague.sensearch.miner.pool.builders;

import com.expleague.ml.data.tools.FeatureSet;
import com.expleague.ml.meta.FeatureMeta;
import com.expleague.sensearch.miner.pool.QueryAndResults;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class PoolBuilder {

  private final ObjectMapper mapper = new ObjectMapper();
  private Path savedDocs;

  public abstract Path acceptDir();

  FeatureMeta[] metaData(FeatureSet features, FeatureSet targetFeatures) {
    FeatureMeta[] metas = new FeatureMeta[features.dim() + targetFeatures.dim()];
    for (int f = 0; f < features.dim(); f++) {
      metas[f] = features.meta(f);
    }
    for (int f = 0; f < targetFeatures.dim(); f++) {
      metas[f + features.dim()] = targetFeatures.meta(f);
    }
    return metas;
  }

  QueryAndResults[] savedData() {
    try {
      return mapper.readValue(acceptDir().resolve("savedData.json").toFile(), QueryAndResults[].class);
    } catch (IOException e) {
      return new QueryAndResults[0];
    }
  }

  QueryAndResults[] positiveData() {
    try {
      return mapper.readValue(acceptDir().resolve("positiveData.json").toFile(), QueryAndResults[].class);
    } catch (IOException e) {
      return new QueryAndResults[0];
    }
  }

  void saveNewData(QueryAndResults[] result) {
    try {
      Files.createDirectories(acceptDir());
      mapper.writeValue(acceptDir().resolve("savedData.json").toFile(), result);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
