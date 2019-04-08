package com.expleague.sensearch.miner.pool.builders;

import com.expleague.ml.data.tools.FeatureSet;
import com.expleague.ml.meta.FeatureMeta;
import com.expleague.sensearch.miner.pool.QueryAndResults;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class PoolBuilder {

  private final ObjectMapper mapper = new ObjectMapper();
  public abstract Path acceptDir();

  public PoolBuilder() {
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
  }

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


  QueryAndResults[] positiveData(int iteration) {
    try {
      return mapper.readValue(acceptDir().resolve("DataIt" + iteration + ".json").toFile(), QueryAndResults[].class);
    } catch (IOException e) {
      return new QueryAndResults[0];
    }
  }

  void saveNewData(QueryAndResults[] result, int iteration) {
    try {
      Files.createDirectories(acceptDir());
      mapper.writeValue(acceptDir().resolve("DataIt" + iteration + ".json").toFile(), result);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
