package com.expleague.sensearch.utils;

import com.expleague.commons.text.lemmer.MyStem;
import com.expleague.sensearch.ConfigImpl;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.Options;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public abstract class SensearchTestCase {

  protected static final String INDEX_DATA_ROOT = "ForIndex";

  protected static final String MINI_WIKI_ZIP = "MiniWiki.zip";
  protected static final String VECTORS_FILE = "vectors.decomp";

  protected static final String MY_STEM_LOGS_ROOT = "MyStemTestLogs";

  private static final Logger LOG = Logger.getLogger(SensearchTestCase.class.getName());

  private static Path testDataRoot;
  private static Path testOutputRoot;
  private static Path myStemLogsRoot;
  private static Path miniWikiRoot;
  private static Path gloveVectorsFile;
  private static TestConfigImpl config;

  @BeforeClass
  public static void initWorkingPaths() throws IOException {
    Properties logProperties = new Properties();
    logProperties.load(Files.newInputStream(Paths.get("log4j.properties")));
    PropertyConfigurator.configure(logProperties);

    //
    LOG.info("Initializing test environment...");

    String workingDir = System.getProperty("user.dir");

    if (!workingDir.endsWith("sensearch")) {
      throw new RuntimeException(
          String.format(
              "Working directory expected to be project root. Received path %s instead",
              workingDir));
    }

    Path projectRoot = Paths.get(workingDir);
    testDataRoot = projectRoot.resolve("src").resolve("test").resolve("DATA");
    if (Files.notExists(testDataRoot) || !Files.isDirectory(testDataRoot)) {
      throw new RuntimeException(
          String.format(
              "Test data root was not found by path: %s",
              testDataRoot.toAbsolutePath().toString()));
    } else {
      LOG.debug(
          String.format(
              "Test data root was found by path: %s", testDataRoot.toAbsolutePath().toString()));
    }

    testOutputRoot = projectRoot.resolve("src").resolve("test").resolve("UNIVERSE");
    if (Files.notExists(testOutputRoot)) {
      LOG.info(
          String.format(
              "Will create test output directory by path: %s",
              testOutputRoot.toAbsolutePath().toString()));
      try {
        Files.createDirectories(testOutputRoot);
      } catch (IOException e) {
        throw new RuntimeException(
            String.format(
                "Failed to create test output directory by path: %s",
                testOutputRoot.toAbsolutePath().toString()),
            e);
      }
    }

    myStemLogsRoot = testDataRoot.resolve(MY_STEM_LOGS_ROOT);
    if (Files.notExists(myStemLogsRoot)) {
      LOG.warn(String.format("My stem logs folder was not found by the path [%s]",
          myStemLogsRoot.toAbsolutePath().toString())
      );

    }

    Path indexDataRoot = testDataRoot.resolve(INDEX_DATA_ROOT);
    if (Files.exists(indexDataRoot)) {
      gloveVectorsFile = indexDataRoot.resolve(VECTORS_FILE);
      miniWikiRoot = indexDataRoot.resolve(MINI_WIKI_ZIP);
    } else {
      LOG.warn(String.format("Data required for index was not found by the path [%s]",
          indexDataRoot.toAbsolutePath().toString())
      );
    }

    config = new TestConfigImpl()
        .setEmbeddingVectors(gloveVectorsFile)
        .setPathToZIP(miniWikiRoot);

    LOG.info("Done initializing test environment");
  }

  @AfterClass
  public static void cleanupUniverse() {
    LOG.debug("Cleaning up...");
    LOG.debug(
        String.format(
            "Will delete tes output directory %s", testOutputRoot.toAbsolutePath().toString()));
    clearOutputRoot();
  }

  protected static Path testOutputRoot() {
    return testOutputRoot;
  }

  protected static Path testDataRoot() {
    return testDataRoot;
  }

  protected static Path miniWikiRoot() {
    return miniWikiRoot;
  }

  protected static Path gloveVectorsFile() {
    return gloveVectorsFile;
  }

  protected static Path myStemLogsPath() {
    return myStemLogsRoot;
  }

  protected static MyStem myStemForTest(String testClassName, String testName) {
    Path pathToSpecificStem =
        myStemLogsRoot.resolve(String.format("%s_%s", testClassName, testName));
    return new LogBasedMyStem(pathToSpecificStem);
  }

  protected static void clearOutputRoot() {
    try {
      FileUtils.cleanDirectory(testOutputRoot.toFile());
    } catch (IOException e) {
      LOG.fatal(
          String.format(
              "Failed to clear test output directory %s",
              testOutputRoot.toAbsolutePath().toString()),
          e);
    }
  }

  protected static Options dbOpenOptions() {
    return new Options()
        .cacheSize(1 << 10)
        .createIfMissing(false)
        .errorIfExists(false)
        .compressionType(CompressionType.NONE);
  }

  protected static Options dbCreateOptions() {
    return new Options()
        .cacheSize(1 << 10)
        .createIfMissing(true)
        .errorIfExists(true)
        .compressionType(CompressionType.NONE);
  }

  protected static TestConfigImpl sensearchConfig() {
    return new TestConfigImpl(config);
  }

  protected ConfigImpl config() {
    return new TestConfigImpl(config);
  }
}
