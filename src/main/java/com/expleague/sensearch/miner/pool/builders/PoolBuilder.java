package com.expleague.sensearch.miner.pool.builders;

import com.expleague.ml.data.tools.FeatureSet;
import com.expleague.ml.meta.FeatureMeta;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.log4j.Logger;

public abstract class PoolBuilder<DS> {

  private static final Logger LOG = Logger.getLogger(PoolBuilder.class);

  private final ObjectMapper mapper = new ObjectMapper();

  public PoolBuilder() {
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
  }

    protected FeatureMeta[] metaData(FeatureSet features, FeatureSet targetFeatures) {
    FeatureMeta[] metas = new FeatureMeta[features.dim() + targetFeatures.dim()];
    for (int f = 0; f < features.dim(); f++) {
      metas[f] = features.meta(f);
    }
    for (int f = 0; f < targetFeatures.dim(); f++) {
      metas[f + features.dim()] = targetFeatures.meta(f);
    }
    return metas;
  }

    protected DS[] readData(Class<DS> cl, int iteration, Path dataPath) {
    DS[] empty = (DS[]) Array.newInstance(cl, 0);
    try {
      Path path = dataPath.resolve("DataIt" + iteration + ".json");
      return (DS[]) mapper.readValue(
          Files.newBufferedReader(path, StandardCharsets.UTF_8), empty.getClass());
    } catch (IOException e) {
      LOG.info("Cannot read file " + dataPath);
      return empty;
    }
  }

    protected void saveNewIterationData(Path savePath, DS[] result, int iteration) {
    try {
      Files.createDirectories(savePath);
      mapper.writeValue(savePath.resolve("DataIt" + iteration + ".json").toFile(), result);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
