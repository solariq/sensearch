package com.expleague.sensearch.utils;

import com.expleague.commons.text.lemmer.MyStem;
import com.expleague.sensearch.ConfigImpl;
import com.expleague.sensearch.core.Lemmer;
import com.expleague.sensearch.donkey.plain.IdGenerator;
import com.expleague.sensearch.donkey.plain.PlainIndexBuilder;
import com.expleague.sensearch.index.Embedding;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.index.plain.EmbeddingImpl;
import com.expleague.sensearch.index.plain.FilterImpl;
import com.expleague.sensearch.index.plain.PlainIndex;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.io.FileUtils;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.Options;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class IndexBasedTestCase extends CrawlerBasedTestCase {

  private static final long CACHE_SIZE = 16 * (1 << 20);
  private static final Options DB_OPTIONS = new Options().cacheSize(CACHE_SIZE);

  protected static final String MINI_INDEX_ROOT = "MiniIndex";

  // TODO: prepare data for logs based lemmer!
  private static final String LOG_BASED_LEMMER_ROOT = "LemmerData";

  private static final Logger LOG = LoggerFactory.getLogger(IndexBasedTestCase.class);

  private static Index miniIndex;

  private static Embedding embedding;

  private static TestConfigImpl indexConfig;

  @BeforeClass
  public static void initIndex() throws IOException {
    Path indexRoot = testDataRoot().resolve(MINI_INDEX_ROOT);
    indexConfig = crawlerConfig().setTemporaryIndex(indexRoot);
    if (!Files.exists(indexRoot)) {
      LOG.info(
          String.format(
              "Index was not found by given path [%s]. Index will be rebuilt",
              indexRoot.toAbsolutePath().toString()));
      buildIndex();
    }

    Path embeddingPath = indexConfig.getTemporaryIndex().resolve(PlainIndexBuilder.EMBEDDING_ROOT);
    embedding =
        new EmbeddingImpl(
                indexConfig,
            JniDBFactory.factory.open(
                embeddingPath.resolve(PlainIndexBuilder.VECS_ROOT).toFile(), DB_OPTIONS)//,
            /*JniDBFactory.factory.open(
                embeddingPath.resolve(PlainIndexBuilder.LSH_ROOT).toFile(), DB_OPTIONS),*/
            /*embeddingPath*/);
    miniIndex =
        new PlainIndex(
            indexConfig, embedding, new FilterImpl(embedding, indexConfig().maxFilterItems()));
  }

  @AfterClass
  public static void removeIndex() throws Exception {
    miniIndex.close();
    FileUtils.deleteDirectory(testDataRoot().resolve(MINI_INDEX_ROOT).toFile());
  }

  private static void buildIndex() throws IOException {
    LOG.info("Rebuilding index...");
    MyStem myStem = myStemForTest("IndexBasedTestCase", "initIndex");
    new PlainIndexBuilder(crawler(), indexConfig, new Lemmer(myStem))
        .buildIndex();
  }

  protected static TestConfigImpl indexConfig() {
    return new TestConfigImpl(indexConfig);
  }

  protected static Index index() {
    return miniIndex;
  }

  protected static Embedding embedding() {
    return embedding;
  }

  protected ConfigImpl config() {
    return new TestConfigImpl(indexConfig);
  }
}
