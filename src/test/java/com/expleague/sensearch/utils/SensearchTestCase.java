package com.expleague.sensearch.utils;

import com.expleague.commons.text.lemmer.MyStem;
import com.expleague.sensearch.Config;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.Options;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public abstract class SensearchTestCase {

  private static final String INDEX_DATA_ROOT = "ForIndex";
  private static final String MINI_WIKI_ZIP = "MiniWiki.zip";
  private static final String VECTORS_FILE = "Vectors.txt.gz";
  private static final String MY_STEM_LOGS_ROOT = "MyStemTestLogs";

  private static final Logger LOG = Logger.getLogger(SensearchTestCase.class.getName());

  private static Path testDataRoot;
  private static Path testOutputRoot;
  private static Path myStemLogsRoot;
  private static Config config;


  @BeforeClass
  public static void initWorkingPaths() {
    //
    String workingDir = System.getProperty("user.dir");
    if (!workingDir.endsWith("sensearch")) {
      throw new RuntimeException(
          String.format(
              "Working directory expected to be project root. Received path %s instead",
              workingDir
          )
      );
    }

    Path projectRoot = Paths.get(workingDir);
    testDataRoot = projectRoot.resolve("src").resolve("test").resolve("DATA");
    if (Files.notExists(testDataRoot) || !Files.isDirectory(testDataRoot)) {
      throw new RuntimeException(String.format(
          "Test data root was not found by path: %s",
          testDataRoot.toAbsolutePath().toString()
      )
      );
    } else {
      LOG.fine(String.format(
          "Test data root was found by path: %s",
          testDataRoot.toAbsolutePath().toString()
          )
      );
    }

    testOutputRoot = projectRoot.resolve("src").resolve("test").resolve("UNIVERSE");
    if (Files.notExists(testOutputRoot)) {
      LOG.info(String.format(
          "Will create test output directory by path: %s",
          testOutputRoot.toAbsolutePath().toString()
          )
      );
      try {
        Files.createDirectories(testOutputRoot);
      } catch (IOException e) {
        throw new RuntimeException(
            String.format(
                "Failed to create test output directory by path: %s",
                testOutputRoot.toAbsolutePath().toString()),
            e
        );
      }
    }

    // TODO: check whether path actually exists
    myStemLogsRoot = testDataRoot.resolve(MY_STEM_LOGS_ROOT);

    Path indexDataRoot = testDataRoot.resolve(INDEX_DATA_ROOT);
    config = new TestConfig(
        indexDataRoot.resolve(MINI_WIKI_ZIP),
        indexDataRoot.resolve(VECTORS_FILE)
    );

  }

  @AfterClass
  public static void cleanupUniverse() {
    LOG.fine("Cleaning up...");
    LOG.fine(String.format("Will delete tes output directory %s",
        testOutputRoot.toAbsolutePath().toString())
    );
    clearOutputRoot();
  }

  protected static Path testOutputRoot() {
    return testOutputRoot;
  }

  protected static Path testDataRoot() {
    return testDataRoot;
  }

  protected static Path myStemLogsPath() {
    return myStemLogsRoot;
  }

  protected static MyStem myStemForTest(String testClassName, String testName) {
    Path pathToSpecificStem = myStemLogsRoot.resolve(
        String.format("%s_%s", testClassName, testName)
    );
    return new LogBasedMyStem(pathToSpecificStem);
  }

  protected static void clearOutputRoot() {
    try {
      FileUtils.cleanDirectory(testOutputRoot.toFile());
    } catch (IOException e) {
      throw new RuntimeException(
          String.format("Failed to clea test output directory %s",
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

  protected Config config() {
    return config;
  }
}
