package com.expleague.sensearch.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;
import org.junit.BeforeClass;

public class SensearchTestCase {

  private static final Logger LOG = Logger.getLogger(SensearchTestCase.class.getName());

  private static Path testDataRoot;
  private static Path testOutputRoot;

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
    testDataRoot = projectRoot.resolve("src").resolve("test").resolve("java").resolve("DATA");
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

    testOutputRoot = projectRoot.resolve("src").resolve("test").resolve("java").resolve("UNIVERSE");
    if (Files.notExists(testOutputRoot)) {
      LOG.info(String.format(
          "Will create test output directory by path: %s",
          testOutputRoot.toAbsolutePath().toString()
          )
      );
      try {
        Files.createDirectories(testDataRoot);
      } catch (IOException e) {
        throw new RuntimeException(
            String.format(
                "Failed to create test output directory by path: %s",
                testDataRoot.toAbsolutePath().toString()),
            e
        );
      }
    }
  }

  protected static Path testOutputRoot() {
    return testOutputRoot;
  }

  protected static Path testDataRoot() {
    return testDataRoot;
  }

}
